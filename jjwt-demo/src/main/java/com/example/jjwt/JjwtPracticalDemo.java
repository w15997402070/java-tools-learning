package com.example.jjwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JJWT 实战演示：模拟用户认证系统
 *
 * 场景：实现一个简单的用户登录/认证/鉴权流程
 * 包含：
 * - 用户登录返回 Token
 * - 请求拦截器验证 Token
 * - Token 刷新
 * - 角色权限校验
 * - Spring Boot 集成指南（注释形式）
 *
 * 注意：本 Demo 为了简化，使用内存模拟，生产环境应集成数据库和 Redis。
 */
public class JjwtPracticalDemo {

    // ============ 配置 ============

    private static final String SECRET = "prod-jwt-secret-key-for-jjwt-practical-demo-2024!!";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // Access Token 有效期：15分钟
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000;
    // Refresh Token 有效期：7天
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000;

    // 模拟 Token 黑名单（实际应存 Redis）
    private static final Set<String> TOKEN_BLACKLIST = new HashSet<>();

    // 模拟用户数据库
    private static final Map<String, UserInfo> USER_DB = new HashMap<>();

    static {
        USER_DB.put("admin", new UserInfo("admin", "admin123", Arrays.asList("ROLE_ADMIN", "ROLE_USER")));
        USER_DB.put("user1", new UserInfo("user1", "pass123", Collections.singletonList("ROLE_USER")));
        USER_DB.put("guest", new UserInfo("guest", "guest", Collections.singletonList("ROLE_GUEST")));
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   JJWT 实战：用户认证系统演示        ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // 场景1：用户登录
        demoLoginFlow();

        // 场景2：Token 拦截验证
        demoTokenValidation();

        // 场景3：角色权限校验
        demoRoleCheck();

        // 场景4：登出（Token 黑名单）
        demoLogout();

        // 场景5：Spring Boot 集成指南
        demoSpringBootIntegration();
    }

    // ============ 场景1：用户登录 ============

    /**
     * 用户登录流程：
     * 1. 验证用户名密码
     * 2. 生成 Access Token + Refresh Token
     * 3. 返回给客户端
     */
    private static void demoLoginFlow() {
        System.out.println("=== 场景1：用户登录 ===");

        // 登录成功
        LoginResult result = login("admin", "admin123");
        if (result != null) {
            System.out.println("【登录成功】用户: admin");
            System.out.println("  Access Token:  " + result.accessToken.substring(0, 50) + "...");
            System.out.println("  Refresh Token: " + result.refreshToken.substring(0, 50) + "...");
            System.out.println("  Access Token 有效期: 15分钟");
            System.out.println("  Refresh Token 有效期: 7天");

            // 解析 Access Token 查看内容
            Claims claims = parseTokenClaims(result.accessToken);
            System.out.println("  Token 中的用户信息: " + claims.getSubject());
            System.out.println("  Token 中的角色: " + claims.get("roles"));
        }

        // 登录失败
        LoginResult failResult = login("admin", "wrong_password");
        System.out.println("\n【登录失败】admin / wrong_password");
        System.out.println("  结果: " + (failResult == null ? "用户名或密码错误" : "登录成功"));
        System.out.println();
    }

