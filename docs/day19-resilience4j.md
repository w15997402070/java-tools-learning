# Day 19 - Resilience4j：微服务弹性框架

## 工具简介

**Resilience4j** 是专为 Java 8+ 和函数式编程设计的轻量级容错框架，是 Netflix Hystrix 的官方推荐替代品（Hystrix 已停止维护）。它基于 Vavr（函数式库）构建，零外部依赖，完整支持微服务中的弹性工程（Resilience Engineering）模式。

| 属性 | 信息 |
|------|------|
| **GitHub** | https://github.com/resilience4j/resilience4j |
| **Stars** | 9.5k+ |
| **版本（Java 8 兼容）** | 1.7.1 |
| **License** | Apache 2.0 |
| **Spring Boot 集成** | resilience4j-spring-boot2（自动配置） |

### 与 Hystrix 对比

| 对比项 | Hystrix | Resilience4j |
|--------|---------|--------------|
| 维护状态 | ❌ 停止维护（2018） | ✅ 活跃维护 |
| 依赖 | RxJava + 重量级 | 轻量，仅需 Vavr |
| 线程隔离 | 强制线程池隔离 | 可选（信号量/线程池） |
| Java 8 特性 | 有限支持 | 原生支持 Lambda/Stream |
| Spring Boot 3 | 不支持 | ✅ 完整支持 |

---

## 核心模块

Resilience4j 采用**装饰器模式（Decorator Pattern）**，每个模块独立，按需组合：

```
CircuitBreaker → RateLimiter → Retry → TimeLimiter → Bulkhead → 实际调用
（推荐包装顺序：从外到内）
```

| 模块 | 作用 | 典型场景 |
|------|------|---------|
| **CircuitBreaker** | 熔断器：失败/慢调用过多时短路 | 保护下游服务、防止级联失败 |
| **RateLimiter** | 限流器：控制每单位时间调用次数 | API 防刷、控制外部接口调用频率 |
| **Retry** | 重试：自动重试失败操作 | 网络抖动、幂等接口瞬时故障 |
| **Bulkhead** | 舱壁隔离：限制并发调用数 | 防止单个依赖耗尽所有资源 |
| **TimeLimiter** | 超时控制：配合 Future 使用 | 防止无限等待 |
| **Cache** | 轻量缓存：基于 Map 实现 | 简单缓存场景（推荐用 Caffeine 替代） |

---

## Maven 依赖配置

```xml
<properties>
    <resilience4j.version>1.7.1</resilience4j.version>
</properties>

<dependencies>
    <!-- 按需引入各模块 -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-circuitbreaker</artifactId>
        <version>${resilience4j.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-ratelimiter</artifactId>
        <version>${resilience4j.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-retry</artifactId>
        <version>${resilience4j.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-bulkhead</artifactId>
        <version>${resilience4j.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-timelimiter</artifactId>
        <version>${resilience4j.version}</version>
    </dependency>

    <!-- vavr：Resilience4j 函数式风格依赖 -->
    <dependency>
        <groupId>io.vavr</groupId>
        <artifactId>vavr</artifactId>
        <version>0.10.4</version>
    </dependency>
</dependencies>
```

**Spring Boot 2.x 一站式依赖（包含全部模块 + 自动配置）：**

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>1.7.1</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<!-- 可选：Actuator 监控 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

## 核心模块使用示例

### 1. CircuitBreaker 熔断器

熔断器有三种状态：

```
CLOSED ──(失败率超标)──▶ OPEN ──(等待超时)──▶ HALF_OPEN ──(探测成功)──▶ CLOSED
                                                              └──(探测失败)──▶ OPEN
```

**编程式配置：**

```java
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)                           // 失败率 > 50% 触发熔断
    .slowCallRateThreshold(50)                          // 慢调用率 > 50% 也触发熔断
    .slowCallDurationThreshold(Duration.ofSeconds(2))   // 超过 2s 视为慢调用
    .slidingWindowSize(10)                               // 滑动窗口大小
    .permittedNumberOfCallsInHalfOpenState(3)           // 半开状态探测次数
    .waitDurationInOpenState(Duration.ofSeconds(5))     // 熔断后等待时间
    .recordExceptions(IOException.class, RuntimeException.class)
    .ignoreExceptions(IllegalArgumentException.class)  // 此异常不计入失败
    .build();

CircuitBreaker cb = CircuitBreaker.of("serviceName", config);

// 事件监听
cb.getEventPublisher()
    .onStateTransition(e -> log.warn("熔断状态变化: {}", e.getStateTransition()))
    .onFailureRateExceeded(e -> log.warn("失败率超标: {}%", e.getFailureRate()));

// 执行调用
try {
    String result = cb.executeSupplier(() -> remoteService.call());
} catch (CallNotPermittedException e) {
    // 熔断器打开，走降级逻辑
    return fallback();
}
```

