# Day 23 - Vert.x Core：事件驱动 / 响应式 JVM 框架

## 工具简介

| 属性 | 详情 |
|------|------|
| **名称** | Eclipse Vert.x |
| **GitHub** | https://github.com/eclipse-vertx/vert.x |
| **官方文档** | https://vertx.io/docs/ |
| **Star 数** | 14k+ |
| **版本（本 Demo）** | 3.9.16（Java 8 兼容；最新 4.x 需 Java 11+） |
| **许可证** | Apache 2.0 |
| **定位** | 多语言、事件驱动、非阻塞 I/O 工具包；类比 Node.js，但跑在 JVM 上 |

Vert.x 的核心理念：**"不要阻塞 Event Loop"**。所有 I/O 操作（HTTP、TCP、数据库、文件）均为异步非阻塞，用少量线程撑起极高并发。相比 Spring MVC（每请求一线程），Vert.x 在 I/O 密集型场景下吞吐量可高 3～10 倍。

### 核心概念速览

```
Vertx 实例
├── EventLoop（事件循环线程，CPU 核心数 × 2）
│   └── Verticle（部署单元，类比 Node.js 的模块）
├── Worker Thread Pool（执行阻塞任务）
├── EventBus（事件总线，进程内或跨集群通信）
│   ├── send()    → 点对点，支持 reply
│   └── publish() → 广播，所有订阅者接收
└── HttpServer / TcpServer / NetClient ...
```

---

## Maven 依赖配置

```xml
<!-- Vert.x Core（必须）-->
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-core</artifactId>
    <version>3.9.16</version>
</dependency>

<!-- Vert.x Web（HTTP Router，可选）-->
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-web</artifactId>
    <version>3.9.16</version>
</dependency>

<!-- 若使用 Java 11+，推荐切换到 4.x -->
<!-- <version>4.5.10</version> -->
```

---

## 核心 API 速查

### 1. 创建 Vertx 实例

```java
// 默认配置
Vertx vertx = Vertx.vertx();

// 自定义线程数
VertxOptions opts = new VertxOptions()
    .setEventLoopPoolSize(8)          // Event Loop 线程数（默认 CPU×2）
    .setWorkerPoolSize(20)            // Worker 线程池大小
    .setBlockedThreadCheckInterval(5000); // 阻塞检测间隔（ms）
Vertx vertx = Vertx.vertx(opts);
```

### 2. 部署 Verticle

```java
// 普通 Verticle（Event Loop 线程）
vertx.deployVerticle(new MyVerticle(), res -> {
    if (res.succeeded()) {
        System.out.println("部署 ID: " + res.result());
    }
});

// Worker Verticle（允许阻塞）
DeploymentOptions opts = new DeploymentOptions()
    .setWorker(true)
    .setInstances(4);       // 部署 4 个实例
vertx.deployVerticle("com.example.MyWorker", opts);
```

### 3. EventBus 通信

```java
EventBus eb = vertx.eventBus();

// 点对点发送（只有一个消费者收到）
eb.send("address", message);

// 请求-回复模式
eb.<JsonObject>request("order.service", payload, reply -> {
    if (reply.succeeded()) {
        JsonObject result = reply.result().body();
    }
});

// 广播（所有订阅者都收到）
eb.publish("log.topic", "系统启动");

// 注册消费者
eb.<JsonObject>consumer("order.service", msg -> {
    // 处理消息
    msg.reply(new JsonObject().put("status", "OK"));
});

// 消息头 + 超时
DeliveryOptions opts = new DeliveryOptions()
    .setSendTimeout(3000)
    .addHeader("X-Trace-Id", "abc123");
eb.request("address", msg, opts, reply -> { ... });
```

### 4. Future / Promise（异步链式）

```java
// 创建 Promise，包装回调 API
Promise<String> promise = Promise.promise();
someAsyncOp(result -> {
    if (result.succeeded()) promise.complete(result.value());
    else promise.fail(result.cause());
});
Future<String> future = promise.future();

// 链式操作
future
    .compose(s -> anotherAsync(s))     // 串行下一步
    .map(s -> s.toUpperCase())         // 同步转换
    .onSuccess(s -> System.out.println("最终结果: " + s))
    .onFailure(err -> err.printStackTrace());

// 并行等待所有完成
CompositeFuture.all(f1, f2, f3).onComplete(ar -> {
    if (ar.succeeded()) {
        String r1 = ar.result().resultAt(0);
        String r2 = ar.result().resultAt(1);
    }
});
```

