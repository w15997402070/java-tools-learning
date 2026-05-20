package com.example.resilience4j;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Resilience4j 基础功能演示
 *
 * 涵盖三大核心模块：
 * 1. CircuitBreaker（熔断器）- 防止级联失败
 * 2. RateLimiter（限流器）  - 控制访问速率
 * 3. Retry（重试）           - 自动重试失败操作
 */
public class Resilience4jBasicDemo {

    // ===== 1. CircuitBreaker 熔断器演示 =====

    /**
     * 熔断器三种状态：
     * CLOSED（关闭）  → 正常放行请求
     * OPEN（打开）    → 短路，直接拒绝请求
     * HALF_OPEN（半开）→ 试探性放行少量请求
     */
    static void circuitBreakerDemo() {
        System.out.println("\n===== 1. CircuitBreaker 熔断器演示 =====");

        // 1.1 构建熔断器配置
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // 失败率阈值：50%（10次中有5次失败即触发熔断）
                .failureRateThreshold(50)
                // 滑动窗口大小：10次请求
                .slidingWindowSize(10)
                // HALF_OPEN 状态下允许的探测请求数
                .permittedNumberOfCallsInHalfOpenState(3)
                // 熔断后等待多久进入 HALF_OPEN 状态
                .waitDurationInOpenState(Duration.ofSeconds(5))
                // 将哪些异常记录为失败（默认所有 Exception）
                .recordExceptions(RuntimeException.class)
                .build();

        // 1.2 通过 Registry 管理（推荐，可统一监控）
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker("paymentService");

        System.out.println("初始状态: " + circuitBreaker.getState());  // CLOSED

        // 1.3 注册状态变化监听器
        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        System.out.println(">>> 状态变化: " + event.getStateTransition()));

        // 1.4 模拟调用：前5次成功，后5次失败 → 应触发熔断
        AtomicInteger callCount = new AtomicInteger(0);

        for (int i = 1; i <= 15; i++) {
            final int idx = i;
            try {
                String result = circuitBreaker.executeSupplier(() -> {
                    int n = callCount.incrementAndGet();
                    if (n > 5 && n <= 10) {
                        throw new RuntimeException("模拟服务故障 #" + idx);
                    }
                    return "支付成功 #" + idx;
                });
                System.out.println("第" + i + "次调用: " + result);
            } catch (Exception e) {
                System.out.println("第" + i + "次调用失败: " + e.getMessage());
            }

            // 短暂停顿，避免日志刷太快
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        // 1.5 查看熔断器指标
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        System.out.println("\n--- 熔断器指标 ---");
        System.out.println("失败率: " + metrics.getFailureRate() + "%");
        System.out.println("成功次数: " + metrics.getNumberOfSuccessfulCalls());
        System.out.println("失败次数: " + metrics.getNumberOfFailedCalls());
        System.out.println("被熔断拒绝次数: " + metrics.getNumberOfNotPermittedCalls());
        System.out.println("当前状态: " + circuitBreaker.getState());
    }

    // ===== 2. RateLimiter 限流器演示 =====

