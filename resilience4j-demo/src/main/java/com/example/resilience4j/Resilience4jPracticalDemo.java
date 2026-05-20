package com.example.resilience4j;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Resilience4j 实战场景演示
 *
 * 模拟真实业务场景：
 * 1. 电商下单系统 - CircuitBreaker + Retry + Fallback
 * 2. 短信验证码接口 - RateLimiter 防刷
 * 3. 微服务调用链 - 多层熔断保护
 * 4. Spring Boot 集成方式说明
 */
public class Resilience4jPracticalDemo {

    // 模拟本地缓存
    private static final Map<String, String> localCache = new HashMap<>();
    static {
        localCache.put("product_1001", "iPhone 15 Pro - ¥8999（缓存）");
        localCache.put("product_1002", "MacBook Pro - ¥14999（缓存）");
    }

    // ===== 1. 电商下单：库存服务弹性保护 =====

    /** 库存服务客户端（模拟） */
    static class InventoryService {
        private static final AtomicInteger callCount = new AtomicInteger(0);

        /** 模拟不稳定的库存查询接口 */
        static int checkStock(String productId) {
            int n = callCount.incrementAndGet();
            // 模拟：每3次调用有2次超时
            if (n % 3 != 0) {
                throw new RuntimeException("库存服务响应超时（" + productId + "）");
            }
            return 100; // 有货
        }
    }

    /** 电商下单的弹性库存查询 */
    static void ecommerceOrderDemo() {
        System.out.println("\n===== 1. 电商下单 - 库存服务弹性保护 =====");

        // 配置：3次中失败2次（66%）触发熔断，最多重试3次
        CircuitBreaker cb = CircuitBreaker.of("inventoryCB",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(5)
                        .failureRateThreshold(60)
                        .waitDurationInOpenState(Duration.ofSeconds(3))
                        .build());

        Retry retry = Retry.of("inventoryRetry",
                RetryConfig.custom()
                        .maxAttempts(3)
                        .waitDuration(Duration.ofMillis(100))
                        .retryExceptions(RuntimeException.class)
                        .build());

        cb.getEventPublisher().onStateTransition(e ->
                System.out.println("  [熔断器] 状态变化: " + e.getStateTransition()));
        retry.getEventPublisher().onRetry(e ->
                System.out.println("  [重试] 第 " + e.getNumberOfRetryAttempts() + " 次重试"));

        // 弹性包装：Retry 外层 + CircuitBreaker 内层
        for (int order = 1; order <= 6; order++) {
            final String productId = "product_" + order;
            try {
                int stock = Retry.decorateSupplier(retry,
                        CircuitBreaker.decorateSupplier(cb,
                                () -> InventoryService.checkStock(productId))).get();
                System.out.println("订单 #" + order + ": 库存=" + stock + "，下单成功 ✓");
            } catch (CallNotPermittedException e) {
                // 熔断降级：走本地缓存/预置值
                System.out.println("订单 #" + order + ": 熔断降级，返回预估库存=50（业务可用）");
            } catch (Exception e) {
                System.out.println("订单 #" + order + ": 库存查询失败，订单暂挂。原因: " + e.getMessage());
            }
        }
    }

    // ===== 2. 短信验证码防刷 =====

