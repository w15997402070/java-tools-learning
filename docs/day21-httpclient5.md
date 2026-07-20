# Day 21 - Apache HttpClient 5

## 工具简介

**Apache HttpClient 5** 是 Apache 出品的企业级 Java HTTP 客户端，是 HttpClient 4.x 的全面重写版本。
与 Spring 官方推荐路线深度对齐（Spring 6 / Spring Boot 3 默认使用 HC5），
同时提供对 JDK 8~21 的兼容支持。

- **GitHub**：https://github.com/apache/httpcomponents-client
- **官方文档**：https://hc.apache.org/httpcomponents-client-5.3.x/
- **最新稳定版**：5.3.1
- **Star**：约 2.5k（Apache 官方仓库，实际广泛使用）

### 与 HttpClient 4.x 的核心区别

| 维度 | HttpClient 4.x | HttpClient 5.x |
|------|-------------|-------------|
| 包名 | `org.apache.http.*` | `org.apache.hc.*` |
| 超时类型 | `int` 毫秒值 | `Timeout` 对象 |
| 异步支持 | 有限 | 原生 async，基于 Java NIO |
| 流式接口 | 无 | `HttpClient5-fluent` 模块 |
| HTTP/2 | 不支持 | 支持 |
| Spring Boot 支持 | 4.x 对应 SB 2.x | 5.x 对应 SB 3.x+ |

---

## Maven 依赖

```xml
<!-- 核心库 -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.3.1</version>
</dependency>

<!-- 流式 API（可选） -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5-fluent</artifactId>
    <version>5.3.1</version>
</dependency>
```

---

## 核心用法速查

### 1. 最简 GET 请求

```java
try (CloseableHttpClient client = HttpClients.createDefault();
     CloseableHttpResponse response = client.execute(new HttpGet("https://example.com/api"))) {
    String body = EntityUtils.toString(response.getEntity());
    System.out.println("状态码: " + response.getCode());
    System.out.println("响应体: " + body);
}
```

### 2. POST JSON

```java
HttpPost post = new HttpPost("https://api.example.com/users");
post.setEntity(new StringEntity(
    "{\"name\":\"张三\",\"age\":25}",
    ContentType.APPLICATION_JSON
));

try (CloseableHttpClient client = HttpClients.createDefault();
     CloseableHttpResponse response = client.execute(post)) {
    System.out.println(response.getCode()); // 201
}
```

### 3. 带参数 URL（推荐 URIBuilder）

```java
URI uri = new URIBuilder("https://api.example.com/search")
        .addParameter("keyword", "java")
        .addParameter("page", "1")
        .build();
HttpGet get = new HttpGet(uri);
```

### 4. 超时配置

```java
RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))
        .setResponseTimeout(Timeout.of(10, TimeUnit.SECONDS))
        .setConnectionRequestTimeout(Timeout.of(3, TimeUnit.SECONDS))
        .build();

CloseableHttpClient client = HttpClients.custom()
        .setDefaultRequestConfig(config)
        .build();
```

### 5. 连接池（生产必备）

```java
PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
cm.setMaxTotal(200);
cm.setDefaultMaxPerRoute(40);

CloseableHttpClient client = HttpClients.custom()
        .setConnectionManager(cm)
        .evictExpiredConnections()
        .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))
        .build();
```

### 6. 拦截器（追加公共 Header）

```java
CloseableHttpClient client = HttpClients.custom()
        .addRequestInterceptorFirst((request, entity, context) -> {
            request.addHeader("Authorization", "Bearer " + getToken());
            request.addHeader("X-Trace-Id", UUID.randomUUID().toString());
        })
        .build();
```

### 7. 表单提交

```java
List<NameValuePair> params = new ArrayList<>();
params.add(new BasicNameValuePair("username", "admin"));
params.add(new BasicNameValuePair("password", "secret"));

HttpPost post = new HttpPost(url);
post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
```

### 8. 文件上传（Multipart）

```java
HttpPost post = new HttpPost(url);
post.setEntity(MultipartEntityBuilder.create()
        .addBinaryBody("file", new File("report.pdf"), ContentType.APPLICATION_PDF, "report.pdf")
        .addTextBody("description", "月度报告", ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
        .build());
```

---

## Spring Boot 集成

### 依赖（Spring Boot 3.x）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- Spring Boot 3 自动集成 HC5；若单独使用，引入 httpclient5 即可 -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.3.1</version>
</dependency>
```

### 注册 Bean（推荐配置类）

```java
@Configuration
public class HttpClientConfig {

    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(40);
        return cm;
    }

    @Bean
    @PreDestroy
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager cm) {
        return HttpClients.custom()
                .setConnectionManager(cm)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))
                .build();
    }

    /**
     * 注册 RestTemplate，底层使用 HC5 连接池（替代默认的 SimpleClientHttpRequestFactory）
     */
    @Bean
    public RestTemplate restTemplate(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }
}
```

### Service 层使用

```java
@Service
public class UserService {

