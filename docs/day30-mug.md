# Day 30: Google Mug — Java 8 字符串处理与双向流工具库

> Google 内部广泛使用的 Java 8+ 小工具库，核心解决两件事：**字符串操作太繁琐**（`indexOf` + off-by-one）和 **Map 流式处理样板代码太多**（`entrySet().stream().map(e -> Map.entry(...))`）。0 依赖，API 设计优雅，是提升日常编码幸福感的利器。

---

## 📌 工具简介

| 项目 | 信息 |
|------|------|
| **GitHub** | https://github.com/google/mug |
| **官网/Javadoc** | http://google.github.io/mug/apidocs/index.html |
| **星标** | 700+（Google 内部使用量大，开源社区偏小众） |
| **版本** | 10.6（最新稳定版，2026-07） |
| **License** | Apache License 2.0 |
| **Java 要求** | Java 8+（核心 `mug` 模块） |
| **依赖** | 0 依赖（Guava/Proto/BigQuery 扩展在独立 artifact） |
| **Maven 坐标** | `com.google.mug:mug:10.6` |

### 核心模块

| Artifact | 说明 | Java 要求 |
|----------|------|-----------|
| `mug` | 核心：BiStream、Substring、StringFormat、Optionals、MoreStreams | Java 8+ |
| `mug-guava` | Guava 扩展：BinarySearch、ImmutableMap 收集器 | Java 8+ |
| `mug-safesql` | 编译期安全的 SQL 模板（SafeSql） | Java 8+ |
| `mug-errorprone` | Error Prone 插件：`@ParametersMustMatchByName` 编译期检查 | Java 8+ |
| `mug-protobuf` | Protobuf 工具 | Java 8+ |
| `dot-parse` | 解析器组合子 | Java 8+ |
| `mug-concurrent24` | 结构化并发（基于虚拟线程） | Java 21+ |

### 为什么学它？

1. **BiStream 是 Guava 团队公认的未来方向** —— 官方 README 明确说 BiStream 最终会并入 Guava。提前掌握等于站在趋势前面。
2. **告别 entrySet 样板** —— `map.entrySet().stream().map(e -> Map.entry(...))` 这类丑陋代码每天在写，BiStream 一行解决。
3. **Substring 让字符串操作声明式化** —— `Substring.between("{","}").from(code)` 比 `s.substring(s.indexOf("{")+1, s.indexOf("}"))` 安全 10 倍。
4. **StringFormat 双向模板** —— 同一个模板既能 `format` 又能 `parse`，比 `String.format` 安全、比正则易读。
5. **0 依赖** —— 引入零负担，不污染依赖树。

---

## 📦 Maven 依赖配置

### 核心依赖

```xml
<dependency>
    <groupId>com.google.mug</groupId>
    <artifactId>mug</artifactId>
    <version>10.6</version>
</dependency>
```

### 可选：Guava 扩展

```xml
<dependency>
    <groupId>com.google.mug</groupId>
    <artifactId>mug-guava</artifactId>
    <version>10.6</version>
</dependency>
```

### 可选：SafeSql（编译期防 SQL 注入模板）

```xml
<dependency>
    <groupId>com.google.mug</groupId>
    <artifactId>mug-safesql</artifactId>
    <version>10.6</version>
</dependency>
```

### 可选：Error Prone 编译期检查

```xml
<build>
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.google.errorprone</groupId>
                        <artifactId>error_prone_core</artifactId>
                        <version>2.23.0</version>
                    </path>
                    <path>
                        <groupId>com.google.mug</groupId>
                        <artifactId>mug-errorprone</artifactId>
                        <version>10.6</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 🚀 核心功能速览

### 1. BiStream — 双向流

让 Map / 成对集合的流式操作像普通 Stream 一样流畅。

```java
import com.google.mu.util.stream.BiStream;

// 从 Map 创建
Map<String, Integer> scores = Map.of("Alice", 90, "Bob", 75);

