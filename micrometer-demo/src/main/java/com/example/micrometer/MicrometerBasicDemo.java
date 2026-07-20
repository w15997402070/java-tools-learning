package com.example.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer 基础演示：Counter / Gauge / Timer / DistributionSummary
 *
 * 核心概念：
 * - Counter：只增不减的计数（如：请求总数、错误次数）
 * - Gauge：瞬时值，可增可减（如：队列长度、CPU使用率）
 * - Timer：耗时统计，自动记录 count/sum/max/percentiles（如：接口响应时间）
 * - DistributionSummary：分布统计（如：请求体大小）
 */
public class MicrometerBasicDemo {

    // 全局简单注册表（生产用 CompositeMeterRegistry 对接 Prometheus 等）
    private static final MeterRegistry registry = new SimpleMeterRegistry();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Micrometer 基础指标 Demo ===\n");

        demoCounter();
        demoGauge();
        demoTimer();
        demoDistributionSummary();
        demoGlobalStaticRegistry();

        System.out.println("\n=== 所有基础指标 Demo 完成 ===");
    }

    /**
     * Counter：只增不减的计数器
     * 典型场景：API 调用次数、订单创建数、异常计数
     */
    private static void demoCounter() throws InterruptedException {
        System.out.println("--- 1. Counter（计数器） ---");

        // 创建 Counter，带 tags 便于多维度过滤
        Counter orderCounter = Counter.builder("orders.created")
                .description("订单创建总数")
                .tag("channel", "app")      // 维度：来源渠道
                .tag("region", "cn")        // 维度：地区
                .register(registry);

        Counter errorCounter = Counter.builder("orders.errors")
                .description("订单创建失败数")
                .tag("channel", "app")
                .register(registry);

        // 模拟：5 次成功，2 次失败
        for (int i = 1; i <= 5; i++) {
            orderCounter.increment();
            if (i == 3 || i == 5) {
                errorCounter.increment();
            }
        }

        // 也可以一次性加 N
        orderCounter.increment(10);

        System.out.println("  订单创建总数: " + orderCounter.count());
        System.out.println("  订单失败次数: " + errorCounter.count());

        // 打印 Prometheus 格式输出
        System.out.println("  [Prometheus 格式]");
        System.out.println("  orders_created_total{channel=\"app\",region=\"cn\"} " + orderCounter.count());
        System.out.println("  orders_errors_total{channel=\"app\"} " + errorCounter.count());
    }

    /**
     * Gauge：瞬时值（可增可减），每次获取时动态计算
     * 典型场景：队列大小、活跃连接数、内存用量
     */
    private static void demoGauge() throws InterruptedException {
        System.out.println("\n--- 2. Gauge（仪表盘/瞬时值） ---");

        // 使用 AtomicInteger 作为数据源
        AtomicInteger queueSize = new AtomicInteger(50);

        Gauge.builder("queue.size", queueSize, AtomicInteger::get)
                .description("消息队列当前长度")
                .tag("queue", "order-process")
                .register(registry);

        System.out.println("  初始队列长度: " + registry.get("queue.size").gauge());

        // 模拟消费消息
        queueSize.set(30);
        System.out.println("  消费后队列长度: " + registry.get("queue.size").gauge());

        queueSize.set(5);
        System.out.println("  快消完时的队列长度: " + registry.get("queue.size").gauge());

        // 多维度 Gauge：JVM 内存示例
        Gauge.builder("jvm.memory.used", Runtime.getRuntime(), rt -> rt.totalMemory() - rt.freeMemory())
                .description("JVM 已用内存（bytes）")
                .baseUnit("bytes")
                .register(registry);

        System.out.printf("  JVM 已用内存: %.2f MB%n",
                registry.get("jvm.memory.used").gauge().value() / 1024.0 / 1024.0);
    }

    /**
     * Timer：耗时 + 调用次数统计
     * 典型场景：HTTP 请求耗时、DB 查询耗时、缓存命中耗时
     */
    private static void demoTimer() throws InterruptedException {
        System.out.println("\n--- 3. Timer（计时器） ---");

        Timer dbQueryTimer = Timer.builder("db.query.time")
                .description("数据库查询耗时")
                .tag("db", "mysql")
                .tag("table", "orders")
                .publishPercentiles(0.5, 0.95, 0.99)  // P50/P95/P99
                .publishPercentileHistogram()           // 启用直方图
                .register(registry);

        // 模拟 5 次查询，耗时从 10ms 到 300ms
        long[] latencies = {50, 80, 120, 200, 300};
        for (long latency : latencies) {
            dbQueryTimer.record(latency, TimeUnit.MILLISECONDS);
        }

        // 也可以用 lambda 自动计时
        dbQueryTimer.record(() -> {
            try { Thread.sleep(25); } catch (InterruptedException ignored) {}
        });

        // 或用 Timer.Sample 手动控制计时起止
        Timer.Sample sample = Timer.start(registry);
        Thread.sleep(60);
        sample.stop(dbQueryTimer);  // 记录到同一个 Timer

        System.out.println("  查询总次数: " + dbQueryTimer.count());
        System.out.printf("  平均耗时: %.2f ms%n",
                dbQueryTimer.mean(TimeUnit.MILLISECONDS));
        System.out.printf("  最大耗时: %.2f ms%n",
                dbQueryTimer.max(TimeUnit.MILLISECONDS));
        System.out.printf("  总耗时: %.2f ms%n",
                dbQueryTimer.totalTime(TimeUnit.MILLISECONDS));
    }

    /**
     * DistributionSummary：分布统计（非耗时）
     * 典型场景：请求体大小、响应体大小、单次拉取消息数
     */
    private static void demoDistributionSummary() {
        System.out.println("\n--- 4. DistributionSummary（分布统计） ---");

        DistributionSummary requestSize = DistributionSummary.builder("http.request.size")
                .description("HTTP 请求体大小")
                .baseUnit("bytes")
                .tag("endpoint", "/api/orders")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        int[] sizes = {1024, 2048, 512, 8192, 4096, 1024, 2048};
        for (int size : sizes) {
            requestSize.record(size);
        }

        System.out.println("  记录次数: " + requestSize.count());
        System.out.printf("  平均大小: %.2f bytes%n", requestSize.mean());
        System.out.printf("  最大大小: %.2f bytes%n", requestSize.max());
        System.out.printf("  总大小: %.2f bytes%n", requestSize.totalAmount());
    }

    /**
     * 全局静态注册表（无需自己管理 registry 实例）
     * 适合快速原型，生产建议显式传入 registry
     */
    private static void demoGlobalStaticRegistry() {
        System.out.println("\n--- 5. 全局静态注册表 Metrics.globalRegistry ---");

        // 添加 SimpleMeterRegistry 到全局
        Metrics.addRegistry(new SimpleMeterRegistry());

        Counter globalCounter = Metrics.counter("global.requests", "type", "api");
        globalCounter.increment(100);

        System.out.println("  全局 Counter 值: " + globalCounter.count());
        System.out.println("  （注意：生产环境应使用 CompositeMeterRegistry + Prometheus 等后端）");
    }
}