### 5. HTTP Server（vertx-web）

```java
Vertx vertx = Vertx.vertx();
Router router = Router.router(vertx);
router.route().handler(BodyHandler.create());   // 解析请求体

router.get("/api/users/:id").handler(ctx -> {
    String id = ctx.pathParam("id");
    ctx.response()
       .putHeader("Content-Type", "application/json")
       .end(new JsonObject().put("id", id).encode());
});

router.post("/api/users").handler(ctx -> {
    JsonObject body = ctx.getBodyAsJson();
    ctx.response().setStatusCode(201).end(body.encode());
});

vertx.createHttpServer()
     .requestHandler(router)
     .listen(8080, result -> {
         if (result.succeeded()) System.out.println("Server started");
     });
```

### 6. 定时器

```java
// 一次性定时器（延迟 1 秒）
vertx.setTimer(1000, id -> System.out.println("Timer fired: " + id));

// 周期性定时器（每 500ms）
long timerId = vertx.setPeriodic(500, id -> System.out.println("Periodic " + id));

// 取消定时器
vertx.cancelTimer(timerId);
```

### 7. executeBlocking（在 Worker 线程执行阻塞代码）

```java
// 第三个参数 false = 多个 executeBlocking 可并行执行
vertx.<String>executeBlocking(promise -> {
    String result = someBlockingOperation(); // 允许阻塞
    promise.complete(result);
}, false, asyncResult -> {
    // 此回调在 Event Loop 线程执行
    System.out.println("结果: " + asyncResult.result());
});
```

---

## Spring Boot 集成方式

### 方案一：作为工具库注入（推荐，最简单）

```xml
<!-- pom.xml：只引入 core 和 web，不需要 starter -->
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-core</artifactId>
    <version>3.9.16</version>
</dependency>
```

```java
@Configuration
public class VertxConfig {
    @Bean
    public Vertx vertx() {
        return Vertx.vertx(new VertxOptions()
            .setEventLoopPoolSize(4)
            .setWorkerPoolSize(20));
    }

    @Bean
    public EventBus eventBus(Vertx vertx) {
        return vertx.eventBus();
    }
}

// 在任意 Spring Bean 中注入使用
@Service
public class NotificationService {
    @Autowired
    private EventBus eventBus;

    public void notifyAsync(String userId, String msg) {
        // 异步广播通知，不阻塞 HTTP 线程
        eventBus.publish("notification." + userId,
            new JsonObject().put("msg", msg).put("ts", System.currentTimeMillis()));
    }
}
```

### 方案二：将 Verticle 作为 Spring Bean 管理

```java
// 让 Verticle 具备依赖注入能力
@Component
public class OrderVerticle extends AbstractVerticle {
    @Autowired
    private OrderRepository orderRepository;  // 注入 Spring Bean

    @Override
    public void start() {
        vertx.eventBus().<JsonObject>consumer("order.create", msg -> {
            // 注意：orderRepository.save() 若是阻塞操作，必须用 executeBlocking 包装！
            vertx.executeBlocking(p -> {
                Order order = orderRepository.save(from(msg.body()));
                p.complete(order);
            }, false, res -> msg.reply(toJson(res.result())));
        });
    }
}

// 启动时部署
@Bean
public void deployOrderVerticle(Vertx vertx, OrderVerticle verticle) {
    vertx.deployVerticle(verticle);
}
```

---

## 注意事项

### ⚠️ Bug 风险 / 常见陷阱

