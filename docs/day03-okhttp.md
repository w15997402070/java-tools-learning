# Day 03 - OkHttp：高性能 HTTP 客户端

## 工具简介

**OkHttp** 是 Square 公司开源的高性能 HTTP/HTTP2 客户端库，是 Android 和 Java 项目中使用最广泛的 HTTP 客户端之一。Retrofit、Coil 等知名库底层都依赖 OkHttp。

| 属性 | 信息 |
|------|------|
| GitHub | https://github.com/square/okhttp |
| ⭐ Stars | 45k+ |
| 最新版本 | 4.12.0 |
| 官方文档 | https://square.github.io/okhttp/ |
| License | Apache 2.0 |

### 核心特性

- ✅ **HTTP/2 支持**：多路复用，同一主机的请求共享一个连接
- ✅ **连接池**：降低请求延迟（HTTP/1.x）
- ✅ **透明 GZIP**：自动压缩/解压请求体，减少带宽消耗
- ✅ **响应缓存**：完全实现 RFC 标准的缓存机制
- ✅ **拦截器链**：可扩展的请求/响应处理管道
- ✅ **同步/异步**：两种调用模式满足不同场景需求
- ✅ **HTTPS**：内置 TLS 支持，可自定义证书验证

---

## Maven 依赖配置

```xml
<dependencies>
    <!-- OkHttp 核心库 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- 日志拦截器（开发调试用，生产建议移除） -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>logging-interceptor</artifactId>
        <version>4.12.0</version>
    </dependency>
</dependencies>
```

> ⚠️ OkHttp 4.x 使用 Kotlin 编写，打包后含 Kotlin stdlib，大约增加 ~1.5MB 包体积。若项目对包大小敏感，可考虑使用 3.x 版本（纯 Java）。

---

## 基础使用

### 1. 创建 OkHttpClient（单例，推荐全局共享）

```java
OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();
```

> 💡 **重要**：`OkHttpClient` 内部维护连接池和线程池，应作为单例使用。每次 `new OkHttpClient()` 会创建新的线程池，造成资源浪费。

### 2. 同步 GET 请求

```java
Request request = new Request.Builder()
        .url("https://api.example.com/users/1")
        .get()
        .build();

try (Response response = client.newCall(request).execute()) {
    if (response.isSuccessful() && response.body() != null) {
        String json = response.body().string(); // 注意：string()只能调用一次！
        System.out.println(json);
    }
}
```

### 3. 异步 GET 请求

```java
client.newCall(request).enqueue(new Callback() {
    @Override
    public void onFailure(Call call, IOException e) {
        // 网络错误
        e.printStackTrace();
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        try (ResponseBody body = response.body()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);
            System.out.println(body.string());
        }
    }
});
```

### 4. POST JSON 请求

```java
MediaType JSON = MediaType.get("application/json; charset=utf-8");
String jsonBody = "{\"name\":\"张三\",\"age\":25}";
RequestBody body = RequestBody.create(jsonBody, JSON);

Request request = new Request.Builder()
        .url("https://api.example.com/users")
        .post(body)
        .build();
```

### 5. POST 表单请求

```java
FormBody formBody = new FormBody.Builder()
        .add("username", "zhangsan")
        .add("password", "secret123")
        .build();

Request request = new Request.Builder()
        .url("https://api.example.com/login")
        .post(formBody)
        .build();
```

---

## Spring Boot 集成

### 方式一：注入 OkHttpClient Bean（推荐）

```java
@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(
            // 生产环境用 NONE 或 BASIC，开发环境用 BODY
            "prod".equals(System.getProperty("env")) ?
                HttpLoggingInterceptor.Level.BASIC :
                HttpLoggingInterceptor.Level.BODY
        );

        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
                .addInterceptor(loggingInterceptor)
                .build();
    }
}
```

### 方式二：与 Feign 集成（微服务场景）

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-okhttp</artifactId>
</dependency>
```

```java
@Configuration
public class FeignConfig {
    @Bean
    public okhttp3.OkHttpClient okHttpClient() {
        return new okhttp3.OkHttpClient();
    }
}
```

### 方式三：与 RestTemplate 集成

```java
@Bean
public RestTemplate restTemplate() {
    OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();

    OkHttp3ClientHttpRequestFactory factory = 
        new OkHttp3ClientHttpRequestFactory(httpClient);
    
    return new RestTemplate(factory);
}
```

### 封装通用 HTTP 工具类（生产推荐）

```java
@Component
public class HttpUtil {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Autowired
    private OkHttpClient okHttpClient;

    /**
     * GET 请求
     */
    public String get(String url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + url);
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * POST JSON 请求
     */
    public String postJson(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + url);
            }
            return response.body() != null ? response.body().string() : "";
        }
    }
}
```

---

## 拦截器（Interceptor）

拦截器是 OkHttp 最强大的功能，可以在请求/响应的各个阶段插入自定义逻辑。

### 认证拦截器

```java
public class AuthInterceptor implements Interceptor {
    private final String token;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request().newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(request);
    }
}
```

### 重试拦截器

```java
public class RetryInterceptor implements Interceptor {
    private final int maxRetries;

