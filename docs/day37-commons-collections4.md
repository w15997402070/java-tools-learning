# Day 37: Apache Commons Collections4 — Java 集合框架增强库

## 1. 工具简介

**Apache Commons Collections4** 是 Apache 基金会维护的 Java 集合增强库，在 JDK `java.util` 基础上补充了大量实用集合类型、工具方法和算法，能显著减少样板代码并提升代码表达力。

**核心能力**：

- **特殊集合类型**：`Bag`/`MultiSet`（可重复计数）、`BidiMap`（双向映射）、`MultiValuedMap`（一键多值）、`CaseInsensitiveMap`（大小写不敏感）、`LinkedMap`（可索引有序 Map）、`PassiveExpiringMap`（自动过期 Map）等。
- **工具类**：`CollectionUtils`、`ListUtils`、`MapUtils`、`SetUtils`、`IterableUtils`、`ComparatorUtils` 等，提供集合运算、分区、默认值、链式比较器等。
- **装饰器集合**：`PredicatedBag`、`SynchronizedMap`、`UnmodifiableList`、`GrowthList` 等，为普通集合增加校验、线程安全、自动扩容等能力。

**GitHub**: https://github.com/apache/commons-collections  
**官方文档**: https://commons.apache.org/proper/commons-collections/  
**Maven Central**: https://mvnrepository.com/artifact/org.apache.commons/commons-collections4  
**星标**: 4.1k+（Apache 官方项目，历史悠久，使用量极大）  
**版本**: 4.4（Java 8 兼容；4.5+ 建议 Java 8+）

### 同类工具对比

| 工具 | 定位 | 优势 | 劣势 |
|------|------|------|------|
| **Commons Collections4** | 集合增强与工具库 | 类型丰富、Apache 官方、与 JDK 无缝集成 | 部分 API 已标记为过时，文档相对分散 |
| **Guava Collections** | Google 集合/工具类 | API 更现代、不可变集合强大 | 版本迭代快，部分 API 已废弃 |
| **Eclipse Collections** | 高性能富集合框架 | 更丰富的原生类型集合、性能优化好 | 学习成本略高、引入包体积较大 |
| **JDK Stream API** | 函数式集合处理 | 无需额外依赖、延迟求值 | 缺乏特殊集合类型与双向映射等能力 |

---

## 2. Maven 依赖

### 2.1 基础依赖（Java 8）

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-collections4</artifactId>
    <version>4.4</version>
</dependency>
```

### 2.2 完整 pom.xml 示例

参见本项目 `commons-collections4-demo/pom.xml`。

---

## 3. 核心功能速查

### 3.1 特殊集合类型

| 类型 | 用途 | 典型场景 |
|------|------|----------|
| `Bag` / `MultiSet` | 可重复计数集合 | 词频统计、分类销量、投票计数 |
| `BidiMap` | key↔value 双向查找 | 短码↔ID、编码↔名称互查 |
| `MultiValuedMap` | 一个 key 对应多个 value | 用户角色↔权限、标签↔文章 |
| `CaseInsensitiveMap` | 大小写不敏感 Map | HTTP Header、配置项读取 |
| `LinkedMap` / `OrderedMap` | 可索引的有序 Map | 流程步骤、排行榜 |
| `PassiveExpiringMap` | 惰性过期 Map | 临时缓存、验证码、限流计数 |
| `SetUniqueList` | 去重且保持顺序 | 浏览历史、最近访问 |
| `GrowthList` | 自动扩容 List | 按索引安全写入稀疏数据 |

### 3.2 工具类常用方法

```java
// CollectionUtils：集合运算与筛选
Collection<String> union = CollectionUtils.union(listA, listB);
Collection<String> intersection = CollectionUtils.intersection(listA, listB);
Collection<String> diff = CollectionUtils.subtract(listA, listB);
Collection<Person> adults = CollectionUtils.select(persons, p -> p.getAge() >= 18);

// ListUtils：分区与空安全
List<List<Integer>> pages = ListUtils.partition(numbers, 20);
List<String> safe = ListUtils.defaultIfNull(maybeNullList, Collections.emptyList());

// MapUtils：反转与默认值
Map<V, K> inverted = MapUtils.invertMap(map);
Integer timeout = MapUtils.getInteger(config, "timeout", 5000);
Map<String, String> emptySafe = MapUtils.emptyIfNull(nullMap);

// SetUtils：集合运算视图
Set<String> unionRoles = SetUtils.union(rolesA, rolesB);
Set<String> missing = SetUtils.difference(required, owned);

// IterableUtils：Iterable 上查找
Person first = IterableUtils.find(persons, p -> p.getAge() >= 18);
int index = IterableUtils.indexOf(persons, p -> p.equals(target));

// ComparatorUtils：链式/空安全比较器
Comparator<Person> byAgeThenName = ComparatorUtils.chainedComparator(
        Comparator.comparingInt(Person::getAge),
        Comparator.comparing(Person::getName));
Comparator<Person> nullSafe = ComparatorUtils.nullLowComparator(byAgeThenName);
```

---

## 4. Spring Boot 集成

Commons Collections4 是纯工具库，**没有 Spring Boot Starter**，直接引入依赖即可在 Service/Util 中使用。下面给出几种典型集成方式。

### 4.1 作为通用集合工具注入使用

```java
@Service
public class UserService {

    private final MultiValuedMap<Long, String> userRoleCache = new ArrayListValuedHashMap<>();

    public void assignRole(Long userId, String role) {
        userRoleCache.put(userId, role);
    }

