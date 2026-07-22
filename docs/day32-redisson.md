# Day 32: Redisson — 高性能 Redis Java 客户端

> 学习日期: 2026-07-22  
> 学习人: 团队技术提升小组

---

## 一、工具简介

**Redisson** 是一个基于 Redis 的 Java 驻内存数据网格（In-Memory Data Grid），它不仅是一个 Redis 客户端，更提供了大量分布式 Java 常用对象的实现。

| 属性 | 说明 |
|------|------|
| **GitHub** | https://github.com/redisson/redisson |
| **官网** | https://redisson.org |
| **星标** | 22k+ |
| **版本** | 3.23.5（Java 8 兼容） |
| **许可证** | Apache 2.0 |

### 核心特性

- **分布式 Java 对象**：RMap、RSet、RList、RQueue、RAtomicLong 等，接口与 JDK 集合一致
- **分布式锁 RLock**：可重入锁、公平锁、红锁（RedLock），内置看门狗自动续期
- **分布式限流 RRateLimiter**：基于令牌桶算法，全局限速
- **延迟队列 RDelayedQueue**：元素在指定延迟后才可被消费
- **发布订阅 RTopic**：基于 Redis Pub/Sub 的广播消息
- **布隆过滤器 RBloomFilter**：高效判重，缓存穿透防护
- **多种连接模式**：单机、哨兵、集群、主从复制

### 与 Jedis / Lettuce 对比

| 特性 | Redisson | Jedis | Lettuce |
|------|----------|-------|---------|
| 线程安全 | ✅ 单例使用 | ❌ 需连接池 | ✅ 单例使用 |
| 分布式锁 | ✅ 内置 | ❌ 需手写 Lua | ❌ 需手写 Lua |
| 分布式集合 | ✅ 丰富 | ❌ 无 | ❌ 无 |
| 异步/响应式 | ✅ 支持 | ❌ 阻塞为主 | ✅ 原生支持 |
| 学习曲线 | 低（接口熟悉） | 中 | 中 |
| 性能 | 高 | 高 | 极高 |

> **选型建议**：需要分布式锁/集合/队列等业务对象 → Redisson；仅需简单 KV 操作 → Lettuce。

---

## 二、Maven 依赖

### 2.1 独立使用

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.23.5</version>
</dependency>
```

### 2.2 Spring Boot 集成（推荐）

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.23.5</version>
</dependency>
```

---

## 三、Spring Boot 集成方式

### 3.1 application.yml 配置

```yaml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://127.0.0.1:6379"
          password: null
          database: 0
          connectionMinimumIdleSize: 10
          connectionPoolSize: 64
          idleConnectionTimeout: 10000
          connectTimeout: 10000
          timeout: 3000
          retryAttempts: 3
          retryInterval: 1500
```

### 3.2 集群模式配置

```yaml
spring:
  redis:
    redisson:
      config: |
        clusterServersConfig:
          nodeAddresses:
            - "redis://192.168.0.1:7000"
            - "redis://192.168.0.2:7000"
            - "redis://192.168.0.3:7000"
          scanInterval: 2000
          password: null
```

### 3.3 哨兵模式配置

```yaml
spring:
  redis:
    redisson:
      config: |
        sentinelServersConfig:
          masterName: myMaster
          sentinelAddresses:
            - "redis://192.168.0.1:26379"
            - "redis://192.168.0.2:26379"
          password: null
```

### 3.4 服务中注入使用

```java
@Service
public class OrderService {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 创建订单（带分布式锁）
     */
    public void createOrder(String orderId) {
        RLock lock = redissonClient.getLock("lock:order:" + orderId);
        lock.lock();
        try {
            // 幂等性检查 + 业务逻辑
        } finally {
            lock.unlock();
        }
    }

    /**
     * 扣减库存（原子操作）
     */
    public boolean deductStock(String skuId, int quantity) {
        RAtomicLong stock = redissonClient.getAtomicLong("stock:" + skuId);
        long current = stock.get();
        if (current >= quantity) {
            return stock.addAndGet(-quantity) >= 0;
        }
        return false;
    }
}
```

