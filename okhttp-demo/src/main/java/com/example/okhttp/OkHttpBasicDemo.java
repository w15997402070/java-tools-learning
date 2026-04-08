package com.example.okhttp;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp 基础使用演示
 *
 * OkHttp 是Square公司开源的高性能HTTP客户端库，支持HTTP/2和连接池。
 * 特性：
 * - 支持HTTP/2，提升网络效率
 * - 连接池复用，减少握手延迟
 * - 自动处理GZIP压缩
 * - 透明的缓存响应
 * - 支持同步/异步请求
 */
public class OkHttpBasicDemo {

    private static final OkHttpClient client;

    static {
        // 创建带日志拦截器的客户端，方便调试
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)    // 连接超时
                .readTimeout(30, TimeUnit.SECONDS)       // 读取超时
                .writeTimeout(30, TimeUnit.SECONDS)       // 写入超时
                .addInterceptor(loggingInterceptor)       // 添加日志拦截器
                .build();
    }

    /**
     * GET 请求 - 同步方式
     * 最简单的HTTP请求，获取URL对应的响应体
     */
    public static void getSync() throws IOException {
        System.out.println("=== GET 同步请求 ===");
        Request request = new Request.Builder()
                .url("https://httpbin.org/get")
                .get()
                .build();

        // 同步调用会阻塞当前线程
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                System.out.println("响应状态码: " + response.code());
                System.out.println("响应体长度: " + body.length() + " 字符");
                System.out.println("响应体(前200字符): " + body.substring(0, Math.min(200, body.length())));
            }
        }
    }

    /**
     * GET 请求 - 带请求头
     * 演示如何添加自定义请求头
     */
    public static void getWithHeaders() throws IOException {
        System.out.println("\n=== GET 带请求头 ===");
        Request request = new Request.Builder()
                .url("https://httpbin.org/headers")
                .get()
                .header("User-Agent", "OkHttp-Demo/1.0")
                .header("Accept", "application/json")
                .header("X-Custom-Header", "Hello-OkHttp")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                System.out.println("响应体:\n" + response.body().string());
            }
        }
    }

    /**
     * GET 请求 - 带查询参数
     * 演示如何构建带参数的URL
     */
    public static void getWithParams() throws IOException {
        System.out.println("\n=== GET 带查询参数 ===");
        // 使用 HttpUrl.Builder 构建带参数的URL
        okhttp3.HttpUrl.Builder urlBuilder = okhttp3.HttpUrl.parse("https://httpbin.org/get").newBuilder();
        urlBuilder.addQueryParameter("name", "张三");
        urlBuilder.addQueryParameter("age", "25");
        urlBuilder.addEncodedQueryParameter("city", "北京");

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                System.out.println("响应体:\n" + response.body().string());
            }
        }
    }

    /**
     * 获取响应头信息
     * OkHttp会自动解析常用响应头
     */
    public static void getResponseHeaders() throws IOException {
        System.out.println("\n=== 获取响应头 ===");
        Request request = new Request.Builder()
                .url("https://httpbin.org/headers")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("Content-Type: " + response.header("Content-Type"));
                System.out.println("Content-Length: " + response.header("Content-Length"));
                System.out.println("Date: " + response.header("Date"));
                System.out.println("所有响应头:");
                response.headers().forEach(header ->
                    System.out.println("  " + header.getFirst() + ": " + header.getSecond())
                );
            }
        }
    }

    /**
     * 主方法 - 运行所有演示
     */
    public static void main(String[] args) {
        try {
            getSync();
            getWithHeaders();
            getWithParams();
            getResponseHeaders();
            System.out.println("\n✅ 基础演示完成！");
        } catch (IOException e) {
            System.err.println("❌ 请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