### 2. RateLimiter 限流器

```java
RateLimiterConfig config = RateLimiterConfig.custom()
    .limitRefreshPeriod(Duration.ofSeconds(1))  // 每 1 秒刷新令牌
    .limitForPeriod(100)                         // 每秒最多 100 次
    .timeoutDuration(Duration.ofMillis(500))     // 等待令牌超时 500ms
    .build();

RateLimiter rateLimiter = RateLimiter.of("apiGateway", config);

// 方式1：executeSupplier（自动获取令牌）
String result = rateLimiter.executeSupplier(() -> doRequest());

// 方式2：手动获取令牌（更灵活）
if (rateLimiter.acquirePermission()) {
    doRequest();
} else {
    throw new TooManyRequestsException("请求过于频繁");
}
```

### 3. Retry 重试

```java
RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)                                         // 最多3次
    .waitDuration(Duration.ofMillis(500))                   // 等待500ms
    .retryExceptions(IOException.class, TimeoutException.class)
    .ignoreExceptions(BusinessException.class)             // 业务异常不重试
    .build();

// 指数退避（推荐生产使用）
RetryConfig configWithBackoff = RetryConfig.custom()
    .maxAttempts(5)
    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
        100,   // 初始等待 100ms
        2.0,   // 每次乘以 2
        0.5,   // 随机抖动系数（±50%）
        5000   // 最大等待 5000ms
    ))
    .build();

Retry retry = Retry.of("orderService", config);
String result = retry.executeSupplier(() -> orderService.createOrder(req));
```

### 4. Bulkhead 舱壁隔离

```java
BulkheadConfig config = BulkheadConfig.custom()
    .maxConcurrentCalls(10)              // 最大并发数
    .maxWaitDuration(Duration.ofMillis(100)) // 排队超时
    .build();

Bulkhead bulkhead = Bulkhead.of("paymentService", config);

// 调用被拒绝时抛出 BulkheadFullException
try {
    String result = bulkhead.executeSupplier(() -> paymentService.pay(order));
} catch (BulkheadFullException e) {
    return "服务繁忙，请稍后重试";
}
```

### 5. 组合使用（Decorator 链）

```java
// 多组件叠加（Java 8 函数式风格）
Supplier<String> decorated = Retry.decorateSupplier(retry,
    CircuitBreaker.decorateSupplier(circuitBreaker,
        RateLimiter.decorateSupplier(rateLimiter,
            () -> remoteService.call())));

// 或使用 vavr Try 实现降级
String result = Try.ofSupplier(decorated)
    .recover(CallNotPermittedException.class, e -> "熔断降级结果")
    .recover(RequestNotPermitted.class, e -> "限流降级结果")
    .recover(Exception.class, e -> "通用降级结果")
    .get();
```

---

## Spring Boot 集成方式

### application.yml 配置

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        slidingWindowType: COUNT_BASED       # COUNT_BASED 或 TIME_BASED
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
  
  retry:
    instances:
      paymentService:
        maxAttempts: 3
        waitDuration: 200ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
  
  ratelimiter:
    instances:
      paymentService:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 500ms
  
  bulkhead:
    instances:
      paymentService:
        maxConcurrentCalls: 20
        maxWaitDuration: 100ms
```

### Service 层注解使用

```java
@Service
public class PaymentService {

    // 叠加多个注解（执行顺序：Retry(CircuitBreaker(RateLimiter(method)))）
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
    @RateLimiter(name = "paymentService")
    public PaymentResult processPayment(Order order) {
        return paymentGateway.charge(order.getAmount(), order.getCardInfo());
    }

    /**
     * 降级方法：参数列表必须与原方法完全一致，最后加 Exception 参数
     */
    public PaymentResult paymentFallback(Order order, Exception ex) {
        log.error("支付服务熔断: {}", ex.getMessage());
        return PaymentResult.pending("支付排队中，稍后通知结果");
    }

    // CallNotPermittedException 专用降级（熔断器打开时）
    public PaymentResult paymentFallback(Order order, CallNotPermittedException ex) {
        return PaymentResult.fail("支付服务暂时不可用，请稍后重试");
    }
}
```

### Actuator 监控端点

```
GET /actuator/health
→ 包含各 CircuitBreaker 的状态（CLOSED/OPEN/HALF_OPEN）

