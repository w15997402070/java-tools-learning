package com.example.okhttp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp 实战场景演示
 *
 * 模拟真实项目中的常见场景：
 * 1. 封装HTTP工具类
 * 2. 调用REST API并反序列化响应
 * 3. 文件上传（MultipartBody）
 * 4. Cookie管理
 * 5. HTTPS/TLS配置
 */
public class OkHttpPracticalDemo {

    private static final Gson GSON = new Gson();

    // ======================== REST API 调用 ========================

    /**
     * 调用 JSONPlaceholder REST API
     * 演示如何将JSON响应反序列化为Java对象
     */
    public static void fetchPostList() throws IOException {
        System.out.println("=== 调用 REST API 获取帖子列表 ===");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("https://jsonplaceholder.typicode.com/posts?_limit=5")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                Type listType = new TypeToken<List<Post>>() {}.getType();
                List<Post> posts = GSON.fromJson(json, listType);

                System.out.println("获取到 " + posts.size() + " 篇帖子:");
                for (Post post : posts) {
                    System.out.printf("  [%d] %s%n", post.id, post.title);
                }
            }
        }
    }

    /**
     * 封装通用 HTTP 工具类
     * 实际项目中通常会封装一个工具类，避免重复创建OkHttpClient
     */
    public static void demonstrateHttpUtil() throws IOException {
        System.out.println("\n=== 封装 HTTP 工具类演示 ===");

        // 获取单例Post
        Post post = HttpUtil.get("https://jsonplaceholder.typicode.com/posts/1", Post.class);
        System.out.println("获取到帖子: " + post.title);

        // 发送POST请求创建资源
        Post newPost = new Post();
        newPost.title = "New Post from OkHttp Demo";
        newPost.body = "This is a test post created by OkHttp";
        newPost.userId = 1;

        Post createdPost = HttpUtil.postJson(
                "https://jsonplaceholder.typicode.com/posts",
                newPost,
                Post.class
        );
        System.out.println("创建帖子成功，ID: " + createdPost.id + ", 标题: " + createdPost.title);
    }

    /**
     * MultipartBody 文件上传演示（模拟，不实际上传文件）
     * 演示如何使用MultipartBody上传文件和参数
     */
    public static void demonstrateMultipartUpload() throws IOException {
        System.out.println("\n=== Multipart 文件上传演示（模拟）===");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // 模拟文件内容（实际使用时用File对象或InputStream）
        byte[] fileContent = "Hello, this is test file content!".getBytes();

        RequestBody fileBody = RequestBody.create(fileContent, MediaType.parse("text/plain"));
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("description", "测试文件上传")
                .addFormDataPart("uploader", "java-okhttp-demo")
                .addFormDataPart("file", "test.txt", fileBody)
                .build();

        Request request = new Request.Builder()
                .url("https://httpbin.org/post")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                System.out.println("上传响应状态: " + response.code());
                // 提取files部分
                int filesIdx = responseBody.indexOf("\"files\":");
                int formIdx = responseBody.indexOf("\"form\":");
                if (filesIdx >= 0) {
                    System.out.println("Files部分: " + responseBody.substring(filesIdx, Math.min(filesIdx + 150, responseBody.length())));
                }
                if (formIdx >= 0) {
                    System.out.println("Form部分: " + responseBody.substring(formIdx, Math.min(formIdx + 200, responseBody.length())));
                }
            }
        }
    }

    /**
     * Cookie 管理演示
     * OkHttp 支持自动管理 Cookie
     */
    public static void demonstrateCookies() throws IOException {
        System.out.println("\n=== Cookie 管理演示 ===");

        // 使用 CookieJar 自动管理 Cookie
        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(new CookieJar() {
                    private List<Cookie> cookies = null;

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookieList) {
                        this.cookies = cookieList;
                        System.out.println("保存 Cookie，数量: " + cookieList.size());
                        for (Cookie c : cookieList) {
                            System.out.println("  Cookie: " + c.name() + " = " + c.value());
                        }
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        return cookies != null ? cookies : java.util.Collections.emptyList();
                    }
                })
                .build();

        // 请求一个会返回Set-Cookie的接口
        Request request = new Request.Builder()
                .url("https://httpbin.org/cookies/set?session=abc123&user=demo")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Cookie设置请求状态: " + response.code());
            System.out.println("Location: " + response.header("Location"));
        }
    }

    /**
     * 响应缓存演示
     * OkHttp 支持RFC标准的HTTP缓存
     */
    public static void demonstrateConnectionPool() {
        System.out.println("\n=== 连接池配置演示 ===");

        // 自定义连接池
        ConnectionPool pool = new ConnectionPool(
                10,              // 最大空闲连接数
                5,               // 保活时间（分钟）
                TimeUnit.MINUTES
        );

        OkHttpClient client = new OkHttpClient.Builder()
                .connectionPool(pool)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        System.out.println("连接池配置完成:");
        System.out.println("  空闲连接数: " + pool.idleConnectionCount());
        System.out.println("  总连接数: " + pool.connectionCount());
    }

    // ======================== 数据模型 ========================

    /**
     * JSONPlaceholder Post 数据模型
     */
    static class Post {
        int userId;
        int id;
        String title;
        String body;
    }

    // ======================== HTTP 工具类封装 ========================

    /**
     * 简单的HTTP工具类封装
     * 生产项目中建议使用单例模式，并扩展错误处理
     */
    static class HttpUtil {
        private static final Gson GSON = new Gson();
        private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // 单例客户端（推荐：整个应用共享一个OkHttpClient实例）
        private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        /**
         * GET 请求并反序列化为指定类型
         */
        public static <T> T get(String url, Class<T> clazz) throws IOException {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("请求失败: " + response.code());
                }
                return GSON.fromJson(response.body().charStream(), clazz);
            }
        }

        /**
         * POST JSON 请求并反序列化响应
         */
        public static <T> T postJson(String url, Object payload, Class<T> clazz) throws IOException {
            RequestBody body = RequestBody.create(GSON.toJson(payload), JSON);
            Request request = new Request.Builder().url(url).post(body).build();
            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("请求失败: " + response.code());
                }
                return GSON.fromJson(response.body().charStream(), clazz);
            }
        }
    }

    public static void main(String[] args) {
        try {
            fetchPostList();
            demonstrateHttpUtil();
            demonstrateMultipartUpload();
            demonstrateCookies();
            demonstrateConnectionPool();
            System.out.println("\n✅ 实战演示完成！");
        } catch (IOException e) {
            System.err.println("❌ 演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
