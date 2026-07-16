package com.example.jjwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

/**
 * JJWT 高级演示：非对称加密（RS256）、刷新令牌（Refresh Token）、
 * 安全工具方法、多种签名算法对比
 *
 * 核心概念：
 * - HS256：对称加密，签名和验证使用同一密钥，适合单体应用
 * - RS256：非对称加密，私钥签名、公钥验证，适合微服务/分布式系统
 * - Refresh Token：长期有效的 Token，用于换取新的 Access Token
 *
 * 安全最佳实践：
 * - Access Token 有效期应尽量短（15-30分钟）
 * - Refresh Token 可稍长（7-30天），但需安全存储
 * - 敏感信息（密码等）不要放入 JWT Payload（Base64URL 编码可解码）
 * - 生产环境密钥应从配置中心或环境变量读取，不能硬编码
 */
public class JjwtAdvancedDemo {

    // 生成密钥的工具方法
    private static final SecretKey HS256_KEY = Jwts.SIG.HS256.key().build();  // 随机生成 HS256 密钥

    // 演示用密钥字符串（生产环境应从外部配置读取）
    private static final String SECRET_STRING = "my-production-secret-key-at-least-256-bits-long!!";
    private static final SecretKey STATIC_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(SECRET_STRING.getBytes())));

    public static void main(String[] args) throws Exception {
        demoRS256Asymmetric();
        demoRefreshTokenFlow();
        demoSecurityUtils();
        demoCommonPitfalls();
    }

    /**
     * 1. RS256 非对称加密演示
     *
     * 适用场景：微服务架构中，认证服务用私钥签发 Token，
     *          其他服务用公钥验证，无需共享密钥。
     */
    private static void demoRS256Asymmetric() throws Exception {
        System.out.println("=== 1. RS256 非对称加密 ===");

        // 生成 RSA 密钥对（2048位）
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // 用私钥签发 Token
        String token = Jwts.builder()
                .subject("user-rsa")
                .claim("auth_method", "rs256")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1800_000)) // 30分钟
                .signWith(privateKey, Jwts.SIG.RS256)      // RS256 签名
                .compact();

        System.out.println("RS256 Token: " + token.substring(0, 80) + "...");

        // 用公钥验证 Token
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(publicKey)                      // 使用公钥验证
                .build()
                .parseSignedClaims(token);

        System.out.println("验证成功！Subject: " + jws.getPayload().getSubject());
        System.out.println("签名算法: " + jws.getHeader().getAlgorithm());
        System.out.println("  - 私钥签名，公钥验证：适合微服务架构");
        System.out.println("  - 认证中心持有私钥签发 Token");
        System.out.println("  - 其他服务只需公钥即可验证");
        System.out.println();
    }

    /**
     * 2. Access Token + Refresh Token 双令牌模式
     *
     * 流程：
     * 1. 用户登录 → 返回 Access Token（短期） + Refresh Token（长期）
     * 2. Access Token 过期 → 用 Refresh Token 换取新的 Access Token
     * 3. Refresh Token 过期 → 需要重新登录
     */
    private static void demoRefreshTokenFlow() {
        System.out.println("=== 2. Access Token + Refresh Token 双令牌模式 ===");

        // 模拟登录：生成 Access Token 和 Refresh Token
        String userId = "user-refresh-demo";
        String accessToken = createAccessToken(userId);
        String refreshToken = createRefreshToken(userId);

        System.out.println("【用户登录成功】");
        System.out.println("  Access Token  (有效期 5 秒): " + accessToken.substring(0, 50) + "...");
        System.out.println("  Refresh Token (有效期 30 秒): " + refreshToken.substring(0, 50) + "...");

        // 验证 Access Token（此时有效）
        System.out.println("\n【第一次验证 Access Token】");
        boolean valid = verifyToken(accessToken);
        System.out.println("  结果: " + (valid ? "有效" : "无效"));

        // 等待 Access Token 过期
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 再次验证 Access Token（已过期）
        System.out.println("\n【Access Token 过期后验证】");
        valid = verifyToken(accessToken);
        System.out.println("  结果: " + (valid ? "有效" : "无效（已过期）"));

        // 使用 Refresh Token 刷新
        System.out.println("\n【使用 Refresh Token 刷新】");
        String newAccessToken = refreshAccessToken(refreshToken);
        if (newAccessToken != null) {
            System.out.println("  刷新成功！");
            System.out.println("  新的 Access Token: " + newAccessToken.substring(0, 50) + "...");
            valid = verifyToken(newAccessToken);
            System.out.println("  新 Token 验证结果: " + (valid ? "有效" : "无效"));
        } else {
            System.out.println("  刷新失败，需要重新登录");
        }
        System.out.println();
    }

    /**
     * 创建短期 Access Token（5秒有效）
     */
    private static String createAccessToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 5000))  // 5秒
                .signWith(HS256_KEY)
                .compact();
    }

    /**
     * 创建长期 Refresh Token（30秒有效）
     */
    private static String createRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())            // jti 用于防止重放攻击
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 30000)) // 30秒
                .signWith(HS256_KEY)
                .compact();
    }

    /**
     * 验证 Token
     */
    private static boolean verifyToken(String token) {
        try {
            Jwts.parser().verifyWith(HS256_KEY).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 用 Refresh Token 换取新的 Access Token
     */
    private static String refreshAccessToken(String refreshToken) {
        try {
            // 先验证 Refresh Token 的有效性
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(HS256_KEY)
                    .build()
                    .parseSignedClaims(refreshToken);

            Claims claims = jws.getPayload();

            // 验证是否是 refresh 类型的 Token
            if (!"refresh".equals(claims.get("type", String.class))) {
                System.out.println("  Token 类型不正确，拒绝刷新");
                return null;
            }

            // 生成新的 Access Token
            return createAccessToken(claims.getSubject());

        } catch (ExpiredJwtException e) {
            System.out.println("  Refresh Token 已过期: " + e.getMessage());
            return null;
        } catch (JwtException e) {
            System.out.println("  Refresh Token 无效: " + e.getMessage());
            return null;
        }
    }

    /**
     * 3. 安全工具方法集合
     */
    private static void demoSecurityUtils() {
        System.out.println("=== 3. 安全工具方法 ===");

        // 生成安全的随机密钥
        SecretKey secureKey = Jwts.SIG.HS256.key().build();
        String base64Key = java.util.Base64.getEncoder().encodeToString(secureKey.getEncoded());
        System.out.println("随机生成 HS256 密钥（Base64）: " + base64Key);

        // 从 Base64 编码恢复密钥
        SecretKey restoredKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Key));
        System.out.println("恢复密钥算法: " + restoredKey.getAlgorithm());

        // 验证密钥长度要求
        System.out.println("\n密钥长度要求：");
        System.out.println("  HS256 最低: 256 bits (32 bytes)");
        System.out.println("  HS384 最低: 384 bits (48 bytes)");
        System.out.println("  HS512 最低: 512 bits (64 bytes)");

        // Token ID (jti) 用于防重放攻击
        String tokenWithJti = Jwts.builder()
                .subject("secureUser")
                .id(UUID.randomUUID().toString())            // jti 唯一标识
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 300_000))
                .signWith(HS256_KEY)
                .compact();

        System.out.println("\n带 jti 的 Token（前50字符）: " + tokenWithJti.substring(0, 50) + "...");
        System.out.println("jti 用途：在服务端记录已使用的 Token ID，防止重放攻击");
        System.out.println();
    }

    /**
     * 4. 常见坑与最佳实践
     */
    private static void demoCommonPitfalls() {
        System.out.println("=== 4. 常见坑与最佳实践 ===");

        // 坑1：密钥过短
        System.out.println("【坑1】密钥过短");
        String shortSecret = "short";
        try {
            Keys.hmacShaKeyFor(shortSecret.getBytes());
            System.out.println("  （未触发异常，密钥够长）");
        } catch (Exception e) {
            System.out.println("  错误: " + e.getMessage());
        }

        // 坑2：签名被篡改
        System.out.println("\n【坑2】Token 被篡改");
        String validToken = Jwts.builder()
                .subject("victim")
                .signWith(HS256_KEY)
                .compact();

        String tamperedToken = validToken + "x";  // 尾部追加字符
        try {
            Jwts.parser().verifyWith(HS256_KEY).build().parseSignedClaims(tamperedToken);
            System.out.println("  验证通过（不应该发生）");
        } catch (SignatureException e) {
            System.out.println("  签名验证失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  验证失败: " + e.getClass().getSimpleName());
        }

        // 坑3：Payload 不是加密的
        System.out.println("\n【坑3】Payload 可被解码（不是加密的！）");
        String payloadToken = Jwts.builder()
                .subject("secretUser")
                .claim("password", "plainText123")           // 危险！密码不应放 JWT
                .signWith(HS256_KEY)
                .compact();

        String[] parts = payloadToken.split("\\.");
        String decodedPayload = new String(
                java.util.Base64.getUrlDecoder().decode(parts[1]));
        System.out.println("  解码后的 Payload: " + decodedPayload);
        System.out.println("  ⚠️ JWT 只签名不加密，敏感信息不要放入 Payload！");

        // 最佳实践总结
        System.out.println("\n【安全最佳实践总结】");
        System.out.println("  1. 使用 HTTPS 传输 JWT，防止中间人攻击");
        System.out.println("  2. Access Token 有效期 15-30 分钟，Refresh Token 7-30 天");
        System.out.println("  3. 敏感数据不放入 JWT Payload");
        System.out.println("  4. 密钥从环境变量/配置中心读取，不硬编码");
        System.out.println("  5. 服务端维护 Token 黑名单（登出时失效）");
        System.out.println("  6. 使用 jti + 服务端记录防止重放攻击");
        System.out.println("  7. 微服务场景优先使用 RS256 非对称加密");
        System.out.println();
    }
}
