package com.example.redisson;

import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.config.Config;

import java.util.concurrent.*;

/**
 * Redisson 进阶演示：分布式锁 / 限流 / 队列 / 发布订阅 / 布隆过滤器
 *
 * 核心概念：
 * - RLock: 基于 Redis 的分布式可重入锁，支持看门狗自动续期
 * - RRateLimiter: 分布式限流器，支持多种限流策略
 * - RQueue/RDelayedQueue: 分布式队列 / 延迟队列
 * - RTopic: 发布订阅（Pub/Sub）
 * - RBloomFilter: 布隆过滤器，高效判断元素是否可能存在
 *
 * 运行前准备：本地 Redis 服务（端口 6379）
 */
public class RedissonAdvancedDemo {

    private static final String REDIS_ADDRESS = "redis://127.0.0.1:6379";

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.useSingleServer().setAddress(REDIS_ADDRESS);

        RedissonClient redisson = null;
        try {
            redisson = Redisson.create(config);
            System.out.println("✅ 成功连接到 Redis\n");

            demoDistributedLock(redisson);       // 分布式锁（可重入 + 看门狗）
            demoRateLimiter(redisson);           // 分布式限流
            demoQueueAndDelayedQueue(redisson);  // 普通队列 + 延迟队列
            demoPubSub(redisson);                // 发布订阅
            demoBloomFilter(redisson);           // 布隆过滤器

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
     * 分布式锁 RLock 演示
     *
     * 特性：
     * - 可重入：同一线程可多次获取同一锁
     * - 看门狗：默认锁有效期 30 秒，若业务未执行完会自动续期（每 10 秒续一次）
     * - 公平锁 / 非公平锁可选
     * - 红锁 RedLock（多主节点，避免单点故障）
     *
     * ⚠️ 注意：
     * - lock() 会启用看门狗，leaseTime 需设为 -1 或不设置
     * - 若手动指定 leaseTime，看门狗不会启动，可能导致锁提前释放
     * - 必须写在 try-finally 中确保解锁
     */
    static void demoDistributedLock(RedissonClient redisson) throws InterruptedException {
        System.out.println("========== RLock (分布式可重入锁) ==========");

        String lockKey = "demo:lock:order:20240722001";
        RLock lock = redisson.getLock(lockKey);

        // 场景1: 标准加锁（推荐）—— 看门狗自动续期
        System.out.println("\n[场景1] 标准 lock() + 看门狗自动续期");
        lock.lock();
        try {
            System.out.println("  ✅ 获取锁成功，执行业务逻辑...");
            System.out.println("  🐕 看门狗会自动续期（默认 30s 过期，每 10s 续一次）");
            Thread.sleep(500);  // 模拟业务耗时
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println("  🔓 锁已释放");
            }
        }

        // 场景2: 尝试获取锁（带超时时间），防止无限等待
        System.out.println("\n[场景2] tryLock(等待时间, 锁有效期, 时间单位)");
        boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
        if (acquired) {
            try {
                System.out.println("  ✅ 3秒内成功获取锁，锁有效期 10 秒（无看门狗）");
            } finally {
                lock.unlock();
                System.out.println("  🔓 锁已释放");
            }
        } else {
            System.out.println("  ❌ 3秒内未获取到锁，执行降级逻辑");
        }

        // 场景3: 可重入演示
        System.out.println("\n[场景3] 可重入锁演示（同一线程多次获取）");
        lock.lock();
        System.out.println("  第1次获取锁, isHeldByCurrentThread=" + lock.isHeldByCurrentThread());
        lock.lock();
        System.out.println("  第2次获取锁, isHeldByCurrentThread=" + lock.isHeldByCurrentThread());
        System.out.println("  （可重入锁允许同一线程多次获取，需对应释放次数）");
        lock.unlock();
        System.out.println("  第1次释放, isHeldByCurrentThread=" + lock.isHeldByCurrentThread());
        lock.unlock();
        System.out.println("  第2次释放, 锁完全释放");

        // 清理
        redisson.getKeys().delete(lockKey);
    }

    /**
     * 分布式限流 RRateLimiter 演示
     *
     * 特性：
     * - 基于 Redis 的分布式令牌桶算法
     * - 全局限速：集群中所有实例共享同一速率限制
     * - 支持预热（Warmup）和匀速（Bursty）模式
     */
    static void demoRateLimiter(RedissonClient redisson) throws InterruptedException {
        System.out.println("\n========== RRateLimiter (分布式限流) ==========");

        RRateLimiter limiter = redisson.getRateLimiter("demo:rate:api:createOrder");

        // 初始化：每秒产生 5 个令牌，最多突发 10 个
        // RateType.OVERALL = 全局限速（所有实例合计）
        limiter.trySetRate(
                RateType.OVERALL,   // 全局限速（非单实例）
                5,                  // 速率: 5 个/秒
                1,                  // 速率时间单位长度
                RateIntervalUnit.SECONDS
        );

        System.out.println("限流规则: 全局限速 5 次/秒");
        System.out.println("模拟 8 次请求:");

        for (int i = 1; i <= 8; i++) {
            // acquire() 阻塞直到获取到令牌
            // tryAcquire() 非阻塞，立即返回是否成功
            boolean allowed = limiter.tryAcquire();
            System.out.println("  请求 #" + i + " -> " + (allowed ? "✅ 通过" : "❌ 被限流"));
            if (!allowed) {
                // 被限流时可选降级处理
                System.out.println("     [降级] 返回 '系统繁忙，请稍后再试'");
            }
            Thread.sleep(100); // 间隔 100ms 发送请求
        }

        // 清理
        limiter.delete();
    }

