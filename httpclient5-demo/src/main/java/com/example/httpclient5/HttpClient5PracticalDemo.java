package com.example.httpclient5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.http.io.entity.UrlEncodedFormEntity;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Apache HttpClient 5 实战演示
 *
 * 演示内容：
 * 1. 单例 HttpClient 工厂（生产推荐模式）
 * 2. 表单提交（application/x-www-form-urlencoded）
 * 3. 多部分表单（multipart/form-data，模拟文件上传）
 * 4. 并发批量请求（线程池 + 共享连接池）
 * 5. JSON 响应反序列化（Jackson 集成）
 * 6. 重试机制与错误处理最佳实践
 * 7. Spring Boot 集成说明（代码注释）
 */
public class HttpClient5PracticalDemo {

    /** 共享连接池（单例，线程安全） */
    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;

    /** 共享 HttpClient（单例）*/
    private static final CloseableHttpClient HTTP_CLIENT;

    /** Jackson ObjectMapper（单例，线程安全） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // ---- 初始化连接池 ----
        CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
        CONNECTION_MANAGER.setMaxTotal(200);
        CONNECTION_MANAGER.setDefaultMaxPerRoute(40);

        // ---- 构建共享 HttpClient ----
        HTTP_CLIENT = HttpClients.custom()
                .setConnectionManager(CONNECTION_MANAGER)
                // 定期驱逐过期和空闲连接，防止"Connection Reset"
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))
                .build();
    }

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    public static void main(String[] args) throws Exception {
        System.out.println("========== Apache HttpClient 5 实战演示 ==========\n");

        // 1. 表单提交
        demoFormSubmit();

        // 2. Multipart 文件上传（模拟）
        demoMultipartUpload();

        // 3. JSON 反序列化
        demoJsonDeserialization();

        // 4. 并发批量请求
        demoConcurrentRequests();

        // 5. 带重试的健壮请求
        demoRetryableRequest();

        System.out.println("\n========== 实战演示结束 ==========");
        System.out.println("\n--- Spring Boot 集成说明 ---");
        printSpringBootIntegration();

        // 关闭共享资源
        HTTP_CLIENT.close();
    }

    /**
     * 演示 1：表单提交（application/x-www-form-urlencoded）
     *
     * 注意：UrlEncodedFormEntity 在 HC5 中位于 org.apache.hc.core5.http.io.entity 包
     */
    private static void demoFormSubmit() throws Exception {
        System.out.println("--- 1. 表单提交（form-urlencoded）---");

        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("username", "admin"));
        formParams.add(new BasicNameValuePair("password", "secret123"));
        formParams.add(new BasicNameValuePair("rememberMe", "true"));