    /**
     * 限流算法：令牌桶（Refresh Period 内刷新一批令牌）
     * 适合：API 限流、防刷单、控制外部服务调用频率
     */
    static void rateLimiterDemo() {
        System.out.println("\n===== 2. RateLimiter 限流器演示 =====");

        // 2.1 配置：每 1 秒最多允许 3 次请求，获取令牌超时 500ms
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))  // 每1秒刷新
                .limitForPeriod(3)                           // 每周期最多3次
                .timeoutDuration(Duration.ofMillis(500))     // 等待令牌超时
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = registry.rateLimiter("smsService");

        System.out.println("配置：每秒最多 3 次请求，等待超时 500ms");
        System.out.println("连续发起 8 次请求：");

        for (int i = 1; i <= 8; i++) {
            final int idx = i;
            try {
                // 用 RateLimiter 包装 Supplier
                String result = rateLimiter.executeSupplier(() -> "短信发送成功 #" + idx);
                System.out.println("第" + i + "次: " + result);
            } catch (Exception e) {
                System.out.println("第" + i + "次: 被限流！" + e.getMessage());
            }
        }

        // 2.2 查看限流器指标
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        System.out.println("\n--- 限流器指标 ---");
        System.out.println("可用权限数: " + metrics.getAvailablePermissions());
        System.out.println("等待权限线程数: " + metrics.getNumberOfWaitingThreads());
    }

    // ===== 3. Retry 重试演示 =====

    /**
     * 重试策略：支持固定间隔、指数退避、随机抖动
     * 适合：幂等操作（查询/网络请求）、瞬时故障恢复
     */
    static void retryDemo() {
        System.out.println("\n===== 3. Retry 重试演示 =====");

        // 3.1 固定间隔重试配置
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                               // 最多尝试3次（含首次）
                .waitDuration(Duration.ofMillis(200))         // 每次重试等待200ms
                .retryExceptions(RuntimeException.class)      // 遇到哪些异常触发重试
                .ignoreExceptions(IllegalArgumentException.class) // 忽略（不重试）
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("orderService");

        // 3.2 注册重试事件监听
        retry.getEventPublisher()
                .onRetry(e -> System.out.println("  >>> 重试第 " + e.getNumberOfRetryAttempts() + " 次..."))
                .onSuccess(e -> System.out.println("  >>> 最终成功（共 " + e.getNumberOfRetryAttempts() + " 次重试）"))
                .onError(e -> System.out.println("  >>> 重试耗尽，最终失败"));

        // 3.3 场景1：第3次才成功（模拟偶发故障）
        System.out.println("场景1：第3次才成功");
        AtomicInteger attempts1 = new AtomicInteger(0);
        try {
            String result = retry.executeSupplier(() -> {
                int n = attempts1.incrementAndGet();
                if (n < 3) throw new RuntimeException("网络超时，第" + n + "次尝试失败");
                return "订单创建成功（第" + n + "次）";
            });
            System.out.println("结果: " + result);
        } catch (Exception e) {
            System.out.println("最终失败: " + e.getMessage());
        }

        // 3.4 场景2：每次都失败 → 重试耗尽
        System.out.println("\n场景2：每次都失败（重试耗尽）");
        RetryConfig retryConfig2 = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(RuntimeException.class)
                .build();
        // 使用 Retry.of 快速构建
        Retry r2 = Retry.of("test", retryConfig2);
        r2.getEventPublisher()
                .onRetry(e -> System.out.println("  重试 #" + e.getNumberOfRetryAttempts()));
        try {
            r2.executeSupplier(() -> {
                throw new RuntimeException("数据库连接失败");
            });
        } catch (Exception e) {
            System.out.println("最终异常: " + e.getMessage());
        }

        // 3.5 场景3：忽略 IllegalArgumentException（不触发重试）
        System.out.println("\n场景3：忽略 IllegalArgumentException");
        try {
            retry.executeSupplier(() -> {
                throw new IllegalArgumentException("参数错误，无需重试");
            });
        } catch (Exception e) {
            System.out.println("异常（无重试）: " + e.getMessage());
        }

        // 3.6 查看重试指标
        Retry.Metrics metrics = retry.getMetrics();
        System.out.println("\n--- 重试器指标 ---");
        System.out.println("成功但有重试的次数: " + metrics.getNumberOfSuccessfulCallsWithRetryAttempt());
        System.out.println("失败且有重试的次数: " + metrics.getNumberOfFailedCallsWithRetryAttempt());
        System.out.println("成功无需重试的次数: " + metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt());
    }

    // ===== 4. Retry 指数退避演示 =====

    /**
     * 指数退避（Exponential Backoff）：每次重试等待时间翻倍
     * 加随机抖动（Jitter）：避免惊群效应（多个客户端同时重试）
     */
    static void retryExponentialBackoffDemo() {
        System.out.println("\n===== 4. Retry 指数退避演示 =====");

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4)
                // 初始等待100ms，每次乘以2，最大等待2s
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialRandomBackoff(100, 2.0, 0.5, 2000))
                .retryExceptions(RuntimeException.class)
                .build();

        Retry retry = Retry.of("exponentialRetry", config);
        retry.getEventPublisher()
                .onRetry(e -> System.out.println("  重试 #" + e.getNumberOfRetryAttempts()
                        + "，等待时间: " + e.getWaitInterval() + "ms"));

        AtomicInteger counter = new AtomicInteger(0);
        long start = System.currentTimeMillis();
        try {
            retry.executeSupplier(() -> {
                int n = counter.incrementAndGet();
                System.out.println("  尝试 #" + n + "（" + (System.currentTimeMillis() - start) + "ms）");
                if (n < 4) throw new RuntimeException("临时故障");
                return "成功";
            });
        } catch (Exception e) {
            System.out.println("最终失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        circuitBreakerDemo();
        rateLimiterDemo();
        retryDemo();
        retryExponentialBackoffDemo();
    }
}