// 链式 mapKeys / filterValues / toMap
Map<String, Integer> high = BiStream.from(scores)
        .filterValues(v -> v >= 80)
        .toMap();

// zip：两个 List 配对
Map<String, Integer> pairs = BiStream.zip(names, ages).toMap();

// 从对象集合提取键值对
Map<String, String> idToName = BiStream.from(people, Person::getId, Person::getName).toMap();

// flatMap：展平嵌套 Map
Map<String, Integer> flat = BiStream.from(nested)
        .flatMap((region, m) -> BiStream.from(m).mapKeys(k -> region + "/" + k))
        .toMap();

// groupingBy：分组（返回 BiStream 再 toMap）
Map<String, List<String>> byDept = employees.stream()
        .collect(BiStream.groupingBy(Emp::getDept,
                Collectors.mapping(Emp::getName, Collectors.toList())))
        .toMap();

// inverse：键值互换
Map<Integer, String> reversed = BiStream.from(scores).inverse().toMap();

// concat：合并（重复键可自定义合并策略）
Map<String, Integer> merged = BiStream.concat(map1, map2).toMap(Integer::sum);
```

### 2. Substring — 声明式字符串操作

```java
import com.google.mu.util.Substring;
import static com.google.mu.util.Substring.first;
import static com.google.mu.util.Substring.last;

// between：提取标记间内容
Optional<String> inner = Substring.between("(", ")").from("call(foo)");
// → Optional.of("foo")

// before / after：定位分隔符前后
Optional<String> dir = Substring.before(last('/')).from("home/foo/Bar.java");
Optional<String> ext = Substring.after(last('.')).from("Bar.java");

// prefix / suffix：移除/添加前后缀
String noHttp = Substring.prefix("http://").removeFrom(url);
String withComma = Substring.suffix(',').addToIfAbsent(line);

// split：两路切割（返回 BiOptional，天然接 BiStream）
Substring.first('=').split("name=Alice")
        .map((k, v) -> k + "→" + v);  // → Optional.of("name→Alice")

// repeatedly：所有匹配（零拷贝 Stream 视图）
List<String> placeholders = Substring.between("{", "}").repeatedly()
        .from("Hello {name}, id={id}")
        .collect(toList());  // → ["name", "id"]

// replaceAllFrom：批量替换
String rendered = Substring.between("{", "}").repeatedly()
        .replaceAllFrom(template, vars::get);

// all(',')：按逗号切割（= first(',').repeatedly().split()）
List<String> items = Substring.all(',').split("a,b,c")
        .map(String::trim).collect(toList());
```

### 3. StringFormat — 双向模板

```java
import com.google.mu.util.StringFormat;

StringFormat pathFmt = new StringFormat("/home/{user}/{date}");

// format：生成
String s = pathFmt.format("alice", "2024-03-14");
// → "/home/alice/2024-03-14"

// parse：提取（回调参数按占位符顺序）
Optional<String> user = pathFmt.parse("/home/alice/2024-03-14", (u, d) -> u);
// → Optional.of("alice")

// matches：仅判断
boolean ok = pathFmt.matches("/home/bob/2024-01-01");
```

### 4. MoreStreams — Stream 增强

```java
import com.google.mu.util.stream.MoreStreams;

// groupConsecutive：连续满足条件的元素分组
List<List<Double>> upStreaks = MoreStreams.groupConsecutive(
        prices.stream(),
        (prev, curr) -> curr >= prev,
        Collectors.toList());
```

### 5. Optionals — Optional 增强

```java
import com.google.mu.util.Optionals;

// optionally：条件构造，替代 if/else 样板
Optional<String> email = Optionals.optionally(user.hasEmail(), user::getEmail);

// or：Java 9 Optional.or 的 Java 8 平替
Optional<String> r = Optionals.or(primary, () -> fallback);
```

---

## 🔧 Spring Boot 集成方式

Mug 是纯工具库，**无需 Starter、无自动配置**，引入依赖即可直接使用。推荐封装为工具类：

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.google.mug</groupId>
    <artifactId>mug</artifactId>
    <version>10.6</version>
</dependency>
```