    static void smsBruteForceProtection() throws InterruptedException {
        System.out.println("\n===== 2. 短信验证码 - RateLimiter 防刷演示 =====");

        // 全局限流：每分钟 5 条（演示用每秒 2 条）
        RateLimiter globalLimiter = RateLimiter.of("smsGlobal",
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .limitForPeriod(2)
                        .timeoutDuration(Duration.ofMillis(0)) // 0=立即失败，不等待
                        .build());

        // 模拟 5 个用户同时请求短信
        System.out.println("5个用户同时请求短信验证码（全局限速 2/秒）：");
        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        for (int user = 1; user <= 5; user++) {
            final int uid = user;
            pool.submit(() -> {
                try {
                    globalLimiter.executeRunnable(() ->
                            System.out.println("  用户 #" + uid + ": 短信发送成功 ✓"));
                } catch (Exception e) {
                    System.out.println("  用户 #" + uid + ": 触发限流，请稍后重试 ✗");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(2, TimeUnit.SECONDS);
        pool.shutdown();

        // 1 秒后第二批
        System.out.println("\n等待 1 秒后再试（令牌刷新）...");
        Thread.sleep(1100);
        for (int i = 1; i <= 2; i++) {
            try {
                globalLimiter.executeRunnable(() ->
                        System.out.println("  重试请求: 短信发送成功 ✓"));
            } catch (Exception e) {
                System.out.println("  重试请求: 仍被限流 ✗");
            }
        }
    }

    // ===== 3. 微服务调用链多层保护 =====

    /**
     * 模拟微服务架构：Gateway → OrderService → UserService → InventoryService
     * 每一层都有独立的熔断器
     */
    static void microserviceChainDemo() {
        System.out.println("\n===== 3. 微服务调用链 - 多层熔断保护 =====");

        // 三个服务各自的熔断器
        CircuitBreaker userCB = CircuitBreaker.of("userService",
                CircuitBreakerConfig.custom().slidingWindowSize(3).failureRateThreshold(66).build());
        CircuitBreaker inventoryCB = CircuitBreaker.of("inventoryService",
                CircuitBreakerConfig.custom().slidingWindowSize(3).failureRateThreshold(66).build());

        AtomicInteger invCallCount = new AtomicInteger(0);
        AtomicInteger userCallCount = new AtomicInteger(0);

        // 模拟 InventoryService（不稳定）
        Supplier<String> inventorySupplier = CircuitBreaker.decorateSupplier(inventoryCB, () -> {
            if (invCallCount.incrementAndGet() % 2 == 0) throw new RuntimeException("库存服务宕机");
            return "库存正常";
        });

        // 模拟 UserService（稳定）
        Supplier<String> userSupplier = CircuitBreaker.decorateSupplier(userCB, () -> {
            userCallCount.incrementAndGet();
            return "用户信息已加载";
        });

        // 模拟 OrderService 聚合调用
        System.out.println("模拟 OrderService 发起 6 次下单（依赖 UserService + InventoryService）：");
        for (int i = 1; i <= 6; i++) {
            System.out.print("  下单 #" + i + ": ");
            try {
                String user = userSupplier.get();
                String inventory = inventorySupplier.get();
                System.out.println(user + " | " + inventory + " → 下单成功 ✓");
            } catch (CallNotPermittedException e) {
                System.out.println("依赖服务熔断，订单创建失败（可排队重试）✗");
            } catch (RuntimeException e) {
                System.out.println("依赖服务异常: " + e.getMessage() + " ✗");
            }
        }

        System.out.println("\n熔断器状态 - UserService: " + userCB.getState()
                + " | InventoryService: " + inventoryCB.getState());
    }

    // ===== 4. Bulkhead + CircuitBreaker 防止级联雪崩 =====

    static void avalancheProtectionDemo() throws InterruptedException {
        System.out.println("\n===== 4. 舱壁 + 熔断 - 防止级联雪崩 =====");

        // 限制最多 2 个并发访问慢服务
        Bulkhead bulkhead = Bulkhead.of("slowService",
                BulkheadConfig.custom()
                        .maxConcurrentCalls(2)
                        .maxWaitDuration(Duration.ofMillis(50))
                        .build());

        CircuitBreaker cb = CircuitBreaker.of("slowServiceCB",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(5)
                        .failureRateThreshold(50)
                        .build());

        cb.getEventPublisher().onStateTransition(e ->
                System.out.println("  [熔断器] " + e.getStateTransition()));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(8);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger bulkheadReject = new AtomicInteger();
        AtomicInteger cbReject = new AtomicInteger();

        for (int i = 1; i <= 8; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    String result = Bulkhead.decorateSupplier(bulkhead,
                            CircuitBreaker.decorateSupplier(cb, () -> {
                                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                                if (idx > 5) throw new RuntimeException("服务过载");
                                return "响应 #" + idx;
                            })).get();
                    success.incrementAndGet();
                    System.out.println("  请求 #" + idx + " 成功: " + result);
                } catch (io.github.resilience4j.bulkhead.BulkheadFullException e) {
                    bulkheadReject.incrementAndGet();
                    System.out.println("  请求 #" + idx + " 被舱壁拒绝（保护线程资源）");
                } catch (CallNotPermittedException e) {
                    cbReject.incrementAndGet();
                    System.out.println("  请求 #" + idx + " 被熔断器拦截");
                } catch (Exception e) {
                    System.out.println("  请求 #" + idx + " 异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.println("\n--- 雪崩防护统计 ---");
        System.out.println("成功: " + success.get() + " | 舱壁拒绝: " + bulkheadReject.get()
                + " | 熔断拒绝: " + cbReject.get());
    }

    /**
     * Spring Boot 集成说明（代码注释形式）
     *
     * <pre>
     * // 1. pom.xml 添加：
     * //    resilience4j-spring-boot2 + spring-boot-starter-aop
     *
     * // 2. application.yml 配置：
     * // resilience4j:
     * //   circuitbreaker:
     * //     instances:
     * //       paymentService:
     * //         registerHealthIndicator: true
     * //         slidingWindowSize: 10
     * //         failureRateThreshold: 50
     * //         waitDurationInOpenState: 5s
     * //   retry:
     * //     instances:
     * //       paymentService:
     * //         maxAttempts: 3
     * //         waitDuration: 200ms
     *
     * // 3. Service 方法直接加注解：
     * // @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
     * // @Retry(name = "paymentService")
     * // @RateLimiter(name = "paymentService")
     * // public String processPayment(Order order) { ... }
     *
     * // public String paymentFallback(Order order, Exception ex) {
     * //     return "支付服务繁忙，请稍后重试";
     * // }
     * </pre>
     */
    static void springBootIntegrationGuide() {
        System.out.println("\n===== 5. Spring Boot 集成要点 =====");
        System.out.println("依赖：resilience4j-spring-boot2 + spring-boot-starter-aop");
        System.out.println("注解：@CircuitBreaker / @Retry / @RateLimiter / @Bulkhead");
        System.out.println("配置：application.yml 中 resilience4j.circuitbreaker.instances 下按服务名配置");
        System.out.println("监控：/actuator/health（含熔断状态）+ /actuator/metrics（含详细指标）");
        System.out.println("注意：fallbackMethod 参数列表必须与原方法一致，最后加 Exception/Throwable 参数");
    }

    public static void main(String[] args) throws InterruptedException {
        ecommerceOrderDemo();
        smsBruteForceProtection();
        microserviceChainDemo();
        avalancheProtectionDemo();
        springBootIntegrationGuide();
    }
}
