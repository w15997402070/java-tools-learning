package com.example.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Byte Buddy 实战演示
 *
 * 本类涵盖三个贴近生产的实战场景：
 * 1. 轻量级 AOP 框架：统一日志 + 执行耗时监控（不引入 Spring AOP / AspectJ）
 * 2. 动态代理工厂：运行时根据策略选择目标类，实现可配置路由
 * 3. 测试 Mock 工厂：无需 Mockito，用 Byte Buddy 快速生成 Stub/Spy
 */
public class ByteBuddyPracticalDemo {

    // ============================================================
    // 一、轻量级 AOP 框架 —— 方法执行监控
    // ============================================================

    /**
     * 监控数据收集器（全局单例）
     */
    static class MetricsCollector {
        private static final ConcurrentHashMap<String, AtomicLong> callCount = new ConcurrentHashMap<>();
        private static final ConcurrentHashMap<String, AtomicLong> totalTime = new ConcurrentHashMap<>();

        static void record(String methodName, long elapsedMs) {
            callCount.computeIfAbsent(methodName, k -> new AtomicLong()).incrementAndGet();
            totalTime.computeIfAbsent(methodName, k -> new AtomicLong()).addAndGet(elapsedMs);
        }

        static void printReport() {
            System.out.println("\n  [MetricsReport] 方法调用统计：");
            for (String name : callCount.keySet()) {
                long count = callCount.get(name).get();
                long total = totalTime.get(name).get();
                System.out.printf("    %-30s 调用次数=%-3d  总耗时=%-6dms  平均=%.1fms%n",
                        name, count, total, (double) total / count);
            }
        }
    }

    /**
     * 性能监控拦截器
     */
    public static class MonitoringInterceptor {
        @RuntimeType
        public static Object monitor(
                @Origin Method method,
                @AllArguments Object[] args,
                @SuperCall Callable<?> superCall) throws Exception {
            long start = System.nanoTime();
            try {
                return superCall.call();
            } finally {
                long elapsed = (System.nanoTime() - start) / 1_000_000; // 转为 ms
                MetricsCollector.record(method.getDeclaringClass().getSimpleName()
                        + "." + method.getName(), elapsed);
            }
        }
    }

    /**
     * 目标业务服务（模拟有耗时的真实业务）
     */
    static class PaymentService {
        public String charge(String userId, double amount) throws InterruptedException {
            Thread.sleep(10); // 模拟 IO
            return "Charged " + amount + " from " + userId;
        }

        public boolean refund(String txId) throws InterruptedException {
            Thread.sleep(5);  // 模拟 IO
            return true;
        }
    }

