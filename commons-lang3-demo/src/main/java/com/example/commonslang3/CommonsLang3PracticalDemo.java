package com.example.commonslang3;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Apache Commons Lang3 实战场景演示
 *
 * 模拟真实业务中最常用的 Lang3 使用场景：
 * 1. 用户注册参数校验（Validate + StringUtils）
 * 2. 订单号/邀请码生成（RandomStringUtils）
 * 3. 通用工具方法封装（ObjectUtils）
 * 4. 配置值类型安全读取（NumberUtils）
 * 5. 接口响应时间监控（StopWatch）
 * 6. 日期范围分析（DateUtils）
 * 7. 统一异常日志处理（ExceptionUtils）
 * 8. Fraction 分数计算（金融/比例场景）
 */
public class CommonsLang3PracticalDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 用户注册参数校验 ==========");
        demoUserRegistration();

        System.out.println("\n========== 2. 订单号/邀请码生成 ==========");
        demoOrderCodeGeneration();

        System.out.println("\n========== 3. 通用工具方法 ObjectUtils ==========");
        demoObjectUtils();

        System.out.println("\n========== 4. 配置值类型安全读取 ==========");
        demoConfigReader();

        System.out.println("\n========== 5. 接口性能监控 ==========");
        demoPerformanceMonitor();

        System.out.println("\n========== 6. 日期范围分析 ==========");
        demoDateRange();

        System.out.println("\n========== 7. 统一异常日志处理 ==========");
        demoExceptionLogging();

        System.out.println("\n========== 8. Fraction 分数计算 ==========");
        demoFraction();
    }

    // -------------------------------------------------------------------------
    // 1. 用户注册校验：Validate + StringUtils 组合
    // -------------------------------------------------------------------------
    private static void demoUserRegistration() {
        // 模拟合法用户
        try {
            UserRegisterDTO user1 = new UserRegisterDTO("zhangsan", "zhang@example.com", "Pass@1234");
            validateUser(user1);
            System.out.println("用户1注册成功: " + user1.username);
        } catch (Exception e) {
            System.out.println("用户1注册失败: " + e.getMessage());
        }

        // 模拟非法用户（用户名空）
        try {
            UserRegisterDTO user2 = new UserRegisterDTO("", "bad@example.com", "123456");
            validateUser(user2);
            System.out.println("用户2注册成功");
        } catch (Exception e) {
            System.out.println("用户2注册失败: " + e.getMessage());
        }

        // 模拟非法用户（邮箱格式错误）
        try {
            UserRegisterDTO user3 = new UserRegisterDTO("lisi", "not-an-email", "Pass@1234");
            validateUser(user3);
            System.out.println("用户3注册成功");
        } catch (Exception e) {
            System.out.println("用户3注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户注册参数校验
     * 使用 Validate（前置条件断言）和 StringUtils（字符串判断）
     */
    private static void validateUser(UserRegisterDTO user) {
        // Validate.notBlank：为空抛 IllegalArgumentException，message 支持格式化
        Validate.notBlank(user.username, "用户名不能为空");
        Validate.isTrue(user.username.length() >= 4 && user.username.length() <= 20,
                "用户名长度须在4~20之间，当前: %d", user.username.length());

        // 使用 StringUtils 做邮箱基本验证
        Validate.notBlank(user.email, "邮箱不能为空");
        Validate.isTrue(StringUtils.contains(user.email, "@") && StringUtils.contains(user.email, "."),
                "邮箱格式不正确: %s", user.email);

        // 密码长度校验
        Validate.notBlank(user.password, "密码不能为空");
        Validate.isTrue(user.password.length() >= 8, "密码长度不能少于8位");
    }

    // -------------------------------------------------------------------------
    // 2. 订单号/邀请码生成
    // -------------------------------------------------------------------------
    private static void demoOrderCodeGeneration() {
        // 订单号：时间戳 + 8位随机数字（纯数字，便于客服查询）
        String orderNo = generateOrderNo();
        System.out.println("订单号: " + orderNo);

        // 邀请码：6位字母+数字组合（避免歧义字符 0/O/1/I/l）
        String inviteCode = generateInviteCode();
        System.out.println("邀请码: " + inviteCode);

        // 临时密码：8位包含大小写+数字
        String tempPassword = RandomStringUtils.random(8,
                "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789");
        System.out.println("临时密码: " + tempPassword);

        // API Key：32位十六进制
        String apiKey = RandomStringUtils.random(32, "0123456789abcdef");
        System.out.println("API Key: " + apiKey);

        // 批量生成演示
        System.out.print("批量生成5个验证码: ");
        for (int i = 0; i < 5; i++) {
            System.out.print(RandomStringUtils.randomNumeric(6) + " ");
        }
        System.out.println();
    }

    private static String generateOrderNo() {
        // 格式：ORD + yyyyMMddHHmmss + 6位随机数
        String timestamp = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
        return "ORD" + timestamp + RandomStringUtils.randomNumeric(6);
    }

    private static String generateInviteCode() {
        // 去掉易混淆字符：0/O/1/I
        return RandomStringUtils.random(6, "ABCDEFGHJKMNPQRSTUVWXYZ23456789");
    }

    // -------------------------------------------------------------------------
    // 3. ObjectUtils —— 通用 null 安全操作
    // -------------------------------------------------------------------------
    private static void demoObjectUtils() {
        // firstNonNull：返回第一个非 null 值（类似 SQL COALESCE）
        String result = ObjectUtils.firstNonNull(null, null, "默认值", "备用值");
        System.out.println("firstNonNull      = " + result);

        // defaultIfNull：null 时返回默认值
        Integer count = null;
        int safeCount = ObjectUtils.defaultIfNull(count, 0);
        System.out.println("defaultIfNull     = " + safeCount);

        // isEmpty：支持 String/Collection/Map/数组/Optional 的空判断
        System.out.println("isEmpty(null)     = " + ObjectUtils.isEmpty(null));
        System.out.println("isEmpty(\"\")       = " + ObjectUtils.isEmpty(""));
        System.out.println("isEmpty([])       = " + ObjectUtils.isEmpty(new int[]{}));
        List<String> emptyList = new ArrayList<>();
        System.out.println("isEmpty(emptyList)= " + ObjectUtils.isEmpty(emptyList));

        // isNotEmpty
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        System.out.println("isNotEmpty(map)   = " + ObjectUtils.isNotEmpty(map));

        // identityToString：带类名的 toString（调试用）
        System.out.println("identityToString  = " + ObjectUtils.identityToString("hello"));
    }

    // -------------------------------------------------------------------------
    // 4. 配置值类型安全读取（从 Map/Properties 中读取，避免强转 NPE）
    // -------------------------------------------------------------------------
    private static void demoConfigReader() {
        // 模拟配置 Map（key->value 均为 String）
        Map<String, String> config = new HashMap<>();
        config.put("server.port", "8080");
        config.put("cache.ttl", "3600");
        config.put("rate.limit", "100.5");
        config.put("feature.enabled", "true");
        config.put("max.retry", "abc"); // 故意写错，测试容错

        // 安全读取整数配置（转换失败返回默认值，不抛异常）
        int port = NumberUtils.toInt(config.get("server.port"), 80);
        int ttl = NumberUtils.toInt(config.get("cache.ttl"), 60);
        int maxRetry = NumberUtils.toInt(config.get("max.retry"), 3);
        double rateLimit = NumberUtils.toDouble(config.get("rate.limit"), 50.0);

        System.out.println("server.port       = " + port);
        System.out.println("cache.ttl         = " + ttl);
        System.out.println("max.retry(err)    = " + maxRetry);
        System.out.println("rate.limit        = " + rateLimit);

        // 配置值脱敏输出（密码、Token 等）
        String dbPassword = "mysecretpassword";
        String masked = StringUtils.leftPad(
                StringUtils.right(dbPassword, 4),
                dbPassword.length(), '*');
        System.out.println("密码脱敏           = " + masked);
    }

    // -------------------------------------------------------------------------
    // 5. 接口性能监控（StopWatch 实战）
    // 注意：Commons Lang3 StopWatch 不支持按阶段命名
    // 如需多阶段监控，需使用多个 StopWatch 实例
    // -------------------------------------------------------------------------
    private static void demoPerformanceMonitor() {
        // 阶段1：参数校验
        StopWatch sw1 = StopWatch.createStarted();
        simulateWork(10);
        sw1.stop();
        System.out.println("参数校验耗时: " + sw1.getTime() + "ms");

        // 阶段2：DB查询
        StopWatch sw2 = StopWatch.createStarted();
        simulateWork(80);
        sw2.stop();
        System.out.println("DB查询耗时:   " + sw2.getTime() + "ms");

        // 阶段3：业务处理
        StopWatch sw3 = StopWatch.createStarted();
        simulateWork(30);
        sw3.stop();
        System.out.println("业务处理耗时: " + sw3.getTime() + "ms");

        // 阶段4：结果组装
        StopWatch sw4 = StopWatch.createStarted();
        simulateWork(5);
        sw4.stop();
        System.out.println("结果组装耗时: " + sw4.getTime() + "ms");

        // 总耗时
        long total = sw1.getTime() + sw2.getTime() + sw3.getTime() + sw4.getTime();
        System.out.println("总耗时:       " + total + "ms");
    }

    private static void simulateWork(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // 6. 日期范围分析（DateUtils + DateFormatUtils）
    // -------------------------------------------------------------------------
    private static void demoDateRange() {
        Date now = new Date();
        String today = DateFormatUtils.format(now, "yyyy-MM-dd");

        // 本周第一天（周一）
        Date weekStart = DateUtils.truncate(now, java.util.Calendar.MONDAY);
        Date weekEnd = DateUtils.addDays(weekStart, 6);

        // 本月第一天/最后一天
        Date monthStart = DateUtils.truncate(now, java.util.Calendar.MONTH);
        Date monthEnd = DateUtils.addSeconds(DateUtils.addMonths(monthStart, 1), -1);

        System.out.println("今天       = " + today);
        System.out.println("本周开始   = " + DateFormatUtils.format(weekStart, "yyyy-MM-dd"));
        System.out.println("本周结束   = " + DateFormatUtils.format(weekEnd, "yyyy-MM-dd"));
        System.out.println("本月开始   = " + DateFormatUtils.format(monthStart, "yyyy-MM-dd"));
        System.out.println("本月结束   = " + DateFormatUtils.format(monthEnd, "yyyy-MM-dd HH:mm:ss"));

        // 计算两个日期的天数差
        Date orderDate = DateUtils.addDays(now, -5);
        long diffMs = now.getTime() - orderDate.getTime();
        long diffDays = diffMs / (1000 * 60 * 60 * 24);
        System.out.println("订单创建   = " + DateFormatUtils.format(orderDate, "yyyy-MM-dd"));
        System.out.println("距今天数   = " + diffDays + " 天");

        // 判断是否过期（30天有效期）
        boolean isExpired = diffDays > 30;
        System.out.println("订单是否过期 = " + isExpired);
    }

    // -------------------------------------------------------------------------
    // 7. 统一异常日志处理（ExceptionUtils 实战）
    // -------------------------------------------------------------------------
    private static void demoExceptionLogging() {
        try {
            processOrder("order-001");
        } catch (Exception e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String rootMsg = rootCause != null ? rootCause.getMessage() : e.getMessage();

            String fullStack = ExceptionUtils.getStackTrace(e);

            System.out.println("根本原因: " + rootMsg);
            System.out.println("堆栈长度: " + fullStack.length() + " chars");
            System.out.println("异常链条: ");
            ExceptionUtils.getThrowableList(e)
                    .forEach(t -> System.out.println("  [" + t.getClass().getSimpleName() + "] " + t.getMessage()));
        }
    }

    private static void processOrder(String orderId) throws Exception {
        try {
            queryInventory(orderId);
        } catch (Exception e) {
            throw new RuntimeException("订单处理失败: " + orderId, e);
        }
    }

    private static void queryInventory(String orderId) throws Exception {
        try {
            callDatabase();
        } catch (Exception e) {
            throw new RuntimeException("库存查询失败", e);
        }
    }

    private static void callDatabase() throws Exception {
        throw new Exception("数据库连接超时 (host=db01:3306, timeout=5000ms)");
    }

    // -------------------------------------------------------------------------
    // 8. Fraction 分数计算（精确比例/折扣/评分场景）
    // -------------------------------------------------------------------------
    private static void demoFraction() {
        Fraction half = Fraction.getFraction(1, 2);
        Fraction third = Fraction.getFraction(1, 3);
        Fraction quarter = Fraction.getFraction(1, 4);

        System.out.println("1/2 = " + half);
        System.out.println("1/3 = " + third);

        // 分数运算
        Fraction sum = half.add(third);
        System.out.println("1/2 + 1/3 = " + sum);

        Fraction diff = half.subtract(quarter);
        System.out.println("1/2 - 1/4 = " + diff);

        Fraction product = half.multiplyBy(third);
        System.out.println("1/2 * 1/3 = " + product);

        Fraction quotient = half.divideBy(third);
        System.out.println("1/2 / 1/3 = " + quotient);

        // 转换为 double
        System.out.println("5/6 转 double = " + sum.doubleValue());

        // 实战：折扣计算
        double originalPrice = 100.0;
        Fraction discount = Fraction.getFraction(8, 10);
        double discountedPrice = originalPrice * discount.doubleValue();
        System.out.println("原价100元打8折 = " + discountedPrice + "元");

        // 从字符串创建分数
        Fraction fromStr = Fraction.getFraction("3/5");
        System.out.println("从字符串创建 3/5 = " + fromStr.doubleValue());
    }

    // =========================================================================
    // 内部辅助 DTO
    // =========================================================================

    /** 用户注册 DTO */
    static class UserRegisterDTO {
        String username;
        String email;
        String password;

        UserRegisterDTO(String username, String email, String password) {
            this.username = username;
            this.email = email;
            this.password = password;
        }
    }
}
