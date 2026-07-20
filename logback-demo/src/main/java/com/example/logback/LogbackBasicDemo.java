package com.example.logback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logback 基础 Demo —— SLF4J API 的日常用法。
 *
 * 知识点：
 * 1. getLogger() 获取日志实例
 * 2. 五个日志级别：TRACE < DEBUG < INFO < WARN < ERROR
 * 3. 参数化消息（{} 占位符）——比字符串拼接更高效（不输出则不拼接）
 * 4. 异常日志：最后一个参数是 Throwable 时自动提取堆栈
 * 5. Logger 命名层级：子 Logger 继承父 Logger 的配置
 * 6. 输出位置由 logback.xml 决定（默认输出到控制台）
 */
public class LogbackBasicDemo {

    // 推荐：每个类一个 Logger，使用类名
    private static final Logger log = LoggerFactory.getLogger(LogbackBasicDemo.class);

    public static void main(String[] args) {
        System.out.println("=== Logback 基础 Demo ===\n");

        // ---- 1. 五种日志级别 ----
        log.trace("TRACE 级别：最详细的调试信息（通常只用于开发阶段）");
        log.debug("DEBUG 级别：调试信息（例如方法进入、变量值）");
        log.info("INFO 级别：关键业务节点（例如用户登录、订单创建）");
        log.warn("WARN 级别：潜在问题（例如配置缺失、降级处理）");
        log.error("ERROR 级别：已经发生的错误（例如接口调用失败）");

        pause();

        // ---- 2. 参数化消息（推荐写法） ----
        // {} 是 SLF4J 的占位符，底层通过 MessageFormatter 拼接
        // 优点：如果对应级别未开启，toString() 根本不会被调用——零开销
        String userName = "张三";
        int orderId = 20240620;
        log.info("用户 [{}] 创建了订单 [{}]", userName, orderId);

        // 参数化也支持多个参数（最多支持 2 个占位符时性能最优，但实际支持任意数量）
        log.info("订单详情：商品={}，数量={}，金额={}元，支付方式={}",
                "ThinkPad X1", 1, 12999.00, "微信支付");

        pause();

        // ---- 3. 异常日志 ----
        // 把 Throwable 放在最后一个参数位置，SLF4J 会自动提取堆栈
        try {
            int result = 100 / 0;
        } catch (Exception e) {
            // 写法一：用占位符 + 异常
            log.error("订单 {} 处理失败", orderId, e);

            // 写法二：纯异常信息（简洁，适合内部异常）
            log.error("计算异常", e);
        }

        pause();

        // ---- 4. Logger 层级 ----
        // 子包中的 Logger 默认继承父包的日志级别和 appender
        // Logger 名 "com.example.logback.service.OrderService" 
        // 会继承 "com.example.logback" 的配置
        Logger serviceLogger = LoggerFactory.getLogger("com.example.logback.service.OrderService");
        serviceLogger.info("子 Logger 输出 —— 继承父 Logger 'com.example.logback' 的配置");

        pause();

        // ---- 5. 条件日志 —— 避免不必要的对象创建 ----
        // 当需要拼接复杂字符串时，先用 isDebugEnabled() 判断
        // 虽然 {} 占位符已经避免了大部分开销，但复杂对象构造仍需注意
        if (log.isDebugEnabled()) {
            log.debug("当前环境变量：{}，JVM参数：{}",
                    System.getenv(), System.getProperty("java.version"));
        }

        log.info("✅ LogbackBasicDemo 完成！");
    }

    private static void pause() {
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        System.out.println("----------------------------");
    }
}
