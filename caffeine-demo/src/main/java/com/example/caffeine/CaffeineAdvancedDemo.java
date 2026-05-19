package com.example.caffeine;

import com.github.benmanes.caffeine.cache.*;

import java.util.concurrent.*;

/**
 * Caffeine 进阶功能演示
 *
 * 涵盖内容：
 * 1. AsyncLoadingCache（异步加载缓存）
 * 2. 软引用 / 弱引用缓存（内存敏感）
 * 3. 移除监听器（RemovalListener）
 * 4. 自定义过期策略（Expiry）
 * 5. 写后刷新（refreshAfterWrite）
 */
public class CaffeineAdvancedDemo {

    // ==================== 演示 1：异步加载缓存 ====================

    /**
     * AsyncLoadingCache：get 返回 CompletableFuture，加载在 ForkJoinPool 或自定义线程池中执行
     * 适合异步 IO 场景（如调用远程服务获取数据）
     */
    static void demoAsyncLoadingCache() throws Exception {
        System.out.println("\n=== 演示 1：异步加载缓存 (AsyncLoadingCache) ===");

        // 创建异步缓存，使用自定义线程池
        ExecutorService executor = Executors.newFixedThreadPool(4);

        AsyncLoadingCache<Integer, String> asyncCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .executor(executor)  // 指定异步执行线程池
                .buildAsync(userId -> {
                    // 模拟异步 IO（如 HTTP 调用、Redis 查询）
                    Thread.sleep(50);  // 50ms 延迟
                    return "AsyncUser-" + userId;
                });

        // get 返回 CompletableFuture，不阻塞当前线程
        CompletableFuture<String> future1 = asyncCache.get(1);
        CompletableFuture<String> future2 = asyncCache.get(2);
        CompletableFuture<String> future3 = asyncCache.get(3);

        // 并发等待所有结果
        CompletableFuture.allOf(future1, future2, future3).join();
        System.out.println("异步加载 userId=1: " + future1.get());
        System.out.println("异步加载 userId=2: " + future2.get());
        System.out.println("异步加载 userId=3: " + future3.get());

        // 第二次 get：已缓存，CompletableFuture 立即完成
        long start = System.currentTimeMillis();
        String cached = asyncCache.get(1).get();
        long cost = System.currentTimeMillis() - start;
        System.out.printf("第二次 get(1)='%s' 耗时 %dms（缓存命中）%n", cached, cost);

        // 转为同步缓存视图
        LoadingCache<Integer, String> syncView = asyncCache.synchronous();
        System.out.println("同步视图 estimatedSize: " + syncView.estimatedSize());

        executor.shutdown();
    }

    // ==================== 演示 2：移除监听器 ====================

    /**
     * RemovalListener：缓存条目被移除时回调（过期/驱逐/手动删除）
     * 用途：记录日志、回收资源、更新统计
     * 注意：回调默认在移除操作的线程中执行，耗时操作应异步处理
     */
    static void demoRemovalListener() throws InterruptedException {
        System.out.println("\n=== 演示 2：移除监听器 (RemovalListener) ===");

        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(3)   // 最多 3 条，触发容量驱逐
                .expireAfterWrite(500, TimeUnit.MILLISECONDS)  // 500ms 后过期
                .removalListener((key, value, cause) -> {
                    // cause：EXPLICIT(手动删除) / REPLACED(更新覆盖) / EXPIRED(过期) / SIZE(容量驱逐) / COLLECTED(GC)
                    System.out.printf("  [移除回调] key=%s value=%s 原因=%s%n", key, value, cause);
                })
                .build();

        // 1. 手动删除 -> EXPLICIT
        cache.put("a", "A");
        cache.invalidate("a");

        // 2. 更新覆盖 -> REPLACED
        cache.put("b", "B-old");
        cache.put("b", "B-new");

        // 3. 容量驱逐 -> SIZE
        cache.put("c", "C");
        cache.put("d", "D");
        cache.put("e", "E");  // 超出 maxSize=3，触发驱逐

        Thread.sleep(200);  // 等待异步驱逐执行

        // 4. 过期 -> EXPIRED
        cache.put("expire-me", "临时数据");
        System.out.println("写入 expire-me: " + cache.getIfPresent("expire-me"));
        Thread.sleep(700);  // 等待 500ms 过期 + 200ms margin
        System.out.println("700ms 后: " + cache.getIfPresent("expire-me"));  // null，触发过期回调
        // 注意：Caffeine 惰性过期，需要访问才触发移除回调；或手动 cleanUp()
        cache.cleanUp();  // 强制清理，触发过期条目的移除回调

        Thread.sleep(100);
    }

    // ==================== 演示 3：自定义过期策略 ====================