    /**
     * 登录方法
     */
    private static LoginResult login(String username, String password) {
        UserInfo user = USER_DB.get(username);
        if (user == null || !user.password.equals(password)) {
            return null;
        }

        Date now = new Date();
        String accessToken = Jwts.builder()
                .subject(username)
                .claim("roles", user.roles)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION))
                .signWith(KEY)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION))
                .signWith(KEY)
                .compact();

        return new LoginResult(accessToken, refreshToken);
    }

    // ============ 场景2：Token 拦截验证 ============

    /**
     * 模拟 API 网关/拦截器的 Token 验证流程
     */
    private static void demoTokenValidation() {
        System.out.println("=== 场景2：Token 拦截验证 ===");

        // 模拟生成有效 Token
        String validToken = Jwts.builder()
                .subject("user1")
                .claim("roles", Collections.singletonList("ROLE_USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(KEY)
                .compact();

        // 模拟生成过期 Token
        String expiredToken = Jwts.builder()
                .subject("temp")
                .issuedAt(new Date(System.currentTimeMillis() - 3600_000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(KEY)
                .compact();

        // 模拟拦截器验证多个请求
        String[] requests = {
                "GET /api/users/me  [有效Token]",
                "GET /api/orders    [过期Token]",
                "POST /api/login    [无Token，登录接口放行]"
        };

        for (int i = 0; i < requests.length; i++) {
            String request = requests[i];
            System.out.println("\n请求 " + (i + 1) + ": " + request);

            String token = null;
            if (i == 0) token = validToken;
            else if (i == 1) token = expiredToken;

            AuthResult authResult = authenticate(token);
            System.out.println("  认证结果: " + authResult.status);
            System.out.println("  用户: " + authResult.username);
            if (authResult.errorMessage != null) {
                System.out.println("  错误信息: " + authResult.errorMessage);
            }
        }
        System.out.println();
    }

    /**
     * 认证方法（模拟拦截器逻辑）
     */
    private static AuthResult authenticate(String token) {
        // 无 Token（如登录接口）
        if (token == null) {
            return new AuthResult("PASS（匿名访问）", "anonymous", null);
        }

        // 检查黑名单
        if (TOKEN_BLACKLIST.contains(token)) {
            return new AuthResult("DENIED", null, "Token 已被吊销");
        }

        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token);

            // 验证 Token 类型
            String type = jws.getPayload().get("type", String.class);
            if (!"access".equals(type)) {
                return new AuthResult("DENIED", null, "Token 类型不正确，需要 Access Token");
            }

            return new AuthResult("AUTHORIZED", jws.getPayload().getSubject(), null);

        } catch (ExpiredJwtException e) {
            return new AuthResult("DENIED", null, "Token 已过期，请刷新");
        } catch (JwtException e) {
            return new AuthResult("DENIED", null, "Token 无效: " + e.getMessage());
        }
    }

    // ============ 场景3：角色权限校验 ============

    /**
     * 角色权限校验：
     * - ROLE_ADMIN: 可以访问所有接口
     * - ROLE_USER: 只能访问用户相关接口
     * - ROLE_GUEST: 只能查看公开内容
     */
    private static void demoRoleCheck() {
        System.out.println("=== 场景3：角色权限校验 ===");

        // 生成不同角色的 Token
        String adminToken = generateTokenForUser("admin");
        String userToken = generateTokenForUser("user1");
        String guestToken = generateTokenForUser("guest");

        // 需要 ADMIN 角色的接口
        checkApiAccess("DELETE /api/admin/users", adminToken, "ROLE_ADMIN");
        checkApiAccess("DELETE /api/admin/users", userToken, "ROLE_ADMIN");
        checkApiAccess("DELETE /api/admin/users", guestToken, "ROLE_ADMIN");

        // 需要 USER 角色的接口
        checkApiAccess("GET /api/orders", userToken, "ROLE_USER");
        checkApiAccess("GET /api/orders", guestToken, "ROLE_USER");

        System.out.println();
    }

    private static String generateTokenForUser(String username) {
        UserInfo user = USER_DB.get(username);
        return Jwts.builder()
                .subject(username)
                .claim("roles", user.roles)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    private static void checkApiAccess(String api, String token, String requiredRole) {
        try {
            Claims claims = parseTokenClaims(token);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            boolean hasAccess = roles != null && roles.contains(requiredRole);
            String user = claims.getSubject();

            System.out.println(api + " → 用户: " + user
                    + ", 角色: " + roles
                    + ", 需要: " + requiredRole
                    + " → " + (hasAccess ? "✅ 允许" : "❌ 拒绝"));
        } catch (Exception e) {
            System.out.println(api + " → ❌ Token 无效");
        }
    }

    // ============ 场景4：登出 ============

    /**
     * 登出流程：将 Token 加入黑名单
     * 实际项目中应存储到 Redis，并设置过期时间 = Token 剩余有效时间
     */
    private static void demoLogout() {
        System.out.println("=== 场景4：用户登出 ===");

        String token = Jwts.builder()
                .subject("user1")
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(KEY)
                .compact();

        // 登出前验证
        AuthResult before = authenticate(token);
        System.out.println("登出前验证: " + before.status);

        // 登出：加入黑名单
        TOKEN_BLACKLIST.add(token);
        System.out.println("已将 Token 加入黑名单");

        // 登出后验证
        AuthResult after = authenticate(token);
        System.out.println("登出后验证: " + after.status);
        System.out.println("  错误信息: " + after.errorMessage);
        System.out.println("  💡 生产环境应使用 Redis 存储黑名单（设置 TTL = Token 剩余有效期）");
        System.out.println();
    }

    // ============ 场景5：Spring Boot 集成指南 ============

    /**
     * Spring Boot 集成指南（注释形式）
     *
     * 实际集成步骤：
     */
    private static void demoSpringBootIntegration() {
        System.out.println("=== 场景5：Spring Boot 集成指南 ===\n");

        System.out.println("【步骤1】添加 Maven 依赖（见 pom.xml）");
        System.out.println("  jjwt-api + jjwt-impl + jjwt-jackson\n");

        System.out.println("【步骤2】创建 JwtUtil 工具类");
        System.out.println("  参考本 Demo 中的 login()/authenticate() 方法\n");

        System.out.println("【步骤3】创建 JwtAuthenticationFilter（拦截器）");
        System.out.println("  public class JwtAuthFilter extends OncePerRequestFilter {");
        System.out.println("      @Override");
        System.out.println("      protected void doFilterInternal(HttpServletRequest req,");
        System.out.println("              HttpServletResponse res, FilterChain chain) {");
        System.out.println("          String token = extractToken(req);");
        System.out.println("          if (token != null && jwtUtil.validateToken(token)) {");
        System.out.println("              Claims claims = jwtUtil.parseToken(token);");
        System.out.println("              // 设置 SecurityContext");
        System.out.println("              UsernamePasswordAuthenticationToken auth = ...;");
        System.out.println("              SecurityContextHolder.getContext()");
        System.out.println("                  .setAuthentication(auth);");
        System.out.println("          }");
        System.out.println("          chain.doFilter(req, res);");
        System.out.println("      }");
        System.out.println("  }\n");

        System.out.println("【步骤4】配置 SecurityConfig");
        System.out.println("  @Configuration");
        System.out.println("  @EnableWebSecurity");
        System.out.println("  public class SecurityConfig {");
        System.out.println("      @Bean");
        System.out.println("      public SecurityFilterChain filterChain(HttpSecurity http) {");
        System.out.println("          http.csrf().disable()");
        System.out.println("              .sessionManagement()");
        System.out.println("              .sessionCreationPolicy(SessionCreationPolicy.STATELESS)");
        System.out.println("              .and()");
        System.out.println("              .addFilterBefore(jwtAuthFilter(), ...);");
        System.out.println("          return http.build();");
        System.out.println("      }");
        System.out.println("  }\n");

        System.out.println("【步骤5】application.yml 配置");
        System.out.println("  jwt:");
        System.out.println("    secret: ${JWT_SECRET:your-base64-secret}");
        System.out.println("    access-token-expiration: 900000   # 15分钟");
        System.out.println("    refresh-token-expiration: 604800000 # 7天\n");

        System.out.println("【步骤6】登录接口示例");
        System.out.println("  @PostMapping(\"/api/auth/login\")");
        System.out.println("  public ResponseEntity<?> login(@RequestBody LoginRequest req) {");
        System.out.println("      // 验证用户名密码");
        System.out.println("      // 生成 Token");
        System.out.println("      LoginResult result = jwtUtil.generateTokens(username, roles);");
        System.out.println("      return ResponseEntity.ok(result);");
        System.out.println("  }\n");

        System.out.println("【步骤7】刷新 Token 接口");
        System.out.println("  @PostMapping(\"/api/auth/refresh\")");
        System.out.println("  public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {");
        System.out.println("      String newAccessToken = jwtUtil.refreshAccessToken(");
        System.out.println("          req.getRefreshToken());");
        System.out.println("      if (newAccessToken == null) {");
        System.out.println("          return ResponseEntity.status(401).build();");
        System.out.println("      }");
        System.out.println("      return ResponseEntity.ok(Map.of(");
        System.out.println("          \"accessToken\", newAccessToken));");
        System.out.println("  }\n");
    }

    // ============ 工具方法 ============

    private static Claims parseTokenClaims(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ============ 内部类 ============

    static class UserInfo {
        String username;
        String password;
        List<String> roles;

        UserInfo(String username, String password, List<String> roles) {
            this.username = username;
            this.password = password;
            this.roles = roles;
        }
    }

    static class LoginResult {
        String accessToken;
        String refreshToken;

        LoginResult(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    static class AuthResult {
        String status;
        String username;
        String errorMessage;

        AuthResult(String status, String username, String errorMessage) {
            this.status = status;
            this.username = username;
            this.errorMessage = errorMessage;
        }
    }
}
