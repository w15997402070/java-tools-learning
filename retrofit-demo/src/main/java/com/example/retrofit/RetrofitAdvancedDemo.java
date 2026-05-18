package com.example.retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Retrofit 2 进阶演示
 *
 * 涵盖以下进阶功能：
 * 1. 自定义 OkHttpClient（超时/日志拦截器/重试）
 * 2. 请求头注解 @Header / @Headers
 * 3. 表单提交 @FormUrlEncoded + @Field
 * 4. OkHttp 响应缓存（Cache）
 * 5. 自定义 Gson（日期格式/字段策略）
 * 6. 多个 Converter（当同一项目有多种 API 格式时）
 * 7. Call.clone() 复用请求
 */
public class RetrofitAdvancedDemo {

    // ==================== 数据模型 ====================

    /** 用户信息 */
    static class User {
        private int id;
        private String name;
        private String email;
        private String phone;
        private String website;

        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "', email='" + email
                    + "', phone='" + phone + "'}";
        }
    }

    /** GitHub Repository（演示真实 API） */
    static class GitHubRepo {
        @SerializedName("id")
        private long id;

        @SerializedName("name")
        private String name;

        @SerializedName("full_name")
        private String fullName;

        @SerializedName("stargazers_count")
        private int stars;

        @SerializedName("forks_count")
        private int forks;

        @SerializedName("description")
        private String description;

        @SerializedName("html_url")
        private String htmlUrl;

        @Override
        public String toString() {
            return "GitHubRepo{name='" + fullName + "', stars=" + stars
                    + ", forks=" + forks + ", desc='" + description + "'}";
        }
    }

    // ==================== Service 接口 ====================

    /** JSONPlaceholder Users API */
    interface UserApiService {

        /** 获取全部用户 */
        @GET("users")
        Call<List<User>> getAllUsers();

        /**
         * 携带静态请求头（多个 @Headers）
         * 适合接口鉴权、内容类型固定的场景
         */
        @Headers({
            "Accept: application/json",
            "X-Custom-Header: Retrofit-Demo"
        })
        @GET("users/{id}")
        Call<User> getUserWithHeaders(@Path("id") int id);

        /**
         * 携带动态请求头（单个 @Header，值由调用方传入）
         * 适合 Token 鉴权（每次调用可传不同 token）
         */
        @GET("users/{id}")
        Call<User> getUserWithAuthToken(@Path("id") int id,
                                        @Header("Authorization") String token);

        /**
         * 表单提交（@FormUrlEncoded + @Field）
         * Content-Type: application/x-www-form-urlencoded
         * 注意：JSONPlaceholder 不支持真实写入，此处仅展示用法
         */
        @FormUrlEncoded
        @POST("users")
        Call<User> createUserForm(@Field("name") String name,
                                  @Field("email") String email,
                                  @Field("phone") String phone);
    }

    /** GitHub API Service */
    interface GitHubApiService {

        /**
         * 搜索仓库
         * GET https://api.github.com/search/repositories?q={query}&sort=stars
         */
        @GET("search/repositories")
        Call<SearchResult> searchRepos(@Query("q") String query,
                                       @Query("sort") String sort,
                                       @Query("per_page") int perPage);

        /** 获取用户的仓库列表 */
        @GET("users/{user}/repos")
        Call<List<GitHubRepo>> getUserRepos(@Path("user") String user,
                                            @Query("sort") String sort,
                                            @Query("per_page") int perPage);
    }

    /** GitHub 搜索结果包装 */
    static class SearchResult {
        @SerializedName("total_count")
        private int totalCount;

        @SerializedName("items")
        private List<GitHubRepo> items;

        public int getTotalCount() { return totalCount; }
        public List<GitHubRepo> getItems() { return items; }
    }

    // ==================== 主程序 ====================

    public static void main(String[] args) throws InterruptedException {

        System.out.println("========== Retrofit 2 进阶演示 ==========\n");

        // ---- 1. 自定义 OkHttpClient ----
        System.out.println("--- 1. 自定义 OkHttpClient（日志拦截器 + 超时）---");

        // 日志拦截器（BODY 级别可打印完整请求/响应内容）
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                message -> System.out.println("[HTTP] " + message)
        );
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC); // NONE/BASIC/HEADERS/BODY

        // 自定义 Header 拦截器（统一为所有请求加上 User-Agent）
        Interceptor headerInterceptor = chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("User-Agent", "Retrofit-Demo/1.0")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .build();
            return chain.proceed(request);
        };

        // 响应缓存（可选，Android 常用，服务端需返回 Cache-Control 头）
        File cacheDir = new File(System.getProperty("java.io.tmpdir"), "retrofit-cache");
        long cacheSize = 10 * 1024 * 1024; // 10 MB
        Cache cache = new Cache(cacheDir, cacheSize);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)     // 应用拦截器（打印日志）
                .addInterceptor(headerInterceptor)       // 统一加请求头
                .cache(cache)                            // 响应缓存
                .connectTimeout(10, TimeUnit.SECONDS)    // 连接超时
                .readTimeout(15, TimeUnit.SECONDS)       // 读超时
                .writeTimeout(10, TimeUnit.SECONDS)      // 写超时
                .retryOnConnectionFailure(true)          // 连接失败自动重试
                .build();

        System.out.println("OkHttpClient 构建成功，缓存目录：" + cacheDir.getAbsolutePath());

        // ---- 2. 自定义 Gson（日期格式 + NULL 序列化） ----
        System.out.println("\n--- 2. 自定义 Gson 转换器 ---");
        Gson customGson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")  // ISO-8601 日期格式
                .serializeNulls()                         // 序列化 null 字段
                .setPrettyPrinting()                      // 美化输出（调试用）
                .create();

        // 使用自定义 Gson 的 Retrofit（指向 JSONPlaceholder）
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(customGson))
                .build();

        UserApiService userService = retrofit.create(UserApiService.class);

        // ---- 3. 请求头演示 ----
        System.out.println("\n--- 3. @Headers 静态请求头 ---");
        try {
            retrofit2.Response<User> resp = userService.getUserWithHeaders(1).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                System.out.println("获取到用户：" + resp.body());
            }
        } catch (IOException e) {
            System.out.println("网络不可达：" + e.getMessage());
        }

        System.out.println("\n--- 4. @Header 动态 Token 请求头 ---");
        try {
            // 模拟动态 Bearer Token
            String fakeToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.demo";
            retrofit2.Response<User> resp = userService.getUserWithAuthToken(2, fakeToken).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                System.out.println("带 Token 获取用户：" + resp.body());
            }
        } catch (IOException e) {
            System.out.println("网络不可达：" + e.getMessage());
        }

        // ---- 4. Call.clone() 重复执行同一请求 ----
        System.out.println("\n--- 5. Call.clone() 复用请求 ---");
        try {
            Call<List<User>> originalCall = userService.getAllUsers();
            // 第一次执行
            retrofit2.Response<List<User>> resp1 = originalCall.execute();
            System.out.println("第一次执行：获取到 " + (resp1.body() != null ? resp1.body().size() : 0) + " 个用户");

            // Call 执行后会标记为已使用，必须 clone 后才能再次执行
            Call<List<User>> clonedCall = originalCall.clone();
            retrofit2.Response<List<User>> resp2 = clonedCall.execute();
            System.out.println("clone后再次执行：获取到 " + (resp2.body() != null ? resp2.body().size() : 0) + " 个用户");
        } catch (IOException e) {
            System.out.println("网络不可达：" + e.getMessage());
        }

        // ---- 5. GitHub API（不同 baseUrl，创建新 Retrofit 实例）----
        System.out.println("\n--- 6. GitHub API 搜索仓库（真实公开API）---");
        Retrofit githubRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GitHubApiService githubService = githubRetrofit.create(GitHubApiService.class);

        try {
            retrofit2.Response<SearchResult> resp = githubService
                    .searchRepos("retrofit language:java", "stars", 5)
                    .execute();
            if (resp.isSuccessful() && resp.body() != null) {
                SearchResult result = resp.body();
                System.out.println("GitHub 搜索 retrofit 仓库，总共：" + result.getTotalCount() + " 个");
                System.out.println("Top 5 按 Star 排序：");
                if (result.getItems() != null) {
                    for (GitHubRepo repo : result.getItems()) {
                        System.out.println("  ★" + repo.stars + " " + repo);
                    }
                }
            } else {
                System.out.println("GitHub API 请求失败：HTTP " + resp.code()
                        + "（可能遭遇限流，每小时60次未认证）");
            }
        } catch (IOException e) {
            System.out.println("网络不可达：" + e.getMessage());
        }

        System.out.println("\n========== 进阶演示结束 ==========");
    }
}
