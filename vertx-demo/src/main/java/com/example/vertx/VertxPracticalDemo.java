package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Day 23 - Vert.x Core 实战演示
 *
 * <p>本类演示三个实战场景：
 * <ol>
 *   <li>RESTful HTTP Server（vertx-web Router）：增删改查订单 API</li>
 *   <li>微服务事件总线通信：前端 Verticle → EventBus → 后端 Service Verticle，松耦合架构</li>
 *   <li>Vert.x 与 Spring Boot 集成方案（代码注释详解，不依赖 Spring 运行）</li>
 * </ol>
 *
 * <p>演示程序会启动一个 HTTP Server（端口 8090），可用 curl 测试：
 * <pre>
 *   curl http://localhost:8090/api/orders
 *   curl -X POST http://localhost:8090/api/orders \
 *        -H "Content-Type:application/json" \
 *        -d '{"product":"Java书籍","qty":2,"price":89.9}'
 * </pre>
 */
public class VertxPracticalDemo {

    // ─────────────────────────────────────────────
    // 1. 订单数据层（内存存储，模拟 DAO）
    // ─────────────────────────────────────────────

    static final Map<Integer, JsonObject> ORDER_STORE = new ConcurrentHashMap<>();
    static final AtomicInteger SEQ = new AtomicInteger(1000);

    // ─────────────────────────────────────────────
    // 2. 订单 Service Verticle（EventBus 后端）
    // ─────────────────────────────────────────────

    /**
     * 订单服务 Verticle：通过 EventBus 暴露 CRUD 接口，与 HTTP 层解耦。
     * <ul>
     *   <li>order.list   → 返回所有订单列表</li>
     *   <li>order.get    → 按 ID 查询订单</li>
     *   <li>order.create → 创建订单</li>
     *   <li>order.delete → 删除订单</li>
     * </ul>
     */
    static class OrderServiceVerticle extends AbstractVerticle {
        @Override
        public void start() {
            EventBus eb = vertx.eventBus();

            // 查询所有订单
            eb.<String>consumer("order.list", msg -> {
                JsonArray arr = new JsonArray();
                ORDER_STORE.values().forEach(arr::add);
                msg.reply(arr);
            });

            // 按 ID 查询
            eb.<Integer>consumer("order.get", msg -> {
                Integer id = msg.body();
                JsonObject order = ORDER_STORE.get(id);
                if (order != null) {
                    msg.reply(order);
                } else {
                    msg.fail(404, "订单不存在: " + id);
                }
            });

            // 创建订单
            eb.<JsonObject>consumer("order.create", msg -> {
                int id = SEQ.incrementAndGet();
                JsonObject order = msg.body()
                        .put("id", id)
                        .put("createdAt", System.currentTimeMillis())
                        .put("status", "PENDING");
                ORDER_STORE.put(id, order);
                System.out.println("[OrderService] 创建订单: " + order);
                msg.reply(order);
            });

            // 删除订单
            eb.<Integer>consumer("order.delete", msg -> {
                Integer id = msg.body();
                JsonObject removed = ORDER_STORE.remove(id);
                if (removed != null) {
                    msg.reply(new JsonObject().put("deleted", id));
                } else {
                    msg.fail(404, "订单不存在: " + id);
                }
            });

            System.out.println("[OrderService] 订单服务已就绪");
        }
    }

    // ─────────────────────────────────────────────
    // 3. HTTP Server Verticle（vertx-web）
    // ─────────────────────────────────────────────

    /**
     * HTTP 层 Verticle：接收 HTTP 请求，通过 EventBus 委托给 OrderServiceVerticle 处理。
     * 这样 HTTP 层和业务层完全解耦，可以独立水平扩展。
     */
    static class HttpServerVerticle extends AbstractVerticle {
        private static final int PORT = 8090;

        @Override
        public void start(Promise<Void> startPromise) {
            // 创建路由器
            Router router = Router.router(vertx);
            // 允许读取请求体
            router.route().handler(BodyHandler.create());

            // GET /api/orders - 查询所有订单
            router.get("/api/orders").handler(this::listOrders);
            // GET /api/orders/:id - 查询单个订单
            router.get("/api/orders/:id").handler(this::getOrder);
            // POST /api/orders - 创建订单
            router.post("/api/orders").handler(this::createOrder);
            // DELETE /api/orders/:id - 删除订单
            router.delete("/api/orders/:id").handler(this::deleteOrder);
            // GET /api/health - 健康检查
            router.get("/api/health").handler(ctx ->
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("status", "UP").put("orders", ORDER_STORE.size()).encode())
            );

