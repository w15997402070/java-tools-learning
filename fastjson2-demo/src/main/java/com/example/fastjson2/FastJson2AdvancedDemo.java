package com.example.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.*;

/**
 * Fastjson2 高级功能演示
 * 涵盖：注解使用、自定义序列化、枚举处理、循环引用处理
 */
public class FastJson2AdvancedDemo {

    // ========== 演示 1：注解使用 ==========

    /**
     * 使用 @JSONField 注解控制序列化行为
     */
    static class Product {
        // 指定序列化时的字段名
        @JSONField(name = "product_name")
        private String name;

        // 指定日期格式
        @JSONField(format = "yyyy-MM-dd HH:mm:ss", name = "create_time")
        private Date createTime;

        // 序列化时忽略该字段
        @JSONField(serialize = false)
        private String secretKey;

        // 反序列化时忽略该字段
        @JSONField(deserialize = false)
        private String internalId;

        // 指定字段顺序
        @JSONField(ordinal = 1)
        private Double price;

        @JSONField(ordinal = 2)
        private Integer stock;

        public Product() {}

        public Product(String name, Date createTime, String secretKey, String internalId, Double price, Integer stock) {
            this.name = name;
            this.createTime = createTime;
            this.secretKey = secretKey;
            this.internalId = internalId;
            this.price = price;
            this.stock = stock;
        }

        // getter 和 setter
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getInternalId() { return internalId; }
        public void setInternalId(String internalId) { this.internalId = internalId; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
    }

    // ========== 演示 2：枚举处理 ==========

    /**
     * 枚举定义，使用 @JSONField 控制序列化
     */
    enum OrderStatus {
        @JSONField(name = "PENDING")
        PENDING(0, "待支付"),

        @JSONField(name = "PAID")
        PAID(1, "已支付"),

        @JSONField(name = "SHIPPED")
        SHIPPED(2, "已发货"),

        @JSONField(name = "COMPLETED")
        COMPLETED(3, "已完成");

        private final int code;
        private final String desc;

        OrderStatus(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() { return code; }
        public String getDesc() { return desc; }
    }

    static class Order {
        private String orderId;
        private OrderStatus status;
        private Double amount;

        public Order() {}

        public Order(String orderId, OrderStatus status, Double amount) {
            this.orderId = orderId;
            this.status = status;
            this.amount = amount;
        }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
    }

    // ========== 演示 3：循环引用处理 ==========

    /**
     * 带有父子关系的节点类（可能产生循环引用）
     */
    static class TreeNode {
        private String name;
        private TreeNode parent;
        private List<TreeNode> children;

        public TreeNode() {}

        public TreeNode(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public TreeNode getParent() { return parent; }
        public void setParent(TreeNode parent) { this.parent = parent; }
        public List<TreeNode> getChildren() { return children; }
        public void setChildren(List<TreeNode> children) { this.children = children; }
    }

    public static void main(String[] args) {
        System.out.println("=== Fastjson2 高级功能演示 ===\n");

        demoAnnotation();
        demoEnumHandling();
        demoCircularReference();
        demoFeatureConfig();
    }

    /**
     * 演示 1：@JSONField 注解的使用
     */
    static void demoAnnotation() {
        System.out.println("--- 1. @JSONField 注解使用 ---");

        Product product = new Product(
                "MacBook Pro",
                new Date(),
                "secret-123456",
                "internal-001",
                12999.00,
                50
        );

        // 默认序列化（会应用注解规则）
        String json = JSON.toJSONString(product, JSONWriter.Feature.PrettyFormat);
        System.out.println("使用注解后的序列化结果：");
        System.out.println(json);

        // 验证字段是否被忽略
        String jsonCompact = JSON.toJSONString(product);
        System.out.println("\n紧凑格式（验证 serialize=false 字段是否被忽略）：");
        System.out.println(jsonCompact);

        System.out.println();
    }

    /**
     * 演示 2：枚举处理
     */
    static void demoEnumHandling() {
        System.out.println("--- 2. 枚举处理 ---");

        Order order = new Order("ORD-20240101-001", OrderStatus.PAID, 299.99);

        // 默认枚举序列化（序列化的是枚举名称）
        String json = JSON.toJSONString(order, JSONWriter.Feature.PrettyFormat);
        System.out.println("枚举序列化结果（默认）：");
        System.out.println(json);

        // 反序列化枚举
        Order deserialized = JSON.parseObject(json, Order.class);
        System.out.println("\n反序列化后订单状态：");
        System.out.println("status: " + deserialized.getStatus());
        System.out.println("status desc: " + deserialized.getStatus().getDesc());

        System.out.println();
    }

    /**
     * 演示 3：循环引用处理
     * Fastjson2 默认会检测循环引用并用引用标识代替
     */
    static void demoCircularReference() {
        System.out.println("--- 3. 循环引用处理 ---");

        // 构建一个简单的树结构
        TreeNode root = new TreeNode("root");
        TreeNode child1 = new TreeNode("child1");
        TreeNode child2 = new TreeNode("child2");

        child1.setParent(root);
        child2.setParent(root);
        root.setChildren(Arrays.asList(child1, child2));

        // 默认处理：Fastjson2 会检测到循环引用并替换为引用标识
        String jsonDefault = JSON.toJSONString(root);
        System.out.println("默认处理循环引用：");
        System.out.println(jsonDefault);

        // 禁用循环引用检测（可能产生 StackOverflowError，仅演示配置方式）
        System.out.println("\n注意：禁用循环引用检测可能在复杂场景下导致栈溢出");
        System.out.println("可以通过 JSONWriter.Feature.DisableCircularCheck 来控制");

        System.out.println();
    }

    /**
     * 演示 4：Feature 配置（序列化/反序列化特性）
     */
    static void demoFeatureConfig() {
        System.out.println("--- 4. Feature 配置 ---");

        Map<String, Object> map = new HashMap<>();
        map.put("name", "张三");
        map.put("age", 28);
        map.put("active", true);
        map.put("nullValue", null);

        // 默认：Map 序列化时 null 值字段会被输出
        String jsonWithNull = JSON.toJSONString(map);
        System.out.println("包含 null 值字段：");
        System.out.println(jsonWithNull);

        // 注意：Fastjson2 默认会序列化 null 值字段
        // 如需忽略特定字段，使用 @JSONField(serialize=false) 注解
        System.out.println("\n提示：Fastjson2 默认会序列化 null 值字段");
        System.out.println("如需忽略特定字段，使用 @JSONField(serialize=false) 注解");

        // 配置：输出时保持 Map 的 key 按字母排序
        String jsonSorted = JSON.toJSONString(map,
                JSONWriter.Feature.MapSortField);
        System.out.println("\nMap key 按字母排序：");
        System.out.println(jsonSorted);

        // 配置：允许读取时字段名不区分大小写
        String jsonInput = "{\"Name\":\"李四\",\"AGE\":30}";
        Map<String, Object> parsedMap = JSON.parseObject(jsonInput,
                new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() {},
                com.alibaba.fastjson2.JSONReader.Feature.FieldBased,
                com.alibaba.fastjson2.JSONReader.Feature.IgnoreSetNullValue);
        System.out.println("\n不区分大小写反序列化：");
        System.out.println(parsedMap);

        System.out.println();
    }
}
