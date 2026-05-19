# Day 18：Caffeine - 高性能本地缓存库

## 工具简介

**Caffeine** 是目前 Java 生态中性能最优的本地缓存库，由 Ben Manes 开发并维护，已被 Spring Boot 2.x/3.x 官方采纳为默认本地缓存实现（替代 Guava Cache）。

- **GitHub**: https://github.com/ben-manes/caffeine
- **Stars**: 16k+
- **版本**: 2.9.3（Java 8 兼容）/ 3.x（Java 11+）
- **License**: Apache 2.0

### 核心优势

| 特性 | 说明 |
|------|------|
| **W-TinyLFU 算法** | 业界最优淘汰算法，命中率比 LRU 高 10%~30% |
| **高并发性能** | 吞吐量接近 ConcurrentHashMap，远超 Guava Cache |
| **异步加载** | AsyncLoadingCache，加载不阻塞读取线程 |
| **多种过期策略** | 按写入时间、访问时间、自定义 Expiry 灵活配置 |
| **Spring Boot 集成** | 官方支持，一行配置即可启用 @Cacheable |
| **缓存统计** | 内置命中率、加载次数、驱逐次数监控 |

### Caffeine vs Guava Cache vs Ehcache

| 对比项 | Caffeine | Guava Cache | Ehcache 3 |
|--------|----------|-------------|-----------|
| 淘汰算法 | W-TinyLFU | LRU | LRU |
| 并发性能 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| 异步加载 | ✅ | ❌ | ❌ |
| 磁盘持久化 | ❌ | ❌ | ✅ |
| Spring Boot 默认 | ✅ | ❌（已弃用） | ✅ |
| 适用场景 | 纯内存、高并发 | 兼容老项目 | 需要持久化 |

---

## Maven 依赖配置

### Java 8 项目（推荐 2.x）
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>2.9.3</version>
</dependency>
```

### Java 11+ 项目（推荐 3.x）
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>
```

### Spring Boot 项目（自动配置）
```xml
<!-- Spring Boot Cache Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Caffeine 本体 -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <!-- Spring Boot BOM 管理版本，无需手动指定 -->
</dependency>
```

---

## 核心 API

### 1. Cache（手动缓存）
```java
Cache<String, User> cache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();

// 写
cache.put("user:1", new User(1, "Alice"));

// 读（命中返回值，未命中返回 null）
User user = cache.getIfPresent("user:1");

// 读（未命中则执行 loader 加载并缓存）
User user2 = cache.get("user:2", id -> userRepository.findById(id));

// 删
cache.invalidate("user:1");
cache.invalidateAll();  // 清空

// 批量写
cache.putAll(mapOf("user:3", u3, "user:4", u4));
```

### 2. LoadingCache（自动加载缓存）
```java
LoadingCache<Long, User> cache = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(userId -> userRepository.findById(userId));  // CacheLoader

// get 自动触发 CacheLoader（无需手动判断 null）
User user = cache.get(1L);

// 批量 get（未缓存的 key 批量触发 CacheLoader）
Map<Long, User> users = cache.getAll(Arrays.asList(1L, 2L, 3L));

// 手动刷新（后台异步，不阻塞读取）
cache.refresh(1L);
```

### 3. AsyncLoadingCache（异步加载缓存）
```java
AsyncLoadingCache<Long, User> asyncCache = Caffeine.newBuilder()
        .maximumSize(500)
        .buildAsync(userId -> {
            // 在线程池中执行异步加载
            return userService.findByIdAsync(userId);
        });

// 返回 CompletableFuture，不阻塞
CompletableFuture<User> future = asyncCache.get(1L);
future.thenAccept(user -> System.out.println("加载完成: " + user));
```

### 4. 过期策略
```java
Caffeine.newBuilder()
    // 写入后 10 分钟过期（TTL，推荐大多数场景）
    .expireAfterWrite(10, TimeUnit.MINUTES)

    // 最后一次访问后 10 分钟过期（适合活跃数据续期）
    .expireAfterAccess(10, TimeUnit.MINUTES)

    // 自定义：每个 key 不同过期时间
    .expireAfter(new Expiry<K, V>() {
        public long expireAfterCreate(K key, V value, long currentTime) {
            return key.startsWith("vip:") ?
                TimeUnit.HOURS.toNanos(1) :
                TimeUnit.MINUTES.toNanos(5);
        }
        public long expireAfterUpdate(K key, V value, long currentTime, long currentDuration) { return currentDuration; }
        public long expireAfterRead(K key, V value, long currentTime, long currentDuration) { return currentDuration; }
    })

    // 写入后 5 分钟自动后台刷新（不过期，返回旧值同时刷新）
    .refreshAfterWrite(5, TimeUnit.MINUTES)
```

