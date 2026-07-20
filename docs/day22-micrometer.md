# Day 22: Micrometer — JVM 微服务指标监控库

## 工具简介

**Micrometer** 是 JVM 生态中最流行的指标采集门面（Metrics Facade），为市面上所有主流监控系统提供统一 API。

- **GitHub**: https://github.com/micrometer-metrics/micrometer
- **GitHub Stars**: 4.2k+
- **版本**: 1.9.17（Java 8 兼容，Spring Boot 2.7.x 内置版本）
- **License**: Apache 2.0

### 核心定位

Micrometer 之于 Metrics，如同 SLF4J 之于 Logging：

```
你的业务代码 → Micrometer API → [Prometheus / JMX / Graphite / InfluxDB / ...]
```

### 支持的监控后端

| 后端          | Registry 类                   | 适用场景                       |
|---------------|-------------------------------|-------------------------------|
| Prometheus    | `PrometheusMeterRegistry`     | 云原生标配，搭配 Grafana        |
| JMX           | `JmxMeterRegistry`            | 传统运维，JConsole/VisualVM 查看 |
| Graphite      | `GraphiteMeterRegistry`       | 传统时序数据库                  |
| InfluxDB      | `InfluxMeterRegistry`         | 时序 + 事件数据库               |
| Datadog       | `DatadogMeterRegistry`        | SaaS 监控                      |
| CloudWatch    | `CloudWatchMeterRegistry`     | AWS 原生监控                   |
| Elastic       | `ElasticMeterRegistry`        | ELK 生态                       |
| Simple        | `SimpleMeterRegistry`         | 开发测试 / 内存存储             |

### 核心指标类型

| 类型                  | 说明                       | 典型场景                     |
|-----------------------|----------------------------|------------------------------|
| `Counter`             | 只增不减的计数              | API 调用次数、错误次数         |
| `Gauge`               | 瞬时值（可增可减）           | 队列长度、内存用量、库存水平    |
| `Timer`               | 耗时 + 调用次数 + 分布      | HTTP 响应时间、DB 查询耗时     |
| `DistributionSummary` | 分布统计（非耗时）           | 请求体大小、消息拉取数量       |
| `FunctionCounter`     | 适配已有计数器的 Counter     | 迁移 legacy 监控              |
| `FunctionTimer`       | 适配已有耗时统计的 Timer     | 迁移 legacy 监控              |
| `LongTaskTimer`       | 长时间运行任务计时           | 批处理任务、数据迁移           |

---

## Maven 依赖配置

```xml
<properties>
    <micrometer.version>1.9.17</micrometer.version>
</properties>

<dependencies>
    <!-- Micrometer 核心 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-core</artifactId>
        <version>${micrometer.version}</version>
    </dependency>

    <!-- Prometheus 后端（生产最常用） -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
        <version>${micrometer.version}</version>
    </dependency>

    <!-- JMX 后端（本地调试） -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-jmx</artifactId>
        <version>${micrometer.version}</version>
    </dependency>
</dependencies>
```

---

## Spring Boot 集成方式

### 1. 添加依赖（Spring Boot 2.x / 3.x 通用）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2. application.yml 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      env: ${spring.profiles.active}
  endpoint:
    prometheus:
      enabled: true
```

### 3. 注入 MeterRegistry 并创建指标

```java
@RestController
public class OrderController {

    private final MeterRegistry registry;
    private final Counter ordersCreated;
    private final Timer orderTimer;