            // 启动 HTTP Server
            HttpServer server = vertx.createHttpServer();
            server.requestHandler(router).listen(PORT, result -> {
                if (result.succeeded()) {
                    System.out.println("[HttpServer] 已启动，监听端口: " + PORT);
                    System.out.println("[HttpServer] 测试地址: http://localhost:" + PORT + "/api/orders");
                    startPromise.complete();
                } else {
                    startPromise.fail(result.cause());
                }
            });
        }

        private void listOrders(RoutingContext ctx) {
            vertx.eventBus().<JsonArray>request("order.list", "", reply -> {
                if (reply.succeeded()) {
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(reply.result().body().encode());
                } else {
                    ctx.response().setStatusCode(500).end("服务异常");
                }
            });
        }

        private void getOrder(RoutingContext ctx) {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                vertx.eventBus().<JsonObject>request("order.get", id, reply -> {
                    if (reply.succeeded()) {
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(reply.result().body().encode());
                    } else {
                        ctx.response().setStatusCode(404)
                                .end(new JsonObject().put("error", reply.cause().getMessage()).encode());
                    }
                });
            } catch (NumberFormatException e) {
                ctx.response().setStatusCode(400).end("无效的订单 ID");
            }
        }

        private void createOrder(RoutingContext ctx) {
            JsonObject body = ctx.getBodyAsJson();
            if (body == null) {
                ctx.response().setStatusCode(400).end("请求体不能为空");
                return;
            }
            vertx.eventBus().<JsonObject>request("order.create", body, reply -> {
                if (reply.succeeded()) {
                    ctx.response()
                            .setStatusCode(201)
                            .putHeader("Content-Type", "application/json")
                            .end(reply.result().body().encode());
                } else {
                    ctx.response().setStatusCode(500).end("创建失败");
                }
            });
        }

        private void deleteOrder(RoutingContext ctx) {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                vertx.eventBus().<JsonObject>request("order.delete", id, reply -> {
                    if (reply.succeeded()) {
                        ctx.response().setStatusCode(204).end();
                    } else {
                        ctx.response().setStatusCode(404)
                                .end(new JsonObject().put("error", reply.cause().getMessage()).encode());
                    }
                });
            } catch (NumberFormatException e) {
                ctx.response().setStatusCode(400).end("无效的订单 ID");
            }
        }
    }

    // ─────────────────────────────────────────────
    // 4. 指标监控 Verticle（演示定期推送）
    // ─────────────────────────────────────────────

    /**
     * 监控 Verticle：每 2 秒统计并广播当前订单数量，模拟指标上报场景。
     */
    static class MetricsVerticle extends AbstractVerticle {
        private static final AtomicLong tick = new AtomicLong(0);

        @Override
        public void start() {
            vertx.setPeriodic(2000, id -> {
                long n = tick.incrementAndGet();
                JsonObject metrics = new JsonObject()
                        .put("tick", n)
                        .put("orderCount", ORDER_STORE.size())
                        .put("timestamp", System.currentTimeMillis());
                vertx.eventBus().publish("metrics.snapshot", metrics);
                System.out.println("[Metrics] 已广播指标快照 #" + n + ": " + metrics);
            });
            // 订阅自己广播的指标（演示同进程消费）
            vertx.eventBus().<JsonObject>consumer("metrics.snapshot", msg ->
                    System.out.println("[MetricsConsumer] 收到快照: orderCount=" + msg.body().getInteger("orderCount")));
        }
    }

    // ─────────────────────────────────────────────
    // main
    // ─────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Vert.x 实战演示 ===\n");

        Vertx vertx = Vertx.vertx();
        CountDownLatch deployLatch = new CountDownLatch(1);

        // 先部署 OrderService，再部署 HttpServer
        vertx.deployVerticle(new OrderServiceVerticle(), ar1 -> {
            vertx.deployVerticle(new HttpServerVerticle(), ar2 -> {
                if (ar2.succeeded()) {
                    deployLatch.countDown();
                } else {
                    ar2.cause().printStackTrace();
                    deployLatch.countDown();
                }
            });
        });
        deployLatch.await(10, TimeUnit.SECONDS);

        // 预置几条订单
        System.out.println("\n--- 预置测试数据 ---");
        CountDownLatch seedLatch = new CountDownLatch(3);
        String[] products = {"Java实战", "Spring Boot教程", "Vert.x权威指南"};
        for (String p : products) {
            vertx.eventBus().<JsonObject>request("order.create",
                    new JsonObject().put("product", p).put("qty", 1).put("price", 79.9), r -> seedLatch.countDown());
        }
        seedLatch.await(5, TimeUnit.SECONDS);

        // 查询所有订单
        System.out.println("\n--- 查询所有订单 ---");
        CountDownLatch queryLatch = new CountDownLatch(1);
        vertx.eventBus().<JsonArray>request("order.list", "", r -> {
            System.out.println("当前订单列表: " + r.result().body().encodePrettily());
            queryLatch.countDown();
        });
        queryLatch.await(5, TimeUnit.SECONDS);

        // 启动 Metrics 监控（演示 2 秒，展示 1 次广播）
        System.out.println("\n--- 启动指标监控（演示 2 秒）---");
        vertx.deployVerticle(new MetricsVerticle());
        Thread.sleep(2500);

        System.out.println("\n=== HTTP Server 运行中 ===");
        System.out.println("可用以下命令测试（另开终端）：");
        System.out.println("  curl http://localhost:8090/api/orders");
        System.out.println("  curl http://localhost:8090/api/health");
        System.out.println("  curl -X POST http://localhost:8090/api/orders \\");
        System.out.println("       -H 'Content-Type:application/json' \\");
        System.out.println("       -d '{\"product\":\"新书\",\"qty\":1,\"price\":59.9}'");
        System.out.println("\n按 Ctrl+C 退出...");

        // 保持运行 10 秒（演示结束后自动退出，正式部署应去掉这段）
        Thread.sleep(10000);
        vertx.close();
        System.out.println("\n=== 实战演示结束 ===");
    }

    // ─────────────────────────────────────────────
    // 5. Spring Boot 集成方案（说明注释）
    // ─────────────────────────────────────────────
    /*
     * ╔══════════════════════════════════════════════════════════════════════╗
     * ║          Vert.x + Spring Boot 集成方案                               ║
     * ╠══════════════════════════════════════════════════════════════════════╣
     * ║                                                                      ║
     * ║  方案一：直接在 Spring Bean 中使用 Vert.x（最简单）                    ║
     * ║  ─────────────────────────────────────────────────────────────────  ║
     * ║  @Configuration                                                      ║
     * ║  public class VertxConfig {                                          ║
     * ║      @Bean                                                           ║
     * ║      public Vertx vertx() {                                          ║
     * ║          VertxOptions options = new VertxOptions()                   ║
     * ║              .setEventLoopPoolSize(2 * Runtime.getRuntime()          ║
     * ║                  .availableProcessors());                            ║
     * ║          return Vertx.vertx(options);                                ║
     * ║      }                                                               ║
     * ║      @Bean                                                           ║
     * ║      public EventBus eventBus(Vertx vertx) {                         ║
     * ║          return vertx.eventBus();                                    ║
     * ║      }                                                               ║
     * ║  }                                                                   ║
     * ║                                                                      ║
     * ║  // 在 Service 中注入 EventBus 发送消息                               ║
     * ║  @Service                                                            ║
     * ║  public class OrderService {                                         ║
     * ║      @Autowired private EventBus eventBus;                           ║
     * ║      public void createOrderAsync(JsonObject order) {                ║
     * ║          eventBus.publish("order.created", order);                   ║
     * ║      }                                                               ║
     * ║  }                                                                   ║
     * ║                                                                      ║
     * ║  方案二：VertxSpring（社区库，自动注入 Verticle）                      ║
     * ║  ─────────────────────────────────────────────────────────────────  ║
     * ║  <dependency>                                                        ║
     * ║      <groupId>io.vertx</groupId>                                     ║
     * ║      <artifactId>vertx-spring-boot-starter-http</artifactId>        ║
     * ║      <version>1.0.0.Beta3</version>                                  ║
     * ║  </dependency>                                                       ║
     * ║  // @SpringBootApplication 配合 @VertxWebFlux 使用                   ║
     * ║                                                                      ║
     * ║  注意事项：                                                            ║
     * ║  1. 不要在 Event Loop 线程上调用 Spring 的 @Transactional 方法          ║
     * ║     （Spring 事务依赖 ThreadLocal，而 Vert.x EventLoop 是多线程调度）     ║
     * ║  2. 阻塞操作必须用 vertx.executeBlocking() 或 Worker Verticle 包装     ║
     * ║  3. Spring Boot 2.x 推荐使用 Vert.x 3.x（Java 8 兼容）                ║
     * ║     Spring Boot 3.x 推荐使用 Vert.x 4.x（Java 11+）                  ║
     * ║                                                                      ║
     * ╚══════════════════════════════════════════════════════════════════════╝
     */
}