---

## 四、核心功能代码示例

### 4.1 分布式锁（RLock）

```java
RLock lock = redissonClient.getLock("myLock");

// 方式1: 阻塞获取，看门狗自动续期（推荐）
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();
}

// 方式2: 非阻塞尝试获取
try {
    boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
    if (acquired) {
        try {
            // 业务逻辑
        } finally {
            lock.unlock();
        }
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### 4.2 限流器（RRateLimiter）

```java
RRateLimiter rateLimiter = redissonClient.getRateLimiter("myRateLimiter");

// 初始化：全局限速 10 次/秒
rateLimiter.trySetRate(
    RateType.OVERALL,   // 全局限速
    10,                 // 速率
    1,                  // 速率时间长度
    RateIntervalUnit.SECONDS
);

// 消费一个令牌
if (rateLimiter.tryAcquire()) {
    // 处理请求
} else {
    // 限流，返回"系统繁忙"
}
```

### 4.3 延迟队列（RDelayedQueue）

```java
RQueue<String> destinationQueue = redissonClient.getQueue("orderQueue");
RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(destinationQueue);

// 投递延迟消息（30分钟后可消费）
delayedQueue.offer("ORDER_001", 30, TimeUnit.MINUTES);

// 消费者轮询（实际应在独立线程/服务中）
String orderId = destinationQueue.poll();
if (orderId != null) {
    // 执行订单超时取消逻辑
}
```

### 4.4 发布订阅（RTopic）

```java
RTopic topic = redissonClient.getTopic("orderEvents");

// 订阅消息
topic.addListener(String.class, (channel, msg) -> {
    System.out.println("收到消息: " + msg);
});

// 发布消息
topic.publish("订单1001 已支付");
```

### 4.5 布隆过滤器（RBloomFilter）

```java
RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("userFilter");

// 初始化：预期 100000 个元素，误判率 1%
bloomFilter.tryInit(100000, 0.01);

bloomFilter.add("user_001");
boolean mightExist = bloomFilter.contains("user_999");
```

---

## 五、注意事项

### 5.1 ⚠️ Bug 风险

1. **锁未释放（死锁）**
   - **问题**：业务异常或 JVM 崩溃导致 `unlock()` 未执行
   - **解决**：务必写在 `try-finally` 中；Redisson 看门狗会在客户端断开时自动释放锁

2. **锁的续期与过期**
   - **问题**：手动设置 `leaseTime` 后，看门狗不会启动，业务执行时间超过锁有效期会导致锁提前释放
   - **解决**：使用 `lock()` 不指定 leaseTime，让看门狗自动续期；或确保 leaseTime 大于业务最大执行时间

3. **可重入锁计数错误**
   - **问题**：同一线程加锁 N 次，必须解锁 N 次，否则锁不会真正释放
   - **解决**：每次 `lock()` 对应一次 `unlock()`，建议封装为工具类自动配对

4. **Redis 主从切换导致锁丢失**
   - **问题**：主节点挂掉，从节点晋升为主节点，但锁信息可能未同步
   - **解决**：使用 RedLock 算法（多个独立 Redis 实例）；或确保 Redis 配置为强同步

### 5.2 ⚠️ 性能问题

1. **大对象序列化**
   - **问题**：默认 JDK 序列化对大对象性能差、占用空间大
   - **解决**：替换为 `JsonJacksonCodec`、`Kryo5Codec` 或 `FstCodec`
   ```java
   Config config = new Config();
   config.setCodec(new JsonJacksonCodec());
   ```

2. **高频计数场景**
   - **问题**：`RAtomicLong.incrementAndGet()` 在极高并发下仍有一定网络开销
   - **解决**：批量累加或使用本地缓存 + 定期同步策略

3. **锁竞争严重**
   - **问题**：高并发下大量线程竞争同一把锁，导致 Redis 压力大
   - **解决**：
     - 缩小锁粒度（按用户 ID/订单 ID 分段）
     - 使用 Lua 脚本实现原子操作，避免加锁
     - 引入本地缓存（Caffeine）+ Redis 二级缓存

4. **延迟队列精度**
   - **问题**：RDelayedQueue 的延迟精度受 `poll()` 轮询间隔影响
   - **解决**：在独立线程中循环 `poll()`，间隔 100-500ms；或使用 Redis 5.0+ 的 Streams

### 5.3 ⚠️ 使用限制

1. **Redis 版本要求**
   - Redisson 3.23.x 要求 Redis 3.0+
   - 部分高级功能（如 Redis Stream）需要 Redis 5.0+

2. **集群模式限制**
   - 批量操作（如 `RMap.readAllMap()`）在集群模式下要求所有 key 在同一 slot
   - 使用 `RedissonBloomFilter` 等高级对象时，需确保配置正确

3. **序列化兼容性**
   - 更换序列化方式后，已存储的数据可能无法反序列化
   - 生产环境更换 Codec 前需清理旧数据或做好迁移方案

4. **订阅发布可靠性**
   - `RTopic` 基于 Redis Pub/Sub，消息不持久化
   - 消费者不在线时消息丢失，重要消息请使用 `RQueue` 或 Redis Stream

5. **连接泄漏**
   - 每个 `RedissonClient` 实例内部维护连接池，应作为单例使用
   - 禁止每次操作都 `create` / `shutdown`

---

## 六、运行方法

### 6.1 启动 Redis

```bash
# Docker 方式（推荐）
docker run -d -p 6379:6379 --name redis redis:7-alpine

