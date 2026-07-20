package com.example.logback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Logback 实战 Demo —— 生产环境最佳实践。
 *
 * 涵盖：
 * 1. JSON 格式日志（配合 logstash-logback-encoder，输出到 logs/app-json.log）
 * 2. 滚动归档策略（按天 + 按大小）
 * 3. 异步日志（AsyncAppender，不阻塞业务线程）
 * 4. 错误告警模式（ERROR 级别自动收集关键上下文）
 * 5. Spring Boot 集成指南（通过 application.yml 配置）
 * 6. 性能注意事项
 */
public class LogbackPracticalDemo {

    private static final Logger log = LoggerFactory.getLogger(LogbackPracticalDemo.class);

    public static void main(String[] args) throws Exception {
        System.out.println("=== Logback 实战 Demo ===\n");
        System.out.println("日志输出位置：");
        System.out.println("  控制台：实时输出（含颜色高亮）");
        System.out.println("  文件：logs/app.log（普通文本格式）");
        System.out.println("  文件：logs/app-json.log（JSON格式，供日志中心采集）");
        System.out.println("  文件：logs/error.log（仅 ERROR 级别）");
        System.out.println();

        // ---- 1. 模拟 Web 服务日志全景 ----
        demoWebServiceLogging();

        // ---- 2. 异步日志压力测试 ----
        demoAsyncLogging();

        // ---- 3. 错误上下文收集 ----
        demoErrorContextCapture();

        // ---- 4. 结构化日志最佳实践 ----
        demoStructuredLogging();

        System.out.println("\n✅ LogbackPracticalDemo 完成！");
        System.out.println("请查看 logs/ 目录下的日志文件。");
    }

    /**
     * 模拟真实 Web 服务的完整日志流。
     *
     * 一个请求的理想日志链路：
     *   INFO  [traceId=xxx] 请求进入 → 参数校验 → 业务处理 → DB操作 → 
     *   INFO  [traceId=xxx] 下游调用 → 响应返回 → 耗时统计
     */
    private static void demoWebServiceLogging() {
        System.out.println("---- 1. Web 服务全链路日志 ----");

        // 模拟 3 个并发请求
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 3; i++) {
            final int reqNum = i;
            executor.submit(() -> processRequest(reqNum));
        }
        executor.shutdown();
        try { executor.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        pause();
    }

    private static void processRequest(int reqNum) {
        String traceId = "REQ-" + String.format("%05d", reqNum);
        MDC.put("traceId", traceId);
        MDC.put("userId", "user_" + reqNum);

        long start = System.currentTimeMillis();
        log.info("【请求入口】POST /api/order/create");

        // 参数校验
        log.debug("参数校验通过：productId=P001, quantity=2");

        // 业务处理
        log.info("库存扣减完成：sku={}, before={}, after={}", "SKU-001", 100, 98);

        // 数据库操作
        log.info("订单写入数据库：orderId=ORD-20240620-{}", String.format("%03d", reqNum));

        // 调用下游（模拟耗时）
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        log.info("调用支付服务完成：payStatus=SUCCESS, payAmount=25998元");

        long cost = System.currentTimeMillis() - start;
        log.info("【请求结束】cost={}ms, status=200", cost);

        MDC.clear();
    }

    /**
     * 异步日志压力测试。
     *
     * 对比：同步 appender vs 异步 appender
     *   - 同步：log.info() 调用会等待磁盘 I/O 完成才返回
     *   - 异步：log.info() 将事件放入队列立即返回，后台线程异步写入
     *
     * Logback 配置异步需要在 logback.xml 中使用：
     *   <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
     *       <appender-ref ref="FILE" />
     *       <queueSize>512</queueSize>
     *       <discardingThreshold>0</discardingThreshold>
     *       <neverBlock>true</neverBlock>
     *   </appender>
     *
     * 注意：
     *   - queueSize：队列容量，满时默认丢弃 TRACE/DEBUG/INFO（可调整）
     *   - neverBlock=true：队列满时不阻塞调用线程（丢弃事件）
     *   - 生产环境建议 neverBlock=false，宁可阻塞也不丢日志
     */
    private static void demoAsyncLogging() {
        System.out.println("---- 2. 异步日志压测（5000条） ----");

        long start = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            log.info("业务日志 #{}：处理记录 {}", i, "data-" + i);
        }
        long cost = System.currentTimeMillis() - start;
        System.out.println("   写入 5000 条日志耗时：" + cost + "ms");
        System.out.println("   （如果使用 AsyncAppender，业务线程几乎无阻塞）");

        pause();
    }

    /**
     * ERROR 级别日志应携带足够的上下文，便于快速定位问题。
     *
     * 最佳实践：
     *   1. 带上 traceId/requestId（由 MDC 自动注入）
     *   2. 带上关键业务参数（userId, orderId, sku 等）
     *   3. 带上异常堆栈（放在最后一个参数）
     *   4. 避免在 ERROR 日志中写敏感信息（密码、Token、身份证号）
     */
    private static void demoErrorContextCapture() {
        System.out.println("---- 3. 错误日志上下文收集 ----");

        MDC.put("traceId", "ERR-DEMO-001");
        MDC.put("userId", "88888");

        try {
            // 模拟第三方支付接口调用失败
            callPaymentApi("ORD-20240620-999");
        } catch (Exception e) {
            // ✅ 好的错误日志：上下文丰富
            log.error("支付接口调用失败，已触发自动重试。" +
                            " orderId={}, amount={}, payChannel={}, retryCount={}",
                    "ORD-20240620-999", 25998, "WECHAT_PAY", 2, e);

            // 也可以使用 Marker 将错误日志单独路由
            // log.error(ERROR_MARKER, "...", e);
        }

        MDC.clear();
        pause();
    }

    /**
     * 结构化日志最佳实践 —— 方便 ELK/Splunk 等日志中心解析和查询。
     *
     * 关键字段规范（建议团队统一）：
     *   traceId     - 分布式链路追踪ID
     *   spanId      - 当前 Span ID
     *   userId      - 操作用户ID
     *   requestUri  - 请求路径
     *   method      - HTTP 方法
     *   statusCode  - HTTP 状态码
     *   costMs      - 耗时（毫秒）
     *   errorCode   - 业务错误码
     *   errorMsg    - 错误消息
     */
    private static void demoStructuredLogging() {
        System.out.println("---- 4. 结构化日志最佳实践 ----");

        // 使用键值对风格的日志（方便 grep / 日志中心解析）
        log.info("API_CALL | method=GET | uri=/api/user/info | status=200 | cost=12ms");

        // 在 MDC 中设置搜索维度
        MDC.put("traceId", "trace-abc-123");
        MDC.put("spanId", "span-xyz-456");
        MDC.put("userId", "10086");
        MDC.put("requestUri", "/api/order/query");
        MDC.put("method", "GET");

        log.info("订单查询：orderCount=15");
        log.info("API_RESPONSE | statusCode=200 | costMs=89");

        MDC.clear();
        pause();
    }

    /**
     * 模拟第三方支付接口调用。
     */
    private static void callPaymentApi(String orderId) throws Exception {
        // 模拟网络超时
        throw new RuntimeException("支付网关连接超时：connect timed out after 3000ms");
    }

    private static void pause() {
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        System.out.println("----------------------------");
    }
}