    @Autowired
    private RestTemplate restTemplate;

    public UserDTO getUser(Long id) {
        return restTemplate.getForObject(
            "https://api.example.com/users/{id}", UserDTO.class, id);
    }

    public UserDTO createUser(UserDTO dto) {
        return restTemplate.postForObject(
            "https://api.example.com/users", dto, UserDTO.class);
    }
}
```

### 属性配置（application.properties）

```properties
# HC5 连接池参数（通过 Bean 配置，无法直接在 properties 中设置，以下为说明）
# 建议在 @Configuration 类中读取 @ConfigurationProperties 注入
http.client.max-total=200
http.client.default-max-per-route=40
http.client.connect-timeout-seconds=5
http.client.response-timeout-seconds=10
```

---

## 注意事项

### Bug 风险

1. **响应体必须消费或关闭**  
   未消费 Entity 会导致连接无法归还池，最终耗尽连接：
   ```java
   // ✅ 正确：消费响应体
   EntityUtils.consume(response.getEntity());
   // ✅ 或使用 try-with-resources 自动关闭
   try (CloseableHttpResponse response = client.execute(request)) { ... }
   // ❌ 错误：忽略 Entity，连接泄漏！
   ```

2. **不要为每个请求创建新 HttpClient**  
   频繁 `HttpClients.createDefault()` 会导致内存泄漏和连接爆炸，应单例共享。

3. **HC4 → HC5 包名迁移**  
   - 请求类从 `org.apache.http.client.methods.HttpGet` → `org.apache.hc.client5.http.classic.methods.HttpGet`
   - Entity 工具从 `org.apache.http.util.EntityUtils` → `org.apache.hc.core5.http.io.entity.EntityUtils`
   - 常见编译报错：找不到 `getStatusLine()` → HC5 改用 `new StatusLine(response).toString()`

4. **超时参数类型变化**  
   HC4：`setSocketTimeout(int milliseconds)` → HC5：`setResponseTimeout(Timeout.of(10, TimeUnit.SECONDS))`

### 性能问题

1. **连接池未配置 `evictExpiredConnections()`**  
   服务端主动断开连接后客户端无感知，下一次请求拿到失效连接报 "Connection Reset"。
   务必开启 `evictExpiredConnections()` + `evictIdleConnections(30s)`。

2. **连接池 MaxPerRoute 设置过小**  
   默认 `defaultMaxPerRoute=2`（来自 HC 底层默认值），高并发时请求排队严重。
   生产建议 `setDefaultMaxPerRoute(20~50)`。

3. **SSL/TLS 握手开销**  
   HTTPS 请求建立连接慢，连接复用尤为重要。长连接 + 连接池是核心优化手段。

### 使用限制

- **Java 8+ 兼容**：HC5 支持 Java 8，但异步 API（`HttpAsyncClient`）在 Java 8 上功能有限制
- **HTTP/2 支持**：需要额外依赖 `httpcomponents-httpcore5-h2`
- **Spring Boot 2.x**：仍使用 HC4，若需 HC5 需手动替换；Spring Boot 3.x 原生支持 HC5

---

## 运行方法

```bash
# 编译
cd java-tools-learning/httpclient5-demo
mvn clean package -DskipTests

# 运行基础演示（GET/POST/PUT/DELETE）
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.httpclient5.HttpClient5BasicDemo"

# 运行进阶演示（连接池/超时/拦截器/Cookie/Basic认证）
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.httpclient5.HttpClient5AdvancedDemo"

# 运行实战演示（表单/文件上传/并发/重试）
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.httpclient5.HttpClient5PracticalDemo"
```

---

## 与同类工具对比

| 工具 | 适用场景 | 优势 | 劣势 |
|------|--------|------|------|
| **HC5** | 企业级、高并发、Spring 集成 | 功能全面、连接池强大、HTTP/2 | API 较繁琐 |
| **OkHttp** (Day 3) | Android/Java 通用 | API 简洁、拦截器优雅 | 非 Apache 生态 |
| **Retrofit** (Day 17) | REST API 客户端封装 | 注解驱动、类型安全 | 底层仍依赖 OkHttp |
| **Spring RestTemplate** | Spring 应用内 HTTP 调用 | 与 Spring 无缝集成 | 同步阻塞 |
| **Spring WebClient** | 响应式/异步场景 | 非阻塞、WebFlux 原生 | 需引入 Reactor |
