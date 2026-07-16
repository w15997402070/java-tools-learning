package com.example.jjwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JJWT 基础演示：Token 的生成与解析
 *
 * 核心概念：
 * - JWT 由三部分组成：Header.Payload.Signature（Base64URL 编码，用 . 分隔）
 * - Header：算法类型（HS256/RS256等）
 * - Payload（Claims）：存放业务数据（sub/iat/exp 等标准字段 + 自定义字段）
 * - Signature：对 Header + Payload 的签名，防止篡改
 *
 * 注意：JJWT 0.12.x 起 API 大改，使用 Builder 模式，不兼容 0.11.x。
 */
public class JjwtBasicDemo {

    // HS256 算法密钥，至少 256 bits（32 bytes）
    private static final String SECRET = "my-very-long-secret-key-for-jjwt-demo-12345678";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static void main(String[] args) {
        demoCreateToken();
        demoParseToken();
        demoTokenWithClaims();
        demoExpiredToken();
    }

    /**
     * 1. 创建最简单的 JWT Token
     */
    private static void demoCreateToken() {
        System.out.println("=== 1. 创建 JWT Token ===");

        String token = Jwts.builder()
                .subject("user123")                          // sub: 主题（通常是用户ID）
                .issuedAt(new Date())                        // iat: 签发时间
                .expiration(new Date(System.currentTimeMillis() + 3600_000)) // exp: 1小时后过期
                .signWith(KEY)                               // 使用 HS256 签名
                .compact();                                  // 生成 Token 字符串

        System.out.println("Token: " + token);
        // Token 格式: xxx.yyy.zzz
        String[] parts = token.split("\\.");
        System.out.println("  - 由 " + parts.length + " 部分组成");
        System.out.println("  - Header(Base64URL): " + parts[0].substring(0, 20) + "...");
        System.out.println("  - Payload(Base64URL): " + parts[1].substring(0, 20) + "...");
        System.out.println("  - Signature: " + parts[2].substring(0, 20) + "...");
        System.out.println();
    }

    /**
     * 2. 解析 Token 并读取 Claims
     */
    private static void demoParseToken() {
        System.out.println("=== 2. 解析 Token ===");

        // 先生成一个 Token
        String token = Jwts.builder()
                .subject("user123")
                .claim("role", "admin")                      // 自定义字段
                .claim("email", "admin@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(KEY)
                .compact();

        // 解析 Token（同时验证签名和过期时间）
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(KEY)                             // 指定验证密钥
                .build()
                .parseSignedClaims(token);                   // 解析并验证

        Claims claims = jws.getPayload();

        System.out.println("Subject (sub): " + claims.getSubject());
        System.out.println("签发时间 (iat): " + claims.getIssuedAt());
        System.out.println("过期时间 (exp): " + claims.getExpiration());
        System.out.println("自定义字段 role: " + claims.get("role", String.class));
        System.out.println("自定义字段 email: " + claims.get("email", String.class));
        System.out.println("Token ID (jti): " + claims.getId());
        System.out.println();
    }

    /**
     * 3. 带多种自定义 Claims 的 Token
     */
    private static void demoTokenWithClaims() {
        System.out.println("=== 3. 带多种自定义 Claims ===");

        // 方式一：逐个添加
        String token1 = Jwts.builder()
                .subject("user456")
                .claim("name", "张三")
                .claim("dept", "研发部")
                .claim("level", 5)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 7200_000)) // 2小时
                .signWith(KEY)
                .compact();

        // 方式二：批量添加（通过 Map）
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("name", "李四");
        extraClaims.put("dept", "产品部");
        extraClaims.put("level", 3);
        extraClaims.put("permissions", new String[]{"read", "write"});

        String token2 = Jwts.builder()
                .subject("user789")
                .claims(extraClaims)                         // 批量设置
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 7200_000))
                .signWith(KEY)
                .compact();

        // 解析验证
        Jws<Claims> jws = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token2);
        Claims claims = jws.getPayload();

        System.out.println("方式一 Token: " + token1.substring(0, 50) + "...");
        System.out.println("方式二 Token Subject: " + claims.getSubject());
        System.out.println("  name: " + claims.get("name", String.class));
        System.out.println("  dept: " + claims.get("dept", String.class));
        System.out.println("  level: " + claims.get("level", Integer.class));
        System.out.println();
    }

    /**
     * 4. 过期 Token 的处理
     *
     * 注意事项：
     * - JJWT 0.12.x 解析过期 Token 会抛出 ExpiredJwtException
     * - 业务中需要捕获此异常并返回 401 状态码
     * - 可以通过 allowedClockSkewSeconds 设置时钟偏差容忍度
     */
    private static void demoExpiredToken() {
        System.out.println("=== 4. 过期 Token 处理 ===");

        // 创建一个仅 1 秒有效的 Token
        String expiredToken = Jwts.builder()
                .subject("tempUser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000)) // 1秒后过期
                .signWith(KEY)
                .compact();

        // 等待 Token 过期
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(expiredToken);
            System.out.println("Token 仍有效（这不应该发生）");
        } catch (Exception e) {
            System.out.println("Token 已过期，异常类型: " + e.getClass().getSimpleName());
            System.out.println("  消息: " + e.getMessage());
            System.out.println("  生产环境中应返回 HTTP 401 Unauthorized");
        }
        System.out.println();
    }
}
