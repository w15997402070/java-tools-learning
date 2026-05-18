# Day 17 - Retrofit 2：类型安全 HTTP 客户端库

## 工具简介

**Retrofit 2** 是 Square 公司出品的一款**类型安全的 HTTP 客户端库**，底层基于 OkHttp，通过 Java 注解将 HTTP API 接口声明为 Java Interface，运行时动态生成实现。

> "A type-safe HTTP client for Android and Java."

Retrofit 的最大优势在于**接口即文档**：通过注解清晰表达 HTTP 方法、路径参数、查询参数、请求体，代码即协议，可读性极强。

- **GitHub**：https://github.com/square/retrofit
- **官方文档**：https://square.github.io/retrofit/
- **当前版本**：2.11.0（2024年最新）
- **Star 数**：42k+（Java 生态最流行的 HTTP 客户端之一）
- **License**：Apache 2.0

---

## 核心注解速查表

| 注解 | 说明 | 示例 |
|------|------|------|
| `@GET("path")` | HTTP GET 请求 | `@GET("users")` |
| `@POST("path")` | HTTP POST 请求 | `@POST("users")` |
| `@PUT("path")` | HTTP PUT 请求 | `@PUT("users/{id}")` |
| `@DELETE("path")` | HTTP DELETE 请求 | `@DELETE("users/{id}")` |
| `@PATCH("path")` | HTTP PATCH 请求 | `@PATCH("users/{id}")` |
| `@Path("key")` | URL 路径替换 `{key}` | `@Path("id") int id` |
| `@Query("key")` | URL 查询参数 `?key=value` | `@Query("page") int page` |
| `@Body` | 请求体（自动序列化为 JSON） | `@Body User user` |
| `@Field("key")` | 表单字段（配合 @FormUrlEncoded） | `@Field("name") String name` |
| `@Header("key")` | 动态请求头 | `@Header("Authorization") String token` |
| `@Headers({...})` | 静态请求头（多个） | `@Headers({"Accept: application/json"})` |
| `@FormUrlEncoded` | 类级注解，表单提交 | 配合 `@Field` 使用 |
| `@Multipart` | 多部分上传（文件） | 配合 `@Part` 使用 |

---

## Maven 依赖配置

```xml
<properties>
    <retrofit.version>2.11.0</retrofit.version>
    <okhttp.version>4.12.0</okhttp.version>
</properties>

<dependencies>
    <!-- Retrofit 2 核心 -->
    <dependency>
        <groupId>com.squareup.retrofit2</groupId>
        <artifactId>retrofit</artifactId>
        <version>${retrofit.version}</version>
    </dependency>

    <!-- Gson 转换器（JSON 自动反序列化） -->
    <dependency>
        <groupId>com.squareup.retrofit2</groupId>
        <artifactId>converter-gson</artifactId>
        <version>${retrofit.version}</version>
    </dependency>

    <!-- RxJava2 适配器（可选，用于响应式编程） -->
    <dependency>
        <groupId>com.squareup.retrofit2</groupId>
        <artifactId>adapter-rxjava2</artifactId>
        <version>${retrofit.version}</version>
    </dependency>

    <!-- RxJava 2（如使用 RxJava 适配器时必须加） -->
    <dependency>
        <groupId>io.reactivex.rxjava2</groupId>
        <artifactId>rxjava</artifactId>
        <version>2.2.21</version>
    </dependency>

    <!-- OkHttp 日志拦截器（开发调试用，生产可关闭） -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>logging-interceptor</artifactId>
        <version>${okhttp.version}</version>
    </dependency>
</dependencies>
```

> **Java 8 兼容性**：Retrofit 2.11.0 编译目标仍为 Java 8，可直接用于 JDK 8 项目。

---

## 快速入门（5 分钟上手）

### 1. 定义数据模型

```java
public class User {
    private int id;
    private String name;
    private String email;
    // getters...
}
```

### 2. 定义 Service 接口

```java
public interface UserService {

    // 获取用户列表
    @GET("users")
    Call<List<User>> getUsers();

    // 根据 ID 获取用户
    @GET("users/{id}")
    Call<User> getUser(@Path("id") int id);

    // 分页查询
    @GET("users")
    Call<List<User>> getUsersPage(@Query("page") int page,
                                  @Query("per_page") int size);

    // 创建用户（JSON Body）
    @POST("users")
    Call<User> createUser(@Body User user);

    // 带认证 Token 请求
    @GET("users/me")
    Call<User> getMe(@Header("Authorization") String bearerToken);
}
```

