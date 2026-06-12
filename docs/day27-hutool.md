# Day 27: Hutool — Java 国产超实用工具类库

## 工具简介

**Hutool** 是 Dromara 组织开源的一款 Java 工具类库，取名自 "糊涂" 谐音，宗旨是 "A set of tools that keep Java sweet."（让 Java 也可以甜甜的）。它封装了 JDK 中常见但繁琐的操作，提供了文件、日期、加密、HTTP、JSON、验证码等数百种开箱即用的工具方法，极大提升日常开发效率。

- **GitHub**: https://github.com/dromara/hutool
- **官方文档**: https://www.hutool.cn
- **版本**: 5.8.25（Java 8 兼容，最新主线 6.x 需 Java 11+）
- **星标**: 29k+
- **Maven Central 月下载量**: 200万+

### 核心模块

| 模块 | 功能 | 对应包 |
|------|------|--------|
| hutool-core | 核心工具（日期/字符串/IO/集合/Bean/类型转换） | `cn.hutool.core.*` |
| hutool-crypto | 加密解密（MD5/SHA/AES/RSA/SM系列） | `cn.hutool.crypto.*` |
| hutool-http | HTTP客户端封装 | `cn.hutool.http.*` |
| hutool-json | JSON处理 | `cn.hutool.json.*` |
| hutool-cache | 缓存实现（FIFO/LFU/LRU/Timed） | `cn.hutool.cache.*` |
| hutool-captcha | 验证码生成 | `cn.hutool.captcha.*` |
| hutool-extra | 扩展（邮件/模板/二维码/Emoji等） | `cn.hutool.extra.*` |
| hutool-cron | 定时任务 | `cn.hutool.cron.*` |

## Maven 依赖配置

```xml
<!-- 全部引入（推荐，jar包仅1.7MB） -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.25</version>
</dependency>

<!-- 按需引入（减少依赖体积） -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-core</artifactId>
    <version>5.8.25</version>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-http</artifactId>
    <version>5.8.25</version>
</dependency>
```

**注意**：`hutool-all` 包含全部模块，但仍在 2MB 以内，日常项目建议直接使用 `hutool-all`。

## 核心功能速览

### 1. 日期时间 (DateUtil)

```java
// 当前时间
DateTime now = DateUtil.date();

// 解析（自动识别格式）
DateUtil.parse("2025-06-12");
DateUtil.parse("2025/06/12 15:30:00");
DateUtil.parse("20250612");

// 日期计算
DateTime tomorrow = DateUtil.offset(now, DateField.DAY_OF_MONTH, 1);
long days = DateUtil.between(start, end, DateUnit.DAY);

// 日期范围
DateUtil.beginOfDay(now);   // 当天 00:00:00
DateUtil.endOfMonth(now);   // 当月最后一天 23:59:59
DateUtil.beginOfWeek(now);  // 本周一
```

### 2. 字符串工具 (StrUtil)

```java
StrUtil.isEmpty(str);              // null或空串
StrUtil.isBlank(str);              // 空白判断
StrUtil.format("Hello, {}!", name); // 类似slf4j格式化
StrUtil.toCamelCase("user_name");  // 下划线转驼峰
StrUtil.hide("13812345678", 3, 7); // 掩码 -> 138****5678
```

### 3. 集合工具 (CollUtil)

```java
List<String> list = CollUtil.newArrayList("a", "b", "c");
CollUtil.intersection(list1, list2);  // 交集
CollUtil.subtract(list1, list2);      // 差集
```

### 4. 加密解密

```java
// 摘要
SecureUtil.md5(data);
DigestUtil.sha256Hex(data);

// AES对称加密
AES aes = SecureUtil.aes("16位密钥".getBytes());
String enc = aes.encryptBase64(data);
String dec = aes.decryptStr(enc);
```

### 5. HTTP客户端

```java
// 简化GET
String result = HttpUtil.get("https://api.example.com/data");

// POST JSON
HttpResponse response = HttpRequest.post(url)
    .header("Content-Type", "application/json")
    .body(jsonBody)
    .timeout(5000)
    .execute();
```

### 6. JSON处理

```java
// 对象转JSON
String json = JSONUtil.toJsonPrettyStr(obj);

// JSON转对象
User user = JSONUtil.toBean(json, User.class);

// 动态JSONObject
JSONObject obj = JSONUtil.createObj()
    .set("code", 200)
    .set("data", subObj);
```

### 7. 雪花算法唯一ID

```java
Snowflake snowflake = IdUtil.getSnowflake(1, 1);
long id = snowflake.nextId();  // 趋势递增19位
String uuid = IdUtil.simpleUUID(); // 32位无横线UUID
```

### 8. 验证码生成

```java
CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(200, 100, 4, 20);
String code = captcha.getCode();           // 验证码文字
String base64 = captcha.getImageBase64Data(); // Base64图片
// HTML: <img src="data:image/png;base64,${base64}"/>
```

