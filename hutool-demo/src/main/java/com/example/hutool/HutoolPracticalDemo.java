package com.example.hutool;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.math.BigDecimal;
import java.util.*;

/**
 * Hutool 实战场景演示
 * 
 * 覆盖：订单编号生成、JSON序列化、邮件发送配置、Bean拷贝、缓存、URL工具
 * 
 * @author java-tools-learning
 */
public class HutoolPracticalDemo {

    public static void main(String[] args) {
        Console.log("========== Hutool 实战场景演示 ==========\n");

        // === 场景1: 订单编号生成 ===
        Console.log("--- 场景1: 电商订单编号生成 ---");
        demoOrderIdGeneration();

        // === 场景2: JSON处理（Hutool JSON） ===
        Console.log("\n--- 场景2: JSON序列化与反序列化 ---");
        demoJsonProcessing();

        // === 场景3: Bean拷贝与属性操作 ===
        Console.log("\n--- 场景3: Bean拷贝与属性操作 ---");
        demoBeanCopy();

        // === 场景4: 本地缓存（LRU/FIFO/Timed） ===
        Console.log("\n--- 场景4: 本地缓存策略 ---");
        demoCache();

        // === 场景5: 邮件发送配置 ===
        Console.log("\n--- 场景5: 邮件发送（配置示例） ---");
        demoMailConfig();

        // === 场景6: 实用工具：URL/手机号/随机 ===
        Console.log("\n--- 场景6: 常用工具方法 ---");
        demoUtilityTools();

        Console.log("\n========== 实战演示完成 ==========");
    }

    /**
     * 场景1: 电商订单编号生成
     * 格式: ORD + yyyyMMdd + 业务线 + Snowflake后6位
     */
    private static void demoOrderIdGeneration() {
        // Snowflake雪花算法（机器ID=1, 数据中心ID=1）
        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
        
        // 生成订单号
        String prefix = "ORD";
        String dateStr = DateUtil.format(DateUtil.date(), "yyyyMMdd");
        
        for (int i = 0; i < 5; i++) {
            String orderId = StrUtil.format("{}{}{}{}", 
                    prefix,
                    dateStr,
                    "01",  // 业务线编号
                    String.valueOf(snowflake.nextId()).substring(10)
            );
            Console.log("订单号[{}]: {}", i + 1, orderId);
        }

        // 退款单号
        String refundId = StrUtil.format("RFD{}{}{}", 
                dateStr, 
                "01",
                String.valueOf(snowflake.nextId()).substring(10)
        );
        Console.log("退款单号: {}", refundId);
    }

    /**
     * 场景2: JSON序列化处理
     * Hutool支持JSONObject/JSONArray，无需引入额外依赖
     */
    private static void demoJsonProcessing() {
        // === 实体类 <=> JSON ===
        Order order = new Order("ORD20250612001", "iPhone 15 Pro", 899900, "已支付");
        String jsonStr = JSONUtil.toJsonPrettyStr(order);
        Console.log("订单JSON:\n{}", jsonStr);

        Order parsed = JSONUtil.toBean(jsonStr, Order.class);
        Console.log("解析后: {} - {} - ¥{}", parsed.getOrderId(), parsed.getProductName(), parsed.getAmount() / 100.0);

        // === JSONObject 动态操作 ===
        JSONObject jsonObj = JSONUtil.createObj()
                .set("code", 200)
                .set("message", "success")
                .set("data", JSONUtil.createObj()
                        .set("userId", "U10001")
                        .set("username", "张三")
                        .set("roles", JSONUtil.createArray().set("admin").set("user"))
                );
        Console.log("\n统一响应JSON:\n{}", JSONUtil.toJsonPrettyStr(jsonObj));
        Console.log("取嵌套值 code: {}", jsonObj.getInt("code"));
        Console.log("取嵌套值 data.userId: {}", jsonObj.getByPath("data.userId"));

        // === JSONArray 操作 ===
        JSONArray arr = JSONUtil.createArray();
        arr.add(JSONUtil.createObj().set("id", 1).set("name", "Java"));
        arr.add(JSONUtil.createObj().set("id", 2).set("name", "Spring"));
        arr.add(JSONUtil.createObj().set("id", 3).set("name", "Hutool"));
        Console.log("\n技术栈列表: {}", JSONUtil.toJsonPrettyStr(arr));
    }