### 2. 封装工具 Bean

```java
@Component
public class MugUtils {

    /** 解析 "k1=v1, k2=v2" 配置串为 Map */
    public Map<String, String> parseConfig(String config) {
        return BiStream.from(
                        Substring.all(',').splitThenTrim(config).collect(toList()),
                        kv -> Substring.first('=').split(kv))
                .toMap();
    }

    /** 模板渲染：{name} 占位符替换 */
    public String render(String template, Map<String, ?> vars) {
        return Substring.between("{", "}").repeatedly()
                .replaceAllFrom(template, ph -> String.valueOf(vars.get(ph)));
    }

    /** Map 键值映射（BiStream 链） */
    public <K, V, NK, NV> Map<NK, NV> transformMap(
            Map<K, V> src,
            BiFunction<K, V, NK> keyMapper,
            BiFunction<K, V, NV> valueMapper) {
        return BiStream.from(src)
                .mapKeys(keyMapper::apply)
                .mapValues(valueMapper::apply)
                .toMap();
    }
}
```

### 3. 在 Service 中使用

```java
@Service
public class OrderService {

    @Autowired
    private MugUtils mugUtils;

    public void process(String config) {
        Map<String, String> cfg = mugUtils.parseConfig(config);
        // ...
    }
}
```

### 4. SafeSql 与 MyBatis/JDBC（可选）

```java
// 编译期防 SQL 注入：{col} 会校验为合法列名，{id} 参数化绑定
SafeSql query = SafeSql.of("select id, `{col}` from Users where id = {id}", col, id);
// → 参数化的 PreparedStatement，列名白名单校验
```

---

## ⚠️ 注意事项

### 1. Java 版本兼容性
- 核心 `mug` 模块 **Java 8+** 兼容，可放心在 Java 8 项目使用。
- `mug-concurrent24`（结构化并发/虚拟线程）需 **Java 21+**，引入前确认 JDK 版本。
- `@ParametersMustMatchByName` 配合 `record` 使用效果最佳，`record` 需 **Java 16+**（Java 8 项目可用普通类 + Error Prone 插件部分支持）。
- **本 Demo 用 JDK 17 编译、target 1.8**，可在 Java 8 运行时使用核心 API。

### 2. Substring 不是万能解析器
- `Substring` 适用于**已知格式**的字符串（内部路径、flag 值、已校验的 URL 等）。
- 对复杂语法或上下文相关文法（如嵌套 JSON、HTML），用专门解析器或正则更合适。
- 例：`upToIncluding(first("://")).removeFrom("Surprise! not a url")` 会得到无意义结果。

### 3. BiStream 一次性消费
- BiStream 基于 Stream，**只能消费一次**，不能像 Map 那样反复遍历。
- 终端操作（`toMap`/`collect`/`forEach`）后不可再用，需重新创建。

### 4. Substring.Pattern 可复用
- `Substring.Pattern` 是**不可变对象**，应复用而非每次 new，避免重复分配。
- 推荐：`stream.map(first('=')::split)` 而非 `stream.map(line -> first('=').split(line))`。

### 5. 版本迭代快，API 可能微调
- Mug 提交非常活跃（8000+ commits），版本号跨度大（已到 10.6）。
- 升级大版本时注意 `BiStream`/`Substring` API 可能有调整，建议固定版本并跑测试。
- 部分 API 在 wiki 中以"实验性"标注，生产环境优先用 README 明确列出的稳定 API。

### 6. 社区与生态
- 开源星标不高（小众），但 Google 内部使用量极大，质量有保障。
- 官方明确表示 BiStream 未来会并入 Guava，届时 API 可能迁移，注意跟进。
- 文档主要在 GitHub wiki 和 Javadoc，无第三方教程，需读官方资料。

