package com.example.redisson;

import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 基础演示：RBucket / RMap / RSet / RList / RAtomicLong
 *
 * 核心概念：
 * - Redisson 将所有 Redis 数据结构封装为 Java 熟悉的接口（Map/Set/List/AtomicLong 等）
 * - 支持自动序列化（默认 JDK 序列化，可替换为 JSON/Kryo/FST 等）
 * - 所有操作都是线程安全的
 *
 * 运行前准备：
 * - 本地启动 Redis 服务（默认端口 6379）
 * - 或者修改配置连接远程 Redis
 */
public class RedissonBasicDemo {

    /** Redis 连接地址，默认本地 */
    private static final String REDIS_ADDRESS = "redis://127.0.0.1:6379";

    public static void main(String[] args) {
        // 1. 创建配置并连接 Redis
        Config config = new Config();
        config.useSingleServer().setAddress(REDIS_ADDRESS);

        RedissonClient redisson = null;
        try {
            redisson = Redisson.create(config);
            System.out.println("✅ 成功连接到 Redis: " + REDIS_ADDRESS);

            // 2. 演示各类基础数据结构
            demoRBucket(redisson);      // 通用对象存储（String）
            demoRMap(redisson);         // Hash 结构
            demoRSet(redisson);         // Set 结构
            demoRList(redisson);        // List 结构
            demoRAtomicLong(redisson);  // 原子计数器

        } catch (Exception e) {
            System.err.println("❌ 连接 Redis 失败: " + e.getMessage());
            System.err.println("   请确保 Redis 服务已启动（默认端口 6379）");
            System.err.println("   Windows: 下载 Redis-x64-xxx.zip 运行 redis-server.exe");
            System.err.println("   Linux:   sudo service redis-server start");
            System.err.println("   Docker:  docker run -d -p 6379:6379 redis:latest");
        } finally {
            if (redisson != null && !redisson.isShutdown()) {
                redisson.shutdown();
                System.out.println("\n🔌 Redis 连接已关闭");
            }
        }
    }

    /**
     * RBucket: Redis String 类型的面向对象封装
     * - 可存储任意 Java 对象（自动序列化）
     * - 支持过期时间设置
     */
    static void demoRBucket(RedissonClient redisson) {
        System.out.println("\n========== RBucket (通用对象存储) ==========");

        RBucket<String> nameBucket = redisson.getBucket("demo:user:name");
        RBucket<Integer> ageBucket = redisson.getBucket("demo:user:age");

        // 设置值（覆盖）
        nameBucket.set("张三");
        ageBucket.set(28);
        System.out.println("设置 name=张三, age=28");

        // 读取值
        String name = nameBucket.get();
        Integer age = ageBucket.get();
        System.out.println("读取 -> name=" + name + ", age=" + age);

        // 设置带过期时间的值（30秒后过期）
        RBucket<String> tokenBucket = redisson.getBucket("demo:token:session001");
        tokenBucket.set("abc123xyz", 30, TimeUnit.SECONDS);
        System.out.println("设置 token（30秒后过期）: " + tokenBucket.get());

        // 若不存在则设置（原子性）
        boolean isNew = nameBucket.setIfAbsent("李四");
        System.out.println("尝试设置 name=李四（若不存在）-> 是否成功: " + isNew + "（当前值: " + nameBucket.get() + "）");

        // 删除
        nameBucket.delete();
        ageBucket.delete();
        System.out.println("已清理 name/age");
    }

    /**
     * RMap: Redis Hash 的封装，接口与 java.util.Map 兼容
     * - 支持本地缓存（LocalCache）提升读取性能
     * - 支持元素过期（单独为每个 key 设置 TTL）
     */
    static void demoRMap(RedissonClient redisson) {
        System.out.println("\n========== RMap (Hash 结构) ==========");

        RMap<String, String> userMap = redisson.getMap("demo:users:1001");

        // 批量写入
        userMap.put("name", "王五");
        userMap.put("email", "wangwu@example.com");
        userMap.put("department", "技术部");
        System.out.println("写入用户数据: " + userMap);

        // 读取单个字段
        System.out.println("name=" + userMap.get("name"));

        // 读取所有字段
        Map<String, String> all = userMap.readAllMap();
        System.out.println("全部字段: " + all);

        // 原子性更新：若 key 存在才替换
        String old = userMap.replace("department", "研发部");
        System.out.println("department 旧值=" + old + ", 新值=" + userMap.get("department"));

        // 使用 fastPut 异步优化（不返回旧值，性能更好）
        boolean added = userMap.fastPut("phone", "13800138000");
        System.out.println("fastPut phone -> 是否新增: " + added);

        // 清理
        userMap.delete();
        System.out.println("已清理 userMap");
    }

