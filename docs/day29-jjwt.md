# Day 29: JJWT (Java JWT) — JSON Web Token Java 库

## 📖 工具简介

**JJWT**（Java JWT）是 Java 生态中最流行的 JWT（JSON Web Token）库，由 Okta 团队维护。它提供了流畅的 Builder API 来创建、解析和验证 JWT Token，是现代 Web 应用中无状态身份认证的核心组件。

JWT 由三部分组成（Base64URL 编码，用 `.` 分隔）：
- **Header**：算法类型（HS256/RS256/ES256 等）
- **Payload（Claims）**：业务数据（sub/iat/exp 标准字段 + 自定义字段）
- **Signature**：对前两部分的签名，防止篡改

核心能力：
- 支持 HS256/HS384/HS512（对称加密）和 RS256/RS384/RS512/ES256 等（非对称加密）
- 流畅的 Builder API，链式创建 Token
- 自动验证签名、过期时间、签发者等
- 支持自定义 Claims 和标准 Registered Claims
- 密钥安全生成工具（`Jwts.SIG.HS256.key().build()`）
- Access Token + Refresh Token 双令牌模式

- **GitHub**: https://github.com/jwtk/jjwt
- **官方文档**: https://github.com/jwtk/jjwt#readme
- **星标**: 10k+
- **版本**: 0.12.5（本 Demo 使用，Java 8 兼容）
- **注意**: JJWT 0.12.x 起 API 大变，不兼容 0.11.x

## 📦 Maven 依赖

```xml
<!-- JJWT 0.12.x 拆分为三个模块 -->

<!-- API（编译依赖） -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>

<!-- 实现（运行时依赖） -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Jackson JSON 序列化器（运行时依赖） -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Jackson Databind（jjwt-jackson 依赖） -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.2</version>
</dependency>
```

> **注意**：0.12.x 需要显式引入 JSON 序列化器（`jjwt-jackson` 或 `jjwt-gson`），否则运行时会报 `Unable to find an implementation class`。

## 🏗️ Spring Boot 集成

### 1. JwtUtil 工具类

```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // 生成 Access Token
    public String generateAccessToken(String username, List<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(getKey())
                .compact();
    }

    // 生成 Refresh Token
    public String generateRefreshToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiration))
                .signWith(getKey())
                .compact();
    }

    // 解析 Token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 验证 Token
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

### 2. JwtAuthFilter 拦截器

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && jwtUtil.validateToken(token)) {
            Claims claims = jwtUtil.parseToken(token);
            String username = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

### 3. SecurityConfig 配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### 4. application.yml

```yaml
jwt:
  secret: ${JWT_SECRET:your-base64-encoded-secret-key-at-least-256-bits}
  access-token-expiration: 900000      # 15分钟
  refresh-token-expiration: 604800000  # 7天
```

## ⚠️ 注意事项

### 1. API 版本兼容性
- **0.12.x 与 0.11.x API 完全不兼容**。`Jwts.parserBuilder()` 已移除，改用 `Jwts.parser()`；`setSigningKey()` 已移除，改用 `.verifyWith()`。
- 如果从旧版迁移，务必参考 [JJWT 升级指南](https://github.com/jwtk/jjwt#upgrade-guides)。

### 2. 密钥长度限制
- **HS256** 密钥至少 **256 bits（32 bytes）**
- **HS384** 密钥至少 **384 bits（48 bytes）**
- **HS512** 密钥至少 **512 bits（64 bytes）**
- 密钥过短会抛出 `WeakKeyException`
- 推荐使用 `Jwts.SIG.HS256.key().build()` 自动生成安全密钥

### 3. JSON 序列化器必需
- 0.12.x 必须显式引入 `jjwt-jackson` 或 `jjwt-gson`，否则运行时报错：
  ```
  Unable to find an implementation class
  ```

### 4. JWT Payload 不加密
- JWT 的 Payload 只是 **Base64URL 编码**，不是加密！
- 任何人都可以解码查看内容
- **绝对不能**将密码、身份证号、银行卡号等敏感信息放入 Payload
- 如需加密，应配合 JWE（JSON Web Encryption）使用

### 5. 签名算法选择
| 算法 | 类型 | 密钥 | 适用场景 |
|------|------|------|----------|
| HS256 | 对称 | 共享密钥 | 单体应用 |
| RS256 | 非对称 | 公钥/私钥对 | 微服务（认证中心私钥签发，其他服务公钥验证） |
| ES256 | 非对称 | 椭圆曲线密钥对 | 移动端/物联网（密钥更短，性能更好） |

### 6. 安全性最佳实践
- Access Token 有效期尽量短（15-30分钟），Refresh Token 可稍长（7-30天）
- 使用 HTTPS 传输 Token
- Token 存储在客户端的安全位置（httpOnly Cookie 或安全存储），避免 XSS
- 服务端维护 Token 黑名单（登出时失效），建议用 Redis + TTL
- 使用 `jti`（Token ID）防止重放攻击

### 7. 常见异常处理
| 异常类型 | 含义 | HTTP 状态码 |
|----------|------|------------|
| `ExpiredJwtException` | Token 已过期 | 401 |
| `SignatureException` | 签名不匹配（被篡改） | 401 |
| `MalformedJwtException` | Token 格式错误 | 400 |
| `UnsupportedJwtException` | 不支持的 JWT 格式 | 400 |
| `PrematureJwtException` | Token 尚未生效（nbf） | 401 |

## 🚀 运行方法

```bash
# 进入项目目录
cd jjwt-demo

# 编译
mvn clean package -DskipTests

# 运行基础演示
mvn exec:java -Dexec.mainClass="com.example.jjwt.JjwtBasicDemo"

# 运行高级演示
mvn exec:java -Dexec.mainClass="com.example.jjwt.JjwtAdvancedDemo"

# 运行实战演示
mvn exec:java -Dexec.mainClass="com.example.jjwt.JjwtPracticalDemo"
```

## 📂 Demo 文件说明

| 文件 | 内容 |
|------|------|
| `JjwtBasicDemo.java` | Token 创建/解析/自定义 Claims/过期处理 |
| `JjwtAdvancedDemo.java` | RS256 非对称加密/Refresh Token/安全工具/常见坑 |
| `JjwtPracticalDemo.java` | 用户认证系统/登录/拦截器/角色鉴权/黑名单/Spring Boot 集成 |

## 🔗 参考资源

- [JJWT GitHub](https://github.com/jwtk/jjwt)
- [JWT.io 在线调试工具](https://jwt.io/)
- [RFC 7519 - JSON Web Token](https://tools.ietf.org/html/rfc7519)
- [JWT 最佳实践](https://datatracker.ietf.org/doc/html/rfc8725)