    /**
     * Expiry 接口：为每个 key 设置不同的过期时间
     * 适合"VIP 用户缓存 1 小时，普通用户缓存 5 分钟"场景
     */
    static void demoCustomExpiry() throws InterruptedException {
        System.out.println("\n=== 演示 3：自定义过期策略 (Expiry) ===");

        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<String, String>() {
                    /**
                     * 创建后的存活时长（纳秒）
                     * VIP 用户 2 秒，普通用户 500ms
                     */
                    @Override
                    public long expireAfterCreate(String key, String value, long currentTime) {
                        if (key.startsWith("vip:")) {
                            return TimeUnit.SECONDS.toNanos(2);   // VIP：2秒
                        }
                        return TimeUnit.MILLISECONDS.toNanos(500);  // 普通：500ms
                    }

                    /**
                     * 更新后的存活时长（返回 Long.MAX_VALUE 表示不因更新而延期）
                     */
                    @Override
                    public long expireAfterUpdate(String key, String value, long currentTime, long currentDuration) {
                        return currentDuration;  // 更新不重置过期时间
                    }

                    /**
                     * 访问后的存活时长（返回 currentDuration 表示访问不延期）
                     */
                    @Override
                    public long expireAfterRead(String key, String value, long currentTime, long currentDuration) {
                        return currentDuration;  // 访问不续期
                    }
                })
                .build();

        cache.put("vip:user1", "VIP-Alice");
        cache.put("normal:user2", "Normal-Bob");

        System.out.println("写入后立即读:");
        System.out.println("  vip:user1    -> " + cache.getIfPresent("vip:user1"));     // VIP-Alice
        System.out.println("  normal:user2 -> " + cache.getIfPresent("normal:user2"));  // Normal-Bob

        Thread.sleep(700);  // 等 700ms
        System.out.println("\n700ms 后:");
        System.out.println("  vip:user1    -> " + cache.getIfPresent("vip:user1"));     // 仍存在（2s未到）
        System.out.println("  normal:user2 -> " + cache.getIfPresent("normal:user2"));  // null（500ms已过）

        Thread.sleep(1500);  // 再等 1.5 秒
        System.out.println("\n再等 1.5 秒后（共 2.2s）:");
        System.out.println("  vip:user1    -> " + cache.getIfPresent("vip:user1"));     // null（2s已过）
    }

    // ==================== 演示 4：refreshAfterWrite ====================

    /**
     * refreshAfterWrite：写入 N 秒后后台异步刷新（不阻塞读取）
     * 与 expireAfterWrite 的区别：
     *   - expireAfterWrite：过期后下一次读触发同步加载（可能慢）
     *   - refreshAfterWrite：到期后返回旧值，同时后台异步刷新（不影响读性能）
     */
    static void demoRefreshAfterWrite() throws InterruptedException {
        System.out.println("\n=== 演示 4：refreshAfterWrite（写后自动刷新）===");

        final int[] loadCount = {0};

        LoadingCache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(100)
                .refreshAfterWrite(1, TimeUnit.SECONDS)  // 1 秒后后台刷新
                .build(key -> {
                    loadCount[0]++;
                    System.out.println("  [加载] key=" + key + " (第 " + loadCount[0] + " 次加载)");
                    return "Value-V" + loadCount[0];
                });

        System.out.println("第一次 get: " + cache.get("config"));   // 触发第 1 次加载 -> Value-V1

        Thread.sleep(1200);  // 等 1.2 秒，触发刷新标记

        // 此时访问：返回旧值 Value-V1，同时后台异步刷新
        System.out.println("1.2 秒后 get: " + cache.get("config")); // 返回 Value-V1，后台触发第 2 次加载

        Thread.sleep(200);  // 等后台加载完成

        System.out.println("刷新完成后 get: " + cache.get("config")); // 返回 Value-V2
        System.out.println("总加载次数: " + loadCount[0]);             // 2
    }

    // ==================== 演示 5：弱引用 / 软引用 ====================

    /**
     * weakKeys / weakValues / softValues：允许 GC 回收缓存条目
     * - weakKeys：key 无强引用时 GC 可回收
     * - weakValues：value 无强引用时 GC 可回收
     * - softValues：内存不足时 GC 回收（适合大对象缓存）
     *
     * 注意：使用引用缓存后无法用 maximumSize（只能用 maximumWeight）
     */
    static void demoReferenceCache() {
        System.out.println("\n=== 演示 5：引用类型缓存（weakValues / softValues）===");

        // softValues：适合缓存图片、大 JSON 等，OOM 前自动腾空间
        Cache<String, byte[]> softCache = Caffeine.newBuilder()
                .softValues()
                .build();

        softCache.put("image-1", new byte[1024 * 1024]);  // 1MB
        softCache.put("image-2", new byte[1024 * 512]);   // 512KB

        System.out.println("softValues 缓存 image-1: " + (softCache.getIfPresent("image-1") != null ? "存在" : "已被GC"));
        System.out.println("softValues 缓存 image-2: " + (softCache.getIfPresent("image-2") != null ? "存在" : "已被GC"));

        // weakValues：value 无其他强引用时会被 GC（较少用，一般用 softValues）
        Cache<String, Object> weakCache = Caffeine.newBuilder()
                .weakValues()
                .build();

        Object obj = new Object();
        weakCache.put("obj", obj);
        System.out.println("weakValues 存入后: " + (weakCache.getIfPresent("obj") != null ? "存在" : "已被GC"));
        // 注意：只要 obj 变量保持强引用，weakValues 就不会被 GC

        System.out.println("（weakValues 仅在 GC 时才可能被清除，正常情况下与普通缓存无异）");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   Day 18 - Caffeine 高性能本地缓存库 进阶演示     ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        demoAsyncLoadingCache();
        demoRemovalListener();
        demoCustomExpiry();
        demoRefreshAfterWrite();
        demoReferenceCache();

        System.out.println("\n✅ 进阶演示完毕");
    }
}