### 5. 容量策略
```java
// 按条数：最多 1000 条
Caffeine.newBuilder().maximumSize(1000)

// 按权重：总权重不超过 10MB（适合大小不均的 value）
Caffeine.newBuilder()
    .maximumWeight(10 * 1024 * 1024)           // 10MB
    .weigher((String k, byte[] v) -> v.length)  // 权重 = 字节数
```

### 6. 移除监听器
```java
Caffeine.newBuilder()
    .removalListener((key, value, cause) -> {
        // cause: EXPLICIT / REPLACED / EXPIRED / SIZE / COLLECTED
        if (cause.wasEvicted()) {
            log.info("缓存驱逐: key={}, cause={}", key, cause);
        }
    })
```

### 7. 缓存统计
```java
Cache<K, V> cache = Caffeine.newBuilder()
        .recordStats()   // 开启统计
        .build();

CacheStats stats = cache.stats();
double hitRate = stats.hitRate();          // 命中率
long hitCount  = stats.hitCount();         // 命中次数
long missCount = stats.missCount();        // 未命中次数
long loadCount = stats.loadCount();        // 加载次数
double avgLoadNanos = stats.averageLoadPenalty();  // 平均加载时间（ns）
long evictionCount  = stats.evictionCount();       // 驱逐次数
```

---

## Spring Boot 集成方式

### 方式一：自动配置（推荐）

**Step 1：启用缓存**
```java
@SpringBootApplication
@EnableCaching
public class Application { }
```

**Step 2：配置 application.yml**
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=300s
```

**Step 3：使用注解**
```java
@Service
public class UserService {

    @Cacheable(cacheNames = "users", key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id);  // 首次查 DB，后续命中缓存
    }

    @CachePut(cacheNames = "users", key = "#user.id")
    public User update(User user) {
        return userRepository.save(user);   // 更新 DB + 同步更新缓存
    }

    @CacheEvict(cacheNames = "users", key = "#id")
    public void delete(Long id) {
        userRepository.deleteById(id);      // 删除 DB + 清除缓存
    }

    @CacheEvict(cacheNames = "users", allEntries = true)
    public void clearAll() { }             // 清空整个缓存
}
```

### 方式二：多 cacheName 不同策略（CacheManager Bean）

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // 为不同 cacheName 设置不同策略
        Map<String, CaffeineSpec> specs = new HashMap<>();
        specs.put("users",    CaffeineSpec.parse("maximumSize=500,expireAfterWrite=300s"));
        specs.put("products", CaffeineSpec.parse("maximumSize=1000,expireAfterWrite=600s"));
        specs.put("config",   CaffeineSpec.parse("maximumSize=100,expireAfterWrite=3600s"));
        manager.setCacheSpecMap(specs);

        return manager;
    }
}
```

### 方式三：直接注入 Cache Bean（精细控制）

```java
@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<Long, User> userCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.info("用户缓存移除: userId={}, cause={}", key, cause))
                .build();
    }
}

@Service
public class UserService {

    @Autowired
    private Cache<Long, User> userCache;

    public User findById(Long id) {
        return userCache.get(id, userRepository::findById);
    }
}
```

### Spring Boot 3.x Actuator 监控（配合 Micrometer）
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
```

```java
// 注册到 Micrometer，暴露到 /actuator/metrics/cache.gets 等
@Bean
public CacheMetricsRegistrar caffeineMetrics(MeterRegistry registry, CacheManager cacheManager) {
    return new CacheMetricsRegistrar(registry, cacheManager, Collections.emptyList());
}
```

---

## 典型使用场景

### 1. 配置中心缓存
```java
// 配置项变化少，读取频繁，适合长 TTL + refreshAfterWrite
LoadingCache<String, String> configCache = Caffeine.newBuilder()
        .maximumSize(200)
        .refreshAfterWrite(5, TimeUnit.MINUTES)   // 5 分钟后台刷新
        .build(configService::getConfig);
```

### 2. 防缓存穿透（空值缓存）
```java
LoadingCache<Long, Optional<User>> cache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(id -> Optional.ofNullable(userRepository.findById(id)));

// 使用
Optional<User> user = cache.get(userId);
if (!user.isPresent()) {
    throw new UserNotFoundException(userId);
}
```

### 3. 接口限流计数器
```java
Cache<String, AtomicInteger> rateLimitCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)  // 1 分钟窗口
        .build();