## Spring Boot 集成方式

Hutool 本质是纯工具类库，**无需 Spring 容器管理**，直接静态方法调用即可：

```java
@RestController
public class OrderController {

    // 直接在业务代码中使用
    @GetMapping("/order/{id}")
    public Result getOrder(@PathVariable String id) {
        // 使用Hutool生成响应
        return Result.success(orderService.findById(id));
    }
}

// 统一响应体
public class Result {
    private int code;
    private String message;
    private Object data;

    public static Result success(Object data) {
        Result r = new Result();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    // 使用Hutool JSON序列化
    @Override
    public String toString() {
        return JSONUtil.toJsonStr(this);
    }
}
```

### 工具类封装示例

```java
// 订单号生成工具
public class OrderIdGenerator {
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    public static String generate() {
        return StrUtil.format("ORD{}{}",
            DateUtil.format(DateUtil.date(), "yyyyMMdd"),
            String.valueOf(SNOWFLAKE.nextId()).substring(10));
    }
}

// 密码加密工具
public class PasswordUtil {
    public static String encrypt(String rawPassword) {
        return SecureUtil.md5(rawPassword + "your_salt");
    }
    public static boolean verify(String raw, String encrypted) {
        return encrypt(raw).equals(encrypted);
    }
}
```

## 注意事项

### 1. 版本选择
- **Java 8 项目**：使用 `5.8.x` 系列（当前推荐 5.8.25）
- **Java 11+ 项目**：可使用 `6.x` 系列（API 有少量不兼容变化）
- 不要混用不同大版本的 Hutool，可能导致 ClassNotFoundException

### 2. JSON 模块对比
- Hutool 内置 JSON 处理（`cn.hutool.json.*`），无需引入 Jackson/Gson
- 功能较弱于 Jackson（不支持数据绑定注解如 `@JsonProperty`）
- **建议**：简单场景用 Hutool JSON，复杂场景仍用 Jackson/Fastjson2
- 如果项目中已有 Jackson，Hutool 的 JSON 可能产生类冲突（极少见）

### 3. HTTP 客户端注意
- Hutool HTTP 底层基于 JDK `HttpURLConnection`，非异步
- 高并发场景建议使用 OkHttp 或 Apache HttpClient 替代
- 不支持 HTTP/2
- 大文件下载时注意设置超时

### 4. 加密模块
- `SecureUtil` 统一入口，但内部实现有多个版本迭代
- **MD5 不安全**：仅用于校验、非安全场景
- **AES 密钥长度**：需 16/24/32 字节（对应 AES-128/192/256）
- 国密算法 SM2/SM3/SM4 需要 Bouncy Castle 额外依赖

### 5. 定时任务（CronUtil）
- Hutool CronUtil 是轻量级实现，非分布式
- 不支持任务持久化
- 复杂调度场景建议使用 Quartz 或 XXL-Job
- `setMatchSecond(true)` 启用秒级后，表达式格式为 7 位

### 6. 已知问题和限制
- **FileUtil.readUtf8String**：大文件（>100MB）会 OOM，需用 `readUtf8Lines` 流式读取
- **BeanUtil.copyProperties**：基于反射，性能弱于 MapStruct 编译期方案
- **缓存模块**：不支持分布式，仅本地缓存；生产环境建议使用 Caffeine 或 Redis
- **模块拆分**：5.8.x 所有模块在一个 JAR 中，6.x 开始按模块拆分独立 JAR

## 运行方法

### 基础演示
```bash
cd hutool-demo
mvn compile
mvn exec:java -Dexec.mainClass="com.example.hutool.HutoolBasicDemo"
```

### 进阶演示
```bash
mvn exec:java -Dexec.mainClass="com.example.hutool.HutoolAdvancedDemo"
```

### 实战演示
```bash
mvn exec:java -Dexec.mainClass="com.example.hutool.HutoolPracticalDemo"
```

### 编译打包
```bash
JAVA_HOME=D:/jdk/jdk17 mvn clean package -DskipTests
```

## 总结

Hutool 是 Java 开发者必备的"瑞士军刀"，能显著减少样板代码。对于国内团队项目，几乎可以替代 Apache Commons、Google Guava 的部分功能。推荐在新项目中直接引入 `hutool-all`，在存量项目中可逐步按需引入以替换手写工具类。

**优势**：
- API 设计符合国人习惯，中文文档完善
- 依赖少、体积小（~1.7MB）
- 覆盖功能非常全面

**劣势**：
- 部分模块性能不如专业库（如 JSON 不如 Jackson、HTTP 不如 OkHttp）
- 版本升级可能有 Breaking Change（尤其 5.x → 6.x）
- 过于大而全可能导致少量未被使用的代码进入 classpath
