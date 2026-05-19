package com.example.caffeine;

import com.github.benmanes.caffeine.cache.*;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Caffeine 实战应用演示
 *
 * 涵盖内容：
 * 1. 多级缓存架构（L1 本地缓存 + L2 模拟 Redis）
 * 2. 缓存穿透防护（空值缓存）
 * 3. 缓存雪崩防护（随机过期 + 分散过期时间）
 * 4. 高并发计数器（原子操作）
 * 5. 配置中心缓存（Spring Boot 集成示例）
 * 6. 性能基准（Caffeine vs HashMap）
 */
public class CaffeinePracticalDemo {

    // ==================== 演示 1：多级缓存 ====================

    /**
     * 典型多级缓存架构：
     * 请求 → L1 Caffeine（本地内存，极快） → L2 Redis（分布式，较快） → DB（最慢）
     *
     * 本示例用 Map 模拟 Redis，演示多级穿透逻辑
     */
    static class TwoLevelCache {

        // L1：Caffeine 本地缓存，容量 500，写后 30 秒过期
        private final LoadingCache<String, String> l1Cache;

        // L2：模拟 Redis（实际项目用 StringRedisTemplate）
        private final Map<String, String> l2Redis = new ConcurrentHashMap<>();

        // DB 查询计数（验证多级缓存效果）
        private final AtomicInteger dbQueryCount = new AtomicInteger(0);

        TwoLevelCache() {
            l1Cache = Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(30, TimeUnit.SECONDS)
                    .recordStats()
                    .build(key -> {
                        // L1 未命中 -> 查 L2 Redis
                        String v = l2Redis.get(key);
                        if (v != null) {
                            System.out.println("  [L2 Redis 命中] key=" + key);
                            return v;
                        }
                        // L2 也未命中 -> 查 DB
                        System.out.println("  [DB 查询] key=" + key);
                        dbQueryCount.incrementAndGet();
                        String dbValue = "DBValue-" + key;
                        // 写入 L2（实际项目：Redis SET with EX）
                        l2Redis.put(key, dbValue);
                        return dbValue;
                    });
        }

        /**
         * 预热 L2 Redis（模拟 Redis 中已有部分热点数据）
         */
        void warmupL2(String key, String value) {
            l2Redis.put(key, value);
        }

        String get(String key) {
            return l1Cache.get(key);
        }

        void printStats() {
            CacheStats stats = l1Cache.stats();
            System.out.printf("  L1 命中率=%.1f%%  命中=%d  未命中=%d  DB查询=%d次%n",
                    stats.hitRate() * 100,
                    stats.hitCount(),
                    stats.missCount(),
                    dbQueryCount.get());
        }
    }

    static void demoTwoLevelCache() {
        System.out.println("\n=== 演示 1：多级缓存（L1 Caffeine + L2 Redis + DB）===");

        TwoLevelCache cache = new TwoLevelCache();

        // 预热 L2：模拟 Redis 中的热点数据
        cache.warmupL2("product:1001", "iPhone16");
        cache.warmupL2("product:1002", "MacBook Pro");

        System.out.println("第一轮请求（冷启动）:");
        System.out.println("  product:1001 -> " + cache.get("product:1001")); // L2 命中
        System.out.println("  product:1002 -> " + cache.get("product:1002")); // L2 命中
        System.out.println("  product:1003 -> " + cache.get("product:1003")); // DB 查询
        System.out.println("  product:1004 -> " + cache.get("product:1004")); // DB 查询

        System.out.println("\n第二轮请求（L1 热缓存）:");
        for (int i = 1; i <= 4; i++) {
            System.out.println("  product:100" + i + " -> " + cache.get("product:100" + i)); // 全部 L1 命中
        }

        cache.printStats();
    }

    // ==================== 演示 2：缓存穿透防护 ====================

    /**
     * 缓存穿透：查询不存在的 key，每次都打穿到 DB
     * 解决方案：对空结果也缓存（存 null 占位符）
     */
    static void demoCachePenetration() {
        System.out.println("\n=== 演示 2：缓存穿透防护（空值缓存）===");

        // 使用 Optional 包装 null（Caffeine 不允许存 null）
        LoadingCache<Long, Optional<String>> cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)  // 空值缓存较短时间
                .build(id -> {
                    System.out.println("  [DB查询] userId=" + id);
                    // 模拟：id=1 存在，其他不存在
                    if (id == 1L) {
                        return Optional.of("Alice");
                    }
                    return Optional.empty();  // 不存在，缓存空值
                });

