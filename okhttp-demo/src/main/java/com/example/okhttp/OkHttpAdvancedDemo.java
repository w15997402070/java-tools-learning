package com.example.okhttp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp 进阶使用演示
 *
 * 覆盖以下功能：
 * 1. POST JSON 请求（同步）
 * 2. POST 表单请求
 * 3. 异步请求
 * 4. 自定义拦截器
 * 5. 超时和重试配置
 */
public class OkHttpAdvancedDemo {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded");
    private static final Gson GSON = new Gson();

    private static final OkHttpClient client;

    static {
        // 构建客户端，添加自定义拦截器
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor("my-api-token"))  // 添加认证拦截器
                .addInterceptor(new RetryInterceptor(3))              // 添加重试拦截器
                .build();
    }

    // ======================== POST 请求演示 ========================

    /**
     * POST JSON 请求 - 同步方式
     * 将Java对象序列化为JSON后发送
     */
    public static void postJson() throws IOException {
        System.out.println("=== POST JSON 请求 ===");

        // 构建请求体
        Map<String, Object> params = new HashMap<>();
        params.put("name", "张三");
        params.put("age", 25);
        params.put("city", "北京");
        params.put("hobbies", new String[]{"Java", "Spring Boot", "OkHttp"});

        String jsonBody = GSON.toJson(params);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url("https://httpbin.org/post")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                System.out.println("POST JSON 响应状态: " + response.code());
                System.out.println("响应(前300字): " + responseBody.substring(0, Math.min(300, responseBody.length())));
            }
        }
    }

    /**
     * POST 表单请求
     * 使用FormBody构建表单数据，等同于HTML表单提交
     */
    public static void postForm() throws IOException {
        System.out.println("\n=== POST 表单请求 ===");

        FormBody formBody = new FormBody.Builder()
                .add("username", "zhangsan")
                .add("password", "secret123")
                .add("remember_me", "true")
                .build();

        Request request = new Request.Builder()
                .url("https://httpbin.org/post")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                System.out.println("表单请求响应状态: " + response.code());
                String body = response.body().string();
                // 提取form部分
                int formIndex = body.indexOf("\"form\":");
                if (formIndex >= 0) {
                    System.out.println("表单数据: " + body.substring(formIndex, Math.min(formIndex + 200, body.length())));
                }
            }
        }
    }

    /**
     * PUT 请求 - 更新资源
     */
    public static void putRequest() throws IOException {
        System.out.println("\n=== PUT 请求 ===");

        JsonObject updateData = new JsonObject();
        updateData.addProperty("id", 1);
        updateData.addProperty("title", "Updated Title");
        updateData.addProperty("body", "Updated content");
        updateData.addProperty("userId", 1);

        RequestBody requestBody = RequestBody.create(GSON.toJson(updateData), JSON);

        Request request = new Request.Builder()
                .url("https://jsonplaceholder.typicode.com/posts/1")
                .put(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                System.out.println("PUT 响应状态: " + response.code());
                System.out.println("更新后的数据: " + response.body().string());
            }
        }
    }

    /**
     * DELETE 请求
     */
    public static void deleteRequest() throws IOException {
        System.out.println("\n=== DELETE 请求 ===");

        Request request = new Request.Builder()
                .url("https://jsonplaceholder.typicode.com/posts/1")
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("DELETE 响应状态: " + response.code());
            if (response.body() != null) {
                System.out.println("DELETE 响应体: " + response.body().string());
            }
        }
    }

    // ======================== 异步请求演示 ========================

    /**
     * 异步 GET 请求
     * 不阻塞当前线程，通过回调处理结果
     */
    public static void asyncGet() throws InterruptedException {
        System.out.println("\n=== 异步 GET 请求 ===");

        // 使用CountDownLatch等待异步任务完成（生产中一般不需要等待）
        CountDownLatch latch = new CountDownLatch(1);

        Request request = new Request.Builder()
                .url("https://jsonplaceholder.typicode.com/todos/1")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("异步请求失败: " + e.getMessage());
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        System.out.println("异步响应线程: " + Thread.currentThread().getName());
                        System.out.println("异步响应结果: " + response.body().string());
                    }
                } finally {
                    response.close();  // 必须关闭response，防止资源泄漏
                    latch.countDown();
                }
            }
        });

        // 等待异步任务完成（最多5秒）
        latch.await(5, TimeUnit.SECONDS);
    }

    // ======================== 拦截器定义 ========================

    /**
     * 自定义认证拦截器
     * 自动在所有请求中添加Authorization请求头
     */
    static class AuthInterceptor implements Interceptor {
        private final String token;

        AuthInterceptor(String token) {
            this.token = token;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();

            // 克隆原始请求并添加认证头
            Request newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();

            return chain.proceed(newRequest);
        }
    }

    /**
     * 自定义重试拦截器
     * 当请求失败时自动重试
     */
    static class RetryInterceptor implements Interceptor {
        private final int maxRetries;

        RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            IOException lastException = null;

            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    Response response = chain.proceed(request);
                    if (response.isSuccessful() || attempt == maxRetries) {
                        return response;
                    }
                    // 服务器错误时重试
                    if (response.code() >= 500) {
                        response.close();
                        System.out.println("服务器错误，第 " + attempt + " 次重试...");
                        Thread.sleep(1000L * (attempt + 1)); // 指数退避
                    } else {
                        return response;
                    }
                } catch (IOException e) {
                    lastException = e;
                    System.out.println("请求异常，第 " + attempt + " 次重试: " + e.getMessage());
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(1000L * (attempt + 1));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("重试被中断", ie);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("重试被中断", e);
                }
            }

            throw lastException != null ? lastException : new IOException("请求失败，已重试 " + maxRetries + " 次");
        }
    }

    public static void main(String[] args) {
        try {
            postJson();
            postForm();
            putRequest();
            deleteRequest();
            asyncGet();
            System.out.println("\n✅ 进阶演示完成！");
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ 演示失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