| 风险 | 说明 | 解决方案 |
|------|------|----------|
| **阻塞 Event Loop** | 在 Event Loop 线程调用 `Thread.sleep()`、JDBC、`HttpClient.execute()` 等阻塞操作，会触发 BlockedThreadChecker 告警并严重降低吞吐量 | 用 `executeBlocking()` 或 `Worker Verticle` 包装所有阻塞操作 |
| **ThreadLocal 失效** | Event Loop 是线程复用的，不同请求可能共享线程；Spring Security 的 `SecurityContextHolder`（基于 ThreadLocal）会错乱 | 用 Vert.x Context 的 `putLocal()`/`getLocal()` 代替 ThreadLocal |
| **Spring @Transactional 问题** | Spring 事务依赖 ThreadLocal 绑定 Connection，而 Vert.x 多线程调度会导致事务上下文丢失 | 将数据库操作全部放在 `executeBlocking()` 中，或使用 Vert.x 的 reactive JDBC（Vert.x 4.x 的 SqlClient） |
| **CompositeFuture 泛型警告** | Vert.x 3.x 的 `CompositeFuture.all()` 方法是原始类型，IDE 会报 unchecked warning | 可加 `@SuppressWarnings("unchecked")`，或升级到 Vert.x 4.x（改用 `Future.all()`） |
| **版本选择** | Vert.x 3.x 最后版本是 3.9.16（维护模式），Vert.x 4.x 是主力版本但需 Java 11+ | Java 8 项目用 3.9.x；新项目建议直接 4.5.x |

### ⚡ 性能注意事项

- **不要创建多个 `Vertx` 实例**：一个应用一个实例即可，多个实例会浪费线程资源
- **EventBus 消息序列化**：跨进程通信时消息会被序列化（Hazelcast/Zookeeper 集群），同进程内是内存引用传递，无序列化开销
- **Verticle 实例数**：CPU 密集型任务 → 实例数 = CPU 核心数；I/O 密集型 → 可适当多部署几个实例
- **JSON 对象复用**：`JsonObject.copy()` 深拷贝，跨 Verticle 传递时注意修改不影响原对象

### 🚫 使用限制

- Vert.x 3.x **不支持** Java 模块系统（JPMS）
- Vert.x EventBus 不是持久化消息队列（消息不落盘），服务重启消息会丢失；持久化需集成 Kafka/RabbitMQ
- `vertx-web` 的 `BodyHandler` 需要放在所有需要解析请求体的路由之前，否则 `getBodyAsJson()` 返回 `null`

---

## 演示类说明

| 文件 | 覆盖功能 |
|------|----------|
| `VertxBasicDemo.java` | Verticle 部署/卸载、EventBus send/reply、publish/subscribe、Worker Verticle、定时器 |
| `VertxAdvancedDemo.java` | Future/Promise 链式、DeliveryOptions（消息头+超时）、CompositeFuture 并行、setPeriodic+cancelTimer、executeBlocking |
| `VertxPracticalDemo.java` | vertx-web RESTful HTTP Server（CRUD）、EventBus 分层解耦架构、Metrics 广播监控、Spring Boot 集成方案注释 |

---

## 运行方法

```bash
# 进入项目目录
cd java-tools-learning/vertx-demo

# 编译打包
set JAVA_HOME=D:/jdk/jdk17
mvn clean package -DskipTests

# 运行基础演示
java -cp target/vertx-demo-1.0-SNAPSHOT.jar com.example.vertx.VertxBasicDemo

# 运行进阶演示
java -cp target/vertx-demo-1.0-SNAPSHOT.jar com.example.vertx.VertxAdvancedDemo

# 运行实战演示（会启动 HTTP Server，端口 8090）
java -cp target/vertx-demo-1.0-SNAPSHOT.jar com.example.vertx.VertxPracticalDemo

# 另开终端测试 HTTP Server
curl http://localhost:8090/api/health
curl http://localhost:8090/api/orders
curl -X POST http://localhost:8090/api/orders \
     -H "Content-Type:application/json" \
     -d '{"product":"Vert.x实战","qty":2,"price":89.9}'
curl -X DELETE http://localhost:8090/api/orders/1001
```

---

## 对比：Vert.x vs Spring WebFlux vs Netty

| 维度 | Vert.x | Spring WebFlux | Netty |
|------|--------|---------------|-------|
| **编程模型** | 回调 / Future / RxJava | Reactor（Mono/Flux） | 纯回调 |
| **上手难度** | 中 | 中（需理解 Reactive Streams） | 高 |
| **与 Spring 集成** | 可集成（非 native） | Spring 生态一等公民 | 低层，需手动整合 |
| **多语言支持** | Java/Kotlin/Groovy/JS/Ruby | 仅 JVM | 仅 JVM |
| **EventBus** | 内置（跨进程集群） | 无 | 无 |
| **适用场景** | 微服务网关、实时推送、IoT | Spring 生态 API 服务 | 网络框架底层 |
