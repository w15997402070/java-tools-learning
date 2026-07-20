# Day 15 - JUnit 5 Java 标准测试框架

## 工具简介

**JUnit 5** 是 JUnit 家族自 2017 年以来的最大版本更新，由 JUnit Platform、JUnit Jupiter 和 JUnit Vintage 三部分组成。引入了全新的 Jupiter 编程模型，全面支持 Java 8+ 特性（Lambda、Stream、接口默认方法），是当前 Java 生态最主流的测试框架。

| 属性 | 内容 |
|------|------|
| **GitHub** | https://github.com/junit-team/junit5 |
| **官方文档** | https://junit.org/junit5/docs/current/user-guide/ |
| **最新稳定版** | 5.10.2 |
| **Star 数** | 13k+ |
| **Java 兼容** | Java 8+（运行时需 JRE 8+，编译可用 Java 17+ 新特性） |
| **License** | Eclipse Public License 2.0 |

---

## Maven 依赖配置

```xml
<!-- JUnit 5 BOM：统一管理所有子模块版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.junit</groupId>
            <artifactId>junit-bom</artifactId>
            <version>5.10.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- JUnit Jupiter API：注解、断言、假设 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-api</artifactId>
        <scope>test</scope>
    </dependency>
    <!-- JUnit Jupiter Engine：测试执行引擎（运行时必须） -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-engine</artifactId>
        <scope>test</scope>
    </dependency>
    <!-- JUnit Jupiter Params：参数化测试（可选） -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-params</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Maven Surefire Plugin 2.x/3.x 内置支持 JUnit 5，无需额外配置 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

> **注意**：`junit-jupiter-engine` 在 `<scope>test</scope>` 下，Maven 运行时需要 `mvn test`（不是 `mvn compile`）。如果用 `mvn compile` 运行测试会找不到引擎类。

---

## 核心注解速览

| 注解 | 作用域 | 说明 |
|------|--------|------|
| `@Test` | 方法 | 标记为测试方法（JUnit 5 版本，包名 `org.junit.jupiter.api.Test`） |
| `@ParameterizedTest` | 方法 | 参数化测试，同一方法多组数据执行 |
| `@RepeatedTest(N)` | 方法 | 重复执行 N 次 |
| `@TestFactory` | 方法 | 动态测试（返回 DynamicTest 集合） |
| `@BeforeAll` | 方法 | 所有测试前执行一次（静态方法） |
| `@AfterAll` | 方法 | 所有测试后执行一次（静态方法） |
| `@BeforeEach` | 方法 | 每个测试前执行 |
| `@AfterEach` | 方法 | 每个测试后执行 |
| `@Disabled` | 类/方法 | 禁用测试 |
| `@Nested` | 类 | 嵌套测试类 |
| `@Tag` | 类/方法 | 标签分组（替代旧版 @Category） |
| `@DisplayName` | 类/方法 | 自定义测试显示名 |

---

## 核心 API 速查

### 断言（Assertions）

```java
import static org.junit.jupiter.api.Assertions.*;

// 基本断言
assertEquals(expected, actual);
assertEquals(expected, actual, "自定义失败消息");
assertEquals(3.14, Math.PI, 0.001); // 带精度的浮点数比较

// 布尔断言
assertTrue(condition);
assertFalse(condition);

// 空值断言
assertNull(object);
assertNotNull(object);

// 引用断言
assertSame(expected, actual);   // 必须同一引用
assertNotSame(expected, actual);

// 数组断言
assertArrayEquals(expectedArr, actualArr);

// 异常断言（最重要）
assertThrows(Exception.class, () -> { /* 代码 */ });
assertThrows(ArithmeticException.class, () -> 1/0,
        "应该抛出 ArithmeticException");

// 分组断言（全部执行再统一报告）
assertAll("多字段校验",
    () -> assertEquals("Alice", user.getName()),
    () -> assertEquals(25, user.getAge()),
    () -> assertNotNull(user.getEmail())
);

// 超时断言
assertTimeout(Duration.ofSeconds(2), () -> { /* 操作 */ });
assertTimeoutPreemptively(Duration.ofSeconds(1), () -> { /* 操作 */ });
```

### 假设（Assumptions）

```java
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assumptions.assumingThat;

// 条件成立才执行测试（不成立则跳过）
assumeTrue(System.getProperty("os.name").contains("Windows"), "仅 Windows");