    /**
     * 场景3: Bean拷贝与属性操作
     */
    private static void demoBeanCopy() {
        // 源对象
        OrderCreateRequest request = new OrderCreateRequest();
        request.setProductName("MacBook Pro 16\"");
        request.setQuantity(2);
        request.setUnitPrice(1999900);
        request.setBuyerName("李四");
        request.setBuyerPhone("139****6789");

        Console.log("请求对象: {}", request);

        // Bean -> Map
        Map<String, Object> beanMap = BeanUtil.beanToMap(request);
        Console.log("Bean转Map: {}", beanMap);

        // Map -> Bean
        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("productName", "iPad Air");
        sourceMap.put("quantity", 1);
        sourceMap.put("unitPrice", 479900);
        OrderCreateRequest newReq = BeanUtil.toBean(sourceMap, OrderCreateRequest.class);
        Console.log("Map转Bean: {}", newReq);

        // Bean拷贝（同名属性自动拷贝）
        OrderDTO dto = new OrderDTO();
        BeanUtil.copyProperties(request, dto);
        Console.log("Bean拷贝结果: {}", dto);

        // 设置/获取嵌套属性
        BeanUtil.setFieldValue(dto, "productName", "华为 Mate 60 Pro");
        Object name = BeanUtil.getFieldValue(dto, "productName");
        Console.log("修改后名称: {}", name);
    }

    /**
     * 场景4: 本地缓存演示
     */
    private static void demoCache() {
        // LRU缓存 (最近最少使用)
        Cache<String, String> lruCache = CacheUtil.newLRUCache(3);
        lruCache.put("key1", "value1");
        lruCache.put("key2", "value2");
        lruCache.put("key3", "value3");
        Console.log("LRU初始: [{}, {}, {}]", 
                lruCache.get("key1"), lruCache.get("key2"), lruCache.get("key3"));

        // 访问key1使其变为最近使用，然后插入key4淘汰key2
        lruCache.get("key1");
        lruCache.put("key4", "value4");
        Console.log("插入key4后 - key2: {}, key4: {}", lruCache.get("key2"), lruCache.get("key4"));

        // 带超时的缓存
        Cache<String, Object> timedCache = CacheUtil.newTimedCache(3000); // 3秒过期
        timedCache.put("session", "abc123_token");
        
        Console.log("立即获取: {}", timedCache.get("session"));
        ThreadUtil.sleep(1500);
        Console.log("1.5秒后: {}", timedCache.get("session"));
        ThreadUtil.sleep(2000);
        Console.log("3.5秒后(应过期): {}", timedCache.get("session"));

        // FIFO缓存
        Cache<String, Integer> fifoCache = CacheUtil.newFIFOCache(3);
        fifoCache.put("a", 1);
        fifoCache.put("b", 2);
        fifoCache.put("c", 3);
        fifoCache.put("d", 4); // 淘汰最早进入的a
        Console.log("FIFO - a: {}, d: {}", fifoCache.get("a"), fifoCache.get("d"));
    }

    /**
     * 场景5: 邮件发送（仅配置示例，实际发送需有效SMTP账号）
     */
    private static void demoMailConfig() {
        Console.log("Hutool邮件发送 - 配置示例（不实际发送）:");

        // 配置QQ邮箱
        String exampleConfig = 
            "MailAccount account = new MailAccount();\n" +
            "account.setHost(\"smtp.qq.com\");\n" +
            "account.setPort(465);\n" +
            "account.setAuth(true);\n" +
            "account.setFrom(\"your-email@qq.com\");\n" +
            "account.setUser(\"your-email@qq.com\");\n" +
            "account.setPass(\"授权码(非邮箱密码)\");  // QQ邮箱需开启SMTP获取授权码\n" +
            "account.setSslEnable(true);\n" +
            "\n" +
            "// 发送邮件\n" +
            "MailUtil.send(account, \"recipient@qq.com\", \"标题\", \"内容\", false);\n" +
            "\n" +
            "// 发送HTML邮件\n" +
            "MailUtil.send(account, \"to@qq.com\", \"主题\", \"<h1>HTML内容</h1>\", true);\n" +
            "\n" +
            "// 带附件\n" +
            "MailUtil.send(account, \"to@qq.com\", \"主题\", \"内容\", false, new File(\"report.pdf\"));\n" +
            "\n" +
            "// 群发（国内常用邮箱SMTP）:\n" +
            "// QQ邮箱: smtp.qq.com:465 (SSL) / smtp.qq.com:587 (STARTTLS)\n" +
            "// 163邮箱: smtp.163.com:465\n" +
            "// 企业微信: smtp.exmail.qq.com:465";

        Console.log(exampleConfig);
    }

