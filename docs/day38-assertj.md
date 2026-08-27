# Day 38: AssertJ — 流畅断言库

> 让测试断言像读自然语言一样简单。

## 工具简介

**AssertJ** 是 Java 生态中最流行的流畅断言（Fluent Assertions）库，提供了大量链式 API，使单元测试和集成测试的断言更直观、更可读。它支持：

- 基础类型（字符串、数字、布尔值、日期、Optional 等）
- 集合、Map、数组、文件、异常
- 递归字段比较、软断言、自定义条件
- 与 JUnit 5 / Spring Boot 无缝集成

- **GitHub**: https://github.com/assertj/assertj
- **官方文档**: https://assertj.github.io/doc/
- **版本**: 3.24.2（Java 8 兼容；AssertJ 3.x 要求 Java 8+）
- **星标**: 3k+（JUnit/Spring Boot 生态事实标准）

## Maven 依赖

```xml
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.24.2</version>
    <scope>test</scope>
</dependency>
```

> 生产代码通常不需要 AssertJ，因此 `<scope>test</scope>` 即可。

## 核心示例

### 1. 字符串与数值断言

```java
import static org.assertj.core.api.Assertions.assertThat;

assertThat("AssertJ is great")
    .startsWith("AssertJ")
    .contains("is")
    .endsWith("great")
    .hasSizeGreaterThan(5);

assertThat(42)
    .isPositive()
    .isGreaterThan(0)
    .isLessThanOrEqualTo(100);
```

### 2. 集合与 Map 断言

```java
assertThat(Arrays.asList("a", "b", "c"))
    .hasSize(3)
    .contains("b")
    .doesNotContain("z")
    .allMatch(s -> s.length() == 1);

assertThat(Map.of("key", "value"))
    .containsKey("key")
    .containsEntry("key", "value");
```

### 3. 异常断言

```java
import static org.assertj.core.api.Assertions.assertThatThrownBy;

assertThatThrownBy(() -> divide(1, 0))
    .isInstanceOf(ArithmeticException.class)
    .hasMessageContaining("zero");
```

### 4. Optional 与日期时间断言

```java
assertThat(Optional.of("hello")).isPresent().hasValue("hello");
assertThat(Optional.empty()).isEmpty();

assertThat(LocalDate.of(2026, 8, 27))
    .isAfter(LocalDate.of(2020, 1, 1))
    .isBeforeOrEqualTo(LocalDate.of(2026, 12, 31));
```

### 5. 递归字段比较

```java
User actual = new User("tom", new Address("Beijing"));
User expected = new User("tom", new Address("Beijing"));

assertThat(actual)
    .usingRecursiveComparison()
    .isEqualTo(expected);

// 忽略自动生成的 id
assertThat(product1)
    .usingRecursiveComparison()
    .ignoringFields("id")
    .isEqualTo(product2);
```

### 6. 软断言

一次运行收集所有失败，适合接口返回体多字段一次性校验：

```java
import org.assertj.core.api.SoftAssertions;

SoftAssertions softly = new SoftAssertions();
softly.assertThat(user.getName()).isEqualTo("tom");
softly.assertThat(user.getAge()).isGreaterThan(0);
softly.assertThat(user.getEmail()).contains("@");
softly.assertAll(); // 最后统一抛出
```

## Spring Boot 集成

Spring Boot 2.x/3.x 默认使用 JUnit 5，只需引入 `spring-boot-starter-test`，AssertJ 已作为传递依赖包含在内：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

推荐在测试类中使用静态导入：

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

### 与 MockMvc 结合使用

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUser() throws Exception {
        mockMvc.perform(get("/users/1"))
               .andExpect(status().isOk())
               .andExpect(result -> {
                   String body = result.getResponse().getContentAsString();
                   assertThatJson(body)
                       .node("username").isString()
                       .isEqualTo("tom");
                   assertThatJson(body)
                       .node("age").isNumber()
                       .isGreaterThan(0);
               });
    }
}
```

> JSON 断言可额外引入 `net.javacrumbs.json-unit:json-unit-assertj`。

## 注意事项

1. **版本与 Java 兼容性**
   - AssertJ 3.x 要求 Java 8+。
   - AssertJ 4.x（未正式发布稳定版）将要求 Java 17+，升级前需确认 JDK 版本。

2. **assertThat 的静态导入冲突**
   - JUnit 5 也提供了 `org.junit.jupiter.api.Assertions.assertThat`，但与 AssertJ 不同。建议统一使用 AssertJ 的 `org.assertj.core.api.Assertions.assertThat`。

3. **递归比较默认忽略 equals 方法**
   - `usingRecursiveComparison()` 按字段值比较，不会调用类的 `equals()`，也不会要求类实现 `equals`/`hashCode`。

4. **软断言不会自动失败**
   - 忘记调用 `assertAll()` 会导致失败的软断言被吞掉，务必在软断言代码块末尾调用。

5. **集合过滤链式使用注意类型**
   - `filteredOn(...)` 之后返回的仍是 ListAssert，可以继续链式断言；`first()` 返回 ObjectAssert，再继续 `extracting` 时类型推导可能变弱，必要时加泛型或分步断言。

6. **避免过度复杂的 lambda 断言**
   - `satisfies` 里嵌套大量 AssertJ 断言会降低可读性；复杂的业务校验建议拆分为自定义 `Condition` 或私有断言方法。

7. **异常断言不捕获意外异常**
   - `assertThatThrownBy` 会执行 lambda；如果 lambda 没有抛异常，测试会失败；如果抛出非预期异常，也会失败。比 `@Test(expected = ...)` 更安全。

## 运行方法

进入 `assertj-demo` 目录：

```bash
# 编译打包
mvn clean package -DskipTests

# 运行基础断言演示
mvn exec:java -Dexec.mainClass="com.example.assertj.AssertJBasicDemo"

# 运行进阶断言演示
mvn exec:java -Dexec.mainClass="com.example.assertj.AssertJAdvancedDemo"

# 运行实战断言演示
mvn exec:java -Dexec.mainClass="com.example.assertj.AssertJPracticalDemo"
```

或者在 IDE 中直接运行对应类的 `main` 方法。

## 参考

- [AssertJ GitHub](https://github.com/assertj/assertj)
- [AssertJ 官方文档](https://assertj.github.io/doc/)
- [AssertJ 3.x Javadoc](https://www.javadoc.io/doc/org.assertj/assertj-core/latest/index.html)