### 3. 创建 Retrofit 实例并调用

```java
// 创建 Retrofit（整个应用共享一个实例）
Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://api.example.com/")   // 必须以 / 结尾！
        .addConverterFactory(GsonConverterFactory.create())
        .build();

// 创建 Service（动态代理）
UserService userService = retrofit.create(UserService.class);

// 同步调用
Response<List<User>> response = userService.getUsers().execute();
if (response.isSuccessful()) {
    List<User> users = response.body();
}

// 异步调用（推荐，不阻塞线程）
userService.getUsers().enqueue(new Callback<List<User>>() {
    @Override
    public void onResponse(Call<List<User>> call, Response<List<User>> response) {
        if (response.isSuccessful()) {
            // 处理成功
        }
    }

    @Override
    public void onFailure(Call<List<User>> call, Throwable t) {
        // 处理网络异常
    }
});
```

---

## Spring Boot 集成方式

### 方式一：配置类注册 Bean（推荐）

```java
@Configuration
public class RetrofitConfig {

    @Value("${external.api.base-url}")
    private String baseUrl;

    @Bean
    public OkHttpClient okHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                // 统一 Token 拦截器（从 Spring Security Context 读取）
                .addInterceptor(chain -> {
                    String token = TokenHolder.getToken(); // 自行实现
                    Request request = token != null
                            ? chain.request().newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build()
                            : chain.request();
                    return chain.proceed(request);
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public Retrofit retrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    @Bean
    public UserService userService(Retrofit retrofit) {
        return retrofit.create(UserService.class);
    }
}
```

### 方式二：使用 retrofit-spring-boot-starter

```xml
<dependency>
    <groupId>com.github.lianjiatech</groupId>
    <artifactId>retrofit-spring-boot-starter</artifactId>
    <version>3.1.4</version>
</dependency>
```

```java
// 只需一个注解即可自动注入
@RetrofitClient(baseUrl = "${external.api.base-url}")
public interface UserService {
    @GET("users/{id}")
    Call<User> getUser(@Path("id") int id);
}

// 在 Service 层直接注入
@Service
public class UserAppService {
    @Autowired
    private UserService userService;
}
```

### 方式三：与 RxJava2 集成（响应式风格）

```java
// Service 接口返回 Single<T> 而非 Call<T>
public interface UserService {
    @GET("users")
    Single<List<User>> getUsersRx();

    @GET("users/{id}")
    Observable<User> getUserRx(@Path("id") int id);
}

// 调用（响应式链）
userService.getUsersRx()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())  // Android
        .subscribe(users -> updateUI(users),
                   error -> handleError(error));
```

---

## 代码示例：RetrofitBasicDemo.java

- `getAllPosts()` — `@GET` 获取列表，同步执行
- `getPostById(@Path)` — `@Path` 路径参数
- `getPostsByUserId(@Query)` — `@Query` 查询参数过滤
- `createPost(@Body)` — `@POST` + `@Body` 创建资源
- `deletePost(@Path)` — `@DELETE` 删除资源
- `enqueue()` — 异步回调方式

## 代码示例：RetrofitAdvancedDemo.java

- 自定义 `OkHttpClient`（日志拦截器 / 统一 Header / 缓存 / 超时）
- `@Headers` 静态请求头 vs `@Header` 动态请求头
- `Call.clone()` 重复执行同一请求
- 访问 GitHub Public API（真实场景演示）

## 代码示例：RetrofitPracticalDemo.java

- `RetrofitClient` 单例工厂模式
- 请求取消（`Call.cancel()`）
- HTTP 错误码分支处理（404/401/500 + `errorBody`）
- 并发多接口聚合（`enqueue` + `AtomicInteger` 计数器）
- `RxJava2` 集成（`Single` / `Observable` / `flatMap` / `filter` / `take`）
- Spring Boot 集成代码指引

---

## 注意事项（Bug 风险 / 性能问题 / 使用限制）

### ⚠️ 1. baseUrl 必须以 `/` 结尾

```java
// ✅ 正确
.baseUrl("https://api.example.com/v1/")

// ❌ 错误（路径会拼接错误）
.baseUrl("https://api.example.com/v1")
```

