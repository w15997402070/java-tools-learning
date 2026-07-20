# Day 28: SLF4J + Logback — Java 日志框架

## 工具简介

**SLF4J**（Simple Logging Facade for Java）是 Java 生态的日志门面（Facade），定义了统一的日志 API。**Logback** 是 SLF4J 的原生实现，由 Log4j 的原作者 Ceki Gülcü 设计开发，是 Spring Boot 的默认日志框架。

两者的关系可以理解为：SLF4J = 接口（JDBC），Logback = 实现（MySQL Driver）。代码只需依赖 SLF4J API，运行时可以灵活切换底层实现（Logback / Log4j2 / java.util.logging）。

- **SLF4J GitHub**: https://github.com/qos-ch/slf4j
- **Logback GitHub**: https://github.com/qos-ch/logback
- **官方文档**: https://logback.qos.ch/manual/
- **版本（本次 Demo）**: SLF4J 2.0.9 + Logback 1.4.14
- **SLF4J 星标**: 2.2k+
- **Logback 星标**: 2.9k+
- **Spring Boot 内置**: 是（spring-boot-starter-logging 默认引入）

### 核心概念

```
应用代码
  ↓ 只依赖 SLF4J API
SLF4J API (org.slf4j.Logger)
  ↓ 运行时绑定
Logback (logback-classic)  ← Spring Boot 默认
  ↓ 配置驱动
logback.xml / logback-spring.xml
```

| 概念 | 说明 |
|------|------|
| **Logger** | 日志记录器，绑定到类/包名，支持层级继承 |
| **Appender** | 日志输出目标（控制台/文件/网络/数据库） |
| **Encoder/Layout** | 日志格式化（Pattern/JSON） |
| **Level** | 日志级别：TRACE < DEBUG < INFO < WARN < ERROR |
| **MDC** | Mapped Diagnostic Context，线程级上下文传递 |
| **Marker** | 日志标签，用于按业务域分流 |
| **TurboFilter** | 全局过滤器，性能优于 Appender 级 Filter |

## Maven 依赖配置

```xml
<properties>
    <slf4j.version>2.0.9</slf4j.version>
    <logback.version>1.4.14</logback.version>
</properties>

<dependencies>
    <!-- SLF4J API —— 代码中只 import 这一个 -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>${slf4j.version}</version>
    </dependency>

    <!-- Logback 实现（包含 logback-classic + logback-core） -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
    </dependency>

    <!-- JSON 格式日志（可选） -->
    <dependency>
        <groupId>net.logstash.logback</groupId>
        <artifactId>logstash-logback-encoder</artifactId>
        <version>7.3</version>
    </dependency>
</dependencies>
```

> **版本兼容性**：SLF4J 2.0.x 需要 Java 8+。如果是 Java 7 或更早项目，使用 SLF4J 1.7.x + Logback 1.2.x。

## Spring Boot 集成方式

Spring Boot 默认已经集成了 SLF4J + Logback，通过 `spring-boot-starter-web` 即可引入。

### 方式一：application.yml 配置（简单场景）

```yaml
logging:
  level:
    root: INFO
    com.example: DEBUG                # 自己的包设置为 DEBUG
    org.springframework: WARN         # Spring 框架降噪
  file:
    path: ./logs                      # 日志文件目录
    name: ./logs/myapp.log           # 日志文件名（与 path 二选一）
  pattern:
    console: "%d{HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} [%X{traceId}] - %msg%n"
  logback:
    rollingpolicy:
      max-file-size: 50MB
      max-history: 30
```

### 方式二：logback-spring.xml（复杂场景）

Spring Boot 会优先加载 `logback-spring.xml`（支持 Spring Profile），其次加载 `logback.xml`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 从 application.yml 读取属性 -->
    <springProperty scope="context" name="LOG_HOME" source="logging.file.path" defaultValue="logs"/>

    <!-- 开发环境：彩色控制台 + DEBUG -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- 生产环境：文件 + JSON + ERROR 分离 -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="ASYNC_FILE"/>
            <appender-ref ref="JSON_FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
    </springProfile>
</configuration>
```

### 方式三：Web Filter 中注入 traceId

```java
@WebFilter("/*")
public class TraceFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);
        try {
            chain.doFilter(req, resp);
        } finally {
            MDC.clear(); // 关键：防止内存泄漏
        }
    }
}
```

### 完整代码示例（Spring Boot 项目）

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @PostMapping("/create")
    public Result<Order> create(@RequestBody CreateOrderRequest request) {
        log.info("创建订单请求：userId={}, productId={}, quantity={}",
                request.getUserId(), request.getProductId(), request.getQuantity());

        try {
            Order order = orderService.create(request);
            log.info("订单创建成功：orderId={}, amount={}", order.getId(), order.getAmount());
            return Result.ok(order);
        } catch (InsufficientStockException e) {
            log.warn("库存不足：productId={}, requested={}, available={}",
                    request.getProductId(), request.getQuantity(), e.getAvailableStock());
            return Result.error("库存不足");
        } catch (Exception e) {
            log.error("订单创建异常：userId={}, productId={}",
                    request.getUserId(), request.getProductId(), e);
            return Result.error("系统异常");
        }
    }
}
```