        // 第一次：id=1 存在
        System.out.println("userId=1: " + cache.get(1L).orElse("不存在"));

        // 第一次：id=999 不存在，触发 DB 查询，缓存空值
        System.out.println("userId=999: " + cache.get(999L).orElse("不存在"));

        // 第二次：id=999 穿透攻击，命中空值缓存，不再查 DB
        System.out.println("userId=999 (再次): " + cache.get(999L).orElse("不存在（已缓存空值）"));

        System.out.println("缓存估计大小: " + cache.estimatedSize()); // 2（含空值）
    }

    // ==================== 演示 3：缓存雪崩防护 ====================

    /**
     * 缓存雪崩：大量 key 同时过期，瞬间所有请求打到 DB
     * 解决方案：随机化过期时间，将过期分散在一个时间区间内
     */
    static void demoCacheAvalanche() {
        System.out.println("\n=== 演示 3：缓存雪崩防护（随机过期时间）===");

        Random random = new Random();

        // 使用 Expiry 为每个 key 设置 [60s, 120s] 随机过期时间
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfter(new Expiry<String, String>() {
                    @Override
                    public long expireAfterCreate(String key, String value, long currentTime) {
                        // 基础 60 秒 + 随机 0~60 秒
                        long base = TimeUnit.SECONDS.toNanos(60);
                        long jitter = TimeUnit.SECONDS.toNanos(random.nextInt(60));
                        return base + jitter;
                    }

                    @Override
                    public long expireAfterUpdate(String key, String value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, String value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();

        // 批量写入商品数据
        for (int i = 1; i <= 10; i++) {
            cache.put("product:" + i, "商品-" + i);
        }

        System.out.println("写入 10 条商品数据，每条过期时间随机分布在 [60s, 120s]");
        System.out.println("商品数据缓存大小: " + cache.estimatedSize());
        System.out.println("（随机过期策略可有效避免同时过期导致的雪崩效应）");
    }

    // ==================== 演示 4：高并发计数器 ====================

    /**
     * 利用 Caffeine 实现接口访问频率统计（滑动窗口计数）
     * 场景：API 限流、热点 URL 统计
     */
    static void demoConcurrentCounter() throws InterruptedException {
        System.out.println("\n=== 演示 4：高并发计数器（API 访问频率统计）===");

        // 统计 key 在最近 1 分钟的访问次数
        Cache<String, AtomicInteger> counterCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();

        // 模拟 20 个线程并发访问 3 个 API
        ExecutorService pool = Executors.newFixedThreadPool(20);
        String[] apis = {"/api/user", "/api/order", "/api/product"};
        int taskCount = 100;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            final String api = apis[i % 3];
            pool.submit(() -> {
                try {
                    // 原子递增，不存在时创建新计数器
                    counterCache.get(api, k -> new AtomicInteger(0)).incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        System.out.println("100 次并发请求后各 API 访问次数:");
        for (String api : apis) {
            AtomicInteger cnt = counterCache.getIfPresent(api);
            System.out.printf("  %-20s -> %d 次%n", api, cnt != null ? cnt.get() : 0);
        }
    }

    // ==================== 演示 5：配置中心缓存（Spring Boot 集成示例）====================

    /**
     * 模拟 Spring Boot 中的配置中心缓存
     * 实际集成请参考文档中的 Spring Boot 集成章节
     */
    static void demoSpringBootIntegration() {
        System.out.println("\n=== 演示 5：Spring Boot 集成示例（代码说明）===");

        System.out.println("Spring Boot 集成 Caffeine 的三种方式：");
        System.out.println();
        System.out.println("【方式 1：自动配置（推荐）】");
        System.out.println("  pom.xml 添加：");
        System.out.println("    <dependency>");
        System.out.println("      <groupId>org.springframework.boot</groupId>");
        System.out.println("      <artifactId>spring-boot-starter-cache</artifactId>");
        System.out.println("    </dependency>");
        System.out.println("    <dependency>");
        System.out.println("      <groupId>com.github.ben-manes.caffeine</groupId>");
        System.out.println("      <artifactId>caffeine</artifactId>");
        System.out.println("    </dependency>");
        System.out.println();
        System.out.println("  application.yml 配置：");
        System.out.println("    spring:");
        System.out.println("      cache:");
        System.out.println("        type: caffeine");
        System.out.println("        caffeine.spec: maximumSize=500,expireAfterWrite=300s");
        System.out.println();
        System.out.println("  启动类加 @EnableCaching");
        System.out.println("  Service 方法加 @Cacheable(cacheNames = \"users\", key = \"#id\")");
        System.out.println();
        System.out.println("【方式 2：手动 CacheManager Bean】");
        System.out.println("  @Bean CaffeineCacheManager 可配置多个不同策略的 cacheName");
        System.out.println();
        System.out.println("【方式 3：直接注入 Cache Bean】");
        System.out.println("  @Bean Cache<K,V> 直接使用 Caffeine.newBuilder().build()");
        System.out.println("  适合需要精细控制（自定义 Expiry、RemovalListener）的场景");

        // 演示模拟 @Cacheable 行为
        System.out.println();
        System.out.println("--- 模拟 @Cacheable 行为 ---");

        LoadingCache<Long, String> userCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build(id -> {
                    System.out.println("  [Service] 调用数据库查询 userId=" + id);
                    return "User-" + id;
                });

        // 模拟 @Cacheable：第一次调用查 DB，第二次命中缓存
        System.out.println("第 1 次调用 getUser(42): " + userCache.get(42L));
        System.out.println("第 2 次调用 getUser(42): " + userCache.get(42L));  // 不再打印 DB 日志
        System.out.println("第 3 次调用 getUser(42): " + userCache.get(42L));

        CacheStats stats = userCache.stats();
        System.out.printf("命中率=%.1f%%，请求 %d 次，命中 %d 次，未命中 %d 次%n",
                stats.hitRate() * 100,
                stats.requestCount(),
                stats.hitCount(),
                stats.missCount());
    }

    // ==================== 演示 6：性能对比 ====================

    /**
     * Caffeine vs ConcurrentHashMap 性能对比
     * Caffeine 使用 W-TinyLFU 算法，在高并发读写场景下性能接近 ConcurrentHashMap
     */
    static void demoPerformance() throws InterruptedException {
        System.out.println("\n=== 演示 6：Caffeine 性能简测 ===");

        int threadCount = 8;
        int opsPerThread = 50_000;
        int keyRange = 1000;
        Random rand = new Random();

        // --- ConcurrentHashMap ---
        Map<Integer, String> hashMap = new ConcurrentHashMap<>();
        for (int i = 0; i < keyRange; i++) hashMap.put(i, "v" + i);

        ExecutorService pool1 = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch1 = new CountDownLatch(threadCount);
        long t1 = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            pool1.submit(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        hashMap.get(rand.nextInt(keyRange));
                    }
                } finally {
                    latch1.countDown();
                }
            });
        }
        latch1.await();
        pool1.shutdown();
        long mapTime = System.currentTimeMillis() - t1;

        // --- Caffeine ---
        Cache<Integer, String> caffeineCache = Caffeine.newBuilder()
                .maximumSize(keyRange)
                .build();
        for (int i = 0; i < keyRange; i++) caffeineCache.put(i, "v" + i);

        ExecutorService pool2 = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch2 = new CountDownLatch(threadCount);
        long t2 = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            pool2.submit(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        caffeineCache.getIfPresent(rand.nextInt(keyRange));
                    }
                } finally {
                    latch2.countDown();
                }
            });
        }
        latch2.await();
        pool2.shutdown();
        long caffeineTime = System.currentTimeMillis() - t2;

        long totalOps = (long) threadCount * opsPerThread;
        System.out.printf("并发读取 %,d 次（%d 线程 × %,d ops）%n", totalOps, threadCount, opsPerThread);
        System.out.printf("ConcurrentHashMap: %4d ms   吞吐量: %,d ops/s%n",
                mapTime, totalOps * 1000 / Math.max(mapTime, 1));
        System.out.printf("Caffeine:          %4d ms   吞吐量: %,d ops/s%n",
                caffeineTime, totalOps * 1000 / Math.max(caffeineTime, 1));
        System.out.println("（Caffeine 在读多写少场景下性能接近 ConcurrentHashMap，同时提供自动过期/淘汰能力）");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   Day 18 - Caffeine 高性能本地缓存库 实战演示     ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        demoTwoLevelCache();
        demoCachePenetration();
        demoCacheAvalanche();
        demoConcurrentCounter();
        demoSpringBootIntegration();
        demoPerformance();

        System.out.println("\n✅ 实战演示完毕");
    }
}