    /**
     * RSet: Redis Set 的封装，自动去重
     * - 支持集合运算：并集、交集、差集
     */
    static void demoRSet(RedissonClient redisson) {
        System.out.println("\n========== RSet (Set 结构) ==========");

        RSet<String> tags = redisson.getSet("demo:article:tags");

        // 添加元素（自动去重）
        tags.add("Java");
        tags.add("Redis");
        tags.add("Java");  // 重复，不会添加
        tags.addAll(Arrays.asList("Spring", "Microservices", "Cache"));
        System.out.println("标签集合（去重后）: " + tags);

        // 判断是否包含
        System.out.println("是否包含 'Java': " + tags.contains("Java"));
        System.out.println("是否包含 'Go': " + tags.contains("Go"));

        // 随机读取（不删除）
        System.out.println("随机取一个: " + tags.random());

        // 差集演示
        RSet<String> otherTags = redisson.getSet("demo:article:tags2");
        otherTags.addAll(Arrays.asList("Java", "Spring", "Cloud"));
        Set<String> diff = tags.readDiff("demo:article:tags2");
        System.out.println("差集 (tags - tags2): " + diff);

        // 清理
        tags.delete();
        otherTags.delete();
        System.out.println("已清理 tags");
    }

    /**
     * RList: Redis List 的封装，保持插入顺序
     * - 支持队列（FIFO）和栈（LIFO）操作
     */
    static void demoRList(RedissonClient redisson) {
        System.out.println("\n========== RList (List 结构) ==========");

        RList<String> queue = redisson.getList("demo:task:queue");

        // 清空并初始化
        queue.clear();
        queue.add("任务A: 发送邮件");
        queue.add("任务B: 生成报表");
        queue.add("任务C: 数据同步");
        System.out.println("任务队列: " + queue);

        // 头部添加（插队）
        queue.add(0, "任务VIP: 紧急审批");
        System.out.println("插队后: " + queue);

        // 读取（不移除）
        System.out.println("第一个任务: " + queue.get(0));
        System.out.println("队列长度: " + queue.size());

        // 移除并返回第一个（模拟队列出队）
        String first = queue.remove(0);
        System.out.println("出队: " + first);
        System.out.println("剩余队列: " + queue);

        // 裁剪列表（保留前2个）
        queue.trim(0, 1);
        System.out.println("裁剪后（保留前2个）: " + queue);

        // 清理
        queue.delete();
        System.out.println("已清理 task queue");
    }

    /**
     * RAtomicLong: Redis String 作为原子计数器的封装
     * - 所有操作原子性，适用于计数、限流、ID 生成等场景
     */
    static void demoRAtomicLong(RedissonClient redisson) {
        System.out.println("\n========== RAtomicLong (原子计数器) ==========");

        RAtomicLong counter = redisson.getAtomicLong("demo:counter:pageview");

        // 重置为 0
        counter.set(0);
        System.out.println("计数器重置为: " + counter.get());

        // 自增
        long v1 = counter.incrementAndGet();
        System.out.println("自增后: " + v1);

        // 增加指定值
        long v2 = counter.addAndGet(10);
        System.out.println("增加 10 后: " + v2);

        // 比较并设置（CAS）
        boolean casSuccess = counter.compareAndSet(11, 100);
        System.out.println("CAS 11->100: " + casSuccess + ", 当前值: " + counter.get());

        // 递减
        long v3 = counter.decrementAndGet();
        System.out.println("自减后: " + v3);

        // 获取并设置新值
        long oldVal = counter.getAndSet(0);
        System.out.println("获取旧值=" + oldVal + ", 重置为 0");

        // 清理
        counter.delete();
        System.out.println("已清理 counter");
    }
}
