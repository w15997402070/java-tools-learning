package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day 23 - Vert.x Core 进阶演示
 *
 * <p>本类演示 Vert.x 进阶特性：
 * <ol>
 *   <li>Future / Promise 异步链式编程（compose / map / onSuccess / onFailure）</li>
 *   <li>EventBus 消息头（Headers）和超时控制（DeliveryOptions）</li>
 *   <li>CompositeFuture：并行执行多个异步任务，等待全部完成</li>
 *   <li>定期任务（setPeriodic）与取消（cancelTimer）</li>
 *   <li>executeBlocking：在 Worker 线程执行阻塞代码，结果回调到 Event Loop</li>
 * </ol>
 */
public class VertxAdvancedDemo {

    // ─────────────────────────────────────────────
    // 1. Future / Promise 链式编程
    // ─────────────────────────────────────────────

    /**
     * 模拟数据库查询 Verticle（返回 Future）。
     * 使用 Promise 将回调式 API 包装为 Future，便于链式调用。
     */
    static class UserServiceVerticle extends AbstractVerticle {

        @Override
        public void start() {
            vertx.eventBus().<JsonObject>consumer("user.find", this::handleFindUser);
            vertx.eventBus().<String>consumer("user.profile", this::handleProfile);
            System.out.println("[UserService] 已就绪");
        }

        private void handleFindUser(Message<JsonObject> msg) {
            String userId = msg.body().getString("userId");
            // 模拟异步查库
            vertx.setTimer(100, id -> {
                if ("unknown".equals(userId)) {
                    msg.fail(404, "用户不存在: " + userId);
                } else {
                    JsonObject user = new JsonObject()
                            .put("userId", userId)
                            .put("name", "张三_" + userId)
                            .put("email", userId + "@example.com");
                    msg.reply(user);
                }
            });
        }

        private void handleProfile(Message<String> msg) {
            String userId = msg.body();
            vertx.setTimer(80, id -> {
                JsonObject profile = new JsonObject()
                        .put("userId", userId)
                        .put("avatar", "https://cdn.example.com/avatar/" + userId + ".png")
                        .put("followCount", new Random().nextInt(1000));
                msg.reply(profile);
            });
        }
    }

    /**
     * 演示 Future / Promise 组合：先查用户，再查 Profile，最后合并返回。
     */
    static Future<JsonObject> getUserWithProfile(Vertx vertx, String userId) {
        Promise<JsonObject> promise = Promise.promise();

        // Step 1：查用户基本信息
        vertx.eventBus().<JsonObject>request("user.find",
                new JsonObject().put("userId", userId),
                userReply -> {
                    if (userReply.failed()) {
                        promise.fail(userReply.cause());
                        return;
                    }
                    JsonObject user = userReply.result().body();

                    // Step 2：查用户 Profile（链式）
                    vertx.eventBus().<JsonObject>request("user.profile", userId, profileReply -> {
                        if (profileReply.failed()) {
                            promise.fail(profileReply.cause());
                            return;
                        }
                        // Step 3：合并结果
                        JsonObject merged = user.mergeIn(profileReply.result().body());
                        promise.complete(merged);
                    });
                });

        return promise.future();
    }

    // ─────────────────────────────────────────────
    // 2. DeliveryOptions：消息头 + 超时
    // ─────────────────────────────────────────────

    static class SlowServiceVerticle extends AbstractVerticle {
        @Override
        public void start() {
            vertx.eventBus().<String>consumer("slow.service", msg -> {
                String traceId = msg.headers().get("X-Trace-Id");
                System.out.println("[SlowService] 收到请求，traceId=" + traceId);
                // 故意等 2 秒，用来触发超时演示
                vertx.setTimer(2000, id -> msg.reply("慢响应结果"));
            });
        }
    }

    // ─────────────────────────────────────────────
    // 3. CompositeFuture：并行执行
    // ─────────────────────────────────────────────

    static Future<JsonObject> fetchOrderAsync(Vertx vertx, int orderId) {
        Promise<JsonObject> p = Promise.promise();
        int delay = 100 + new Random().nextInt(200); // 模拟不同延迟
        vertx.setTimer(delay, id -> p.complete(new JsonObject()
                .put("orderId", orderId)
                .put("amount", orderId * 99.0)
                .put("status", "PAID")));
        return p.future();
    }

    // ─────────────────────────────────────────────
    // main
    // ─────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Vert.x 进阶演示 ===\n");

        // --- 演示 1：Future / Promise 链式 ---
        System.out.println("--- 1. Future / Promise 链式异步编程 ---");
        Vertx v1 = Vertx.vertx();
        CountDownLatch latch1 = new CountDownLatch(2);