boolean allow(String userId) {
    AtomicInteger count = rateLimitCache.get(userId, k -> new AtomicInteger(0));
    return count.incrementAndGet() <= 100;  // 每分钟最多 100 次
}
```

### 4. 多级缓存（本地 + Redis）
```java
// L1：Caffeine（本地，毫秒级）  L2：Redis（分布式，百毫秒级）
LoadingCache<String, Object> l1Cache = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build(key -> {
            Object v = redisTemplate.opsForValue().get(key);
            if (v != null) return v;
            Object dbVal = db.query(key);
            redisTemplate.opsForValue().set(key, dbVal, 5, TimeUnit.MINUTES);
            return dbVal;
        });
```

---

## 注意事项

### ⚠️ Bug 风险

1. **不能存 null**  
   Caffeine 的 `put(key, null)` 和 `CacheLoader` 返回 `null` 均会抛 `NullPointerException`。  
   解决：用 `Optional<V>` 包装空值，或用特殊占位符对象。

2. **estimatedSize() 不精确**  
   由于异步清理机制，`estimatedSize()` 在清理完成前可能高于实际大小，不可用于精确统计。

3. **weakKeys 导致 equals/hashCode 问题**  
   使用 `weakKeys()` 时，key 的相等性改为 `==`（引用相等）而非 `equals()`，可能导致意外 miss。  
   **建议：除非确有必要，不要使用 weakKeys()。**

4. **refreshAfterWrite 与 expireAfterWrite 不能同时使用**  
   若两者同时配置，`expireAfterWrite` 仍然有效，到期后下一次读触发同步加载（refreshAfterWrite 失效）。  
   推荐：只用 `expireAfterWrite`（强一致），或只用 `refreshAfterWrite` + 较大的 `expireAfterWrite`（软过期）。

5. **移除监听器不保证同步执行**  
   `removalListener` 默认在触发移除的线程中执行，但异步驱逐（SIZE/EXPIRED）可能在维护线程中执行，不要在监听器中做阻塞操作。

### ⚡ 性能问题

1. **recordStats() 有性能开销**  
   开统计会增加约 5%~10% 的开销，生产环境按需开启，建议通过 JVM flag 或配置文件控制。

2. **maximumWeight 的 weigher 不能返回 0**  
   权重必须 ≥ 1，否则会抛 `IllegalArgumentException`。

3. **大量 invalidate() 性能影响**  
   频繁调用 `invalidateAll()` 会触发大量移除回调，避免在热路径上使用。

### 🚫 使用限制

1. **不支持分布式缓存**  
   Caffeine 是纯本地内存缓存，多节点之间不共享缓存数据。需要分布式一致性时必须结合 Redis 等分布式缓存。

2. **JVM 重启后缓存数据丢失**  
   没有持久化能力，重启后需要重新预热。

3. **maximumSize 和 maximumWeight 不能同时使用**  
   只能选其一。

4. **softValues/weakValues 不支持 maximumSize**  
   引用类型缓存只能使用 `maximumWeight` 进行容量控制。

5. **Caffeine 2.x 与 3.x API 差异**  
   3.x 需要 Java 11+，且移除了部分过时 API。Spring Boot 3.x 默认使用 Caffeine 3.x。

---

## 运行方法

### 编译打包
```bash
cd caffeine-demo
mvn clean package -DskipTests
```

### 运行演示
```bash
# 基础演示（手动缓存、LoadingCache、过期、容量、统计）
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.caffeine.CaffeineBasicDemo"

# 进阶演示（异步缓存、移除监听器、自定义过期、引用缓存）
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.caffeine.CaffeineAdvancedDemo"

# 实战演示（多级缓存、穿透防护、雪崩防护、计数器、性能测试）
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.caffeine.CaffeinePracticalDemo"
```

### 或直接运行 JAR
```bash
java -cp target/caffeine-demo-1.0.0.jar com.example.caffeine.CaffeineBasicDemo
java -cp target/caffeine-demo-1.0.0.jar com.example.caffeine.CaffeineAdvancedDemo
java -cp target/caffeine-demo-1.0.0.jar com.example.caffeine.CaffeinePracticalDemo
```

---

## 延伸阅读

- [Caffeine GitHub Wiki](https://github.com/ben-manes/caffeine/wiki)
- [W-TinyLFU 算法论文](https://dl.acm.org/doi/10.1145/3149149)
- [Spring Boot 缓存文档](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.caching)
- [Caffeine vs Guava Benchmarks](https://github.com/ben-manes/caffeine/wiki/Benchmarks)
