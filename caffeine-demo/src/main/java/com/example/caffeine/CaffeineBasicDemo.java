package com.example.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 基础功能演示
 *
 * 涵盖内容：
 * 1. 手动缓存（Cache）- 基础操作
 * 2. 自动加载缓存（LoadingCache）- 自动填充
 * 3. 过期策略 - expireAfterWrite / expireAfterAccess
 * 4. 最大容量 - maximumSize / maximumWeight
 * 5. 缓存统计（Stats）
 */
public class CaffeineBasicDemo {

    // ==================== 演示 1：手动缓存基础操作 ====================

    /**
     * 最简单的 Caffeine 缓存：手动 put/get/invalidate
     */
    static void demoManualCache() {
        System.out.println("\n=== 演示 1：手动缓存 (Cache) ===");

        // 构建一个最大容量 100、写后 5 分钟过期的缓存
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        // put：写入
        cache.put("user:1001", "Alice");
        cache.put("user:1002", "Bob");
        cache.put("user:1003", "Charlie");

        // getIfPresent：存在则返回，不存在返回 null（不触发加载）
        String val = cache.getIfPresent("user:1001");
        System.out.println("getIfPresent user:1001 -> " + val);   // Alice

        String miss = cache.getIfPresent("user:9999");
        System.out.println("getIfPresent user:9999 -> " + miss);   // null

        // get(key, loader)：存在返回缓存值；不存在则调用 loader 加载并缓存
        String loaded = cache.get("user:9999", k -> {
            System.out.println("  [DB查询] key=" + k + " 不在缓存，从数据库加载...");
            return "Unknown-" + k;
        });
        System.out.println("get with loader user:9999 -> " + loaded);  // Unknown-user:9999

        // 第二次 get：已缓存，不再触发 loader
        String cached = cache.get("user:9999", k -> {
            System.out.println("  [DB查询] 不应该出现这行");
            return "should-not-load";
        });
        System.out.println("第二次 get user:9999 -> " + cached);  // Unknown-user:9999

        // invalidate：删除单个 key
        cache.invalidate("user:1001");
        System.out.println("invalidate 后 getIfPresent user:1001 -> " + cache.getIfPresent("user:1001")); // null

        // invalidateAll(keys)：批量删除
        cache.invalidateAll(Arrays.asList("user:1002", "user:1003"));
        System.out.println("批量 invalidate 后缓存估计大小: " + cache.estimatedSize()); // ≈ 1

        // putAll：批量写入
        Map<String, String> batch = new HashMap<>();
        batch.put("user:2001", "Dave");
        batch.put("user:2002", "Eve");
        cache.putAll(batch);
        System.out.println("putAll 后 user:2001 -> " + cache.getIfPresent("user:2001")); // Dave

        System.out.println("当前缓存估计大小: " + cache.estimatedSize());
    }

    // ==================== 演示 2：自动加载缓存 (LoadingCache) ====================

    /**
     * LoadingCache：get 时自动调用 CacheLoader 加载
     * 适合"先查缓存，未命中则查 DB"的典型场景
     */
    static void demoLoadingCache() {
        System.out.println("\n=== 演示 2：自动加载缓存 (LoadingCache) ===");

        LoadingCache<Integer, String> userCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(userId -> {
                    // CacheLoader：模拟从数据库加载用户名
                    System.out.println("  [DB查询] 加载 userId=" + userId);
                    return "UserName-" + userId;
                });

        // 第一次 get -> 触发 CacheLoader
        System.out.println("get(1) -> " + userCache.get(1));  // UserName-1
        System.out.println("get(2) -> " + userCache.get(2));  // UserName-2

        // 第二次 get -> 命中缓存，不触发 CacheLoader
        System.out.println("get(1) 再次 -> " + userCache.get(1));  // UserName-1（无 DB 日志）

        // getAll：批量加载（未缓存的 key 批量调用 loader）
        Map<Integer, String> result = userCache.getAll(Arrays.asList(1, 2, 3, 4));
        System.out.println("getAll([1,2,3,4]) -> " + result);  // 1/2 来自缓存，3/4 触发 DB

        // refresh：强制刷新（后台异步重新加载，用于热数据更新）
        userCache.refresh(1);
        System.out.println("refresh(1) 触发异步重新加载");

        // asMap：获取快照（只读视图）
        System.out.println("缓存快照 keySet: " + userCache.asMap().keySet());
    }

    // ==================== 演示 3：过期策略 ====================