## 注意事项

### 1. MDC 内存泄漏（严重）

MDC 基于 ThreadLocal，请求结束后必须调用 `MDC.clear()`。建议在 Filter 的 `finally` 块中执行，否则在 Tomcat 线程池场景下会：
- 旧请求的 traceId 污染下一个请求
- 长期运行导致内存泄漏

```java
// ✅ 正确：finally 中清理
try {
    MDC.put("traceId", traceId);
    chain.doFilter(req, resp);
} finally {
    MDC.clear();
}
```

### 2. 多线程下 MDC 不可见

MDC 只在线程内传递。线程池中的子线程不会自动继承父线程的 MDC。

```java
// ❌ 子线程拿不到 MDC
executor.submit(() -> log.info("traceId 不见了"));

// ✅ 手动传递
Map<String, String> context = MDC.getCopyOfContextMap();
executor.submit(() -> {
    MDC.setContextMap(context);
    try {
        log.info("traceId 可见");
    } finally {
        MDC.clear();
    }
});
```

### 3. 日志级别性能差异

- **TRACE/DEBUG 在生产环境应关闭**，否则大量 I/O 拖垮性能
- 使用参数化消息 `log.debug("user={}", user)` 而非字符串拼接 `log.debug("user=" + user)`
- 对复杂对象，用 `log.isDebugEnabled()` 包裹，避免不必要的 `toString()` 调用

### 4. 敏感信息泄露

不要在日志中输出密码、Token、身份证号、手机号等敏感信息。

```java
// ❌ 危险
log.info("用户登录：password={}", password);

// ✅ 脱敏后输出
log.info("用户登录：password=***, phone=138****1234");
```

### 5. Logback 配置热加载

在 `logback.xml` 中添加 `scan="true"` 可以自动重载配置（默认 60 秒扫描一次）：

```xml
<configuration scan="true" scanPeriod="60 seconds">
```

> 注意：仅修改配置文件会触发重载；如果修改了类路径下的 logback.xml 并重新部署，需要重启应用。

### 6. 异步日志丢数据风险

```xml
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>  <!-- 队列满时不丢弃 -->
    <neverBlock>false</neverBlock>                 <!-- 队列满时阻塞 -->
</appender>
```

- `neverBlock=true`：队列满时直接丢弃事件，不阻塞业务线程（有丢日志风险）
- `neverBlock=false`：队列满时阻塞业务线程（保证不丢日志，但可能影响响应时间）
- 生产环境建议 `neverBlock=false`，并通过监控队列大小调整 `queueSize`

### 7. 依赖冲突

项目中可能引入多套日志框架，需要排除冲突的依赖：

```xml
<!-- 排除 commons-logging（Spring 默认引入） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <exclusions>
        <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 引入 jcl-over-slf4j 桥接 -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jcl-over-slf4j</artifactId>
    <version>2.0.9</version>
</dependency>
```

### 8. 日志文件滚动配置

生产环境必须配置日志滚动策略，否则单个文件会无限增长直到磁盘写满：

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>logs/app-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
    <maxFileSize>50MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>3GB</totalSizeCap>
</rollingPolicy>
```

## 运行方法

```bash
# 进入项目目录
cd d:/ai/workbuddy/java-tools-learning/logback-demo

# 编译
mvn clean package -DskipTests

# 运行基础 Demo
mvn exec:java -Dexec.mainClass="com.example.logback.LogbackBasicDemo"

# 运行进阶 Demo
mvn exec:java -Dexec.mainClass="com.example.logback.LogbackAdvancedDemo"

# 运行实战 Demo
mvn exec:java -Dexec.mainClass="com.example.logback.LogbackPracticalDemo"

# 查看生成的日志文件
ls logs/
# app.log app-json.log app-error.log
```

### 验证日志配置

```bash
# 运行后检查日志文件是否生成
cat logs/logback-demo.log

# 确认 JSON 日志格式
cat logs/logback-demo-json.log | head -5

# 确认 ERROR 日志隔离
cat logs/logback-demo-error.log
```

## 常见 logback.xml 配置模板

### 纯控制台（开发环境）

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) %cyan(%logger{36}) - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="DEBUG">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### 文件 + 控制台（最常用）

见本项目 `logback-demo/src/main/resources/logback.xml`。

### ELK 采集专用（JSON）

```xml
<appender name="JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.json</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/app-%d{yyyy-MM-dd}.%i.json.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>7</maxHistory>
    </rollingPolicy>
</appender>
```

## 总结

| 特性 | 说明 |
|------|------|
| **日志门面** | SLF4J 解耦 API 和实现，方便切换底层框架 |
| **参数化日志** | `{}` 占位符，避免不必要的字符串拼接 |
| **MDC** | 线程级上下文，分布式链路追踪的基础 |
| **异步日志** | AsyncAppender 不阻塞业务线程 |
| **热加载** | `scan="true"` 支持运行时修改日志级别 |
| **JSON 格式** | logstash-logback-encoder 支持 ELK/Splunk 采集 |
| **Spring Boot** | 默认集成，零配置即可使用 |
