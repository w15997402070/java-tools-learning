# Day 5: Google Guava - Java 工具类库

## 📖 概述

**Google Guava** 是一个开源的 Java 工具库，由 Google 开发和维护。它提供了许多 JDK 中没有的实用工具类，涵盖了集合、缓存、并发、字符串处理、IO 等多个领域。

| 项目信息 | 说明 |
|----------|------|
| **GitHub** | https://github.com/google/guava |
| **星标** | 48k+ |
| **最新版本** | 33.1.0-jre (2024-03) |
| **Maven Central** | `com.google.guava:guava:33.1.0-jre` |
| **Java 版本** | JDK 8+ |
| **状态** | ✅ 已完成 |

## 🎯 核心特性

### 1. **集合工具**
- **不可变集合** (`ImmutableList`, `ImmutableSet`, `ImmutableMap`) - 线程安全，拒绝修改
- **扩展集合** (`Multimap`, `Multiset`, `BiMap`, `Table`) - 提供更强大的数据结构
- **集合操作工具** (`Sets`, `Maps`, `Lists`) - 集合间的交并差操作

### 2. **字符串处理**
- `Strings` - 字符串判空、补全、重复等操作
- `CharMatcher` - 强大的字符匹配器，比正则更高效
- `Joiner` / `Splitter` - 比 `String.join` / `split` 更灵活

### 3. **缓存系统**
- `CacheBuilder` - 构建本地缓存，支持过期策略、最大容量
- `LoadingCache` - 自动加载缓存，当 key 不存在时调用加载器
- 支持统计、移除监听器

### 4. **并发工具**
- `ListenableFuture` - 可监听的异步任务，支持回调机制
- `RateLimiter` - 令牌桶限流算法，控制访问频率
- `EventBus` - 轻量级事件总线，实现发布-订阅模式

### 5. **其他实用工具**
- `Preconditions` - 优雅的参数验证，替代 if-throw 模式
- `Optional` (早期版本) - 空值安全处理，Java 8 `Optional` 的前身
- `Hashing` - 各种哈希算法（MD5, SHA256, MurmurHash）
- `Suppliers` - 延迟求值和缓存供应商

## 🚀 快速开始

### 1. 添加依赖

```xml
<!-- Maven -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.1.0-jre</version>
</dependency>

<!-- Gradle -->
implementation 'com.google.guava:guava:33.1.0-jre'
```

### 2. 基本使用示例

```java
// 不可变集合
ImmutableList<String> fruits = ImmutableList.of("Apple", "Banana", "Orange");

// 字符串处理
String joined = Joiner.on(", ").skipNulls().join("Java", null, "Guava");

// 参数验证
Preconditions.checkArgument(age > 0, "Age must be positive: %s", age);

// 字符串匹配
String digits = CharMatcher.digit().retainFrom("abc123def456");
```

## 📁 项目结构

```
guava-demo/
├── pom.xml                              # Maven 配置
└── src/main/java/com/example/guava/
    ├── GuavaCollectionsDemo.java        # 集合工具演示
    ├── GuavaStringUtilsDemo.java        # 字符串工具演示
    └── GuavaPracticalDemo.java          # 高级工具实战演示
```

## 📝 详细功能介绍

### 1. 不可变集合

**优势**：
- 线程安全，无需同步
- 防御性编程，避免意外的修改
- 运行时性能优化

```java
// 创建不可变集合的几种方式
// 1. of() 方法
ImmutableSet<String> colors = ImmutableSet.of("Red", "Green", "Blue");

// 2. copyOf() 方法
List<String> original = Arrays.asList("a", "b", "c");
ImmutableList<String> copy = ImmutableList.copyOf(original);

// 3. Builder 模式（适合动态构建）
ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer>builder()
    .put("Alice", 95)
    .put("Bob", 87)
    .build();
```

### 2. 扩展集合

#### Multimap (一键多值)
解决了 `Map<K, List<V>>` 的样板代码问题。

```java
// 每个 course 可以有多个 student
ArrayListMultimap<String, String> courseStudents = ArrayListMultimap.create();
courseStudents.put("Java", "Alice");
courseStudents.put("Java", "Bob");
courseStudents.put("Python", "Carol");

// 获取 Java 课程的所有学生
List<String> javaStudents = courseStudents.get("Java");
```

#### BiMap (双向 Map)
key 和 value 都唯一，可以互相查找。

```java
HashBiMap<String, Integer> userIds = HashBiMap.create();
userIds.put("alice", 1001);
userIds.put("bob", 1002);

// 正向查找
id = userIds.get("alice");      // 1001
// 反向查找
name = userIds.inverse().get(1002);  // "bob"
```

