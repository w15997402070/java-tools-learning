package com.example.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Micrometer 进阶演示：
 * - CompositeMeterRegistry（多后端同时写）
 * - PrometheusMeterRegistry（生产最常用）
 * - JmxMeterRegistry（JConsole 查看）
 * - MeterFilter（指标过滤/转换）
 * - FunctionCounter / FunctionTimer（基于已有监控对象的指标）
 */
public class MicrometerAdvancedDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Micrometer 进阶指标 Demo ===\n");

        demoCompositeRegistry();
        demoPrometheusRegistry();
        demoMeterFilter();
        demoFunctionCounterTimer();

        System.out.println("\n=== 所有进阶指标 Demo 完成 ===");
    }

    /**
     * CompositeMeterRegistry：同时写入多个后端
     * Spring Boot Actuator 默认使用此模式
     */
    private static void demoCompositeRegistry() {
        System.out.println("--- 1. CompositeMeterRegistry（多后端聚合） ---");

        // 创建组合注册表，同时写入 Simple + JMX
        CompositeMeterRegistry composite = new CompositeMeterRegistry();

        composite.add(new SimpleMeterRegistry());
        composite.add(new JmxMeterRegistry(
                io.micrometer.jmx.JmxConfig.DEFAULT, Clock.SYSTEM));

        // 向 composite 注册指标 → 自动写入所有子 registry
        Counter c = Counter.builder("composite.demo")
                .tag("type", "test")
                .register(composite);
        c.increment(42);

        System.out.println("  JMX 模式下 Counter 值: " + c.count());
        System.out.println("  （可在 JConsole MBeans → metrics 中查看 composite.demo）");
        System.out.println("  Composite 管理的子 Registry 数量: " + composite.getRegistries().size());
    }

    /**
     * PrometheusMeterRegistry：导出 Prometheus 格式指标
     * 这是云原生环境最常用的后端
     */
    private static void demoPrometheusRegistry() {
        System.out.println("\n--- 2. PrometheusMeterRegistry（Prometheus 格式） ---");

        PrometheusMeterRegistry promRegistry =
                new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // 注册一个 Counter
        Counter httpRequests = Counter.builder("http_server_requests_seconds_count")
                .description("HTTP 请求总数")
                .tag("method", "GET")
                .tag("uri", "/api/users")
                .tag("status", "200")
                .register(promRegistry);

        httpRequests.increment(128);

        // 注册一个 Timer（Prometheus 自动生成 _count/_sum/_max/_bucket）
        Timer timer = Timer.builder("http_server_requests_seconds")
                .description("HTTP 请求耗时")
                .tag("method", "GET")
                .tag("uri", "/api/users")
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(30))
                .register(promRegistry);

        timer.record(50, TimeUnit.MILLISECONDS);
        timer.record(120, TimeUnit.MILLISECONDS);
        timer.record(80, TimeUnit.MILLISECONDS);

        // 输出 Prometheus 文本格式（这就是 /actuator/prometheus 端点返回的内容）
        System.out.println("  [Prometheus scrape 输出示例]");
        String scrape = promRegistry.scrape();
        // 只打印前 15 行
        String[] lines = scrape.split("\n");
        for (int i = 0; i < Math.min(lines.length, 15); i++) {
            if (!lines[i].startsWith("#")) {
                System.out.println("  " + lines[i]);
            }
        }
        System.out.println("  ... (共 " + lines.length + " 行)");
    }

    /**
     * MeterFilter：指标过滤与转换
     * - deny/accept：按名称/tag 过滤
     * - rename：重命名指标
     * - map：修改 tag
     * - maxAllow：限制指标数量
     */
    private static void demoMeterFilter() {
        System.out.println("\n--- 3. MeterFilter（指标过滤与转换） ---");

        MeterRegistry registry = new SimpleMeterRegistry();

        // 配置过滤器链
        registry.config()
                // 过滤：拒绝所有 "jvm." 开头的指标（减少噪音）
                .meterFilter(MeterFilter.deny(id -> id.getName().startsWith("jvm.")))
                // 过滤：只接受 "business." 和 "app." 开头的
                .meterFilter(MeterFilter.accept(id ->
                        id.getName().startsWith("business.") || id.getName().startsWith("app.")))
                // 重命名：app.uptime → app.start.time
                .meterFilter(MeterFilter.rename("app.uptime",
                        new Meter.Id("app.start.time", Tags.empty(), null, null, Meter.Type.OTHER)))
                // 通用 tag 添加
                .commonTags("application", "micrometer-demo", "env", "dev");

        // 这些指标会被接受
        Counter.builder("business.orders").register(registry).increment(10);
        Counter.builder("business.revenue").register(registry).increment(50000);

        // 这个指标会被拒绝（不是 business. 或 app. 开头）
        Counter.builder("other.metric").register(registry).increment(1);

        System.out.println("  Registry 中的 Meter 数量: " + registry.getMeters().size());
        registry.getMeters().forEach(m ->
                System.out.println("    " + m.getId().getName() + " → tags: " + m.getId().getTags()));
        System.out.println("  （注意：other.metric 被过滤规则拒绝了）");
    }

    /**
     * FunctionCounter / FunctionTimer：基于已有监控数据的指标
     * 不修改业务代码，将已有的计数器/耗时监控适配为 Micrometer 指标
     */
    private static void demoFunctionCounterTimer() throws InterruptedException {
        System.out.println("\n--- 4. FunctionCounter & FunctionTimer（适配已有监控） ---");

        MeterRegistry registry = new SimpleMeterRegistry();

        // ---------- FunctionCounter ----------
        // 假设已有 Guava AtomicLong 计数的缓存命中计数器
        AtomicLong cacheHits = new AtomicLong(9999);

        FunctionCounter.builder("cache.hits.total", cacheHits, AtomicLong::get)
                .description("缓存命中总次数")
                .tag("cache", "local")
                .register(registry);

        cacheHits.incrementAndGet();  // 10000
        cacheHits.incrementAndGet();  // 10001

        System.out.println("  缓存命中: " + registry.get("cache.hits.total").functionCounter().count());

        // ---------- FunctionTimer ----------
        // 假设已有 totalTime + count（来自 legacy 监控）
        AtomicLong totalTimeNanos = new AtomicLong(0);
        AtomicLong totalCount = new AtomicLong(0);

        // 模拟业务代码在外部累加
        totalTimeNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(500)); // 500ms
        totalCount.incrementAndGet();

        totalTimeNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(300)); // 300ms
        totalCount.incrementAndGet();

        totalTimeNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(200)); // 200ms
        totalCount.incrementAndGet();

        FunctionTimer.builder("legacy.api.time", totalCount, AtomicLong::get,
                        totalTimeNanos, AtomicLong::get, TimeUnit.NANOSECONDS)
                .description("Legacy API 耗时（通过 FunctionTimer 适配）")
                .register(registry);

        FunctionTimer ft = registry.get("legacy.api.time").functionTimer();
        System.out.printf("  Legacy API 调用次数: %.0f%n", ft.count());
        System.out.printf("  Legacy API 总耗时: %.2f ms%n", ft.totalTime(TimeUnit.MILLISECONDS));
        System.out.printf("  Legacy API 平均耗时: %.2f ms%n", ft.mean(TimeUnit.MILLISECONDS));
    }
}