    @Override
    public Response intercept(Chain chain) throws IOException {
        int attempt = 0;
        IOException lastException;
        do {
            try {
                Response response = chain.proceed(chain.request());
                if (response.code() < 500) return response;
                response.close();
            } catch (IOException e) {
                lastException = e;
            }
            attempt++;
            Thread.sleep(1000L * attempt); // 指数退避
        } while (attempt <= maxRetries);
        throw lastException;
    }
}
```

### 应用拦截器 vs 网络拦截器

| 对比项 | 应用拦截器（addInterceptor） | 网络拦截器（addNetworkInterceptor） |
|--------|------------------------------|--------------------------------------|
| 调用时机 | 在缓存/重试之前 | 在实际网络请求时 |
| 缓存响应 | 能看到缓存响应 | 看不到缓存响应 |
| 重定向 | 只调用一次 | 每次重定向都调用 |
| 适用场景 | 认证、日志、重试 | 修改网络层请求头 |

---

## 注意事项

### ⚠️ Bug 风险

1. **`response.body().string()` 只能调用一次**
   - `ResponseBody.string()` 读取后流即关闭，第二次调用返回空字符串或抛异常
   - 解决：读取到变量后复用，或使用 `response.peekBody(Long.MAX_VALUE)` 提前缓存

2. **必须关闭 Response**
   - 未关闭的 Response 会导致连接无法回到连接池，造成连接泄漏
   - 推荐使用 `try-with-resources`：`try (Response response = ...)`
   - 异步回调中不能用 try-with-resources，需在 finally 中手动 `response.close()`

3. **异步回调不在主线程**
   - `onResponse` 在 OkHttp 的 Dispatcher 线程池中执行
   - Android 中不能直接更新 UI，需切回主线程

4. **OkHttpClient 4.x 依赖 Kotlin stdlib**
   - 若项目不用 Kotlin，会额外引入 kotlin-stdlib（~1.5MB）
   - 对包大小敏感的场景可使用 3.14.x 纯 Java 版本

### ⚠️ 性能问题

1. **不要每次请求都创建新的 OkHttpClient**
   - 每个 OkHttpClient 有独立的连接池和线程池，频繁创建会耗尽资源
   - 正确做法：全局单例，或通过 Spring 注入

2. **合理配置连接池**
   ```java
   // 默认：5个空闲连接，5分钟保活
   // 高并发场景建议调大：
   new ConnectionPool(20, 5, TimeUnit.MINUTES)
   ```

3. **大响应体分块读取**
   ```java
   // 不要一次 body.string()，改用流式处理：
   try (BufferedSource source = response.body().source()) {
       // 按需读取
   }
   ```

### ⚠️ 使用限制

1. **OkHttp 4.x 最低支持 Android 5.0+ / Java 8+**

2. **自签证书处理**（仅开发环境）：
   ```java
   // ⚠️ 生产环境严禁使用！会绕过证书验证
   TrustManager[] trustAllCerts = new TrustManager[]{
       new X509TrustManager() {
           public void checkClientTrusted(...) {}
           public void checkServerTrusted(...) {}
           public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
       }
   };
   SSLContext sslContext = SSLContext.getInstance("SSL");
   sslContext.init(null, trustAllCerts, new SecureRandom());
   client = new OkHttpClient.Builder()
           .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager)trustAllCerts[0])
           .hostnameVerifier((hostname, session) -> true)
           .build();
   ```

3. **线程安全**：`OkHttpClient` 本身线程安全，但 `Request.Builder` 不是，不要在多线程间共享 Builder 实例

---

## Demo 说明

| 文件 | 演示内容 |
|------|----------|
| `OkHttpBasicDemo.java` | 同步 GET、带请求头、带查询参数、响应头解析 |
| `OkHttpAdvancedDemo.java` | POST JSON/表单、PUT/DELETE、异步请求、自定义拦截器 |
| `OkHttpPracticalDemo.java` | REST API 调用、工具类封装、文件上传、Cookie 管理、连接池配置 |

## 运行方法

```bash
cd okhttp-demo
mvn clean package -DskipTests
# 运行基础演示
mvn exec:java -Dexec.mainClass="com.example.okhttp.OkHttpBasicDemo"
# 运行进阶演示
mvn exec:java -Dexec.mainClass="com.example.okhttp.OkHttpAdvancedDemo"
# 运行实战演示
mvn exec:java -Dexec.mainClass="com.example.okhttp.OkHttpPracticalDemo"
```

> 注意：Demo 中访问了 `httpbin.org` 和 `jsonplaceholder.typicode.com` 等公共接口，运行时需要网络连接。

---

## 与同类库对比

| 库 | 优势 | 劣势 | 推荐场景 |
|----|------|------|----------|
| **OkHttp** | 功能完整、生态成熟、拦截器强大 | 4.x 引入 Kotlin 依赖 | 通用 Java/Android 项目 |
| **HttpClient (Apache)** | 功能丰富、高度可配置 | API 繁琐，学习成本高 | 企业级复杂 HTTP 需求 |
| **HttpURLConnection** | JDK 内置，无额外依赖 | API 原始、不支持 HTTP/2 | 极简依赖场景 |
| **WebClient (Spring)** | 响应式非阻塞 | 需引入 Spring WebFlux | 响应式微服务 |
| **Feign** | 声明式、与 Spring Cloud 完美集成 | 底层仍依赖 OkHttp/Apache | Spring Cloud 微服务 |

---

*Last updated: 2026-04-07 | 作者: java-tools-learning 项目*