若 baseUrl 不以 `/` 结尾，相对路径拼接会出错（如 `users` 会拼成 `https://api.example.com/users` 而不是 `/v1/users`）。

### ⚠️ 2. Call 只能执行一次，复用需 clone()

```java
Call<User> call = service.getUser(1);
call.execute();          // 第一次 OK
call.execute();          // ❌ 抛出 IllegalStateException: Already executed

Call<User> cloned = call.clone();
cloned.execute();        // ✅ clone 后可再次执行
```

### ⚠️ 3. 同步调用不可在主线程执行（Android）

在 Android 中，`call.execute()` 会直接阻塞线程，主线程调用将抛出 `NetworkOnMainThreadException`。务必在 IO 线程中调用，或改用 `enqueue()`。

### ⚠️ 4. Retrofit 实例应全局单例

每次 `new Retrofit.Builder().build()` 都会创建新的线程池和连接池，导致资源浪费。应用层必须单例（Spring Bean 或 static 持有）。

### ⚠️ 5. 错误响应体需手动读取

```java
if (!response.isSuccessful()) {
    ResponseBody errorBody = response.errorBody();
    if (errorBody != null) {
        String error = errorBody.string(); // 只能读一次！
        // 再次 errorBody.string() 返回 ""
    }
}
```

`errorBody` 是流式的，只能读取一次。如需多次使用，先转字符串缓存。

### ⚠️ 6. Converter 的注册顺序很重要

当注册多个 Converter 时，Retrofit 按注册顺序依次尝试，第一个能处理的生效。

```java
// ✅ Gson 在最后兜底
.addConverterFactory(ScalarsConverterFactory.create())  // 处理 String
.addConverterFactory(GsonConverterFactory.create())     // 处理 JSON
```

### ⚠️ 7. `@Body` 不能与 `@FormUrlEncoded` 同时使用

`@Body` 用于 JSON 请求体，`@FormUrlEncoded` + `@Field` 用于表单提交，两者互斥。

### ⚠️ 8. RxJava 错误不捕获会崩溃

RxJava2 中若 `subscribe()` 没有传 `onError` 处理器，且发生异常，会直接抛 `UndeliverableException` 导致进程崩溃。**务必提供 onError 回调**。

### 📈 性能建议

| 场景 | 建议 |
|------|------|
| 大量并发请求 | 使用 `RxJava2` + `Schedulers.io()`，避免手动创建线程 |
| 高频重复请求（如 Token 刷新） | 配置 OkHttp Cache + `max-age` |
| 超时配置 | connectTimeout ≤ 10s，readTimeout 视接口而定 |
| 日志打印 | 生产环境设为 `NONE` 或 `BASIC`，`BODY` 级别性能较差 |

---

## 与同类库对比

| 特性 | Retrofit 2 | OkHttp（裸用） | Spring RestTemplate | WebClient (Spring) |
|------|------------|---------------|--------------------|--------------------|
| 接口声明式 | ✅ | ❌ | ❌ | ❌ |
| 类型安全 | ✅ | ❌ | 部分 | ✅ |
| RxJava/Reactor 支持 | ✅（adapter） | ❌ | ❌ | ✅（原生） |
| 学习曲线 | 低 | 低 | 低 | 中 |
| 适用场景 | 第三方 REST API | 定制 HTTP 场景 | Spring MVC 同步 | Spring WebFlux 响应式 |

---

## 运行方法

```bash
cd retrofit-demo

# 1. 编译打包
mvn clean package -DskipTests

# 2. 运行基础演示（需要网络访问 jsonplaceholder.typicode.com）
mvn exec:java -Dexec.mainClass="com.example.retrofit.RetrofitBasicDemo" -pl .

# 或直接运行 IDE 中的 main 方法
```

> **提示**：演示代码使用 https://jsonplaceholder.typicode.com 公开 API（永久免费），运行前确保可访问该域名。如无法访问，网络异常捕获逻辑会输出友好提示。

---

## 参考资料

- 官方文档：https://square.github.io/retrofit/
- GitHub：https://github.com/square/retrofit
- OkHttp 官方文档：https://square.github.io/okhttp/
- RxJava2 适配器说明：https://github.com/square/retrofit/tree/trunk/retrofit-adapters/rxjava2
