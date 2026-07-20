package com.example.httpclient5;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Apache HttpClient 5 进阶演示
 *
 * 演示内容：
 * 1. 连接池配置（PoolingHttpClientConnectionManager）
 * 2. 超时设置（连接超时、响应超时、连接请求超时）
 * 3. 拦截器（请求/响应拦截，记录日志/追加公共头）
 * 4. Cookie 管理（BasicCookieStore、手动设置 Cookie）
 * 5. 基本认证（Basic Auth）
 */
public class HttpClient5AdvancedDemo {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    public static void main(String[] args) throws Exception {
        System.out.println("========== Apache HttpClient 5 进阶演示 ==========\n");

        // 1. 连接池配置
        demoConnectionPool();

        // 2. 超时配置
        demoTimeout();

        // 3. 请求/响应拦截器
        demoInterceptors();

        // 4. Cookie 管理
        demoCookies();

        // 5. Basic 认证
        demoBasicAuth();

        System.out.println("\n========== 进阶演示结束 ==========");
    }

    /**
     * 演示 1：连接池配置
     *
     * HttpClient 5 默认使用连接池，通过 PoolingHttpClientConnectionManager 精细控制：
     * - setMaxTotal：最大总连接数
     * - setDefaultMaxPerRoute：每个路由（Host）的最大连接数
     * - setMaxPerRoute：针对特定路由的最大连接数
     *
     * 生产环境建议将 ConnectionManager 作为单例共享，避免反复创建连接。
     */
    private static void demoConnectionPool() throws Exception {
        System.out.println("--- 1. 连接池配置 ---");

        // 创建连接池管理器
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);             // 最大总连接数
        cm.setDefaultMaxPerRoute(20);    // 每个 Host 默认最大连接数

        // 针对特定 Host 设置更大的连接数
        HttpHost targetHost = new HttpHost("jsonplaceholder.typicode.com", 443, "https");
        cm.setMaxPerRoute(new org.apache.hc.client5.http.HttpRoute(targetHost), 50);

