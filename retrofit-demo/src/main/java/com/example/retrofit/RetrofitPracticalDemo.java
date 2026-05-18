package com.example.retrofit;

import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Retrofit 2 实战演示
 *
 * 涵盖以下实战场景：
 * 1. RxJava2 适配器（Observable / Single 返回类型）
 * 2. 统一封装 RetrofitClient 单例工厂
 * 3. 通用响应体包装（ApiResponse<T>）
 * 4. 并发多接口请求（并行 + 顺序聚合）
 * 5. 请求取消（Call.cancel()）
 * 6. 错误处理最佳实践（HTTP 错误码 / 网络异常分支）
 * 7. Spring Boot 集成指引（配置示例）
 */
public class RetrofitPracticalDemo {

    // ==================== 通用响应体封装 ====================

    /**
     * 常见后端统一响应格式
     * { "code": 0, "message": "success", "data": {...} }
     */
    static class ApiResponse<T> {
        @SerializedName("code")
        private int code;

        @SerializedName("message")
        private String message;

        @SerializedName("data")
        private T data;

        public boolean isSuccess() { return code == 0; }
        public int getCode() { return code; }
        public String getMessage() { return message; }
        public T getData() { return data; }

        @Override
        public String toString() {
            return "ApiResponse{code=" + code + ", message='" + message + "', data=" + data + "}";
        }
    }

    /** 用于 JSONPlaceholder 测试的 Todo 模型 */
    static class Todo {
        private int id;
        private int userId;
        private String title;
        private boolean completed;

        public int getId() { return id; }
        public String getTitle() { return title; }
        public boolean isCompleted() { return completed; }

        @Override
        public String toString() {
            return "Todo{id=" + id + ", title='" + title + "', completed=" + completed + "}";
        }
    }

    /** Post 简化模型（复用） */
    static class Post {
        private int id;
        private int userId;
        private String title;
        private String body;

        public int getId() { return id; }
        public String getTitle() { return title; }

        @Override
        public String toString() {
            return "Post{id=" + id + ", title='" + title + "'}";
        }
    }

    // ==================== Service 接口 ====================

    /** JSONPlaceholder 综合服务接口（普通 Call） */
    interface PlaceholderService {
        @GET("todos")
        Call<List<Todo>> getAllTodos();

        @GET("todos/{id}")
        Call<Todo> getTodoById(@Path("id") int id);

        @GET("posts")
        Call<List<Post>> getPosts(@Query("_limit") int limit);

        @GET("posts/{id}")
        Call<Post> getPost(@Path("id") int id);
    }

    /**
     * 支持 RxJava2 的 Service 接口
     * 返回 Observable<T> / Single<T> / Completable
     * 需要在 Retrofit 中注册 RxJava2CallAdapterFactory
     */
    interface RxPlaceholderService {

        /** 返回 Observable：适合多次 emit 的流式场景 */
        @GET("todos")
        Observable<List<Todo>> getTodosRx();

        /** 返回 Single：只 emit 一次，适合请求-响应模型 */
        @GET("posts")
        Single<List<Post>> getPostsRx(@Query("_limit") int limit);

        @GET("posts/{id}")
        Single<Post> getPostByIdRx(@Path("id") int id);
    }

    // ==================== RetrofitClient 单例工厂 ====================

    /**
     * RetrofitClient 单例封装
     * 生产环境建议：
     * 1. baseUrl 从配置文件读取
     * 2. OkHttpClient 由 Spring Bean 管理
     * 3. 使用 Map 按 baseUrl 缓存多个 Retrofit 实例
     */
    static class RetrofitClient {

        private static volatile RetrofitClient instance;
        private final Retrofit retrofit;
        private final Retrofit rxRetrofit;