    /**
     * 场景6: 常用工具方法
     */
    private static void demoUtilityTools() {
        // === URL编码/解码 ===
        String url = "https://www.baidu.com/s?wd=Java工具库";
        Console.log("URL编码: {}", URLUtil.encode("Java工具库"));
        Console.log("完整URL构建: {}", URLUtil.encodeAll(url));

        // === 手机号/身份证校验 ===
        String phone = "13812345678";
        Console.log("手机号校验 '{}': {}", phone, Validator.isMobile(phone));
        Console.log("邮箱校验 'test@qq.com': {}", Validator.isEmail("test@qq.com"));
        Console.log("身份证校验 '340823199001011234': {}", 
                Validator.isCitizenId("340823199001011234"));
        Console.log("IPv4校验 '192.168.1.1': {}", Validator.isIpv4("192.168.1.1"));

        // === 随机工具 ===
        Console.log("随机6位数字: {}", RandomUtil.randomNumbers(6));
        Console.log("随机UUID: {}", RandomUtil.randomString(32));
        Console.log("随机中文姓名: {}", RandomUtil.randomChinese());
        Console.log("随机列表元素: {}", RandomUtil.randomEle(
                Arrays.asList("Spring", "MyBatis", "Redis", "Kafka", "Hutool")
        ));

        // === 金额计算（BigDecimal精确计算）===
        BigDecimal price = NumberUtil.toBigDecimal("199.99");
        BigDecimal count = NumberUtil.toBigDecimal("3");
        BigDecimal total = NumberUtil.mul(price, count);
        Console.log("总价(精确): ¥{} * {} = ¥{}", price, count, total);
        
        // === 数字转中文 ===
        Console.log("1205 -> 中文数字: {}", NumberUtil.isInteger("1205") 
                ? "一千二百零五" : "非整数");

        // === 获取当前JVM信息 ===
        Console.log("JVM可用处理器: {}", RuntimeUtil.getProcessorCount());
        Console.log("JVM最大内存: {} MB", RuntimeUtil.getMaxMemory() / 1024 / 1024);
    }

    // ========== 内部实体类 ==========

    static class Order {
        private String orderId;
        private String productName;
        private int amount;  // 分为单位
        private String status;

        public Order() {}
        public Order(String orderId, String productName, int amount, String status) {
            this.orderId = orderId;
            this.productName = productName;
            this.amount = amount;
            this.status = status;
        }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    static class OrderCreateRequest {
        private String productName;
        private int quantity;
        private int unitPrice;
        private String buyerName;
        private String buyerPhone;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public int getUnitPrice() { return unitPrice; }
        public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }
        public String getBuyerName() { return buyerName; }
        public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
        public String getBuyerPhone() { return buyerPhone; }
        public void setBuyerPhone(String buyerPhone) { this.buyerPhone = buyerPhone; }

        @Override
        public String toString() {
            return JSONUtil.toJsonStr(this);
        }
    }

    static class OrderDTO {
        private String productName;
        private String buyerName;
        private String buyerPhone;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getBuyerName() { return buyerName; }
        public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
        public String getBuyerPhone() { return buyerPhone; }
        public void setBuyerPhone(String buyerPhone) { this.buyerPhone = buyerPhone; }

        @Override
        public String toString() {
            return JSONUtil.toJsonStr(this);
        }
    }
}
