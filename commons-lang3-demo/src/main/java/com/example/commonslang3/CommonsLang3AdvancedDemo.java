package com.example.commonslang3;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CommonsLang3AdvancedDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. Random 随机工具 ==========");
        demoRandom();

        System.out.println("\n========== 2. Builder 系列 ==========");
        demoBuilders();

        System.out.println("\n========== 3. Pair / Triple 元组 ==========");
        demoPairTriple();

        System.out.println("\n========== 4. FieldUtils / MethodUtils 反射 ==========");
        demoReflection();

        System.out.println("\n========== 5. StopWatch 精确计时 ==========");
        demoStopWatch();

        System.out.println("\n========== 6. DateUtils 日期工具 ==========");
        demoDateUtils();

        System.out.println("\n========== 7. ExceptionUtils 异常工具 ==========");
        demoExceptionUtils();
    }

    private static void demoRandom() {
        String verifyCode = RandomStringUtils.randomNumeric(6);
        System.out.println("6位数字验证码   = " + verifyCode);

        String randomAlpha = RandomStringUtils.randomAlphabetic(8);
        System.out.println("8位随机字母     = " + randomAlpha);

        String token = RandomStringUtils.randomAlphanumeric(16);
        System.out.println("16位随机Token   = " + token);

        String fromChars = RandomStringUtils.random(8, "ABCDEF0123456789");
        System.out.println("8位十六进制字符串 = " + fromChars);

        int randomInt = RandomUtils.nextInt(1, 100);
        long randomLong = RandomUtils.nextLong(1000L, 9999L);
        double randomDouble = RandomUtils.nextDouble(0.0, 1.0);
        System.out.println("随机整数 [1,100) = " + randomInt);
        System.out.println("随机长整型       = " + randomLong);
        System.out.println("随机浮点 [0,1)  = " + String.format("%.4f", randomDouble));
    }

    private static void demoBuilders() {
        Product p1 = new Product(1L, "商品A", 99.9);
        Product p2 = new Product(1L, "商品A", 99.9);
        Product p3 = new Product(2L, "商品B", 199.0);

        System.out.println("p1.toString() = " + p1.toString());
        System.out.println("p1.equals(p2) = " + p1.equals(p2));
        System.out.println("p1.equals(p3) = " + p1.equals(p3));
        System.out.println("p1.hashCode() = " + p1.hashCode());
        System.out.println("p2.hashCode() = " + p2.hashCode());

        List<Product> products = Arrays.asList(
                new Product(3L, "商品C", 50.0),
                new Product(1L, "商品A", 99.9),
                new Product(2L, "商品B", 199.0)
        );
        products.sort((a, b) -> new CompareToBuilder()
                .append(a.id, b.id)
                .append(a.name, b.name)
                .toComparison());
        System.out.println("排序后:");
        products.forEach(p -> System.out.println("  " + p));
    }

    private static void demoPairTriple() {
        Pair<String, Integer> user = ImmutablePair.of("张三", 25);
        System.out.println("用户姓名: " + user.getLeft() + ", 年龄: " + user.getRight());
        System.out.println("Pair.toString: " + user);

        MutablePair<String, String> kv = MutablePair.of("key", "value1");
        kv.setRight("value2");
        System.out.println("MutablePair: " + kv);

        Triple<String, Integer, Double> score = Triple.of("李四", 90, 95.5);
        System.out.println("姓名: " + score.getLeft()
                + ", 笔试: " + score.getMiddle()
                + ", 机试: " + score.getRight());

        Pair<List<String>, Long> page = queryPage(1, 10);
        System.out.println("分页数据数量: " + page.getLeft().size()
                + ", 总记录数: " + page.getRight());
    }

    private static Pair<List<String>, Long> queryPage(int pageNum, int pageSize) {
        List<String> data = Arrays.asList("item1", "item2", "item3");
        return ImmutablePair.of(data, 100L);
    }

    private static void demoReflection() throws Exception {
        SensitiveConfig config = new SensitiveConfig("jdbc:mysql://localhost/db", "root", "secret123");
        System.out.println("原始对象: " + config);

        Object password = FieldUtils.readField(config, "password", true);
        System.out.println("读取私有字段 password = " + password);

        FieldUtils.writeField(config, "password", "newPassword", true);
        Object newPassword = FieldUtils.readField(config, "password", true);
        System.out.println("修改后 password = " + newPassword);

        Field[] fields = FieldUtils.getAllFields(SensitiveConfig.class);
        System.out.print("所有字段: ");
        for (Field f : fields) {
            System.out.print(f.getName() + " ");
        }
        System.out.println();

        Object result = MethodUtils.invokeMethod(config, true, "getConnectionInfo");
        System.out.println("调用私有方法 getConnectionInfo() = " + result);
    }

    private static void demoStopWatch() throws InterruptedException {
        StopWatch watch = StopWatch.createStarted();

        Thread.sleep(100);
        watch.split();
        long splitTime = watch.getSplitTime();

        Thread.sleep(50);
        watch.stop();

        System.out.println("分割点耗时 = " + splitTime + "ms");
        System.out.println("总耗时     = " + watch.getTime() + "ms");
        System.out.println("总耗时(ns) = " + watch.getNanoTime() + "ns");
        System.out.println("格式化输出 = " + watch.formatTime());

        StopWatch watch2 = new StopWatch();
        watch2.start();
        Thread.sleep(50);
        watch2.suspend();
        Thread.sleep(200);
        watch2.resume();
        Thread.sleep(50);
        watch2.stop();
        System.out.println("暂停后实际计时 ≈ " + watch2.getTime() + "ms");
    }

    private static void demoDateUtils() {
        Date now = new Date();
        System.out.println("当前时间 = " + DateFormatUtils.format(now, "yyyy-MM-dd HH:mm:ss"));

        Date tomorrow = DateUtils.addDays(now, 1);
        Date nextMonth = DateUtils.addMonths(now, 1);
        Date nextHour = DateUtils.addHours(now, 1);
        System.out.println("明天       = " + DateFormatUtils.format(tomorrow, "yyyy-MM-dd"));
        System.out.println("下个月     = " + DateFormatUtils.format(nextMonth, "yyyy-MM-dd"));
        System.out.println("一小时后   = " + DateFormatUtils.format(nextHour, "HH:mm:ss"));

        Date truncatedDay = DateUtils.truncate(now, java.util.Calendar.DAY_OF_MONTH);
        System.out.println("今天00:00  = " + DateFormatUtils.format(truncatedDay, "yyyy-MM-dd HH:mm:ss"));

        Date sameDay = DateUtils.addHours(now, 2);
        System.out.println("isSameDay  = " + DateUtils.isSameDay(now, sameDay));

        try {
            Date parsed = DateUtils.parseDate("2026-05-05", "yyyy-MM-dd", "MM/dd/yyyy", "yyyyMMdd");
            System.out.println("多格式解析 = " + DateFormatUtils.format(parsed, "yyyy-MM-dd"));
        } catch (Exception e) {
            System.out.println("解析失败: " + e.getMessage());
        }
    }

    private static void demoExceptionUtils() {
        Exception dbException = new Exception("连接超时");
        RuntimeException serviceException = new RuntimeException("查询用户失败", dbException);
        Exception bizException = new Exception("下单失败", serviceException);

        Throwable rootCause = ExceptionUtils.getRootCause(bizException);
        System.out.println("根本原因: " + (rootCause != null ? rootCause.getMessage() : "null"));

        List<Throwable> chain = ExceptionUtils.getThrowableList(bizException);
        System.out.println("异常链深度: " + chain.size());
        chain.forEach(t -> System.out.println("  -> " + t.getMessage()));

        String stackTrace = ExceptionUtils.getStackTrace(bizException);
        System.out.println("堆栈前100字符: " + stackTrace.substring(0, Math.min(100, stackTrace.length())));

        boolean hasNpe = ExceptionUtils.indexOfType(bizException, NullPointerException.class) >= 0;
        System.out.println("含NPE? = " + hasNpe);
    }

    static class Product {
        long id;
        String name;
        double price;

        Product(long id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("id", id)
                    .append("name", name)
                    .append("price", price)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Product)) return false;
            Product that = (Product) o;
            return new EqualsBuilder()
                    .append(id, that.id)
                    .append(name, that.name)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(id)
                    .append(name)
                    .toHashCode();
        }
    }

    static class SensitiveConfig {
        private String url;
        private String username;
        private String password;

        SensitiveConfig(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        @Override
        public String toString() {
            return "SensitiveConfig{url='" + url + "', username='" + username + "', password='***'}";
        }

        @SuppressWarnings("unused")
        private String getConnectionInfo() {
            return url + " (user=" + username + ")";
        }
    }
}
