package com.example.guava;

import com.google.common.cache.*;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.util.concurrent.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * GuavaPracticalDemo - Guava 高级工具实战演示
 *
 * 演示以下功能：
 * 1. Cache - 本地缓存
 * 2. RateLimiter - 限流器
 * 3. Hashing - 哈希工具
 * 4. EventBus - 事件总线
 * 5. ListenableFuture - 可监听的Future
 * 6. Files - 文件工具
 */
public class GuavaPracticalDemo {
    
    public static void main(String[] args) throws Exception {
        demoCache();
        demoRateLimiter();
        demoHashing();
        demoEventBus();
        demoListenableFuture();
        demoFiles();
    }
    
    /**
     * 演示 Cache - 本地缓存（类似 ConcurrentHashMap + 过期策略）
     */
    private static void demoCache() {
        System.out.println("========== 1. Cache（本地缓存）==========");
        
        // 创建缓存
        Cache<String, String> cache = CacheBuilder.newBuilder()
                .maximumSize(1000)                    // 最大条目数
                .expireAfterWrite(10, TimeUnit.SECONDS)  // 写入后10秒过期
                .expireAfterAccess(5, TimeUnit.SECONDS)  // 访问后5秒过期
                .recordStats()                        // 记录命中率统计
                .build();
        
        // 基本操作
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        System.out.println("key1: " + cache.getIfPresent("key1"));
        System.out.println("key3: " + cache.getIfPresent("key3"));
        
        // 自动加载缓存（当key不存在时）
        LoadingCache<String, String> loadingCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) {
                        System.out.println("[CacheLoader] 加载 key: " + key);
                        return "加载的值-" + key + "-" + System.currentTimeMillis();
                    }
                });
        
        try {
            System.out.println("第一次获取（触发加载）: " + loadingCache.get("user1"));
            System.out.println("第二次获取（使用缓存）: " + loadingCache.get("user1"));
            
            // 批量获取
            System.out.println("批量获取（user2不在缓存）:");
            List<String> keys = Arrays.asList("user1", "user2", "user3");
            System.out.println("结果: " + loadingCache.getAll(keys));
            
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        
        // 缓存统计
        System.out.println("命中率: " + cache.stats().hitRate());
        System.out.println("平均加载时间: " + cache.stats().averageLoadPenalty() + "ns");
        
        // 移除监听器
        cache = CacheBuilder.newBuilder()
                .maximumSize(10)
                .removalListener(new RemovalListener<String, String>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, String> notification) {
                        System.out.println("[RemovalListener] 移除原因: " + notification.getCause() + 
                                          ", key: " + notification.getKey());
                    }
                })
                .build();
        
        for (int i = 0; i < 15; i++) {
            cache.put("key" + i, "value" + i);
        }
        
        System.out.println();
    }
    
    /**
     * 演示 RateLimiter - 令牌桶限流算法
     */
    private static void demoRateLimiter() {
        System.out.println("========== 2. RateLimiter（限流器）==========");
        
        // 创建限流器：每秒允许2个请求
        RateLimiter limiter = RateLimiter.create(2.0);
        
        System.out.println("获取5个令牌所需时间:");
        for (int i = 1; i <= 5; i++) {
            double waitTime = limiter.acquire(); // 阻塞直到获取令牌
            System.out.println(String.format("请求 %d: 等待了 %.2f 秒", i, waitTime));
        }
        
        // 尝试获取（非阻塞）
        RateLimiter tryLimiter = RateLimiter.create(1.0);
        
        System.out.println("\n尝试获取令牌:");
        for (int i = 1; i <= 5; i++) {
            boolean acquired = tryLimiter.tryAcquire();
            System.out.println(String.format("尝试 %d: %s", i, acquired ? "成功" : "失败"));
            
            if (!acquired) {
                try { Thread.sleep(500); } catch (InterruptedException e) {}
            }
        }
        
        // 预热模式
        System.out.println("\n预热模式（从冷启动开始缓慢增加速率）:");
        RateLimiter warmupLimiter = RateLimiter.create(10.0, 3, TimeUnit.SECONDS);
        
        long start = System.currentTimeMillis();
        for (int i = 1; i <= 20; i++) {
            warmupLimiter.acquire();
            long elapsed = System.currentTimeMillis() - start;
            System.out.println(String.format("请求 %d: %.3f秒", i, elapsed / 1000.0));
        }
        
        System.out.println();
    }
    
    /**
     * 演示 Hashing - 哈希工具
     */
    private static void demoHashing() {
        System.out.println("========== 3. Hashing（哈希/散列工具）==========");
        
        String input = "Hello, Guava!";
        
        // MD5
        String md5 = Hashing.md5().hashString(input, StandardCharsets.UTF_8).toString();
        System.out.println("MD5:    " + md5);
        
        // SHA-256
        String sha256 = Hashing.sha256().hashString(input, StandardCharsets.UTF_8).toString();
        System.out.println("SHA-256: " + sha256);
        
        // SHA-512
        String sha512 = Hashing.sha512().hashString(input, StandardCharsets.UTF_8).toString();
        System.out.println("SHA-512: " + sha512.substring(0, 32) + "...");
        
        // 一致性哈希
        String consistent = Hashing.murmur3_128().hashString(input, StandardCharsets.UTF_8).toString();
        System.out.println("Murmur3 (128位): " + consistent);
        
        // 计算海明距离
        long hash1 = Hashing.murmur3_32().hashString("abc", StandardCharsets.UTF_8).asInt();
        long hash2 = Hashing.murmur3_32().hashString("abd", StandardCharsets.UTF_8).asInt();
        System.out.println("海明距离（abc vs abd）: " + Long.bitCount(hash1 ^ hash2));
        
        // 哈希分片（一致性哈希的一种应用）
        System.out.println("\n一致性哈希分片示例:");
        for (int i = 0; i < 10; i++) {
            String key = "user" + i + "@example.com";
            int shard = Hashing.consistentHash(
                    Hashing.murmur3_128().hashString(key, StandardCharsets.UTF_8).asLong(),
                    5);  // 5个分片
            System.out.println(String.format("key: %-25s -> 分片: %d", key, shard));
        }
        
        // Bloom Filter（布隆过滤器）需要额外依赖
        System.out.println("\n注意：Bloom Filter 需要 guava 的 com.google.common.hash.BloomFilter");
        
        System.out.println();
    }
    
    /**
     * 演示 EventBus - 事件总线（发布-订阅模式）
     */
    private static void demoEventBus() {
        System.out.println("========== 4. EventBus（事件总线）==========");
        
        // 创建事件总线
        EventBus eventBus = new EventBus("DemoEventBus");
        
        // 注册订阅者
        UserService userService = new UserService();
        OrderService orderService = new OrderService();
        AuditService auditService = new AuditService();
        
        eventBus.register(userService);
        eventBus.register(orderService);
        eventBus.register(auditService);
        
        // 发布事件
        System.out.println("发布 UserCreatedEvent:");
        eventBus.post(new UserCreatedEvent("alice", "alice@example.com"));
        
        System.out.println("\n发布 OrderPlacedEvent:");
        eventBus.post(new OrderPlacedEvent("order123", 299.99, "alice"));
        
        System.out.println("\n发布 GenericEvent:");
        eventBus.post(new GenericEvent("这是一个通用事件"));
        
        // 注销订阅者
        eventBus.unregister(orderService);
        
        System.out.println("\n注销 OrderService 后发布 OrderPlacedEvent:");
        eventBus.post(new OrderPlacedEvent("order456", 499.99, "bob"));
        
        System.out.println();
    }
    
    /**
     * 演示 ListenableFuture - 可监听结果的异步任务
     */
    private static void demoListenableFuture() throws Exception {
        System.out.println("========== 5. ListenableFuture（可监听Future）==========");
        
        // 创建线程池
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(
                Executors.newFixedThreadPool(3)
        );
        
        // 提交异步任务
        ListenableFuture<String> future = executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                System.out.println("[任务开始] 线程: " + Thread.currentThread().getName());
                Thread.sleep(1000);
                return "任务完成 - " + System.currentTimeMillis();
            }
        });
        
        // 添加回调
        Futures.addCallback(future, new FutureCallback<String>() {
            @Override
            public void onSuccess(String result) {
                System.out.println("[回调] 成功: " + result);
            }
            
            @Override
            public void onFailure(Throwable t) {
                System.out.println("[回调] 失败: " + t.getMessage());
            }
        }, executor);
        
        // 多个任务组合
        System.out.println("提交3个并行任务:");
        ListenableFuture<String> task1 = executor.submit(() -> {
            Thread.sleep(500);
            return "任务1完成";
        });
        
        ListenableFuture<String> task2 = executor.submit(() -> {
            Thread.sleep(300);
            return "任务2完成";
        });
        
        ListenableFuture<String> task3 = executor.submit(() -> {
            Thread.sleep(700);
            return "任务3完成";
        });
        
        // 所有任务完成后执行
        ListenableFuture<List<String>> allFutures = Futures.allAsList(task1, task2, task3);
        
        Futures.addCallback(allFutures, new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> results) {
                System.out.println("[所有任务完成] 结果: " + results);
            }
            
            @Override
            public void onFailure(Throwable t) {
                System.out.println("[某个任务失败] " + t.getMessage());
            }
        }, executor);
        
        // 阻塞获取结果
        System.out.println("主线程等待所有任务完成...");
        List<String> results = allFutures.get();
        System.out.println("获取到的结果: " + results);
        
        // 转换结果
        ListenableFuture<Integer> transformed = Futures.transform(task1, 
                String::length, executor);
        
        transformed.addListener(() -> {
            try {
                System.out.println("转换后结果长度: " + transformed.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
        
        // 优雅关闭
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        
        System.out.println();
    }
    
    /**
     * 演示 Files - 文件工具（已弃用，推荐 Java NIO）
     * 但在 Java 8 之前提供了有用的功能
     */
    private static void demoFiles() throws IOException {
        System.out.println("========== 6. Files 工具（已弃用）==========");
        
        // 注意：Guava Files 工具已在 31.0 版本弃用，推荐使用 Java NIO
        System.out.println("Guava Files 工具类已在较新版本弃用，建议使用 Java NIO 的 Files 类");
        
        // 演示一些仍有用的功能
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "guava-demo");
        tempDir.mkdirs();
        File testFile = new File(tempDir, "test.txt");
        
        // 写入文件（Guava方式，不推荐在新项目中使用）
        com.google.common.io.Files.write("测试内容".getBytes(StandardCharsets.UTF_8), testFile);
        
        // 读取文件
        byte[] content = com.google.common.io.Files.toByteArray(testFile);
        System.out.println("读取内容: " + new String(content, StandardCharsets.UTF_8));
        
        // 复制文件
        File copyFile = new File(tempDir, "test_copy.txt");
        com.google.common.io.Files.copy(testFile, copyFile);
        System.out.println("文件复制成功: " + copyFile.exists());
        
        // 移动文件
        File movedFile = new File(tempDir, "test_moved.txt");
        com.google.common.io.Files.move(testFile, movedFile);
        System.out.println("文件移动成功: " + movedFile.exists() + ", 原文件: " + testFile.exists());
        
        // 获取文件哈希
        String fileHash = com.google.common.io.Files.asByteSource(movedFile)
                .hash(Hashing.sha256())
                .toString();
        System.out.println("文件SHA-256哈希: " + fileHash);
        
        // 清理
        movedFile.delete();
        copyFile.delete();
        tempDir.delete();
        
        System.out.println("\n建议: 在新项目中，使用 java.nio.file.Files 替代 Guava Files");
        System.out.println();
    }
    
    // ========== EventBus 相关类 ==========
    
    static class UserCreatedEvent {
        private String username;
        private String email;
        
        public UserCreatedEvent(String username, String email) {
            this.username = username;
            this.email = email;
        }
        
        public String getUsername() { return username; }
        public String getEmail() { return email; }
    }
    
    static class OrderPlacedEvent {
        private String orderId;
        private double amount;
        private String userId;
        
        public OrderPlacedEvent(String orderId, double amount, String userId) {
            this.orderId = orderId;
            this.amount = amount;
            this.userId = userId;
        }
        
        public String getOrderId() { return orderId; }
        public double getAmount() { return amount; }
        public String getUserId() { return userId; }
    }
    
    static class GenericEvent {
        private String message;
        
        public GenericEvent(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
    }
    
    static class UserService {
        @Subscribe
        public void onUserCreated(UserCreatedEvent event) {
            System.out.println("[UserService] 用户创建: " + event.getUsername() + ", email: " + event.getEmail());
        }
        
        @Subscribe
        public void onOrderPlaced(OrderPlacedEvent event) {
            System.out.println("[UserService] 用户 " + event.getUserId() + " 下单: " + event.getOrderId());
        }
    }
    
    static class OrderService {
        @Subscribe
        public void onOrderPlaced(OrderPlacedEvent event) {
            System.out.println("[OrderService] 订单创建: " + event.getOrderId() + ", 金额: " + event.getAmount());
        }
    }
    
    static class AuditService {
        @Subscribe
        public void onAnyEvent(Object event) {
            System.out.println("[AuditService] 审计事件: " + event.getClass().getSimpleName());
        }
    }
}