    /**
     * 分布式队列 + 延迟队列演示
     *
     * RQueue: 普通 FIFO 队列
     * RDelayedQueue: 延迟队列，元素在指定时间后才可被消费
     *
     * 应用场景：
     * - RQueue: 异步任务队列、消息缓冲
     * - RDelayedQueue: 订单超时取消、定时提醒、延迟重试
     */
    static void demoQueueAndDelayedQueue(RedissonClient redisson) throws InterruptedException {
        System.out.println("\n========== RQueue + RDelayedQueue (队列) ==========");

        // --- 普通队列 ---
        System.out.println("\n[普通队列 RQueue]");
        RQueue<String> taskQueue = redisson.getQueue("demo:queue:tasks");
        taskQueue.clear();
        taskQueue.add("任务1-数据清洗");
        taskQueue.add("任务2-发送通知");
        taskQueue.add("任务3-更新索引");
        System.out.println("入队 3 个任务: " + taskQueue);

        String task = taskQueue.poll();  // 出队（不移除用 peek）
        System.out.println("出队: " + task);
        System.out.println("剩余: " + taskQueue);
        taskQueue.delete();

        // --- 延迟队列 ---
        System.out.println("\n[延迟队列 RDelayedQueue]");
        RQueue<String> destinationQueue = redisson.getQueue("demo:queue:delayedDest");
        RDelayedQueue<String> delayedQueue = redisson.getDelayedQueue(destinationQueue);

        destinationQueue.clear();

        // 投递延迟消息：3 秒后可消费
        delayedQueue.offer("订单1001-超时取消", 3, TimeUnit.SECONDS);
        System.out.println("投递延迟消息: 订单1001-超时取消 (3秒后生效)");

        // 再投递一个 1 秒后的
        delayedQueue.offer("订单1002-发货提醒", 1, TimeUnit.SECONDS);
        System.out.println("投递延迟消息: 订单1002-发货提醒 (1秒后生效)");

        // 轮询消费（实际生产环境应在独立线程/服务中消费）
        System.out.println("等待消费...");
        long start = System.currentTimeMillis();
        while (destinationQueue.size() < 2) {
            String msg = destinationQueue.poll();
            if (msg != null) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("  ⏰ " + elapsed + "ms 后消费: " + msg);
            }
            Thread.sleep(200);
        }

        delayedQueue.delete();
        destinationQueue.delete();
    }

    /**
     * 发布订阅 RTopic 演示
     *
     * 特性：
     * - 基于 Redis Pub/Sub 实现
     * - 支持多消费者同时接收同一条消息（广播）
     * - 消息不持久化，消费者不在线则收不到
     */
    static void demoPubSub(RedissonClient redisson) throws InterruptedException {
        System.out.println("\n========== RTopic (发布订阅 / PubSub) ==========");

        String topicName = "demo:topic:orderEvents";
        RTopic topic = redisson.getTopic(topicName);

        // 注册两个监听器（模拟两个微服务实例）
        int[] receivedCount = {0};

        int listenerId1 = topic.addListener(String.class, (channel, msg) -> {
            System.out.println("  📨 [监听器A] 收到消息: " + msg);
            receivedCount[0]++;
        });

        int listenerId2 = topic.addListener(String.class, (channel, msg) -> {
            System.out.println("  📨 [监听器B] 收到消息: " + msg);
            receivedCount[0]++;
        });

        Thread.sleep(100); // 等待监听器注册完成

        // 发布消息
        System.out.println("发布消息: '订单1001 已支付'");
        topic.publish("订单1001 已支付");

        Thread.sleep(100); // 等待消息送达

        System.out.println("发布消息: '订单1002 已发货'");
        topic.publish("订单1002 已发货");

        Thread.sleep(200);
        System.out.println("总计收到次数: " + receivedCount[0] + "（2个监听器 × 2条消息 = 4）");

        // 移除监听器
        topic.removeListener(listenerId1);
        topic.removeListener(listenerId2);
        System.out.println("监听器已移除");
    }

    /**
     * 布隆过滤器 RBloomFilter 演示
     *
     * 特性：
     * - 空间效率极高的概率型数据结构
     * - 判断 "可能存在" 或 "肯定不存在"
     * - 有一定误判率（误判元素存在），但不会出现漏判
     * - 适合缓存穿透防护、URL 去重、邮箱黑名单等场景
     */
    static void demoBloomFilter(RedissonClient redisson) {
        System.out.println("\n========== RBloomFilter (布隆过滤器) ==========");

        RBloomFilter<String> bloomFilter = redisson.getBloomFilter("demo:bloom:userIds");

        // 初始化：预期插入 10000 个元素，误判率 0.01（1%）
        // 参数一经初始化不可更改，若需调整需删除后重建
        bloomFilter.tryInit(10000, 0.01);

        // 添加元素
        bloomFilter.add("user_001");
        bloomFilter.add("user_002");
        bloomFilter.add("user_003");
        System.out.println("已添加 user_001, user_002, user_003");

        // 判断是否存在
        System.out.println("user_001 可能存在? " + bloomFilter.contains("user_001") + " (一定存在)");
        System.out.println("user_999 可能存在? " + bloomFilter.contains("user_999") + " (大概率不存在，可能误判)");

        // 统计信息
        System.out.println("预计元素数: " + bloomFilter.getExpectedInsertions());
        System.out.println("误判率: " + bloomFilter.getFalseProbability());
        System.out.println("实际已添加: " + bloomFilter.count());

        bloomFilter.delete();
        System.out.println("布隆过滤器已清理");
    }
}
