package com.example.micrometer;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer 实战演示：
 * - 电商订单处理全链路指标埋点
 * - Prometheus HTTP 端点暴露（模拟 /actuator/prometheus）
 * - 性能影响测试
 * - Spring Boot 集成指南（注释形式）
 */
public class MicrometerPracticalDemo {

    // ========== 指标定义 ==========
    private static final PrometheusMeterRegistry registry =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    // 订单 Counter
    private static final Counter ordersCreated = Counter.builder("shop.orders.created")
            .description("下单总数")
            .tag("shop", "main")
            .register(registry);

    private static final Counter ordersPaid = Counter.builder("shop.orders.paid")
            .description("支付成功数")
            .tag("shop", "main")
            .register(registry);

    private static final Counter ordersFailed = Counter.builder("shop.orders.failed")
            .description("订单失败数")
            .tag("shop", "main")
            .register(registry);

    // 各阶段耗时 Timer
    private static final Timer orderCreationTime = Timer.builder("shop.order.creation.time")
            .description("订单创建耗时")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

    private static final Timer paymentTime = Timer.builder("shop.order.payment.time")
            .description("支付处理耗时")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

    private static final Timer inventoryCheckTime = Timer.builder("shop.inventory.check.time")
            .description("库存查询耗时")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

    // 库存 Gauge
    private static final AtomicInteger inventoryLevel = new AtomicInteger(10000);

    static {
        Gauge.builder("shop.inventory.level", inventoryLevel, AtomicInteger::get)
                .description("当前库存水平")
                .tag("sku", "DEFAULT")
                .register(registry);
    }

    // ========== 业务模拟 ==========
    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Micrometer 实战：电商订单全链路指标监控 ===\n");

        // 启动 Prometheus HTTP 端点
        startPrometheusEndpoint(8080);

        // 模拟 60 秒订单流
        System.out.println("模拟 20 轮订单处理（每轮间隔 1 秒）...\n");
        for (int round = 1; round <= 20; round++) {
            // 每轮随机 1~5 个订单
            int batchSize = 1 + random.nextInt(5);
            for (int i = 0; i < batchSize; i++) {
                processOrder("ORDER-" + round + "-" + i);
            }
            Thread.sleep(1000);
        }

        // 打印汇总指标
        printSummary();