# Windows（下载 Redis-x64-xxx.zip）
redis-server.exe

# Linux
sudo service redis-server start
```

### 6.2 编译运行

```bash
# 进入项目目录
cd redisson-demo

# 编译
mvn clean package -DskipTests

# 运行基础演示
java -cp target/redisson-demo-1.0-SNAPSHOT.jar com.example.redisson.RedissonBasicDemo

# 运行进阶演示
java -cp target/redisson-demo-1.0-SNAPSHOT.jar com.example.redisson.RedissonAdvancedDemo

# 运行实战演示
java -cp target/redisson-demo-1.0-SNAPSHOT.jar com.example.redisson.RedissonPracticalDemo
```

### 6.3 IDEA 中直接运行

1. 右键 `RedissonBasicDemo.java` → `Run`
2. 确保本地 Redis 已启动（端口 6379）
3. 若 Redis 在其他地址，修改代码中的 `REDIS_ADDRESS` 常量

---

## 七、学习总结

| 能力 | 掌握程度 |
|------|----------|
| Redis 基础数据结构操作（String/Hash/Set/List） | ✅ 熟练 |
| 分布式锁（可重入 + 看门狗续期） | ✅ 熟练 |
| 分布式限流（令牌桶） | ✅ 熟练 |
| 延迟队列（订单超时取消） | ✅ 熟练 |
| 发布订阅（广播消息） | ✅ 了解 |
| 布隆过滤器（缓存穿透防护） | ✅ 了解 |
| Spring Boot 集成 | ✅ 熟练 |

### 下一步学习建议

1. **RedLock 算法**：多 Redis 实例实现高可用分布式锁
2. **Redis Stream**：替代 Pub/Sub 实现持久化消息队列
3. **Redisson Live Object Service**：将 Java 对象直接映射到 Redis
4. **结合 Spring Cache**：使用 Redisson 作为 Spring Cache 的 Redis 实现

---

## 参考链接

- [Redisson GitHub](https://github.com/redisson/redisson)
- [Redisson 官方文档](https://redisson.org/documentation.html)
- [Redis 官方文档](https://redis.io/documentation)
- [Redisson Spring Boot 集成指南](https://github.com/redisson/redisson/tree/master/redisson-spring-boot-starter)
