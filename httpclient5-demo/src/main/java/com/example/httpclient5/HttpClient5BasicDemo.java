package com.example.httpclient5;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Apache HttpClient 5 基础演示
 *
 * 演示内容：
 * 1. 基础 GET 请求（获取响应体、状态码、响应头）
 * 2. POST/PUT/DELETE 请求（RESTful API）
 * 3. 请求头设置与响应头读取
 * 4. 使用 try-with-resources 正确关闭资源
 *
 * 核心 API 变化（HttpClient 4 → 5）：
 * - CloseableHttpClient 仍是主入口，但包名由 org.apache.http 改为 org.apache.hc
 * - EntityUtils.toString() 第二参数改为 Charset，不再接受字符串
 * - 响应状态用 new StatusLine(response).toString() 获取
 */
public class HttpClient5BasicDemo {

    /** JSONPlaceholder 公开测试 API */
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    public static void main(String[] args) throws Exception {
        System.out.println("========== Apache HttpClient 5 基础演示 ==========\n");

        // 1. 基础 GET 请求
        demoGet();

        // 2. 带请求参数的 GET
        demoGetWithParams();

        // 3. POST 请求（创建资源）
        demoPost();

        // 4. PUT 请求（更新资源）
        demoPut();

        // 5. DELETE 请求（删除资源）
        demoDelete();

        System.out.println("\n========== 演示结束 ==========");
    }

    /**
     * 演示 1：基础 GET 请求
     * 获取文章列表，读取状态码、响应头、响应体（截断输出）
     */
    private static void demoGet() throws IOException, ParseException {
        System.out.println("--- 1. 基础 GET 请求 ---");

        // HttpClients.createDefault() 创建带连接池的默认客户端
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/posts/1");

            // 添加自定义请求头
            request.addHeader("Accept", "application/json");
            request.addHeader("User-Agent", "Apache-HttpClient5-Demo/1.0");

            try (CloseableHttpResponse response = client.execute(request)) {
                // 获取状态码
                int statusCode = response.getCode();
                System.out.println("状态码: " + statusCode);
                System.out.println("状态行: " + new StatusLine(response));

                // 读取响应头
                System.out.println("Content-Type: " + response.getFirstHeader("Content-Type").getValue());

                // 读取响应体（自动处理编码）
                String body = EntityUtils.toString(response.getEntity());
                // 只打印前200字符
                System.out.println("响应体(前200字): " + body.substring(0, Math.min(body.length(), 200)));
            }
        }
        System.out.println();
    }

    /**
     * 演示 2：带查询参数的 GET 请求
     * 使用 URI Builder 构造带参数的 URL（推荐方式，避免手动拼接）
     */
    private static void demoGetWithParams() throws IOException, ParseException, URISyntaxException {
        System.out.println("--- 2. 带查询参数的 GET 请求 ---");

        // HttpClient 5 推荐使用 URIBuilder 构造带参数 URL
        URI uri = new org.apache.hc.core5.net.URIBuilder(BASE_URL + "/posts")
                .addParameter("userId", "1")
                .addParameter("_limit", "2")
                .build();

        System.out.println("请求 URL: " + uri);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(new HttpGet(uri))) {

            String body = EntityUtils.toString(response.getEntity());
            System.out.println("状态码: " + response.getCode());
            // 仅打印前300字符展示效果
            System.out.println("响应(前300字): " + body.substring(0, Math.min(body.length(), 300)) + "...");
        }
        System.out.println();
    }

    /**
     * 演示 3：POST 请求（发送 JSON 请求体）
     * 关键：使用 StringEntity + ContentType.APPLICATION_JSON
     */
    private static void demoPost() throws IOException, ParseException {
        System.out.println("--- 3. POST 请求（创建资源）---");

        String jsonBody = "{\"title\":\"Apache HttpClient 5\",\"body\":\"现代HTTP客户端\",\"userId\":1}";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL + "/posts");

            // 设置请求体，ContentType 自动设置 Content-Type 头
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println("状态码: " + response.getCode()); // 201 Created
                String body = EntityUtils.toString(response.getEntity());
                System.out.println("响应体: " + body);
            }
        }
        System.out.println();
    }

    /**
     * 演示 4：PUT 请求（全量更新资源）
     */
    private static void demoPut() throws IOException, ParseException {
        System.out.println("--- 4. PUT 请求（更新资源）---");

        String jsonBody = "{\"id\":1,\"title\":\"Updated Title\",\"body\":\"Updated Body\",\"userId\":1}";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(BASE_URL + "/posts/1");
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println("状态码: " + response.getCode()); // 200 OK
                String body = EntityUtils.toString(response.getEntity());
                System.out.println("响应体: " + body);
            }
        }
        System.out.println();
    }

    /**
     * 演示 5：DELETE 请求
     */
    private static void demoDelete() throws IOException, ParseException {
        System.out.println("--- 5. DELETE 请求（删除资源）---");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(BASE_URL + "/posts/1");

            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println("状态码: " + response.getCode()); // 200 OK
                String body = EntityUtils.toString(response.getEntity());
                System.out.println("响应体: " + body); // {}
            }
        }
        System.out.println();
    }
}