        System.out.println("\nPrometheus 端点持续运行中：http://localhost:8080/metrics");
        System.out.println("按 Ctrl+C 停止");
    }

    /**
     * 模拟订单处理全流程
     */
    private static void processOrder(String orderId) {
        ordersCreated.increment();

        // 1. 创建订单（耗时 10~100ms）
        orderCreationTime.record(() -> sleepRandom(10, 100));

        // 2. 库存检查（耗时 5~30ms）
        inventoryCheckTime.record(() -> sleepRandom(5, 30));

        // 3. 扣减库存
        int after = inventoryLevel.addAndGet(-random.nextInt(3));
        if (after < 0) inventoryLevel.set(0);

        // 4. 支付（耗时 30~500ms），10% 概率失败
        boolean paySuccess = random.nextInt(100) >= 10;
        paymentTime.record(() -> sleepRandom(30, 500));

        if (paySuccess) {
            ordersPaid.increment();
        } else {
            ordersFailed.increment();
            // 支付失败，恢复库存
            inventoryLevel.addAndGet(random.nextInt(3));
        }
    }

    /**
     * 启动 HTTP 服务器，暴露 /metrics 端点（Prometheus 格式）
     */
    private static void startPrometheusEndpoint(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/metrics", exchange -> {
            String scrape = registry.scrape();
            byte[] bytes = scrape.getBytes("UTF-8");

            exchange.getResponseHeaders().set("Content-Type",
                    "text/plain; charset=UTF-8; version=0.0.4");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // 健康检查端点
        server.createContext("/health", exchange -> {
            String body = "{\"status\":\"UP\"}";
            byte[] bytes = body.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("✓ Prometheus 端点已启动: http://localhost:" + port + "/metrics\n");
    }

    /**
     * 打印汇总指标
     */
    private static void printSummary() {
        System.out.println("\n==================== 订单处理汇总 ====================");

        System.out.printf("  下单总数:    %.0f%n", ordersCreated.count());
        System.out.printf("  支付成功:    %.0f%n", ordersPaid.count());
        System.out.printf("  支付失败:    %.0f%n", ordersFailed.count());
        System.out.printf("  成功率:      %.1f%%%n",
                ordersPaid.count() / ordersCreated.count() * 100);
        System.out.printf("  剩余库存:    %d%n", inventoryLevel.get());

        System.out.println("\n  --- 各阶段耗时 ---");
        System.out.printf("  订单创建:     avg=%.2f ms,  p99=%.2f ms%n",
                orderCreationTime.mean(TimeUnit.MILLISECONDS),
                orderCreationTime.percentile(0.99, TimeUnit.MILLISECONDS));
        System.out.printf("  库存查询:     avg=%.2f ms,  p99=%.2f ms%n",
                inventoryCheckTime.mean(TimeUnit.MILLISECONDS),
                inventoryCheckTime.percentile(0.99, TimeUnit.MILLISECONDS));
        System.out.printf("  支付处理:     avg=%.2f ms,  p99=%.2f ms%n",
                paymentTime.mean(TimeUnit.MILLISECONDS),
                paymentTime.percentile(0.99, TimeUnit.MILLISECONDS));

        System.out.println("\n  --- Prometheus scrape 地址 ---");
        System.out.println("  curl http://localhost:8080/metrics");
        System.out.println("=======================================================");

        // 性能基准说明
        System.out.println("\n  性能基准参考（来自 Micrometer 官方）：");
        System.out.println("  - Counter.increment():       ~30 ns/op");
        System.out.println("  - Timer.record(Runnable):    ~100 ns/op (不含业务耗时)");
        System.out.println("  - Gauge 监控:                几乎零开销（仅在 scrape 时读取）");
        System.out.println("  - Prometheus scrape:         < 5ms（正常规模 <1000 metrics）");
    }

    private static void sleepRandom(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + random.nextInt(maxMs - minMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ============================================================
    // Spring Boot 集成指南（注释形式）
    // ============================================================
    /*
     * ## Spring Boot 2.x / 3.x 与 Micrometer 集成
     *
     * ### 1. Maven 依赖
     *
     * ```xml
     * <!-- Spring Boot Actuator（内置 Micrometer） -->
     * <dependency>
     *     <groupId>org.springframework.boot</groupId>
     *     <artifactId>spring-boot-starter-actuator</artifactId>
     * </dependency>
     *
     * <!-- Prometheus 导出（Spring Boot 2.x 用 micrometer-registry-prometheus） -->
     * <dependency>
     *     <groupId>io.micrometer</groupId>
     *     <artifactId>micrometer-registry-prometheus</artifactId>
     * </dependency>
     * ```
     *
     * ### 2. application.yml 配置
     *
     * ```yaml
     * management:
     *   endpoints:
     *     web:
     *       exposure:
     *         include: health,prometheus,metrics  # 暴露端点
     *   metrics:
     *     export:
     *       prometheus:
     *         enabled: true
     *     tags:
     *       application: ${spring.application.name}  # 全局 tag
     * ```
     *
     * ### 3. 自定义指标
     *
     * ```java
     * @RestController
     * public class OrderController {
     *
     *     private final MeterRegistry meterRegistry;  // 注入全局 registry
     *
     *     public OrderController(MeterRegistry meterRegistry) {
     *         this.meterRegistry = meterRegistry;
     *     }
     *
     *     @PostMapping("/orders")
     *     public String createOrder() {
     *         // Counter 自动暴露为 /actuator/prometheus
     *         meterRegistry.counter("orders.created", "shop", "main").increment();
     *         return "OK";
     *     }
     * }
     * ```
     *
     * ### 4. 使用 @Timed 注解（需要 AOP）
     *
     * ```java
     * @Configuration
     * public class MetricsConfig {
     *     @Bean
     *     public TimedAspect timedAspect(MeterRegistry registry) {
     *         return new TimedAspect(registry);
     *     }
     * }
     *
     * @Service
     * public class PaymentService {
     *     @Timed(value = "payment.process", percentiles = {0.5, 0.95, 0.99})
     *     public void process() { ... }
     * }
     * ```
     *
     * ### 5. 对接 Grafana
     *
     * - Prometheus 抓取 /actuator/prometheus
     * - Grafana 数据源配置 Prometheus URL
     * - 导入 Micrometer JVM 监控面板（ID: 4701）
     *
     * ### 6. 常见问题
     *
     * - 指标名不要用 '.' 而用 '_'？Prometheus 会自动转换，但建议统一
     * - high-cardinality tag 会导致 Prometheus 存储膨胀，避免用 userId 等
     * - Timer 默认 1/5/10/30/60s 的 SLO buckets，可通过
     *   management.metrics.distribution.slo 自定义
     */
}