    /**
     * AOP 工厂：给任意类添加性能监控
     */
    @SuppressWarnings("unchecked")
    static <T> T wrapWithMonitoring(Class<T> targetClass) throws Exception {
        Class<? extends T> monitored = new ByteBuddy()
                .subclass(targetClass)
                .method(ElementMatchers.isPublic())
                .intercept(MethodDelegation.to(MonitoringInterceptor.class))
                .make()
                .load(ByteBuddyPracticalDemo.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        return monitored.newInstance();
    }

    static void scenario1_aopMonitoring() throws Exception {
        System.out.println("\n===== 场景一：轻量级 AOP 性能监控 =====");

        PaymentService svc = wrapWithMonitoring(PaymentService.class);

        // 多次调用模拟真实流量
        for (int i = 0; i < 3; i++) {
            svc.charge("user_" + i, 99.9 * (i + 1));
        }
        svc.refund("TX001");
        svc.refund("TX002");

        MetricsCollector.printReport();
    }

    // ============================================================
    // 二、动态代理工厂 —— 运行时路由
    // ============================================================

    /**
     * 统一消息发送接口
     */
    interface MessageSender {
        void send(String to, String content);
    }

    /** 邮件实现 */
    static class EmailSender implements MessageSender {
        public void send(String to, String content) {
            System.out.println("  [Email] 发送给 " + to + "：" + content);
        }
    }

    /** SMS 实现 */
    static class SmsSender implements MessageSender {
        public void send(String to, String content) {
            System.out.println("  [SMS] 发送给 " + to + "：" + content);
        }
    }

    /**
     * 路由拦截器：根据收件人前缀选择不同的真实发送器
     */
    public static class RoutingInterceptor {
        // 静态持有真实实例
        static final Map<String, MessageSender> ROUTES = new HashMap<>();
        static {
            ROUTES.put("email:", new EmailSender());
            ROUTES.put("sms:", new SmsSender());
        }

        @RuntimeType
        public static void route(@AllArguments Object[] args) {
            String to = (String) args[0];
            String content = (String) args[1];
            MessageSender sender = null;
            for (Map.Entry<String, MessageSender> entry : ROUTES.entrySet()) {
                if (to.startsWith(entry.getKey())) {
                    sender = entry.getValue();
                    break;
                }
            }
            if (sender == null) {
                System.out.println("  [Router] 未找到路由，丢弃消息：" + to);
                return;
            }
            sender.send(to, content);
        }
    }

    static void scenario2_dynamicRouter() throws Exception {
        System.out.println("\n===== 场景二：动态代理路由工厂 =====");

        // 生成一个路由代理类，实现 MessageSender 接口
        Class<?> routerClass = new ByteBuddy()
                .subclass(Object.class)
                .implement(MessageSender.class)
                .method(ElementMatchers.named("send"))
                .intercept(MethodDelegation.to(RoutingInterceptor.class))
                .make()
                .load(ByteBuddyPracticalDemo.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        MessageSender router = (MessageSender) routerClass.newInstance();

        router.send("email:alice@example.com", "您的订单已发货");
        router.send("sms:13800138000", "验证码：1234");
        router.send("push:device_token_xxx", "新消息通知");
    }

    // ============================================================
    // 三、测试 Mock 工厂（无 Mockito）
    // ============================================================

    /**
     * 目标服务：外部依赖，不可用于单元测试
     */
    static class ExternalApiClient {
        public String fetchUserProfile(String userId) {
            throw new RuntimeException("真实接口不可用于测试！");
        }

        public boolean sendVerifyCode(String phone) {
            throw new RuntimeException("短信接口不可在测试中调用！");
        }
    }

    /**
     * Stub 拦截器工厂：根据 Map 配置返回固定值
     */
    public static class StubInterceptor {
        /** key = "方法名", value = 要返回的固定值 */
        public static Map<String, Object> stubMap = new HashMap<>();

        @RuntimeType
        public static Object stub(@Origin Method method) {
            String name = method.getName();
            if (stubMap.containsKey(name)) {
                System.out.println("  [Stub] " + name + " 返回预设值：" + stubMap.get(name));
                return stubMap.get(name);
            }
            throw new UnsupportedOperationException("方法 " + name + " 未配置 Stub");
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T mockWithStubs(Class<T> clazz, Map<String, Object> stubs) throws Exception {
        StubInterceptor.stubMap = stubs;

        Class<? extends T> stubClass = new ByteBuddy()
                .subclass(clazz)
                .method(ElementMatchers.isPublic())
                .intercept(MethodDelegation.to(StubInterceptor.class))
                .make()
                .load(ByteBuddyPracticalDemo.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        return stubClass.newInstance();
    }

    static void scenario3_mockFactory() throws Exception {
        System.out.println("\n===== 场景三：Mock 工厂（替代 Mockito 的轻量方案）=====");

        // 配置 Stub 映射
        Map<String, Object> stubs = new HashMap<>();
        stubs.put("fetchUserProfile", "{\"id\":\"U001\",\"name\":\"Alice\",\"vip\":true}");
        stubs.put("sendVerifyCode", true);

        ExternalApiClient mockClient = mockWithStubs(ExternalApiClient.class, stubs);

        String profile = mockClient.fetchUserProfile("U001");
        System.out.println("fetchUserProfile 结果：" + profile);

        boolean sent = mockClient.sendVerifyCode("13800138000");
        System.out.println("sendVerifyCode 结果：" + sent);

        System.out.println("\n  -> 整个测试过程未调用真实外部接口，安全隔离 ✓");
    }

    // ========== main ==========

    public static void main(String[] args) throws Exception {
        System.out.println("========== Byte Buddy 实战演示 ==========");
        scenario1_aopMonitoring();
        scenario2_dynamicRouter();
        scenario3_mockFactory();
        System.out.println("\n========== 实战演示完成 ==========");
    }
}