    public Collection<String> getRoles(Long userId) {
        return userRoleCache.get(userId);
    }
}
```

### 4.2 配置读取大小写不敏感

```java
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Map<String, String> headers = new CaseInsensitiveMap<>();
    // getter / setter
}
```

### 4.3 构建本地过期缓存

```java
@Component
public class VerifyCodeCache {
    // 5 分钟过期
    private final Map<String, String> cache =
            new PassiveExpiringMap<>(TimeUnit.MINUTES.toMillis(5));

    public void put(String phone, String code) {
        cache.put(phone, code);
    }

    public String get(String phone) {
        return cache.get(phone);
    }
}
```

### 4.4 与 Stream API 混合使用

Commons Collections4 与 JDK Stream 互补：用 CC4 生成特殊集合类型后，继续用 Stream 做后续链式处理。

```java
MultiValuedMap<String, String> rolePermissions = new ArrayListValuedHashMap<>();
rolePermissions.put("ADMIN", "user:read");
rolePermissions.put("ADMIN", "user:write");

rolePermissions.get("ADMIN").stream()
        .filter(p -> p.startsWith("user:"))
        .collect(Collectors.toList());
```

---

## 5. 注意事项

### 5.1 版本与包名变化

- **3.x** 与 **4.x** 包名不同：4.x 统一在 `org.apache.commons.collections4` 下。
- 不要同时引入 `commons-collections`（3.x）和 `commons-collections4`（4.x），类冲突会导致难以排查的 `NoSuchMethodError` / `ClassCastException`。
- 间接依赖可能带入 3.x，建议使用 `mvn dependency:tree` 排查并 `exclude` 掉旧版本。

### 5.2 大小写不敏感 Map 的 key 存储行为

`CaseInsensitiveMap` 内部会将 key 转换为小写后存储，`keySet()` 返回的是转换后的 key，而不是原始 key。如果业务需要保留原始 key，应使用自定义包装或 Guava `TreeMap` + `String.CASE_INSENSITIVE_ORDER`。

```java
CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
map.put("Content-Type", "json");
System.out.println(map.keySet()); // [content-type]
```

### 5.3 PassiveExpiringMap 是惰性过期

`PassiveExpiringMap` 只有在 **访问/写入** 时才会清理过期条目，不会主动启动定时任务。因此不适合需要精确控制内存释放时机的场景（如大缓存、长生命周期对象）。

### 5.4 BidiMap 要求 value 唯一

`BidiMap` 的 value 也必须是唯一的。如果 put 一个已存在的 value，会覆盖原 key 的映射，而不是报错。强依赖 value 唯一性时，业务层应前置校验。

### 5.5 MultiValuedMap 取值不可直接修改

`MultiValuedMap.get(key)` 返回的 `Collection` 是内部视图的副本或不可修改视图（取决于实现），直接 `clear()` 或 `add()` 可能不生效或抛异常。应使用 `put`、`putAll`、`remove`、`removeMapping` 等 API。

### 5.6 CollectionUtils 返回的是视图/新集合

`union`、`intersection`、`subtract` 等返回的集合可能是 `SetView` 等不可修改视图，不要尝试修改。如需可变副本，包一层 `new ArrayList<>(...)`。

### 5.7 性能考虑

- `Bag` 基于 Map 实现，统计计数非常高效，但遍历 `uniqueSet()` 顺序不稳定。
- `ListUtils.partition` 会复制原 List 到多个子 List，大数据量分批处理时建议结合 `subList` 或 Stream 分批。
- `PassiveExpiringMap` 每次访问都会检查过期，高频访问时有一定开销；追求高性能过期缓存请用 Caffeine。

### 5.8 线程安全

Commons Collections4 大部分装饰器/工具类 **不是线程安全的**。如需并发，请使用对应的 Synchronized 装饰器（如 `Collections.synchronizedXXX` 或 CC4 的 `SynchronizedSortedMap`）或并发集合（如 `ConcurrentHashMap`）。

---

## 6. 运行方法

### 6.1 编译打包

```bash
cd commons-collections4-demo
mvn clean package -DskipTests
```

### 6.2 运行演示

```bash
java -cp "target/commons-collections4-demo-1.0-SNAPSHOT.jar:target/dependency/commons-collections4-4.4.jar" \
  com.example.commonscollections4.CommonsCollections4BasicDemo

java -cp "target/commons-collections4-demo-1.0-SNAPSHOT.jar:target/dependency/commons-collections4-4.4.jar" \
  com.example.commonscollections4.CommonsCollections4AdvancedDemo

java -cp "target/commons-collections4-demo-1.0-SNAPSHOT.jar:target/dependency/commons-collections4-4.4.jar" \
  com.example.commonscollections4.CommonsCollections4PracticalDemo
```

Windows 下将 `:` 改为 `;`：

```cmd
java -cp "target\commons-collections4-demo-1.0-SNAPSHOT.jar;C:\Users\admin\.m2\repository\org\apache\commons\commons-collections4\4.4\commons-collections4-4.4.jar" com.example.commonscollections4.CommonsCollections4BasicDemo
```

> 如果希望 `target/dependency` 存在，可运行 `mvn dependency:copy-dependencies`。

---

## 7. 学习总结

- Commons Collections4 补充了 JDK 没有的 `Bag`、`BidiMap`、`MultiValuedMap`、`CaseInsensitiveMap` 等特殊集合，很多业务场景能直接用。
- `CollectionUtils`、`MapUtils`、`ListUtils`、`SetUtils` 等工具类能减少手写循环与空指针判断。
- 与 Spring Boot 集成无需 Starter，直接作为工具库使用即可。
- 注意版本冲突、惰性过期、视图不可修改、线程安全等常见坑。