// 条件成立才执行内部断言
assumingThat(condition, () -> assertEquals(expected, actual));
```

---

## 高级特性详解

### 1. 参数化测试（@ParameterizedTest）

```java
// 1. @ValueSource：最简参数
@ParameterizedTest
@ValueSource(ints = {1, 2, 3, 4, 5})
void testEven(int n) { assertEquals(0, n % 2); }

// 2. @CsvSource：多列 CSV 数据
@ParameterizedTest
@CsvSource({
    "apple,     5",
    "banana,    6",
    "cherry,    6"
})
void testLength(String s, int len) {
    assertEquals(len, s.trim().length());
}

// 3. @EnumSource：枚举遍历
@ParameterizedTest
@EnumSource(Month.class)
void testMonth(Month m) { assertNotNull(m); }

// 4. @MethodSource：自定义参数源
@ParameterizedTest
@MethodSource("stringProvider")
void testStrings(String s) { assertNotNull(s); }

static Stream<String> stringProvider() {
    return Stream.of("a", "b", "c");
}

// 5. 多参数 MethodSource
static Stream<Arguments> calcProvider() {
    return Stream.of(
        Arguments.of(2, 3, 5),
        Arguments.of(10, 20, 30)
    );
}

@ParameterizedTest
@MethodSource("calcProvider")
void testCalc(int a, int b, int sum) {
    assertEquals(sum, a + b);
}
```

### 2. 嵌套测试（@Nested）

```java
class OuterTest {
    @Nested
    @DisplayName("用户注册测试")
    class UserRegisterTests {
        @BeforeEach
        void setUp() { /* 初始化 */ }

        @Test
        void validInput() { /* */ }

        @Nested
        class ValidationTests {
            @Test
            void usernameTooShort() { /* */ }
            @Test
            void invalidEmail() { /* */ }
        }
    }
}
```

### 3. 动态测试（@TestFactory）

```java
@TestFactory
List<DynamicTest> dynamicTests() {
    return Arrays.asList(
        DynamicTest.dynamicTest("加法测试",
            () -> assertEquals(5, 2 + 3)),
        DynamicTest.dynamicTest("乘法测试",
            () -> assertEquals(6, 2 * 3))
    );
}
```

### 4. 条件执行

```java
@EnabledOnOs({OS.WINDOWS, OS.LINUX})          // 仅指定平台
@DisabledOnOs(OS.WINDOWS)                      // 排除指定平台
@EnabledIfSystemProperty(named = "java.version", matches = "1\\.8.*")
@EnabledIf("'${os.name}'.contains('Windows')")  // SPEL 表达式
@EnabledIfEnvironmentVariable(named = "ENV", matches = "prod")
```

### 5. @RepeatedTest 重复测试

```java
@RepeatedTest(10)
void stabilityTest(RepetitionInfo info) {
    System.out.println("第 " + info.getCurrentRepetition()
        + "/" + info.getTotalRepetitions() + " 次");
    // 稳定性验证...
}
```

---

## Spring Boot 集成

### 1. 添加依赖（Spring Boot 2.4+）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <!-- 自动引入 JUnit 5 + Mockito + AssertJ -->
</dependency>
```

> Spring Boot 2.4+ 默认使用 JUnit 5（移除了 Vintage 引擎对 JUnit 4 的默认支持）。

### 2. Service 层测试示例

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getUserShouldReturn200() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Alice"));
    }
}
```

### 3. Service 单元测试（@MockBean）

```java
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.findById(1L))
            .thenReturn(Optional.of(new User("Alice", "alice@example.com", 25)));
    }

    @Test
    void findByIdShouldReturnUser() {
        User user = userService.findById(1L);
        assertEquals("Alice", user.getUsername());
    }
}
```

---

## Maven 运行命令

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=JUnit5BasicDemo

# 运行指定测试方法
mvn test -Dtest=JUnit5BasicDemo#demoAssertEquals

# 按 Tag 运行
mvn test -Dgroups="fast"

# 排除 Tag
mvn test -DexcludedGroups="slow"

# 生成测试报告
mvn test surefire-report:report
# 报告位置：target/site/surefire-report.html

# 并行执行（需添加配置）
mvn test -Dparallel=methods -DthreadCount=4
```

### Maven Surefire Tag 配置示例

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <groups>fast,unit</groups>
        <excludedGroups>slow,integration</excludedGroups>
        <!-- 并行执行 -->
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

---

## JUnit 4 到 JUnit 5 迁移