        HttpPost post = new HttpPost(BASE_URL + "/posts");
        // UrlEncodedFormEntity 自动编码参数，Content-Type 设为 application/x-www-form-urlencoded
        post.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(post)) {
            System.out.println("表单提交状态: " + response.getCode());
            System.out.println("响应体: " + EntityUtils.toString(response.getEntity()));
        }
        System.out.println();
    }

    /**
     * 演示 2：Multipart 文件上传（模拟）
     *
     * MultipartEntityBuilder 支持：
     * - addBinaryBody：二进制文件
     * - addTextBody：文本字段
     * - addPart：自定义 ContentBody
     */
    private static void demoMultipartUpload() throws Exception {
        System.out.println("--- 2. Multipart 文件上传（模拟）---");

        // 模拟文件内容（真实场景替换为 new File("path/to/file")）
        byte[] fileContent = "Hello, this is a test file content".getBytes(StandardCharsets.UTF_8);

        HttpPost post = new HttpPost(BASE_URL + "/posts");
        post.setEntity(MultipartEntityBuilder.create()
                .addBinaryBody("file", fileContent, ContentType.TEXT_PLAIN, "test.txt")
                .addTextBody("description", "测试上传文件", ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8))
                .addTextBody("userId", "1")
                .build());

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(post)) {
            System.out.println("Multipart 上传状态: " + response.getCode());
            System.out.println("响应体: " + EntityUtils.toString(response.getEntity()));
        }
        System.out.println();
    }

    /**
     * 演示 3：JSON 响应反序列化（Jackson 集成）
     *
     * 实践建议：定义 DTO 类，使用 objectMapper.readValue() 直接映射，
     * 避免到处写 getString("fieldName")。
     */
    private static void demoJsonDeserialization() throws Exception {
        System.out.println("--- 3. JSON 反序列化（Jackson 集成）---");

        URI uri = new URIBuilder(BASE_URL + "/users/1").build();

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(new HttpGet(uri))) {
            String json = EntityUtils.toString(response.getEntity());
            // 解析为 JsonNode（灵活方式）
            JsonNode node = OBJECT_MAPPER.readTree(json);

            System.out.println("用户ID: " + node.get("id").asInt());
            System.out.println("用户名: " + node.get("username").asText());
            System.out.println("邮箱: " + node.get("email").asText());
            System.out.println("城市: " + node.at("/address/city").asText());
            System.out.println("公司: " + node.at("/company/name").asText());
        }
        System.out.println();
    }

    /**
     * 演示 4：并发批量请求（共享连接池 + 线程池）
     *
     * 核心要点：
     * - CloseableHttpClient 是线程安全的，可以多线程共享
     * - 每个线程独立 execute()，连接池自动分配连接
     * - 结果用 Future 收集，统计成功/失败数量
     */
    private static void demoConcurrentRequests() throws Exception {
        System.out.println("--- 4. 并发批量请求（5并发，10个请求）---");

        ExecutorService executor = Executors.newFixedThreadPool(5);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            final int postId = i;
            futures.add(executor.submit(() -> {
                try {
                    URI uri = new URIBuilder(BASE_URL + "/posts/" + postId).build();
                    try (CloseableHttpResponse resp = HTTP_CLIENT.execute(new HttpGet(uri))) {
                        EntityUtils.consume(resp.getEntity()); // 务必消费响应体，释放连接
                        if (resp.getCode() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("请求 posts/" + postId + " 失败: " + e.getMessage());
                }
                return null;
            }));
        }

        // 等待所有请求完成
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        executor.shutdown();

        System.out.println("并发请求完成 → 成功: " + successCount.get()
                + ", 失败: " + failCount.get()
                + ", 耗时: " + elapsed + "ms");

        // 查看连接池状态
        System.out.println("连接池状态 → " + CONNECTION_MANAGER.getTotalStats());
        System.out.println();
    }

    /**
     * 演示 5：带重试的健壮请求
     *
     * 真实业务中网络抖动不可避免，简单重试策略可大幅提升可用性：
     * - 最多重试 3 次
     * - 指数退避：100ms → 200ms → 400ms
     * - 只重试幂等请求（GET/HEAD/PUT/DELETE）
     */
    private static void demoRetryableRequest() throws Exception {
        System.out.println("--- 5. 带重试的健壮请求 ---");

        String result = executeWithRetry(BASE_URL + "/posts/5", 3);
        if (result != null) {
            JsonNode node = OBJECT_MAPPER.readTree(result);
            System.out.println("重试请求成功 → 文章标题: " + node.get("title").asText());
        }
        System.out.println();
    }

    /**
     * 通用重试方法（GET 请求）
     *
     * @param url       请求 URL
     * @param maxRetry  最大重试次数
     * @return          响应体字符串，失败返回 null
     */
    private static String executeWithRetry(String url, int maxRetry) {
        int attempt = 0;
        long delay = 100; // 初始等待 100ms

        while (attempt <= maxRetry) {
            try {
                try (CloseableHttpResponse response = HTTP_CLIENT.execute(new HttpGet(url))) {
                    int code = response.getCode();
                    String body = EntityUtils.toString(response.getEntity());

                    if (code >= 200 && code < 300) {
                        System.out.println("  第 " + (attempt + 1) + " 次请求成功，状态码: " + code);
                        return body;
                    }

                    // 5xx 服务端错误可重试，4xx 不重试
                    if (code >= 400 && code < 500) {
                        System.err.println("  客户端错误 " + code + "，不再重试");
                        return null;
                    }

                    System.err.println("  第 " + (attempt + 1) + " 次请求失败，状态码: " + code + "，准备重试...");
                }
            } catch (IOException e) {
                System.err.println("  第 " + (attempt + 1) + " 次请求异常: " + e.getMessage() + "，准备重试...");
            }

            attempt++;
            if (attempt <= maxRetry) {
                try {
                    Thread.sleep(delay);
                    delay *= 2; // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        System.err.println("  已达最大重试次数 " + maxRetry + "，请求失败");
        return null;
    }

    /**
     * 演示 6：Spring Boot 集成说明（注释形式）
     *
     * Spring Boot 集成步骤：
     * 1. 引入 spring-boot-starter-web（已包含 httpclient5 自动配置支持）
     * 2. 在 application.properties 中配置连接池参数
     * 3. 注册 RestTemplate（底层使用 HttpClient5）或直接注入 CloseableHttpClient Bean
     */
    private static void printSpringBootIntegration() {
        System.out.println(
            "// ===== Spring Boot 集成最佳实践 =====\n" +
            "\n" +
            "// 1. pom.xml 依赖\n" +
            "// <dependency>\n" +
            "//   <groupId>org.apache.httpcomponents.client5</groupId>\n" +
            "//   <artifactId>httpclient5</artifactId>\n" +
            "//   <version>5.3.1</version>\n" +
            "// </dependency>\n" +
            "\n" +
            "// 2. 注册 Bean\n" +
            "// @Bean\n" +
            "// public CloseableHttpClient httpClient() {\n" +
            "//     PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();\n" +
            "//     cm.setMaxTotal(200);\n" +
            "//     cm.setDefaultMaxPerRoute(40);\n" +
            "//     return HttpClients.custom()\n" +
            "//             .setConnectionManager(cm)\n" +
            "//             .evictExpiredConnections()\n" +
            "//             .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))\n" +
            "//             .build();\n" +
            "// }\n" +
            "\n" +
            "// 3. 注册 RestTemplate（使用 HC5 作为底层）\n" +
            "// @Bean\n" +
            "// public RestTemplate restTemplate(CloseableHttpClient httpClient) {\n" +
            "//     HttpComponentsClientHttpRequestFactory factory =\n" +
            "//             new HttpComponentsClientHttpRequestFactory(httpClient);\n" +
            "//     return new RestTemplate(factory);\n" +
            "// }\n" +
            "\n" +
            "// 4. 在 Service 层注入使用\n" +
            "// @Autowired\n" +
            "// private RestTemplate restTemplate;\n" +
            "// String result = restTemplate.getForObject(url, String.class);"
        );
    }
}