    public OrderController(MeterRegistry registry) {
        this.registry = registry;
        this.ordersCreated = Counter.builder("orders.created")
                .description("订单创建总数")
                .register(registry);
        this.orderTimer = Timer.builder("order.create.time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @PostMapping("/orders")
    public String createOrder() {
        ordersCreated.increment();
        return orderTimer.record(() -> {
            // 业务逻辑
            return "OK";
        });
    }
}
```

### 4. 使用 @Timed 注解（AOP 方式）

```java
@Configuration
public class MetricsConfig {
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
public class PaymentService {
    @Timed(value = "payment.process", percentiles = {0.5, 0.95, 0.99})
    public void process() {
        // 支付逻辑
    }
}
```

### 5. 对接 Grafana 监控面板

1. Prometheus 配置抓取 `/actuator/prometheus`
2. Grafana 添加 Prometheus 数据源
3. 导入现成面板
   - **JVM 监控**: Grafana Dashboard ID `4701`（Micrometer JVM 指标）
   - **Spring Boot 监控**: ID `10280` 或 `12900`

---

## 注意事项

### 1. High Cardinality Tag 陷阱

```java
// ❌ 错误：用 userId 作为 tag 会导致时间序列爆炸
Counter.builder("api.requests")
    .tag("userId", "12345")
    .register(registry);

// ✅ 正确：用有限维度
Counter.builder("api.requests")
    .tag("endpoint", "/api/orders")
    .tag("status", "200")
    .register(registry);
```

**影响**：Prometheus 每个唯一 tag 组合都会生成独立时间序列。userId 这种高基数 tag 会导致内存和存储爆炸。

### 2. 指标命名规范

- Prometheus 默认将 `.` 转换为 `_`（`http.requests` → `http_requests`）
- Counter 自动追加 `_total` 后缀（`orders_created_total`）
- Timer 自动生成 `_count` / `_sum` / `_max` / `_bucket`
- **建议统一使用 `.` 分隔**，保持 Micrometer 推荐风格

### 3. Timer 的 SLO Buckets 配置

默认 buckets（1ms, 5ms, 10ms, 30ms, 60s...）可能不适合你的场景。

```yaml
# 自定义 buckets：适合 P99 在 200ms 以内的接口
management:
  metrics:
    distribution:
      slo:
        http.server.requests: 10ms, 50ms, 100ms, 200ms, 500ms, 1s
      percentiles-histogram:
        http.server.requests: true
```

### 4. 性能开销

| 操作                     | 开销           | 说明                              |
|--------------------------|----------------|-----------------------------------|
| `Counter.increment()`    | ~30 ns/op      | 极低，可忽略                       |
| `Timer.record(Runnable)` | ~100 ns/op     | 不含业务逻辑耗时，仅有 wrap 开销    |
| `Gauge`                  | ~0（读取时计算）| 本身无开销，scrape 时调用 getter   |
| Prometheus scrape        | <5ms（<1000条）| 正常规模下影响极小                  |

结论：生产环境放心使用，性能影响可忽略。

### 5. 避免在指标回调中做耗时操作

```java
// ❌ 错误：Gauge 的 getter 中做耗时 I/O
Gauge.builder("db.connections", () -> {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM ...", Integer.class);
}).register(registry);

// ✅ 正确：用定时任务更新 AtomicInteger，Gauge 只读内存值
```

### 6. Spring Boot 3.x 变化

- Spring Boot 3 使用 Micrometer 1.10+（底层升级，API 兼容）
- Observation API 是 Micrometer 1.10+ 的新推荐方式，但仍支持旧 API
- Spring Boot 3 使用 Micrometer 1.10+（底层升级，API 兼容），内置 JVM 指标替代第三方 extras 库

### 7. 生产环境 ConcurrentModificationException 风险

- 不要在多线程下同时 `registry.remove(meter)` 和 `registry.forEachMeter()`
- 不要在 scrape 期间移除 Meter，会触发 CME

---

## 运行方法

```bash
# 1. 编译
cd D:/ai/workbuddy/java-tools-learning/micrometer-demo
set JAVA_HOME=D:/jdk/jdk17
mvn clean package -DskipTests

# 2. 运行基础 Demo
java -cp target/micrometer-demo-1.0-SNAPSHOT.jar com.example.micrometer.MicrometerBasicDemo

# 3. 运行进阶 Demo
java -cp target/micrometer-demo-1.0-SNAPSHOT.jar com.example.micrometer.MicrometerAdvancedDemo

# 4. 运行实战 Demo（启动 Prometheus 端点，访问 http://localhost:8080/metrics）
java -cp target/micrometer-demo-1.0-SNAPSHOT.jar com.example.micrometer.MicrometerPracticalDemo
```

---

## Prometheus + Grafana 监控栈架构

```
┌─────────────────────┐
│   Spring Boot App   │
│  /actuator/prometheus│──── HTTP /metrics ────┐
└─────────────────────┘                        │
                                               ▼
                                    ┌───────────────────┐
                                    │    Prometheus     │
                                    │  15s scrape 间隔   │
                                    └────────┬──────────┘
                                             │
                                             ▼
                                    ┌───────────────────┐
                                    │     Grafana       │
                                    │  可视化 + 告警     │
                                    └───────────────────┘
```

---

## 常见问题

**Q: 多个 Registry 如何同时使用？**
使用 `CompositeMeterRegistry` 聚合多个 Registry，Spring Boot 默认使用此模式。

**Q: Counter 可以减吗？**
不行。Counter 设计上只增不减。需要可增可减用 Gauge。需要"率"用 `rate()` 函数（在 Prometheus 端计算）。

**Q: 指标数据存在哪？**
Micrometer 不负责存储。数据由后端（Prometheus/JMX/InfluxDB）自行存储和管理。

**Q: 怎么对接公司已有的监控平台？**
micrometer-registry-* 覆盖几乎所有主流后端。如果是自研平台，可以实现自定义 `MeterRegistry`。

---

## 参考资料

- [Micrometer 官方文档](https://micrometer.io/docs)
- [Micrometer Concepts](https://micrometer.io/docs/concepts)
- [Spring Boot Metrics 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
- [Prometheus Java Client](https://github.com/prometheus/client_java)
- [Grafana Dashboard 市场](https://grafana.com/grafana/dashboards/)