| JUnit 4 | JUnit 5 | 差异 |
|---------|---------|------|
| `@Category(Some.class)` | `@Tag("some")` | Tag 替代 Category |
| `@RunWith(MockitoJUnitRunner.class)` | `@ExtendWith(MockitoExtension.class)` | Runner → Extension |
| `@Rule ExpectedException` | `assertThrows()` | 断言替代 Rule |
| `@Before` | `@BeforeEach` | Every → Each |
| `@After` | `@AfterEach` | Every → Each |
| `@BeforeClass` | `@BeforeAll` | 静态方法 |
| `@Ignore` | `@Disabled` | 更语义化 |
| `Assert.*` | `Assertions.*` | 包名变化 |

> **重要**：如果项目仍在使用 JUnit 4（历史代码），需要加入 `junit-vintage-engine` 以兼容：
> ```xml
> <dependency>
>     <groupId>org.junit.vintage</groupId>
>     <artifactId>junit-vintage-engine</artifactId>
>     <scope>test</scope>
> </dependency>
> ```

---

## 注意事项与常见坑

### 1. `@BeforeAll` / `@AfterAll` 必须是 static
JUnit 5 中这两个注解**必须**配合 `static` 方法使用（在普通实例方法上会被忽略）。如果需要非 static 生命周期，可以使用 `@TestInstance(Lifecycle.PER_CLASS)`：

```java
@TestInstance(Lifecycle.PER_CLASS)  // 类的所有测试共享一个实例
class MyTest {
    @BeforeAll
    void init() { /* 非 static 也可以 */ }
}
```

### 2. Maven Surefire 版本过旧
Maven 3.6+ 自带的 Surefire 2.22.0+ 已支持 JUnit 5，无需额外配置。如果使用旧版 Maven（<3.6），请手动升级 surefire 版本到 2.22.0+。

### 3. 编译不过：找不到 `org.junit.jupiter.api.Test`
检查是否引入了 `junit-jupiter-api`（不是 `junit` 4.x 的 `org.junit.Test`）。

### 4. 参数化测试乱码（Windows 环境）
Maven Surefire 默认使用系统编码。显式指定 UTF-8：
```xml
<configuration>
    <argLine>-Dfile.encoding=UTF-8</argLine>
</configuration>
```

### 5. `@Disabled` 不会静默跳过
被 `@Disabled` 标记的测试在报告中会显示为 `DISABLED`，不会像 JUnit 4 的 `@Ignore` 那样完全消失。配合 CI 可以追踪被禁用的测试。

### 6. assertTimeoutPreemptively 会真正中断线程
如果被测代码中有 `finally` 块中的不可中断操作（如 `Socket.read()`），`assertTimeoutPreemptively` 会强制中断线程，可能导致资源泄漏。优先使用 `assertTimeout()`。

### 7. Spring Boot 2.4+ 不带 Vintage 引擎
如果需要同时运行 JUnit 4 和 JUnit 5 测试，显式添加：
```xml
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <scope>test</scope>
</dependency>
```

### 8. `@Nested` 测试类继承限制
`@Nested` 测试类**不能**继承父类的 `@BeforeAll` / `@AfterAll`（因为这些方法必须是 static）。如需共享 fixtures，考虑使用 `@TestInstance(Lifecycle.PER_CLASS)` 或提取到工具类。

---

## 最佳实践

1. **每个测试独立**：测试之间不应有执行顺序依赖（除明确用 `@Order` 标注）
2. **测试名称即文档**：`@DisplayName` 使用中文或英文描述测试意图
3. **AAA 模式**：Arrange（准备数据）→ Act（执行）→ Assert（验证）
4. **少用 `if-else` 做断言**：用 `assertAll` 批量断言，一目了然
5. **参数化优先于重复测试**：相同逻辑多组数据，用 `@ParameterizedTest` 比 `@RepeatedTest` 更清晰
6. **Mock 外部依赖**：数据库/API 测试用 `@MockBean`，纯单元测试不依赖 Spring 上下文
7. **Tag 分类**：`fast`（<100ms）、`slow`、`integration`、`db` 等，便于 CI 筛选

---

## 运行方法

```bash
# 进入项目目录
cd d:/ai/workbuddy/java-tools-learning/junit5-demo

# Maven 编译（含测试编译，不运行）
mvn clean compile test-compile

# 运行所有测试
mvn test

# 查看测试报告
# 路径：junit5-demo/target/surefire-reports/*.txt
# HTML报告：junit5-demo/target/site/surefire-report.html

# 只运行基础演示
mvn test -Dtest=JUnit5BasicDemo

# 只运行参数化测试
mvn test -Dtest=JUnit5AdvancedDemo#testIsEven

# 只运行实战演示
mvn test -Dtest=JUnit5PracticalDemo
```
