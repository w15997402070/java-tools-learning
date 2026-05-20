package com.example.resilience4j;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Resilience4j 进阶功能演示
 *
 * 涵盖：
 * 1. Bulkhead（舱壁隔离）     - 限制并发调用数，隔离故障
 * 2. 组合使用（Decorator链）   - 多个弹性组件叠加
 * 3. 降级回退（Fallback）      - 失败后返回默认值
 * 4. CircuitBreaker 高级配置  - 慢调用检测、半开探测
 */
public class Resilience4jAdvancedDemo {

    // ===== 1. Bulkhead 舱壁隔离演示 =====

    /**
     * Bulkhead 模式：将资源池分隔，防止某个调用耗尽所有线程
     * Resilience4j 提供两种实现：
     * - SemaphoreBulkhead（信号量，默认）：同一线程，限制并发数
     * - ThreadPoolBulkhead：独立线程池，完全隔离（需引入额外依赖）
     */
    static void bulkheadDemo() throws InterruptedException {
        System.out.println("\n===== 1. Bulkhead 舱壁隔离演示 =====");

        // 1.1 配置：最多 3 个并发，等待队列深度 1
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(3)                      // 最多同时 3 个并发
                .maxWaitDuration(Duration.ofMillis(100))    // 超过时等待 100ms
                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(config);
        Bulkhead bulkhead = registry.bulkhead("thirdPartyApi");

        // 1.2 注册事件监听
        bulkhead.getEventPublisher()
                .onCallPermitted(e -> System.out.println("  [许可] 调用被允许"))
                .onCallRejected(e -> System.out.println("  [拒绝] 舱壁满载，调用被拒绝"))
                .onCallFinished(e -> System.out.println("  [完成] 调用结束"));

        // 1.3 模拟 6 个并发请求，只有 3 个能进入
        System.out.println("发起 6 个并发请求（最大并发=3）：");
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch latch = new CountDownLatch(6);

        for (int i = 1; i <= 6; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    String result = bulkhead.executeSupplier(() -> {
                        // 模拟慢接口（100ms）
                        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                        return "第三方API响应 #" + idx;
                    });
                    System.out.println("  请求 #" + idx + " 成功: " + result);
                } catch (BulkheadFullException e) {
                    System.out.println("  请求 #" + idx + " 被舱壁拒绝（并发超限）");
                } catch (Exception e) {
                    System.out.println("  请求 #" + idx + " 异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(3, TimeUnit.SECONDS);
        executor.shutdown();

        // 1.4 指标
        Bulkhead.Metrics metrics = bulkhead.getMetrics();
        System.out.println("\n--- 舱壁指标 ---");
        System.out.println("当前并发调用数: " + metrics.getAvailableConcurrentCalls());
        System.out.println("最大并发调用数: " + metrics.getMaxAllowedConcurrentCalls());
    }

    // ===== 2. 组合使用（Decorator 链）=====

    /**
     * 将多个弹性组件叠加应用到同一个操作
     * 推荐顺序（从外到内）：
     * CircuitBreaker → RateLimiter → Retry → TimeLimiter → Bulkhead → 实际调用
     *
     * 注：Resilience4j 提供 Decorators 工具类实现链式包装
     */
    static void decoratorChainDemo() {
        System.out.println("\n===== 2. Decorator 链（组合使用）演示 =====");

        // 创建各组件
        CircuitBreaker cb = CircuitBreaker.ofDefaults("combinedCB");
        Retry retry = Retry.of("combinedRetry",
                RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofMillis(100)).build());
        RateLimiter rl = RateLimiter.of("combinedRL",
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .limitForPeriod(5)
                        .timeoutDuration(Duration.ofMillis(200))
                        .build());

        // 模拟一个"不稳定的外部服务"
        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<String> remoteCall = () -> {
            int n = callCount.incrementAndGet();
            if (n % 3 != 0) throw new RuntimeException("服务不稳定，第" + n + "次");
            return "外部服务响应（第" + n + "次调用成功）";
        };

        // 使用 Decorators 链式包装
        // 注意：Java 8 需要手动嵌套，Resilience4j Decorators 工具类更优雅
        Supplier<String> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(cb,
                        RateLimiter.decorateSupplier(rl, remoteCall)));

        System.out.println("发起 5 次调用（Retry + CircuitBreaker + RateLimiter 叠加）：");
        for (int i = 1; i <= 5; i++) {
            try {
                String result = decorated.get();
                System.out.println("第" + i + "次: " + result);
            } catch (CallNotPermittedException e) {
                System.out.println("第" + i + "次: 熔断拦截");
            } catch (RequestNotPermitted e) {
                System.out.println("第" + i + "次: 限流拦截");
            } catch (Exception e) {
                System.out.println("第" + i + "次: 最终失败 - " + e.getMessage());
            }
        }
    }

