package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day 23 - Vert.x Core 基础演示
 *
 * <p>本类演示 Vert.x 三个核心概念：
 * <ol>
 *   <li>Verticle：Vert.x 最小部署单元，每个 Verticle 跑在自己的线程上（Event Loop 或 Worker）</li>
 *   <li>EventBus：Verticle 之间、集群节点之间异步通信的"神经系统"，支持 Publish-Subscribe 和 Request-Reply</li>
 *   <li>Timer：基于事件循环的非阻塞定时器</li>
 * </ol>
 *
 * <p>运行方式（编译后）：
 * <pre>
 *   mvn clean package -DskipTests
 *   java -cp target/vertx-demo-1.0-SNAPSHOT.jar com.example.vertx.VertxBasicDemo
 * </pre>
 */
public class VertxBasicDemo {

    // ─────────────────────────────────────────────
    // 1. Hello Verticle
    // ─────────────────────────────────────────────

    /**
     * 最简 Verticle：重写 start() 方法即可，Vert.x 会在 Event Loop 线程上调用它。
     * 注意：start() 是非阻塞的，不要在里面做任何 I/O 阻塞操作。
     */
    static class HelloVerticle extends AbstractVerticle {
        @Override
        public void start(Promise<Void> startPromise) {
            System.out.println("[HelloVerticle] 已部署，运行线程: " + Thread.currentThread().getName());

            // 设置一个定时器，1 秒后打印消息
            vertx.setTimer(1000, id -> {
                System.out.println("[HelloVerticle] Timer 触发！timer id = " + id);
                startPromise.complete(); // 告知 Vert.x 启动完成
            });
        }

        @Override
        public void stop() {
            System.out.println("[HelloVerticle] 已卸载");
        }
    }

    // ─────────────────────────────────────────────
    // 2. EventBus 点对点（Point-to-Point）：send / reply
    // ─────────────────────────────────────────────

    /**
     * 消息服务 Verticle：订阅地址 "order.service"，收到订单后回复处理结果。
     * <p>send() = 点对点投递，只有一个消费者收到消息，并可以 reply。
     */
    static class OrderServiceVerticle extends AbstractVerticle {
        private static final String ADDRESS = "order.service";
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void start() {
            EventBus eb = vertx.eventBus();
            // 注册消费者
            eb.<JsonObject>consumer(ADDRESS, (Message<JsonObject> msg) -> {
                JsonObject order = msg.body();
                int orderId = counter.incrementAndGet();
                System.out.println("[OrderService] 收到订单: " + order.encodePrettily());

                // 模拟处理，直接回复
                JsonObject reply = new JsonObject()
                        .put("orderId", orderId)
                        .put("status", "ACCEPTED")
                        .put("message", "订单 #" + orderId + " 已受理");
                msg.reply(reply);
            });
            System.out.println("[OrderService] 已监听地址: " + ADDRESS);
        }
    }

    // ─────────────────────────────────────────────
    // 3. EventBus 发布-订阅（Publish-Subscribe）：publish
    // ─────────────────────────────────────────────

    /**
     * 日志订阅者 Verticle：订阅广播地址 "log.topic"，接收系统日志事件。
     * <p>publish() = 广播，所有订阅者都会收到消息，不能 reply。
     */
    static class LogSubscriberVerticle extends AbstractVerticle {
        private static final String TOPIC = "log.topic";
        private final String name;

        LogSubscriberVerticle(String name) {
            this.name = name;
        }

        @Override
        public void start() {
            vertx.eventBus().<String>consumer(TOPIC, msg -> {
                System.out.println("[" + name + "] 收到日志事件: " + msg.body()
                        + "  (headers=" + msg.headers() + ")");
            });
            System.out.println("[" + name + "] 已订阅: " + TOPIC);
        }
    }

    // ─────────────────────────────────────────────
    // 4. Worker Verticle：执行阻塞任务
    // ─────────────────────────────────────────────

