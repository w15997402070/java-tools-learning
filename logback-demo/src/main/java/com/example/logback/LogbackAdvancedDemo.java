package com.example.logback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.UUID;

/**
 * Logback 进阶 Demo —— MDC、Marker、自定义 Appender、配置化过滤。
 *
 * 知识点：
 * 1. MDC（Mapped Diagnostic Context）—— 线程级上下文传递，用于分布式链路追踪
 * 2. Marker —— 给日志打标签，配合 logback.xml 中的 <turboFilter> 做条件路由
 * 3. MDC 与线程池的风险（子线程丢失 MDC）
 * 4. AsyncAppender 异步写日志，不阻塞业务线程
 */
public class LogbackAdvancedDemo {

    private static final Logger log = LoggerFactory.getLogger(LogbackAdvancedDemo.class);

    // 定义 Marker：用于区分不同业务域的日志
    private static final Marker BIZ_MARKER = MarkerFactory.getMarker("BIZ");
    private static final Marker SECURITY_MARKER = MarkerFactory.getMarker("SECURITY");
    private static final Marker PERFORMANCE_MARKER = MarkerFactory.getMarker("PERF");

    public static void main(String[] args) {
        System.out.println("=== Logback 进阶 Demo ===\n");

        // ---- 1. MDC：请求级上下文 ----
        demoMDC();

        // ---- 2. Marker：业务日志分流 ----
        demoMarker();

        // ---- 3. MDC 与多线程 ----
        demoMDCWithThreads();

        System.out.println("\n✅ LogbackAdvancedDemo 完成！");
    }

    /**
     * MDC（Mapped Diagnostic Context）演示。
     *
     * 典型场景：
     *   - 在 Web Filter/Interceptor 中 put requestId，后续所有日志自动带上
     *   - 日志中心（ELK/Splunk）按 traceId 聚合查询一个请求的全部日志
     *   - 在 logback.xml 中用 %X{key} 访问 MDC 值
     *
     * 注意：MDC 基于 ThreadLocal，仅在线程内传递。
     */
    private static void demoMDC() {
        System.out.println("---- 1. MDC 请求链路追踪 ----");

        // 模拟 Web 请求入口——Filter 中设置 traceId
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);
        MDC.put("userId", "10086");
        MDC.put("remoteIp", "192.168.1.100");

        log.info("接收到用户请求");
        log.info("查询数据库完成 —— 耗时 45ms");
        log.info("调用下游服务完成 —— 耗时 120ms");
        log.warn("缓存命中率低于阈值：60%");

        // 离开请求时清理 MDC，防止内存泄漏
        MDC.clear();
        System.out.println("   MDC 已清理（实际应在 Filter.finally 中执行）");

        pause();
    }

    /**
     * Marker 演示 —— 给日志打标签，在 logback.xml 中按标签分流。
     *
     * 场景：
     *   - BIZ Marker → 写一条日志到业务日志文件
     *   - SECURITY Marker → 写一条日志到安全审计文件（同时触发告警）
     *   - PERFORMANCE Marker → 写一条日志到性能监控文件
     *
     * 在 logback.xml 的 <appender> 中通过 <turboFilter> 按 Marker 过滤：
     *   <turboFilter class="ch.qos.logback.classic.turbo.MarkerFilter">
     *       <Marker>BIZ</Marker>
     *       <OnMatch>ACCEPT</OnMatch>
     *       <OnMismatch>DENY</OnMismatch>
     *   </turboFilter>
     */
    private static void demoMarker() {
        System.out.println("---- 2. Marker 业务日志分流 ----");

        // 业务日志
        log.info(BIZ_MARKER, "用户下单成功：订单号={}", "ORD-20240620-001");

        // 安全审计日志
        log.warn(SECURITY_MARKER, "用户登录IP异常：user={}, ip={}, location={}",
                "admin", "10.0.0.1", "异地登录");

        // 性能监控日志
        log.info(PERFORMANCE_MARKER, "接口响应时间：uri={}, cost={}ms, qps={}",
                "/api/order/create", 320, 150);

        // 嵌套 Marker —— SECURITY 是父 Marker，LOGIN_FAIL 是子 Marker
        Marker loginFailMarker = MarkerFactory.getMarker("LOGIN_FAIL");
        SECURITY_MARKER.add(loginFailMarker);
        log.error(SECURITY_MARKER,
                "用户连续登录失败——账号锁定：user={}, attempts=5", "wangzhengpeng");

        pause();
    }

    /**
     * MDC 在多线程环境下的陷阱与解决方案。
     *
     * 问题：MDC 基于 InheritableThreadLocal，但线程池中的线程是复用的，
     * 子线程不会自动继承父线程的 MDC，且上次遗留的 MDC 可能污染下次执行。
     *
     * 解决方案：
     *   1. 使用 MDC.getCopyOfContextMap() 手动传递
     *   2. 使用 SLF4J 的 MDC 工具类（如 SLF4J 2.0 的 MDCCloseable）
     *   3. 线程池包装为 MdcAwareThreadPoolExecutor
     */
    private static void demoMDCWithThreads() {
        System.out.println("---- 3. MDC 与多线程 ----");

        MDC.put("traceId", "MAIN-TRACE-001");

        // 方案 A：手动传递 MDC 到子线程
        java.util.Map<String, String> contextMap = MDC.getCopyOfContextMap();

        Thread childThread = new Thread(() -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            log.info("子线程（手动传递 MDC）—— traceId 可见");
            MDC.clear(); // 线程结束前清理
        }, "child-thread-1");
        childThread.start();
        try { childThread.join(); } catch (InterruptedException ignored) {}

        // 方案 B：使用 SLF4J 2.0 的 MDCCloseable（try-with-resources）
        // try (MDCCloseable ignored = MDC.putCloseable("key", "value")) { ... }

        log.info("主线程 —— traceId 仍然可见");
        MDC.clear();

        pause();
    }

    private static void pause() {
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        System.out.println("----------------------------");
    }
}