        // 使用连接池构建 Client
        try (CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(cm)
                // 空闲连接超过 30s 自动关闭
                .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))
                .build()) {

            // 模拟发出请求
            try (CloseableHttpResponse response = client.execute(new HttpGet(BASE_URL + "/posts/1"))) {
                System.out.println("连接池请求状态: " + response.getCode());
            }

            // 查看连接池状态
            PoolStats stats = cm.getTotalStats();
            System.out.println("连接池状态 → 可用: " + stats.getAvailable()
                    + ", 租用中: " + stats.getLeased()
                    + ", 等待: " + stats.getPending()
                    + ", 最大: " + stats.getMax());
        }
        System.out.println();
    }

    /**
     * 演示 2：超时设置
     *
     * HttpClient 5 中超时使用 Timeout 类（而非 HttpClient 4 的毫秒 int）：
     * - connectTimeout：建立 TCP 连接的超时
     * - responseTimeout：等待服务器响应的超时（等价于 HC4 的 socketTimeout）
     * - connectionRequestTimeout：从连接池获取连接的超时
     */
    private static void demoTimeout() throws Exception {
        System.out.println("--- 2. 超时配置 ---");

        // RequestConfig 配置超时参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))         // 连接超时 5s
                .setResponseTimeout(Timeout.of(10, TimeUnit.SECONDS))       // 响应超时 10s
                .setConnectionRequestTimeout(Timeout.of(3, TimeUnit.SECONDS)) // 获取连接超时 3s
                .build();

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpGet request = new HttpGet(BASE_URL + "/todos/1");
            // 也可以在单个请求上覆盖全局配置
            request.setConfig(requestConfig);

            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println("超时配置请求状态: " + response.getCode());
                System.out.println("响应体: " + EntityUtils.toString(response.getEntity()));
            }
        }
        System.out.println();
    }

    /**
     * 演示 3：请求/响应拦截器
     *
     * 拦截器用途：
     * - 追加公共请求头（如 Authorization Token、TraceId）
     * - 记录请求/响应日志
     * - 请求重试前修改参数
     *
     * HttpClient 5 的拦截器签名与 HC4 一致，但包名变化：
     * org.apache.hc.core5.http.HttpRequestInterceptor / HttpResponseInterceptor
     */
    private static void demoInterceptors() throws Exception {
        System.out.println("--- 3. 拦截器（公共请求头 + 日志）---");

        try (CloseableHttpClient client = HttpClients.custom()
                // 请求拦截器：追加公共 Header
                .addRequestInterceptorFirst((HttpRequest request, org.apache.hc.core5.http.EntityDetails entity,
                                             HttpContext context) -> {
                    request.addHeader("X-Request-Id", "demo-" + System.currentTimeMillis());
                    request.addHeader("X-App-Version", "1.0.0");
                    System.out.println("[拦截器] 请求: " + request.getMethod() + " " + request.getRequestUri());
                    System.out.println("[拦截器] 已追加公共 Header: X-Request-Id, X-App-Version");
                })
                // 响应拦截器：记录状态码
                .addResponseInterceptorLast((HttpResponse response, org.apache.hc.core5.http.EntityDetails entity,
                                             HttpContext context) -> {
                    System.out.println("[拦截器] 响应状态: " + new StatusLine(response));
                })
                .build()) {

            try (CloseableHttpResponse response = client.execute(new HttpGet(BASE_URL + "/users/1"))) {
                String body = EntityUtils.toString(response.getEntity());
                System.out.println("响应(前100字): " + body.substring(0, Math.min(body.length(), 100)) + "...");
            }
        }
        System.out.println();
    }

    /**
     * 演示 4：Cookie 管理
     *
     * HttpClient 5 提供 BasicCookieStore 进行 Cookie 管理：
     * - 自动接收服务器 Set-Cookie 并在后续请求中携带
     * - 可手动预置 Cookie
     *
     * 注意：CookieStore 并非线程安全，多线程场景应使用 ThreadLocal 或同步包装。
     */
    private static void demoCookies() throws Exception {
        System.out.println("--- 4. Cookie 管理 ---");

        // 创建 CookieStore 并预置 Cookie
        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("session_id", "abc123xyz");
        cookie.setDomain("jsonplaceholder.typicode.com");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build()) {

            // 请求时客户端会自动携带 Cookie
            HttpGet request = new HttpGet(BASE_URL + "/posts/1");

            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println("携带 Cookie 请求状态: " + response.getCode());
                System.out.println("CookieStore 中的 Cookie 数量: " + cookieStore.getCookies().size());
                if (!cookieStore.getCookies().isEmpty()) {
                    cookieStore.getCookies().forEach(c ->
                            System.out.println("  Cookie: " + c.getName() + "=" + c.getValue() + " domain=" + c.getDomain()));
                }
            }
        }
        System.out.println();
    }

    /**
     * 演示 5：HTTP Basic 认证
     *
     * 使用 BasicCredentialsProvider 为指定 Host 绑定用户名/密码，
     * HttpClient 会在收到 401 挑战时自动发送 Authorization 头。
     *
     * 注意：生产环境中推荐改用 Bearer Token（OAuth2），避免明文密码。
     */
    private static void demoBasicAuth() throws Exception {
        System.out.println("--- 5. Basic 认证 ---");

        // 配置凭证提供者
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope("httpbin.org", 80),
                new UsernamePasswordCredentials("user", "pass".toCharArray())
        );

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {

            // httpbin.org/basic-auth/{user}/{pass} 返回 200，否则 401
            HttpGet request = new HttpGet("https://httpbin.org/basic-auth/user/pass");

            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println("Basic Auth 响应状态: " + response.getCode());
                String body = EntityUtils.toString(response.getEntity());
                System.out.println("响应体: " + body.trim());
            }
        }
        System.out.println();
    }
}
