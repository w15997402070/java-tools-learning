# Day 14 - Apache Commons Lang3

## 工具简介

**Apache Commons Lang3** 是 Apache Commons 项目中使用最广泛的 Java 工具类库，是对 `java.lang` 包的强力补充。它提供了大量实用的工具类，覆盖字符串处理、数组操作、数字转换、随机数生成、日期工具、反射、异常处理等核心场景，几乎所有企业级 Java 项目都有它的身影。

| 属性     | 内容 |
|----------|------|
| **GitHub** | https://github.com/apache/commons-lang |
| **官方文档** | https://commons.apache.org/proper/commons-lang/ |
| **最新稳定版** | 3.14.0 |
| **Star 数** | 3.5k+（Apache 官方维护，实际使用量极大） |
| **Java 兼容** | Java 8+ |
| **License** | Apache License 2.0 |

---

## Maven 依赖配置

```xml
<!-- Apache Commons Lang3 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.14.0</version>
</dependency>
```

> **注意**：旧版 `commons-lang`（groupId 为 `commons-lang`）是 Java 5 时代的遗留版本，包名为 `org.apache.commons.lang`；Lang3 的包名为 `org.apache.commons.lang3`，两者可同时共存但应优先使用 Lang3。

---

## 核心工具类速览

| 工具类 | 功能 |
|--------|------|
| `StringUtils` | 字符串处理（null 安全，弥补 JDK 短板） |
| `NumberUtils` | 字符串→数字安全转换，带默认值 |
| `ArrayUtils` | 数组增删/查找/反转/合并 |
| `BooleanUtils` | boolean/int/String 互转 |
| `RandomStringUtils` | 随机字符串（验证码/Token） |
| `RandomUtils` | 随机数（有界范围） |
| `ObjectUtils` | null 安全的通用操作 |
| `Validate` | 前置条件断言（参数校验） |
| `StopWatch` | 精确计时器（代码性能分析） |
| `DateUtils` | 日期计算（Java 8 以前的兼容方案） |
| `DateFormatUtils` | 日期格式化 |
| `ExceptionUtils` | 异常链分析与堆栈格式化 |
| `Pair` / `Triple` | 二元组 / 三元组 |
| `FieldUtils` | 反射读写字段（含私有字段） |
| `MethodUtils` | 反射调用方法（含私有方法） |
| `ToStringBuilder` | 自动生成 toString |
| `EqualsBuilder` | 自动实现 equals |
| `HashCodeBuilder` | 自动实现 hashCode |
| `Fraction` | 精确分数运算 |

---

## 核心功能详解

### 1. StringUtils — 最常用的工具类

`StringUtils` 所有方法对 `null` 免疫（不抛 NPE），是 JDK `String` 方法的安全替代。

```java
// 空值判断三件套
StringUtils.isEmpty(null)    // true
StringUtils.isEmpty("")      // true
StringUtils.isEmpty(" ")     // false（有空格）
StringUtils.isBlank(" ")     // true（仅空白字符）
StringUtils.isNotBlank(" ")  // false

// defaultIfBlank：空/空白时返回默认值
StringUtils.defaultIfBlank(null, "匿名用户") // "匿名用户"
StringUtils.defaultIfBlank("  ", "匿名用户") // "匿名用户"

// 填充对齐
StringUtils.leftPad("42", 8, '0')    // "00000042"
StringUtils.center("OK", 10, '*')    // "****OK****"
StringUtils.repeat('-', 20)          // "--------------------"

// 截断
StringUtils.abbreviate("这是一段很长的文本", 8)  // "这是一段..."
StringUtils.left("Hello World", 5)               // "Hello"

// 分割（自动跳过空段，不同于 String.split）
String[] parts = StringUtils.split("a,b,,c", ',');
// parts = ["a", "b", "c"]（长度3，跳过了空段）

// 差异比较
StringUtils.difference("abcde", "abxyz")            // "xyz"
StringUtils.getLevenshteinDistance("kitten", "sitting") // 3
```

### 2. NumberUtils — 安全的类型转换

```java
// 转换失败不抛异常，返回默认值
NumberUtils.toInt("123")          // 123
NumberUtils.toInt("abc")          // 0（默认值）
NumberUtils.toInt("abc", -1)      // -1（自定义默认值）
NumberUtils.toLong(null, 0L)      // 0L
NumberUtils.toDouble("3.14")      // 3.14

// 数字判断
NumberUtils.isCreatable("42")     // true
NumberUtils.isCreatable("0x1A")   // true（支持十六进制）
NumberUtils.isParsable("3.14")    // true

// 多参数最大/最小
NumberUtils.max(1, 5, 3, 9, 2)   // 9
NumberUtils.min(1, 5, 3, 9, 2)   // 1
```

