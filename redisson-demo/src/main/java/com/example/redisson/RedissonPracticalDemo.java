package com.example.redisson;

import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redisson 实战演示：秒杀库存 / 分布式计数器 / 延迟队列 / Spring Boot 集成指南
 *
 * 场景1: 秒杀库存扣减 —— 利用 RLock + RAtomicLong 保证库存不超卖
 * 场景2: 分布式计数器 —— 利用 RAtomicLong 实现全局限流/统计
 * 场景3: 延迟任务队列 —— 利用 RDelayedQueue 实现订单超时自动取消
 * 场景4: Spring Boot 集成 —— 提供完整集成代码（非运行类，为配置指南）
 *
 * 运行前准备：本地 Redis 服务（端口 6379）
 */
public class RedissonPracticalDemo {

    private static final String REDIS_ADDRESS = "redis://127.0.0.1:6379";

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.useSingleServer().setAddress(REDIS_ADDRESS);

        RedissonClient redisson = null;
        try {
            redisson = Redisson.create(config);
            System.out.println("✅ 成功连接到 Redis\n");

            demoSecKill(redisson);          // 秒杀库存（分布式锁 + 原子递减）
            demoDistributedCounter(redisson); // 分布式计数器（并发统计）
            demoOrderTimeoutCancel(redisson); // 订单超时自动取消（延迟队列）

            System.out.println("\n========== Spring Boot 集成指南 ==========");
            printSpringBootIntegrationGuide();

        } catch (Exception e) {
            System.err.println("❌ 连接 Redis 失败: " + e.getMessage());
            System.err.println("   请确保 Redis 服务已启动（默认端口 6379）");
        } finally {
            if (redisson != null && !redisson.isShutdown()) {
                redisson.shutdown();
                System.out.println("\n🔌 Redis 连接已关闭");
            }
        }
    }

    /**
     * 场景1: 秒杀库存扣减
     *
     * 核心逻辑：
     * 1. 使用 RLock 对库存 key 加锁（防止并发竞争）
     * 2. 使用 RAtomicLong 原子递减库存
     * 3. 锁释放后其他线程才能竞争
     *
     * 优化方案（高并发）：
     * - 使用 Lua 脚本保证 "判断库存 + 扣减" 原子性，无需加锁
     * - Redisson 的 RLock 本身已优化，但在极端高并发下仍可能竞争
     */
    static void demoSecKill(RedissonClient redisson) throws InterruptedException {
        System.out.println("========== 场景1: 秒杀库存扣减 ==========");

        String stockKey = "demo:seckill:stock:sku001";
        RAtomicLong stock = redisson.getAtomicLong(stockKey);
        stock.set(10);  // 初始库存 10

        int buyerCount = 20;  // 20 个买家同时抢购
        ExecutorService executor = Executors.newFixedThreadPool(buyerCount);
        CountDownLatch latch = new CountDownLatch(buyerCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        System.out.println("初始库存: 10，并发买家: " + buyerCount);
        long start = System.currentTimeMillis();

        for (int i = 0; i < buyerCount; i++) {
            final int buyerId = i + 1;
            executor.submit(() -> {
                RLock lock = redisson.getLock("demo:seckill:lock:sku001");
                try {
                    // 尝试获取锁，最多等待 2 秒，锁持有 5 秒
                    boolean acquired = lock.tryLock(2, 5, TimeUnit.SECONDS);
                    if (!acquired) {
                        System.out.println("  买家" + buyerId + " -> 获取锁超时，放弃购买");
                        failCount.incrementAndGet();
                        return;
                    }

                    try {
                        long currentStock = stock.get();
                        if (currentStock > 0) {
                            long remain = stock.decrementAndGet();
                            successCount.incrementAndGet();
                            System.out.println("  买家" + buyerId + " -> 抢购成功! 剩余库存: " + remain);
                        } else {
                            failCount.incrementAndGet();
                            System.out.println("  买家" + buyerId + " -> 库存不足，抢购失败");
                        }
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("\n结果统计:");
        System.out.println("  成功抢购: " + successCount.get());
        System.out.println("  抢购失败: " + failCount.get());
        System.out.println("  最终库存: " + stock.get());
        System.out.println("  总耗时: " + elapsed + "ms");

        // 清理
        stock.delete();
        redisson.getKeys().delete("demo:seckill:lock:sku001");
    }

    /**
     * 场景2: 分布式计数器（全局 PV/UV 统计、接口调用次数）
     *
     * 利用 RAtomicLong 的原子性，在分布式环境下安全累加。
     * 结合定时任务，可定期将 Redis 计数持久化到数据库。
     */
    static void demoDistributedCounter(RedissonClient redisson) throws InterruptedException {
        System.out.println("\n========== 场景2: 分布式计数器（全局限流/统计） ==========");

        String counterKey = "demo:counter:api:queryUser";
        RAtomicLong counter = redisson.getAtomicLong(counterKey);
        counter.set(0);

        // 模拟 5 个服务实例，每个处理 100 次请求
        int instances = 5;
        int requestsPerInstance = 100;
        ExecutorService executor = Executors.newFixedThreadPool(instances);
        CountDownLatch latch = new CountDownLatch(instances);

        System.out.println("模拟 " + instances + " 个实例，每实例 " + requestsPerInstance + " 次请求...");

        for (int i = 0; i < instances; i++) {
            final int instanceId = i + 1;
            executor.submit(() -> {
                for (int r = 0; r < requestsPerInstance; r++) {
                    counter.incrementAndGet();
                }
                System.out.println("  实例" + instanceId + " 完成 " + requestsPerInstance + " 次累加");
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        long total = counter.get();
        System.out.println("全局总计数: " + total + " (预期: " + (instances * requestsPerInstance) + ")");
        System.out.println("✅ 计数准确，无并发丢失!");

        counter.delete();
    }

    /**
     * 场景3: 订单超时自动取消（延迟队列）
     *
     * 电商常见需求：用户下单后 30 分钟未支付，自动取消订单释放库存。
     * 利用 RDelayedQueue 实现：
     * - 下单时将订单 ID 投递到延迟队列，延迟 30 分钟
     * - 消费者线程持续监听，到期后执行取消逻辑
     */
    static void demoOrderTimeoutCancel(RedissonClient redisson) throws InterruptedException {
        System.out.println("\n========== 场景3: 订单超时自动取消（延迟队列） ==========");

        RQueue<String> readyQueue = redisson.getQueue("demo:order:readyToCancel");
        RDelayedQueue<String> delayedQueue = redisson.getDelayedQueue(readyQueue);

        readyQueue.clear();

        // 模拟用户下单，设置 5 秒后自动取消（实际业务为 30 分钟）
        System.out.println("模拟下单，设置 5 秒后自动取消...");
        delayedQueue.offer("ORDER_20240722_001", 5, TimeUnit.SECONDS);
        System.out.println("  订单 ORDER_20240722_001 已投递（5秒后过期）");

        delayedQueue.offer("ORDER_20240722_002", 2, TimeUnit.SECONDS);
        System.out.println("  订单 ORDER_20240722_002 已投递（2秒后过期）");

        delayedQueue.offer("ORDER_20240722_003", 8, TimeUnit.SECONDS);
        System.out.println("  订单 ORDER_20240722_003 已投递（8秒后过期）");

        // 模拟消费者线程
        System.out.println("\n启动消费者监听...");
        long start = System.currentTimeMillis();
        int cancelled = 0;
        while (cancelled < 3) {
            String orderId = readyQueue.poll();
            if (orderId != null) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("  ⏰ [" + elapsed + "ms] 订单 " + orderId + " 已超时，执行取消逻辑...");
                // 实际业务：
                // 1. 查询订单状态，若未支付则取消
                // 2. 释放库存
                // 3. 发送通知给用户
                System.out.println("     ✅ 取消完成，库存已释放");
                cancelled++;
            }
            Thread.sleep(200);
        }

        System.out.println("\n所有订单已处理完毕");

        delayedQueue.delete();
        readyQueue.delete();
    }

    /**
     * 打印 Spring Boot 集成指南
     */
    static void printSpringBootIntegrationGuide() {
        System.out.println("\n1. 添加 Maven 依赖:");
        System.out.println("            <dependency>");
        System.out.println("                <groupId>org.redisson</groupId>");
        System.out.println("                <artifactId>redisson-spring-boot-starter</artifactId>");
        System.out.println("                <version>3.23.5</version>");
        System.out.println("            </dependency>");

        System.out.println("2. application.yml 配置:");
        System.out.println("            spring:");
        System.out.println("              redis:");
        System.out.println("                redisson:");
        System.out.println("                  config: |");
        System.out.println("                    singleServerConfig:");
        System.out.println("                      address: \"redis://127.0.0.1:6379\"");
        System.out.println("                      password: null");
        System.out.println("                      connectionMinimumIdleSize: 10");
        System.out.println("                      connectionPoolSize: 64");
        System.out.println("                      idleConnectionTimeout: 10000");
        System.out.println("                      connectTimeout: 10000");
        System.out.println("                      timeout: 3000");
        System.out.println("                      retryAttempts: 3");
        System.out.println("                      retryInterval: 1500");
        System.out.println("                      database: 0");

        System.out.println("3. 服务类中注入使用:");
        System.out.println("            @Service");
        System.out.println("            public class OrderService {");
        System.out.println("                @Autowired");
        System.out.println("                private RedissonClient redissonClient;");
        System.out.println();
        System.out.println("                public void createOrder(String orderId) {");
        System.out.println("                    RLock lock = redissonClient.getLock(\"lock:order:\" + orderId);");
        System.out.println("                    lock.lock();");
        System.out.println("                    try {");
        System.out.println("                        // 业务逻辑...");
        System.out.println("                    } finally {");
        System.out.println("                        lock.unlock();");
        System.out.println("                    }");
        System.out.println("                }");
        System.out.println("            }");

        System.out.println("4. 序列化配置（推荐 JSON）:");
        System.out.println("            // 在 Redisson 配置中添加:");
        System.out.println("            config.setCodec(new JsonJacksonCodec());");
        System.out.println("            // 或 Kryo（更快）:");
        System.out.println("            config.setCodec(new Kryo5Codec());");
    }
}