    /**
     * 两种过期策略对比：
     * - expireAfterWrite：写入后 N 秒过期（TTL）
     * - expireAfterAccess：最后一次访问后 N 秒过期（LRU 式）
     */
    static void demoExpirePolicy() throws InterruptedException {
        System.out.println("\n=== 演示 3：过期策略对比 ===");

        // --- expireAfterWrite：写入后 1 秒过期 ---
        Cache<String, String> writeExpire = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();

        writeExpire.put("token", "abc123");
        System.out.println("写入后立即读: " + writeExpire.getIfPresent("token"));  // abc123

        Thread.sleep(1200);  // 等 1.2 秒
        System.out.println("1.2 秒后读 (expireAfterWrite): " + writeExpire.getIfPresent("token"));  // null

        // --- expireAfterAccess：访问后 1 秒过期 ---
        Cache<String, String> accessExpire = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build();

        accessExpire.put("session", "xyz");
        System.out.println("写入后读: " + accessExpire.getIfPresent("session"));  // xyz

        // 每 500ms 访问一次，续命
        for (int i = 0; i < 3; i++) {
            Thread.sleep(500);
            String v = accessExpire.getIfPresent("session");
            System.out.println("  500ms 后访问 " + (i + 1) + ": " + v);  // 每次都续命，不过期
        }

        // 停止访问，等待 1.5 秒
        Thread.sleep(1500);
        System.out.println("停止访问 1.5 秒后: " + accessExpire.getIfPresent("session"));  // null
    }

    // ==================== 演示 4：最大容量策略 ====================

    /**
     * maximumSize：按条数驱逐（LRU/LFU 混合策略 W-TinyLFU）
     * maximumWeight：按权重驱逐（适合缓存大小不均的对象）
     */
    static void demoEvictionPolicy() throws InterruptedException {
        System.out.println("\n=== 演示 4：容量淘汰策略 ===");

        // --- maximumSize：最多 5 条 ---
        Cache<Integer, String> sizeCache = Caffeine.newBuilder()
                .maximumSize(5)
                .build();

        for (int i = 1; i <= 8; i++) {
            sizeCache.put(i, "value-" + i);
        }

        // Caffeine 使用异步 Scheduler，给一点时间让淘汰生效
        Thread.sleep(100);

        System.out.println("写入 8 条，maximumSize=5");
        System.out.println("估计缓存大小: " + sizeCache.estimatedSize());  // ≈ 5

        // 验证：后写入的更容易保留（早写入的被淘汰）
        for (int i = 1; i <= 8; i++) {
            String v = sizeCache.getIfPresent(i);
            System.out.println("  key=" + i + " -> " + (v != null ? v : "(已淘汰)"));
        }

        // --- maximumWeight：按权重限制 ---
        Cache<String, byte[]> weightCache = Caffeine.newBuilder()
                .maximumWeight(1024)   // 最大总权重 1024（字节）
                .weigher((String k, byte[] v) -> v.length)  // 权重 = value 字节数
                .build();

        weightCache.put("small", new byte[100]);   // 100
        weightCache.put("medium", new byte[400]);  // 400
        weightCache.put("large", new byte[600]);   // 600，超出，触发淘汰

        Thread.sleep(100);
        System.out.println("\nmaximumWeight=1024，写入 100+400+600 字节");
        System.out.println("  small  -> " + (weightCache.getIfPresent("small") != null ? "存在" : "已淘汰"));
        System.out.println("  medium -> " + (weightCache.getIfPresent("medium") != null ? "存在" : "已淘汰"));
        System.out.println("  large  -> " + (weightCache.getIfPresent("large") != null ? "存在" : "已淘汰"));
    }

    // ==================== 演示 5：缓存统计 ====================

    /**
     * recordStats()：开启统计，监控命中率、加载时间等
     * 生产环境建议通过 Micrometer 上报到监控系统
     */
    static void demoStats() {
        System.out.println("\n=== 演示 5：缓存统计 (Stats) ===");

        LoadingCache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(100)
                .recordStats()  // 开启统计
                .build(k -> {
                    // 模拟加载耗时
                    return "loaded-" + k;
                });

        // 触发 8 次加载
        for (int i = 1; i <= 8; i++) {
            cache.get("key-" + i);
        }
        // 再 get 已有的 key -> 命中
        for (int i = 1; i <= 5; i++) {
            cache.get("key-" + i);  // 5 次命中
        }
        // 访问不存在的 key -> 加载 miss
        cache.get("key-99");

        CacheStats stats = cache.stats();
        System.out.println("请求总数:    " + stats.requestCount());       // 8+5+1 = 14
        System.out.println("命中次数:    " + stats.hitCount());           // 5
        System.out.println("未命中次数:  " + stats.missCount());          // 9
        System.out.printf ("命中率:      %.2f%%\n", stats.hitRate() * 100); // ~35.7%
        System.out.println("加载次数:    " + stats.loadCount());          // 9
        System.out.println("加载失败次数:" + stats.loadFailureCount());   // 0
        System.out.printf ("平均加载耗时:%.3f ms\n", stats.averageLoadPenalty() / 1_000_000.0);
        System.out.println("驱逐次数:    " + stats.evictionCount());      // 0
    }

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   Day 18 - Caffeine 高性能本地缓存库 基础演示     ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        demoManualCache();
        demoLoadingCache();
        demoExpirePolicy();
        demoEvictionPolicy();
        demoStats();

        System.out.println("\n✅ 基础演示完毕");
    }
}