### 3. RandomStringUtils — 随机字符串生成

```java
// 6位数字验证码
RandomStringUtils.randomNumeric(6)         // "482917"

// 8位字母
RandomStringUtils.randomAlphabetic(8)      // "jKpMnQrS"

// 16位字母+数字（Token/密码）
RandomStringUtils.randomAlphanumeric(16)   // "aB3cD7eF9gH1iJ2k"

// 指定字符集（去掉易混淆字符）
RandomStringUtils.random(6, "ABCDEFGHJKMNPQRSTUVWXYZ23456789")  // 邀请码
```

### 4. Pair / Triple — 轻量级多值返回

```java
// 不需要为"分页查询结果"单独定义 PageResult 类
Pair<List<User>, Long> result = ImmutablePair.of(userList, totalCount);
List<User> users = result.getLeft();
Long total = result.getRight();

// Triple：三元组
Triple<String, Integer, Double> score = Triple.of("张三", 90, 95.5);
score.getLeft();   // "张三"
score.getMiddle(); // 90
score.getRight();  // 95.5
```

### 5. Validate — 参数校验断言

```java
// 比手写 if-throw 更简洁
Validate.notBlank(username, "用户名不能为空");
Validate.isTrue(age >= 18, "年龄必须大于18岁，当前: %d", age);
Validate.notNull(order, "订单不能为null");
Validate.notEmpty(items, "购物车不能为空");
Validate.inclusiveBetween(1, 100, quantity, "数量必须在1~100之间");
```

### 6. StopWatch — 精确计时

```java
StopWatch sw = new StopWatch("API监控");
sw.start("参数校验");
// ... 业务代码 ...
sw.stop();

sw.start("DB查询");
// ... 业务代码 ...
sw.stop();

System.out.println(sw.prettyPrint()); // 格式化输出各阶段耗时和占比
```

### 7. ExceptionUtils — 异常链分析

```java
try {
    processOrder(orderId);
} catch (Exception e) {
    // 找根本原因
    Throwable rootCause = ExceptionUtils.getRootCause(e);
    String rootMsg = rootCause != null ? rootCause.getMessage() : e.getMessage();
    
    // 格式化完整堆栈（用于日志存储）
    String fullStack = ExceptionUtils.getStackTrace(e);
    log.error("根因: {}, 堆栈: {}", rootMsg, fullStack);
    
    // 获取异常链列表
    List<Throwable> chain = ExceptionUtils.getThrowableList(e);
    // chain: [BizException, ServiceException, DBException]
}
```

---

## Spring Boot 集成

Commons Lang3 是纯工具库，不需要特别的 Spring 集成，直接引入依赖即可在任何 Bean 中使用。

### 典型集成场景

**1. 统一参数校验工具类**

```java
@Component
public class ParamValidator {
    
    public void validatePageRequest(int pageNum, int pageSize) {
        Validate.isTrue(pageNum >= 1, "页码必须>=1");
        Validate.inclusiveBetween(1, 200, pageSize, "每页数量必须在1~200之间");
    }
    
    public void validateNotBlank(String value, String fieldName) {
        Validate.notBlank(value, "%s不能为空", fieldName);
    }
}
```

**2. 全局异常处理器中使用 ExceptionUtils**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        String rootMsg = rootCause != null ? rootCause.getMessage() : e.getMessage();
        
        // 只记录根本原因到日志，不暴露给前端
        log.error("请求异常 url={}, rootCause={}", request.getRequestURI(), rootMsg);
        log.debug("详细堆栈: {}", ExceptionUtils.getStackTrace(e));
        
        return Result.error("系统异常，请稍后重试");
    }
}
```

**3. 订单号生成服务**

```java
@Service
public class OrderNoService {
    
    /**
     * 生成唯一订单号：ORD + yyyyMMddHHmmss + 6位随机数
     */
    public String generateOrderNo() {
        return "ORD" 
            + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss")
            + RandomStringUtils.randomNumeric(6);
    }
    
    /**
     * 生成邀请码：6位，去掉易混淆字符
     */
    public String generateInviteCode() {
        return RandomStringUtils.random(6, "ABCDEFGHJKMNPQRSTUVWXYZ23456789");
    }
}
```

**4. 配置值安全读取**

```java
@Service
public class ConfigService {
    
    @Autowired
    private Environment env;
    
    public int getIntConfig(String key, int defaultValue) {
        return NumberUtils.toInt(env.getProperty(key), defaultValue);
    }
    