    // ===== 3. Fallback 降级回退演示 =====

    /**
     * Resilience4j 本身不提供 @Fallback 注解（那是 Spring Cloud 的功能）
     * 但通过 Try.ofSupplier() 或 executeSupplier + catch 可以实现降级
     *
     * 常见模式：
     * - 返回缓存数据
     * - 返回默认值
     * - 调用备用服务
     */
    static void fallbackDemo() {
        System.out.println("\n===== 3. Fallback 降级回退演示 =====");

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("userService");

        // 3.1 使用 Try（vavr）实现优雅降级
        System.out.println("方式1：使用 Try 实现降级");
        for (int i = 1; i <= 8; i++) {
            final int idx = i;
            String result = io.vavr.control.Try.ofSupplier(
                    CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
                        if (idx % 2 == 0) throw new RuntimeException("用户服务不可用");
                        return "用户信息-" + idx;
                    })
            ).recover(throwable -> {
                // 降级：返回缓存/默认数据
                if (throwable instanceof CallNotPermittedException) {
                    return "【熔断降级】默认用户信息（来自缓存）";
                }
                return "【异常降级】用户信息获取失败，返回默认值";
            }).get();

            System.out.println("  第" + i + "次: " + result);
        }

        // 3.2 传统 try-catch 方式降级
        System.out.println("\n方式2：try-catch 降级");
        CircuitBreaker cb2 = CircuitBreaker.ofDefaults("productService");
        for (int i = 1; i <= 3; i++) {
            try {
                String product = cb2.executeSupplier(() -> {
                    throw new RuntimeException("商品服务超时");
                });
                System.out.println("  商品信息: " + product);
            } catch (Exception e) {
                // 降级处理
                String fallbackResult = getFallbackProduct();
                System.out.println("  降级结果: " + fallbackResult + "（原因：" + e.getMessage() + "）");
            }
        }
    }

    /** 降级方法：返回兜底商品信息 */
    private static String getFallbackProduct() {
        return "热门推荐商品列表（来自本地缓存）";
    }

    // ===== 4. CircuitBreaker 慢调用检测 =====

    /**
     * 除了失败率，还可以基于慢调用率触发熔断
     * 当慢调用（超过阈值耗时）占比过高时，同样认为服务不健康
     */
    static void slowCallDetectionDemo() throws InterruptedException {
        System.out.println("\n===== 4. CircuitBreaker 慢调用检测演示 =====");

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                // 超过 200ms 视为慢调用
                .slowCallDurationThreshold(Duration.ofMillis(200))
                // 慢调用率超过 60% 触发熔断
                .slowCallRateThreshold(60.0f)
                // 失败率阈值（设高避免影响演示）
                .failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofSeconds(3))
                .build();

        CircuitBreaker cb = CircuitBreaker.of("slowCallCB", config);
        cb.getEventPublisher()
                .onSlowCallRateExceeded(e -> System.out.println("  >>> 慢调用率超标: " + e.getSlowCallRate() + "%"))
                .onStateTransition(e -> System.out.println("  >>> 状态变化: " + e.getStateTransition()));

        System.out.println("发起 5 次调用（其中 4 次故意延迟 300ms）：");
        for (int i = 1; i <= 7; i++) {
            final int idx = i;
            try {
                String result = cb.executeSupplier(() -> {
                    // 前4次模拟慢响应
                    if (idx <= 4) {
                        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                        return "慢响应 #" + idx + "（300ms）";
                    }
                    return "正常响应 #" + idx;
                });
                System.out.println("  第" + i + "次: " + result);
            } catch (CallNotPermittedException e) {
                System.out.println("  第" + i + "次: 熔断器打开，请求被拒绝");
            } catch (Exception e) {
                System.out.println("  第" + i + "次: 异常 - " + e.getMessage());
            }
        }

        System.out.println("\n最终状态: " + cb.getState());
        CircuitBreaker.Metrics m = cb.getMetrics();
        System.out.println("慢调用次数: " + m.getNumberOfSlowCalls());
        System.out.println("慢调用率: " + m.getSlowCallRate() + "%");
    }

    public static void main(String[] args) throws InterruptedException {
        bulkheadDemo();
        decoratorChainDemo();
        fallbackDemo();
        slowCallDetectionDemo();
    }
}