#### Multiset (带计数的 Set)
统计元素出现次数。

```java
Multiset<String> wordCount = HashMultiset.create();
wordCount.add("Java");
wordCount.add("Java");
wordCount.add("Python");

int javaCount = wordCount.count("Java");  // 2
```

#### Table (二维表格)
相当于 `Map<R, Map<C, V>>`，使用更简洁。

```java
Table<String, String, Integer> grades = HashBasedTable.create();
grades.put("Alice", "Math", 95);
grades.put("Alice", "English", 88);

Map<String, Integer> aliceGrades = grades.row("Alice");
Map<String, Integer> mathGrades = grades.column("Math");
```

### 3. 字符串工具

#### CharMatcher (字符匹配器)
比正则表达式更高效，专门用于字符匹配。

```java
// 保留数字
String digits = CharMatcher.digit().retainFrom("abc123def456");  // "123456"

// 移除控制字符
String clean = CharMatcher.invisible().removeFrom("hello\tworld\n");

// 自定义匹配规则
CharMatcher vowels = CharMatcher.anyOf("aeiouAEIOU");
String noVowels = vowels.removeFrom("Hello World");  // "Hll Wrld"
```

#### Joiner & Splitter
增强版的字符串连接和分割。

```java
// Joiner 示例
String joined1 = Joiner.on(", ").join("A", "B", "C");           // "A, B, C"
String joined2 = Joiner.on("; ").skipNulls().join("A", null, "C"); // "A; C"

// Splitter 示例
List<String> parts1 = Splitter.on(',').splitToList("a,b,c");    // ["a","b","c"]
List<String> parts2 = Splitter.on(',')
    .trimResults()
    .omitEmptyStrings()
    .splitToList(" a ,, b , c ");  // ["a","b","c"]
```

### 4. 缓存系统

Guava Cache 是一个全功能的本地缓存实现。

```java
LoadingCache<String, String> cache = CacheBuilder.newBuilder()
    .maximumSize(1000)                    // 最大缓存条目数
    .expireAfterWrite(10, TimeUnit.MINUTES)  // 写入后10分钟过期
    .expireAfterAccess(5, TimeUnit.MINUTES)  // 5分钟不访问过期
    .recordStats()                        // 开启统计功能
    .removalListener(notification -> {    // 移除监听器
        System.out.println("Removed: " + notification.getKey());
    })
    .build(new CacheLoader<String, String>() {
        @Override
        public String load(String key) {
            // 当缓存不存在时，调用此方法加载数据
            return expensiveOperation(key);
        }
    });

// 使用缓存
String value = cache.get("someKey");  // 自动加载如果不存在

// 批量获取
cache.getAll(Arrays.asList("key1", "key2", "key3"));

// 缓存统计
CacheStats stats = cache.stats();
System.out.println("命中率: " + stats.hitRate());
System.out.println("平均加载时间: " + stats.averageLoadPenalty() + "ns");
```

### 5. 并发工具

#### RateLimiter (限流器)
基于令牌桶算法实现限流。

```java
// 每秒允许2个请求
RateLimiter limiter = RateLimiter.create(2.0);

// 阻塞获取令牌
limiter.acquire();  // 阻塞直到有可用令牌

// 尝试获取（非阻塞）
if (limiter.tryAcquire()) {
    // 获取成功，执行业务逻辑
} else {
    // 获取失败，拒绝请求或返回错误
}
```

#### ListenableFuture (可监听的 Future)
增强的 Future，支持回调机制。

```java
ListeningExecutorService executor = MoreExecutors.listeningDecorator(
    Executors.newFixedThreadPool(3)
);

ListenableFuture<String> future = executor.submit(() -> {
    // 异步任务
    return "Result";
});

// 添加回调
Futures.addCallback(future, new FutureCallback<String>() {
    @Override
    public void onSuccess(String result) {
        System.out.println("Task succeeded: " + result);
    }
    
    @Override
    public void onFailure(Throwable t) {
        System.err.println("Task failed: " + t.getMessage());
    }
}, executor);
```

#### EventBus (事件总线)
轻量级的事件发布-订阅框架。

```java
// 创建事件总线
EventBus eventBus = new EventBus();

// 订阅者
class UserEventListener {
    @Subscribe
    public void handleUserCreated(UserCreatedEvent event) {
        System.out.println("User created: " + event.getUsername());
    }
}

// 注册订阅者
eventBus.register(new UserEventListener());

// 发布事件
eventBus.post(new UserCreatedEvent("alice", "alice@example.com"));
```