        private RetrofitClient(String baseUrl) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        // 统一添加 Token（实际从 ThreadLocal 或 Spring SecurityContext 读取）
                        okhttp3.Request request = chain.request().newBuilder()
                                .header("Authorization", "Bearer demo-token")
                                .header("Content-Type", "application/json")
                                .build();
                        return chain.proceed(request);
                    })
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            // 普通 Call 版本
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            // RxJava2 版本（额外添加 RxJava2CallAdapterFactory）
            rxRetrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        }

        public static RetrofitClient getInstance(String baseUrl) {
            if (instance == null) {
                synchronized (RetrofitClient.class) {
                    if (instance == null) {
                        instance = new RetrofitClient(baseUrl);
                    }
                }
            }
            return instance;
        }

        public <T> T createService(Class<T> serviceClass) {
            return retrofit.create(serviceClass);
        }

        public <T> T createRxService(Class<T> serviceClass) {
            return rxRetrofit.create(serviceClass);
        }
    }

    // ==================== 主程序 ====================

    public static void main(String[] args) throws InterruptedException {

        System.out.println("========== Retrofit 2 实战演示 ==========\n");

        final String BASE_URL = "https://jsonplaceholder.typicode.com/";
        RetrofitClient client = RetrofitClient.getInstance(BASE_URL);

        // ---- 场景1：请求取消演示 ----
        System.out.println("--- 场景1：请求取消（Call.cancel()）---");
        PlaceholderService service = client.createService(PlaceholderService.class);
        Call<List<Todo>> cancelableCall = service.getAllTodos();

        // 模拟：启动后立即取消
        new Thread(() -> {
            try {
                Thread.sleep(10); // 极短延迟
                cancelableCall.cancel();
                System.out.println("已取消请求！isCanceled=" + cancelableCall.isCanceled());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        try {
            Response<List<Todo>> resp = cancelableCall.execute();
            System.out.println("请求完成（未被取消）：" + resp.code());
        } catch (IOException e) {
            if (cancelableCall.isCanceled()) {
                System.out.println("请求已被取消（符合预期）");
            } else {
                System.out.println("其他网络异常：" + e.getMessage());
            }
        }

        // ---- 场景2：错误处理最佳实践 ----
        System.out.println("\n--- 场景2：HTTP 错误码 / 网络异常分支处理 ---");
        try {
            // 故意请求一个不存在的资源（404）
            Response<Todo> resp = service.getTodoById(99999).execute();
            if (resp.isSuccessful()) {
                System.out.println("成功：" + resp.body());
            } else {
                // HTTP 错误（4xx / 5xx）
                switch (resp.code()) {
                    case 404:
                        System.out.println("资源不存在（404）：" + resp.message());
                        break;
                    case 401:
                        System.out.println("未授权，请重新登录（401）");
                        break;
                    case 500:
                        System.out.println("服务器内部错误（500）");
                        break;
                    default:
                        // 读取错误响应体
                        String errorBody = resp.errorBody() != null
                                ? resp.errorBody().string() : "无错误信息";
                        System.out.println("其他HTTP错误 " + resp.code() + "：" + errorBody);
                }
            }
        } catch (IOException e) {
            // 纯网络/IO 异常（连接超时、DNS 失败等）
            System.out.println("网络不可达：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        // ---- 场景3：并发多接口聚合（手动并行 + 等待） ----
        System.out.println("\n--- 场景3：并发多接口聚合 ---");
        try {
            long start = System.currentTimeMillis();

            // 并行发起 3 个请求（每个在独立线程）
            Call<List<Todo>> todosCall = service.getAllTodos();
            Call<List<Post>> postsCall = service.getPosts(10);

            // 用 Future-like 方式并行
            final List<Todo>[] todos = new List[1];
            final List<Post>[] posts = new List[1];
            final AtomicInteger latch = new AtomicInteger(2);

            todosCall.enqueue(new retrofit2.Callback<List<Todo>>() {
                @Override
                public void onResponse(Call<List<Todo>> call, Response<List<Todo>> response) {
                    todos[0] = response.body();
                    latch.decrementAndGet();
                }
                @Override
                public void onFailure(Call<List<Todo>> call, Throwable t) {
                    System.out.println("Todos 请求失败：" + t.getMessage());
                    latch.decrementAndGet();
                }
            });

            postsCall.enqueue(new retrofit2.Callback<List<Post>>() {
                @Override
                public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                    posts[0] = response.body();
                    latch.decrementAndGet();
                }
                @Override
                public void onFailure(Call<List<Post>> call, Throwable t) {
                    System.out.println("Posts 请求失败：" + t.getMessage());
                    latch.decrementAndGet();
                }
            });

            // 等待两个并行请求完成
            while (latch.get() > 0) {
                Thread.sleep(100);
            }

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("并发请求完成，耗时：" + elapsed + "ms");
            System.out.println("  Todos 数量：" + (todos[0] != null ? todos[0].size() : "失败"));
            System.out.println("  Posts 数量：" + (posts[0] != null ? posts[0].size() : "失败"));
        } catch (Exception e) {
            System.out.println("并发请求异常：" + e.getMessage());
        }

        // ---- 场景4：RxJava2 集成演示 ----
        System.out.println("\n--- 场景4：RxJava2 集成（Observable / Single）---");
        RxPlaceholderService rxService = client.createRxService(RxPlaceholderService.class);

        // Single：一次性请求，map 转换，error 处理
        AtomicInteger rxDone = new AtomicInteger(0);
        rxService.getPostsRx(5)
                .subscribeOn(Schedulers.io())
                .map(postList -> {
                    // 链式处理：过滤已完成的 Post（这里 Post 没有 completed 字段，仅演示 map 用法）
                    List<String> titles = new ArrayList<>();
                    for (Post p : postList) {
                        titles.add("Post#" + p.getId() + ": " + p.getTitle());
                    }
                    return titles;
                })
                .subscribe(
                        titles -> {
                            System.out.println("RxJava Single 获取到 " + titles.size() + " 条 Post：");
                            for (String t : titles) {
                                System.out.println("  " + t);
                            }
                            rxDone.incrementAndGet();
                        },
                        error -> {
                            System.out.println("RxJava 错误：" + error.getMessage());
                            rxDone.incrementAndGet();
                        }
                );

        // Observable：流式处理
        rxService.getTodosRx()
                .subscribeOn(Schedulers.io())
                .flatMap(Observable::fromIterable)   // List<Todo> → Observable<Todo>
                .filter(todo -> !todo.isCompleted()) // 只看未完成
                .take(3)                             // 取前3条
                .subscribe(
                        todo -> System.out.println("未完成 Todo：" + todo),
                        error -> System.out.println("Observable 错误：" + error.getMessage()),
                        () -> {
                            System.out.println("Observable 流结束");
                            rxDone.incrementAndGet();
                        }
                );

        // 等待 RxJava 异步执行
        int waitCount = 0;
        while (rxDone.get() < 2 && waitCount < 50) {
            Thread.sleep(200);
            waitCount++;
        }

        // ---- 场景5：Spring Boot 集成指引 ----
        System.out.println("\n--- 场景5：Spring Boot 集成方案（代码指引）---");
        System.out.println(buildSpringBootIntegrationGuide());

        System.out.println("\n========== 实战演示结束 ==========");
    }

    /**
     * 生成 Spring Boot 集成代码片段
     * 实际项目中将这段代码写入 RetrofitConfig.java 即可
     */
    private static String buildSpringBootIntegrationGuide() {
        return "\n  /** Spring Boot 集成方式（Bean 配置）*/"
                + "\n  @Configuration"
                + "\n  public class RetrofitConfig {"
                + "\n"
                + "\n      @Value(\"${api.base-url:https://api.example.com/}\")"
                + "\n      private String baseUrl;"
                + "\n"
                + "\n      @Bean"
                + "\n      public OkHttpClient okHttpClient() {"
                + "\n          return new OkHttpClient.Builder()"
                + "\n              .addInterceptor(new HttpLoggingInterceptor().setLevel(BASIC))"
                + "\n              .connectTimeout(10, SECONDS)"
                + "\n              .readTimeout(15, SECONDS)"
                + "\n              .build();"
                + "\n      }"
                + "\n"
                + "\n      @Bean"
                + "\n      public Retrofit retrofit(OkHttpClient client) {"
                + "\n          return new Retrofit.Builder()"
                + "\n              .baseUrl(baseUrl)"
                + "\n              .client(client)"
                + "\n              .addConverterFactory(GsonConverterFactory.create())"
                + "\n              .addCallAdapterFactory(RxJava2CallAdapterFactory.create())"
                + "\n              .build();"
                + "\n      }"
                + "\n"
                + "\n      // 每个 Service 注册为 Spring Bean，注入使用即可"
                + "\n      @Bean"
                + "\n      public UserApiService userApiService(Retrofit retrofit) {"
                + "\n          return retrofit.create(UserApiService.class);"
                + "\n      }"
                + "\n  }";
    }
}