        v1.deployVerticle(new UserServiceVerticle(), ar -> {
            // 成功路径
            getUserWithProfile(v1, "u001")
                    .onSuccess(result -> {
                        System.out.println("[main] 合并用户信息: " + result.encodePrettily());
                        latch1.countDown();
                    })
                    .onFailure(err -> {
                        System.out.println("[main] 查询失败: " + err.getMessage());
                        latch1.countDown();
                    });

            // 失败路径（不存在的用户）
            getUserWithProfile(v1, "unknown")
                    .onSuccess(r -> latch1.countDown())
                    .onFailure(err -> {
                        System.out.println("[main] 预期失败: " + err.getMessage());
                        latch1.countDown();
                    });
        });
        latch1.await(8, TimeUnit.SECONDS);
        v1.close();

        Thread.sleep(300);
        // --- 演示 2：DeliveryOptions 消息头 + 超时 ---
        System.out.println("\n--- 2. DeliveryOptions：消息头 + 超时控制 ---");
        Vertx v2 = Vertx.vertx();
        CountDownLatch latch2 = new CountDownLatch(1);

        v2.deployVerticle(new SlowServiceVerticle(), ar -> {
            DeliveryOptions opts = new DeliveryOptions()
                    .setSendTimeout(500)                       // 500ms 超时
                    .addHeader("X-Trace-Id", "TRACE-20260526") // 自定义消息头
                    .addHeader("X-Source", "VertxDemo");

            v2.<String>eventBus().request("slow.service", "ping", opts, reply -> {
                if (reply.succeeded()) {
                    System.out.println("[main] 收到回复: " + reply.result().body());
                } else {
                    // 超时会抛出 ReplyException（TIMEOUT）
                    System.out.println("[main] 请求超时（预期行为）: " + reply.cause().getMessage());
                }
                latch2.countDown();
            });
        });
        latch2.await(5, TimeUnit.SECONDS);
        v2.close();

        Thread.sleep(300);
        // --- 演示 3：CompositeFuture 并行查订单 ---
        System.out.println("\n--- 3. CompositeFuture：并行执行多个异步任务 ---");
        Vertx v3 = Vertx.vertx();
        CountDownLatch latch3 = new CountDownLatch(1);

        long start = System.currentTimeMillis();
        Future<JsonObject> f1 = fetchOrderAsync(v3, 1001);
        Future<JsonObject> f2 = fetchOrderAsync(v3, 1002);
        Future<JsonObject> f3 = fetchOrderAsync(v3, 1003);

        CompositeFuture.all(f1, f2, f3).onComplete(ar -> {
            long elapsed = System.currentTimeMillis() - start;
            if (ar.succeeded()) {
                System.out.println("[main] 3 个订单全部完成，耗时 " + elapsed + "ms（并行加速）");
                for (int i = 0; i < ar.result().size(); i++) {
                    System.out.println("  订单 " + (i + 1) + ": " + ((JsonObject) ar.result().resultAt(i)));
                }
            } else {
                System.out.println("[main] 部分订单失败: " + ar.cause().getMessage());
            }
            latch3.countDown();
        });
        latch3.await(5, TimeUnit.SECONDS);
        v3.close();

        Thread.sleep(300);
        // --- 演示 4：setPeriodic + cancelTimer ---
        System.out.println("\n--- 4. setPeriodic 定期任务 + cancelTimer 取消 ---");
        Vertx v4 = Vertx.vertx();
        CountDownLatch latch4 = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);

        // 每 300ms 触发一次
        long[] timerId = new long[1];
        timerId[0] = v4.setPeriodic(300, id -> {
            int n = count.incrementAndGet();
            System.out.println("[Periodic] 第 " + n + " 次触发");
            if (n >= 3) {
                v4.cancelTimer(timerId[0]);
                System.out.println("[Periodic] 已取消定时器");
                latch4.countDown();
            }
        });
        latch4.await(5, TimeUnit.SECONDS);
        v4.close();

        Thread.sleep(300);
        // --- 演示 5：executeBlocking ---
        System.out.println("\n--- 5. executeBlocking：在 Worker 线程执行阻塞代码 ---");
        Vertx v5 = Vertx.vertx();
        CountDownLatch latch5 = new CountDownLatch(1);

        v5.<String>executeBlocking(blockingPromise -> {
            System.out.println("[executeBlocking] 运行线程: " + Thread.currentThread().getName());
            try {
                Thread.sleep(300); // 阻塞操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            blockingPromise.complete("阻塞任务结果: " + System.currentTimeMillis());
        }, false, asyncResult -> {
            // 此回调在 Event Loop 线程执行
            System.out.println("[executeBlocking] 结果回调线程: " + Thread.currentThread().getName());
            System.out.println("[executeBlocking] 结果: " + asyncResult.result());
            latch5.countDown();
        });
        latch5.await(5, TimeUnit.SECONDS);
        v5.close();

        System.out.println("\n=== 进阶演示全部完成 ===");
    }
}