### 6. 其他实用工具

#### Preconditions (前置条件检查)
优雅的参数验证。

```java
public void process(String name, int age, List<String> items) {
    // 检查参数
    Preconditions.checkNotNull(name, "Name cannot be null");
    Preconditions.checkArgument(age >= 0, "Age must be non-negative: %s", age);
    Preconditions.checkElementIndex(0, items.size(), "Items list cannot be empty");
    
    // 业务逻辑...
}
```

#### Hashing (哈希工具)
提供多种哈希算法实现。

```java
// 各种哈希算法
String md5 = Hashing.md5().hashString("Hello", UTF_8).toString();
String sha256 = Hashing.sha256().hashString("Hello", UTF_8).toString();

// 一致性哈希
int shard = Hashing.consistentHash(
    Hashing.murmur3_128().hashString("key", UTF_8).asLong(),
    10  // 10个分片
);
```

## ✅ 最佳实践

### 何时使用 Guava

**推荐使用场景**：
1. **需要不可变集合** - 线程安全、防御性编程
2. **需要高级数据结构** - Multimap、Table、BiMap 等
3. **需要本地缓存** - 简单场景下的内存缓存
4. **需要限流控制** - API 限流、请求频率控制
5. **字符串处理复杂** - CharMatcher 处理字符匹配

**不推荐使用场景**：
1. **简单功能已有 JDK 实现** - 使用 JDK 的自带功能
2. **大型分布式缓存** - 考虑 Redis、Memcached
3. **Java 8+ 的高级特性** - 优先使用 Stream、Optional 等
4. **文件操作** - 使用 Java NIO 的 Files 类

### 版本选择

```xml
<!-- 根据 JDK 版本选择 -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.1.0-jre</version>  <!-- JDK 8+ -->
    <!-- 或 -->
    <!--<version>33.1.0-android</version>-->  <!-- Android 项目 -->
</dependency>
```

### 性能优化建议

1. **不可变集合** - 适合作为常量或缓存值
2. **缓存配置** - 合理设置缓存大小和过期时间
3. **CharMatcher 预编译** - 创建 static final 实例
4. **避免过度使用** - 只在需要时使用 Guava 特性

## 🔄 与 Spring Boot 集成

### 1. 依赖管理
Spring Boot 已包含对 Guava 的支持，可以直接使用。

### 2. 缓存集成
可以将 Guava Cache 与 Spring Cache 集成：

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        GuavaCacheManager cacheManager = new GuavaCacheManager();
        cacheManager.setCacheBuilder(
            CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(10, TimeUnit.MINUTES)
        );
        return cacheManager;
    }
}

@Service
class UserService {
    
    @Cacheable(cacheNames = "users")
    public User getUser(Long id) {
        // 从数据库查询
        return userRepository.findById(id);
    }
}
```

## ⚠️ 注意事项

### 已知问题

1. **版本冲突** - 注意依赖中的 Guava 版本冲突
2. **过时 API** - 部分 API 已标记为 `@Deprecated`，注意替换
3. **Android 兼容性** - 使用 `guava-android` 版本
4. **内存使用** - Cache 默认不限大小，需要显式配置

### 替代方案

| 功能 | Guava | JDK 替代 | 第三方替代 |
|------|-------|----------|-----------|
| 不可变集合 | `ImmutableList` | `List.copyOf()` (Java 10+) | - |
| Optional | `Optional` (早期) | `java.util.Optional` (Java 8+) | - |
| 字符串工具 | `CharMatcher` | 正则表达式 `Pattern` | Apache Commons Lang |
| 缓存 | Guava Cache | `ConcurrentHashMap` + 过期策略 | Caffeine, Ehcache |
| 事件总线 | `EventBus` | 观察者模式 | Spring Event |

## 📚 更多资源

1. **官方文档**：https://github.com/google/guava/wiki
2. **API 文档**：https://guava.dev/releases/latest-jre/api/docs/
3. **使用案例**：https://github.com/google/guava/wiki/GuavaExplained
4. **代码示例**：本项目中的 Demo 文件

## 🎯 实战练习

1. 使用 `Multimap` 实现一个学生选课系统
2. 使用 `Cache` 缓存 API 调用结果
3. 使用 `RateLimiter` 实现接口限流
4. 使用 `EventBus` 解耦模块间通信

---

**总结**：Guava 是一个功能强大的 Java 工具库，尤其适合需要高级集合操作、本地缓存和并发控制的场景。虽然 JDK 在后来版本中吸收了一些 Guava 的特性，但 Guava 仍然在许多领域保持着优势，是 Java 开发者工具箱中的重要组成部分。