    /**
     * Worker Verticle 运行在 Worker Thread Pool，专门用于执行阻塞操作（数据库查询、文件 I/O 等）。
     * 普通 Verticle 的 Event Loop 不允许阻塞超过 2 秒（默认会触发 BlockedThreadChecker 告警）。
     */
    static class BlockingWorkerVerticle extends AbstractVerticle {
        @Override
        public void start() throws Exception {
            System.out.println("[BlockingWorker] 运行在 Worker 线程: " + Thread.currentThread().getName());
            // Worker Verticle 中允许调用 Thread.sleep 等阻塞操作
            Thread.sleep(200);
            System.out.println("[BlockingWorker] 阻塞操作完成，Worker 线程: " + Thread.currentThread().getName());
        }
    }

    // ─────────────────────────────────────────────
    // main：组合演示
    // ─────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Vert.x 基础演示 ===\n");

        // --- 演示 1：Hello Verticle + Timer ---
        System.out.println("--- 1. Hello Verticle & Timer ---");
        Vertx v1 = Vertx.vertx();
        CountDownLatch latch1 = new CountDownLatch(1);
        v1.deployVerticle(new HelloVerticle(), res -> {
            if (res.succeeded()) {
                System.out.println("[main] HelloVerticle 部署 ID: " + res.result());
            }
            latch1.countDown();
        });
        latch1.await(3, TimeUnit.SECONDS);
        v1.close();

        Thread.sleep(300);
        // --- 演示 2：EventBus send / reply ---
        System.out.println("\n--- 2. EventBus 点对点（send / reply）---");
        Vertx v2 = Vertx.vertx();
        CountDownLatch latch2 = new CountDownLatch(1);

        // 先部署服务 Verticle
        v2.deployVerticle(new OrderServiceVerticle(), ar -> {
            EventBus eb = v2.eventBus();

            JsonObject order = new JsonObject()
                    .put("product", "Java实战书籍")
                    .put("qty", 3)
                    .put("price", 99.9);

            // 发送请求并等待回复（Request-Reply 模式）
            eb.request("order.service", order, reply -> {
                if (reply.succeeded()) {
                    System.out.println("[main] 收到服务回复: " + reply.result().body());
                } else {
                    System.out.println("[main] 请求失败: " + reply.cause().getMessage());
                }
                latch2.countDown();
            });
        });
        latch2.await(5, TimeUnit.SECONDS);
        v2.close();

        Thread.sleep(300);
        // --- 演示 3：EventBus publish / subscribe ---
        System.out.println("\n--- 3. EventBus 发布-订阅（publish）---");
        Vertx v3 = Vertx.vertx();
        CountDownLatch latch3 = new CountDownLatch(1);

        // 部署两个日志订阅者
        v3.deployVerticle(new LogSubscriberVerticle("日志订阅者-A"));
        v3.deployVerticle(new LogSubscriberVerticle("日志订阅者-B"));

        // 等待订阅者就绪后广播消息
        v3.setTimer(500, id -> {
            v3.eventBus().publish("log.topic", "系统启动成功，时间: " + System.currentTimeMillis());
            v3.setTimer(300, id2 -> latch3.countDown());
        });
        latch3.await(5, TimeUnit.SECONDS);
        v3.close();

        Thread.sleep(300);
        // --- 演示 4：Worker Verticle ---
        System.out.println("\n--- 4. Worker Verticle（阻塞任务专用线程池）---");
        Vertx v4 = Vertx.vertx();
        CountDownLatch latch4 = new CountDownLatch(1);

        // DeploymentOptions.setWorker(true) 标记为 Worker Verticle
        DeploymentOptions workerOpts = new DeploymentOptions().setWorker(true);
        v4.deployVerticle(new BlockingWorkerVerticle(), workerOpts, res -> {
            System.out.println("[main] Worker Verticle 部署完成: " + res.result());
            latch4.countDown();
        });
        latch4.await(5, TimeUnit.SECONDS);
        v4.close();

        System.out.println("\n=== 基础演示全部完成 ===");
    }
}