### 7. 与 Guava 的关系
- Mug 不依赖 Guava（核心 0 依赖），但与 Guava 配合极佳。
- `mug-guava` 模块提供 `BinarySearch` 等 Guava 风格扩展。
- `BiStream.collect(ImmutableMap::toImmutableMap)` 可直接收集到 Guava 不可变集合（方法引用即 BiCollector）。

---

## 📋 异常处理参考

| 场景 | API | 行为 |
|------|-----|------|
| 模式未匹配 | `Substring.Pattern.from(s)` | 返回 `Optional.empty()`，不抛异常 |
| 模板不匹配 | `StringFormat.parse(s, fn)` | 返回 `Optional.empty()` |
| 占位符数量不符 | `StringFormat.parse` | 编译期通过 lambda 元数约束，类型安全 |
| 必须匹配时 | `.orElseThrow(...)` | 显式抛出业务异常 |
| split 两路 | `first('=').split(s)` | 返回 `BiOptional`，未匹配则 empty |
| 重复键 toMap | `toMap(mergeFunction)` | 用合并函数处理，否则抛 IllegalStateException |

---

## 🏃 运行方法

### 编译

```bash
cd mug-demo
mvn clean package -DskipTests
```

### 运行 Demo（需 JDK 8+）

```bash
# 基础演示
mvn exec:java -Dexec.mainClass="com.example.mug.MugBasicDemo"

# 进阶演示
mvn exec:java -Dexec.mainClass="com.example.mug.MugAdvancedDemo"

# 实战演示
mvn exec:java -Dexec.mainClass="com.example.mug.MugPracticalDemo"
```

### Demo 文件说明

| 文件 | 内容 |
|------|------|
| `MugBasicDemo.java` | BiStream 基础（from/zip/mapKeys/filter/toMap/inverse）+ Substring 基础（between/first/last/split/repeatedly） |
| `MugAdvancedDemo.java` | StringFormat 双向模板 + MoreStreams.groupConsecutive + Optionals 条件构造 + BiStream flatMap/groupingBy/concat |
| `MugPracticalDemo.java` | 4 个实战场景：配置解析、日志统计、Map 流水线、模板渲染 + Spring Boot 集成指南 |

---

## 📚 参考资源

- [GitHub 仓库](https://github.com/google/mug)
- [Javadoc](http://google.github.io/mug/apidocs/index.html)
- [BiStream 设计说明](https://github.com/google/mug/blob/master/mug/src/main/java/com/google/mu/util/stream/README.md)
- [Substring 使用指南](https://github.com/google/mug/wiki/Substring-Explained)
- [StringFormat 使用指南](https://github.com/google/mug/wiki/StringFormat-Explained)
- [SafeSql 说明](https://github.com/google/mug/blob/master/mug-safesql/src/main/java/com/google/mu/safesql/README.md)

---

## 💡 适用场景总结

| 场景 | 推荐工具 | 收益 |
|------|----------|------|
| Map 键值转换/过滤/分组 | BiStream | 告别 entrySet 样板，可读性 ×3 |
| 两个 List 配对成 Map | BiStream.zip | 一行替代手写循环 |
| 嵌套 Map 展平 | BiStream.flatMap | 链式表达，避免多层 stream 套娃 |
| 提取字符串片段 | Substring.between/before/after | 告别 indexOf + off-by-one |
| 批量替换占位符 | Substring.repeatedly().replaceAllFrom | 零拷贝，性能优 |
| 解析 key=value 配置 | Substring.split + BiStream | 类型安全，天然接 Map |
| 日志/路径模板解析 | StringFormat.parse | 双向模板，比正则易读 |
| 条件构造 Optional | Optionals.optionally | 替代 if/else 样板 |
| 连续元素分组 | MoreStreams.groupConsecutive | Java 8 Stream 缺失能力 |
| 防 SQL 注入 | SafeSql（mug-safesql） | 编译期校验，参数化绑定 |