    public double getDoubleConfig(String key, double defaultValue) {
        return NumberUtils.toDouble(env.getProperty(key), defaultValue);
    }
}
```

**5. 接口性能监控 AOP**

```java
@Aspect
@Component
public class PerformanceAspect {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceAspect.class);
    
    @Around("@annotation(com.example.annotation.Monitor)")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        StopWatch sw = StopWatch.createStarted();
        try {
            return pjp.proceed();
        } finally {
            sw.stop();
            long elapsed = sw.getTime();
            if (elapsed > 1000) { // 超过1秒告警
                log.warn("慢接口告警: {} 耗时 {}ms", pjp.getSignature(), elapsed);
            }
        }
    }
}
```

---

## 注意事项与踩坑

### 1. `StringUtils.split` vs `String.split` 行为差异

```java
// JDK String.split —— 包含空字符串
"a,b,,c".split(",")           // ["a", "b", "", "c"]（长度4）

// Commons Lang3 StringUtils.split —— 自动跳过空段
StringUtils.split("a,b,,c", ',')   // ["a", "b", "c"]（长度3）

// 如果需要保留空段，使用 StringUtils.splitPreserveAllTokens
StringUtils.splitPreserveAllTokens("a,b,,c", ',')  // ["a", "b", "", "c"]（长度4）
```

### 2. `RandomStringUtils` 不是加密安全随机

```java
// ❌ 不要用于生成密码重置Token、支付签名等安全场景
String unsafeToken = RandomStringUtils.randomAlphanumeric(32);

// ✅ 安全场景应使用 SecureRandom
import java.security.SecureRandom;
SecureRandom sr = new SecureRandom();
byte[] bytes = new byte[32];
sr.nextBytes(bytes);
String secureToken = Base64.getEncoder().encodeToString(bytes);
```

### 3. `Validate` 抛出的是 `IllegalArgumentException`

```java
// Validate.notBlank 抛 IllegalArgumentException（不是 NullPointerException）
// 如果在 @ExceptionHandler 中捕获，记得处理 IllegalArgumentException
Validate.notBlank(username, "用户名不能为空"); // 抛 IllegalArgumentException: 用户名不能为空

// Validate.validIndex 抛 IndexOutOfBoundsException
// Validate.notNull    抛 NullPointerException
```

### 4. `StopWatch` 不是线程安全的

```java
// ❌ 不要在多线程间共享同一个 StopWatch
static final StopWatch sw = new StopWatch(); // 危险！

// ✅ 每次使用都创建新实例，或使用 ThreadLocal
StopWatch sw = StopWatch.createStarted(); // 方法内部局部变量
```

### 5. `DateUtils` vs Java 8 `java.time`

```java
// Lang3 的 DateUtils 基于 Date/Calendar，适合维护旧代码
// 新项目应优先使用 Java 8 java.time（LocalDate, LocalDateTime 等）
// 如果项目已经用 Java 8+，DateUtils 只在维护遗留代码时使用
```

### 6. `ToStringBuilder` 反射模式有性能开销

```java
// ❌ 反射模式（方便但慢，不适合高频调用的实体）
return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);

// ✅ 手动 append 模式（推荐生产代码）
return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("id", id)
        .append("name", name)
        .toString();
```

### 7. `commons-lang` (旧版) vs `commons-lang3` 版本冲突

```xml
<!-- 两者包名不同，可以共存，但建议统一使用 Lang3 -->
<!-- 旧版（不推荐新项目使用） -->
<dependency>
    <groupId>commons-lang</groupId>
    <artifactId>commons-lang</artifactId>
    <version>2.6</version>
</dependency>

<!-- 新版（推荐） -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.14.0</version>
</dependency>
```

---

## 运行方法

```bash
cd java-tools-learning/commons-lang3-demo

# 编译构建
mvn clean package -DskipTests

# 运行基础演示（StringUtils/NumberUtils/ArrayUtils/BooleanUtils）
mvn exec:java -Dexec.mainClass="com.example.commonslang3.CommonsLang3BasicDemo"

# 运行进阶演示（Builder/Pair/反射/StopWatch/DateUtils）
mvn exec:java -Dexec.mainClass="com.example.commonslang3.CommonsLang3AdvancedDemo"

# 运行实战场景演示（用户注册/订单号/接口监控/分数计算）
mvn exec:java -Dexec.mainClass="com.example.commonslang3.CommonsLang3PracticalDemo"
```

---

## 总结

Apache Commons Lang3 是 Java 开发的必备工具库，核心价值在于：

1. **null 安全**：所有方法对 null 免疫，避免 NullPointerException
2. **减少样板代码**：用 `StringUtils.defaultIfBlank`、`NumberUtils.toInt` 替代大量 if-null 判断
3. **功能完整**：从字符串到日期、从随机数到反射，一个依赖覆盖大多数工具需求
4. **久经考验**：Apache 维护超过 20 年，被几乎所有 Java 项目间接依赖

**适用场景**：参数校验、随机码生成、配置值读取、性能计时、异常日志、轻量级多值返回、遗留代码维护