GET /actuator/metrics/resilience4j.circuitbreaker.calls?tag=name:paymentService
→ 调用统计

GET /actuator/resilience4jcircuitbreakers
→ 所有熔断器概览（需引入 resilience4j-spring-boot2）
```

---

## 注意事项（Bug 风险、性能问题、使用限制）

### ⚠️ 常见坑

**1. CircuitBreaker 统计窗口不够导致熔断不触发**
```
// 错误：窗口太小，几次调用就满了，统计不准
slidingWindowSize(2)  // ❌ 生产环境最少设 10-20

// 正确：根据 QPS 和时间窗口合理设置
slidingWindowSize(20)  // ✓
```

**2. Retry 用于非幂等操作**
```java
// ❌ 危险：支付操作不幂等，重试可能造成重复扣款
@Retry(name = "paymentService")
public void chargeCard(Order order) { ... }

// ✓ 正确：先判断支付状态，或使用幂等 Token 保证安全
@Retry(name = "paymentService")
public PaymentStatus chargeCardIdempotent(String idempotentKey, Order order) { ... }
```

**3. fallbackMethod 签名不匹配导致 NoSuchMethodException**
```java
// ❌ 错误：缺少 Exception 参数
public String fallback(String orderId) { ... }

// ✓ 正确：最后一个参数必须是 Exception 或其子类
public String fallback(String orderId, Exception ex) { ... }
// ✓ 也可以针对特定异常
public String fallback(String orderId, CallNotPermittedException ex) { ... }
```

**4. 版本兼容：Java 8 用 1.x，Java 11+ 推荐 2.x**
```xml
<!-- Java 8 环境 -->
<version>1.7.1</version>

<!-- Java 11+ 环境（更多功能，更好性能） -->
<version>2.2.0</version>
```

**5. 注解失效：同类内部调用不走代理**
```java
// ❌ 失效：内部调用绕过 AOP 代理
public void outerMethod() {
    this.annotatedMethod();  // 注解不生效！
}

// ✓ 正确：注入自身代理，或拆分到不同 Service
@Autowired
private PaymentService self;
self.annotatedMethod();
```

### 📊 性能注意事项

| 模块 | 性能影响 | 建议 |
|------|---------|------|
| CircuitBreaker | 极低（CAS 操作） | 所有远程调用都加 |
| RateLimiter | 极低 | 对外暴露的接口都加 |
| Retry | 低（仅在失败时延迟） | 注意非幂等操作 |
| Bulkhead（信号量） | 极低 | 保护共享资源 |
| ThreadPoolBulkhead | 中（线程切换） | 需要完全隔离时才用 |

### 🔒 生产配置建议

```yaml
resilience4j:
  circuitbreaker:
    instances:
      default:
        slidingWindowSize: 20          # 足够大的统计窗口
        failureRateThreshold: 50       # 50% 失败率触发熔断
        slowCallRateThreshold: 50      # 50% 慢调用也触发
        slowCallDurationThreshold: 2s  # 2s 以上算慢调用
        waitDurationInOpenState: 30s   # 熔断后等待 30s 再探测
        registerHealthIndicator: true  # 接入 Actuator 监控
```

---

## Demo 运行方法

```bash
# 进入项目目录
cd d:/ai/workbuddy/java-tools-learning/resilience4j-demo

# 编译
mvn clean package -DskipTests -DJAVA_HOME=D:/jdk/jdk17

# 运行各演示类
# 注意：需要指定 JAVA_HOME 指向 JDK（而非 JRE）
set JAVA_HOME=D:/jdk/jdk17

# 基础演示（CircuitBreaker + RateLimiter + Retry）
mvn exec:java -Dexec.mainClass="com.example.resilience4j.Resilience4jBasicDemo"

# 进阶演示（Bulkhead + Decorator 链 + Fallback + 慢调用检测）
mvn exec:java -Dexec.mainClass="com.example.resilience4j.Resilience4jAdvancedDemo"

# 实战场景（电商下单 + 短信防刷 + 微服务链 + 雪崩防护）
mvn exec:java -Dexec.mainClass="com.example.resilience4j.Resilience4jPracticalDemo"
```

---

## 扩展阅读

- [Resilience4j 官方文档](https://resilience4j.readme.io/docs)
- [Spring Cloud Circuit Breaker 文档](https://spring.io/projects/spring-cloud-circuitbreaker)
- [微服务弹性模式（微软架构中心）](https://learn.microsoft.com/zh-cn/azure/architecture/patterns/circuit-breaker)
- [Hystrix 迁移到 Resilience4j 指南](https://resilience4j.readme.io/docs/getting-started-3)
