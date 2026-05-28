package com.example.jackson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Jackson 基础演示
 *
 * 覆盖内容：
 * 1. ObjectMapper 基本序列化/反序列化
 * 2. 常用注解：@JsonProperty / @JsonIgnore / @JsonAlias / @JsonFormat
 * 3. 泛型集合处理（TypeReference）
 * 4. 树型模型（JsonNode / ObjectNode / ArrayNode）
 * 5. Java 8 日期时间（LocalDateTime）处理
 *
 * 依赖：jackson-databind 2.17.x, jackson-datatype-jsr310
 */
public class JacksonBasicDemo {

    // ============================================================
    //  1. 普通 POJO（无注解，字段名即 JSON key）
    // ============================================================
    static class Product {
        private Long id;
        private String name;
        private double price;
        private boolean available;

        // Jackson 默认需要无参构造器（或配置DeserializationFeature）
        public Product() {}

        public Product(Long id, String name, double price, boolean available) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.available = available;
        }

        // Getter / Setter（Jackson默认通过JavaBean规范读写属性）
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }

        @Override
        public String toString() {
            return "Product{id=" + id + ", name='" + name + "', price=" + price + ", available=" + available + "}";
        }
    }

    // ============================================================
    //  2. 带注解的 POJO
    // ============================================================
    @JsonIgnoreProperties(ignoreUnknown = true)  // 忽略JSON中多余字段，防止反序列化失败
    static class UserDTO {

        @JsonProperty("user_id")                 // JSON字段名映射（驼峰 <-> 下划线）
        private Long userId;

        @JsonProperty("full_name")
        private String fullName;

        @JsonIgnore                              // 序列化/反序列化时完全忽略此字段
        private String password;

        @JsonAlias({"phone", "mobile", "tel"})   // 反序列化时支持多个别名
        private String phoneNumber;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")  // 日期格式化
        private Date createdAt;

        @JsonInclude(JsonInclude.Include.NON_NULL)     // 为null时不输出该字段
        private String remark;

        public UserDTO() {}

        public UserDTO(Long userId, String fullName, String password, String phoneNumber) {
            this.userId = userId;
            this.fullName = fullName;
            this.password = password;
            this.phoneNumber = phoneNumber;
            this.createdAt = new Date();
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }

        @Override
        public String toString() {
            return "UserDTO{userId=" + userId + ", fullName='" + fullName + "', phone='" + phoneNumber + "'}";
        }
    }

    // ============================================================
    //  3. Java 8 日期时间 POJO
    // ============================================================
    static class OrderDTO {
        private Long orderId;
        private String status;
        private LocalDateTime createdAt;
        private LocalDate deliveryDate;

        public OrderDTO() {}

        public OrderDTO(Long orderId, String status, LocalDateTime createdAt, LocalDate deliveryDate) {
            this.orderId = orderId;
            this.status = status;
            this.createdAt = createdAt;
            this.deliveryDate = deliveryDate;
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDate getDeliveryDate() { return deliveryDate; }
        public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }

        @Override
        public String toString() {
            return "OrderDTO{orderId=" + orderId + ", status='" + status
                    + "', createdAt=" + createdAt + ", deliveryDate=" + deliveryDate + "}";
        }
    }

    public static void main(String[] args) throws JsonProcessingException {

        // ---- 创建 ObjectMapper（线程安全，复用） ----
        ObjectMapper mapper = new ObjectMapper();
        // 美化输出
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // 注册 Java 8 日期时间模块
        mapper.registerModule(new JavaTimeModule());
        // 禁用将日期输出为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        System.out.println("========== 1. 基础序列化（对象 -> JSON） ==========");
        Product product = new Product(1L, "苹果 MacBook Pro", 14999.99, true);
        String productJson = mapper.writeValueAsString(product);
        System.out.println("Product JSON:\n" + productJson);

        System.out.println("\n========== 2. 基础反序列化（JSON -> 对象） ==========");
        String json = "{\"id\":2,\"name\":\"华为 MateBook\",\"price\":9999.0,\"available\":false}";
        Product fromJson = mapper.readValue(json, Product.class);
        System.out.println("Parsed: " + fromJson);

        System.out.println("\n========== 3. @JsonProperty / @JsonIgnore 注解演示 ==========");
        UserDTO user = new UserDTO(1001L, "张三", "secret123", "13800138000");
        String userJson = mapper.writeValueAsString(user);
        System.out.println("UserDTO JSON（password已被忽略，字段名已映射）:\n" + userJson);

        // 反序列化时 @JsonAlias 的别名支持
        String aliasJson = "{\"user_id\":1002,\"full_name\":\"李四\",\"mobile\":\"13900139000\"}";
        UserDTO fromAlias = mapper.readValue(aliasJson, UserDTO.class);
        System.out.println("通过别名 mobile 解析 phoneNumber: " + fromAlias);

        System.out.println("\n========== 4. 泛型集合（TypeReference） ==========");
        List<Product> products = Arrays.asList(
                new Product(1L, "iPhone 15", 7999.0, true),
                new Product(2L, "Samsung S24", 6999.0, true),
                new Product(3L, "OPPO Find X7", 5999.0, false)
        );
        String listJson = mapper.writeValueAsString(products);
        System.out.println("List JSON:\n" + listJson);

        // 使用 TypeReference 保留泛型信息
        List<Product> parsedList = mapper.readValue(listJson, new TypeReference<List<Product>>() {});
        System.out.println("解析列表数量: " + parsedList.size());

        // Map 类型
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("host", "localhost");
        config.put("port", 3306);
        config.put("maxConnections", 50);
        String configJson = mapper.writeValueAsString(config);
        System.out.println("\nConfig JSON:\n" + configJson);
        Map<String, Object> parsedConfig = mapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        System.out.println("host: " + parsedConfig.get("host") + ", port: " + parsedConfig.get("port"));

        System.out.println("\n========== 5. 树型模型（JsonNode） ==========");
        // 读取为 JsonNode，适合处理未知结构的 JSON
        String complexJson = "{"
                + "\"userId\":100,"
                + "\"profile\":{\"name\":\"王五\",\"age\":30},"
                + "\"tags\":[\"java\",\"spring\",\"kotlin\"]"
                + "}";
        JsonNode root = mapper.readTree(complexJson);

        System.out.println("userId: " + root.get("userId").asLong());
        System.out.println("profile.name: " + root.path("profile").path("name").asText());
        System.out.println("tags[0]: " + root.path("tags").get(0).asText());
        System.out.println("不存在的字段（用path避免NPE）: '" + root.path("notExist").asText("默认值") + "'");

        // 构建 ObjectNode（动态拼 JSON）
        ObjectNode node = mapper.createObjectNode();
        node.put("code", 200);
        node.put("message", "success");
        ArrayNode dataArray = node.putArray("data");
        dataArray.addObject().put("id", 1).put("name", "item1");
        dataArray.addObject().put("id", 2).put("name", "item2");
        System.out.println("\n动态构建 JSON:\n" + mapper.writeValueAsString(node));

        System.out.println("\n========== 6. Java 8 日期时间处理 ==========");
        OrderDTO order = new OrderDTO(
                5001L, "PENDING",
                LocalDateTime.of(2026, 5, 28, 16, 30, 0),
                LocalDate.of(2026, 6, 1)
        );
        String orderJson = mapper.writeValueAsString(order);
        System.out.println("OrderDTO JSON:\n" + orderJson);

        OrderDTO parsedOrder = mapper.readValue(orderJson, OrderDTO.class);
        System.out.println("Parsed OrderDTO: " + parsedOrder);

        System.out.println("\n========== 7. 忽略未知字段（@JsonIgnoreProperties） ==========");
        // JSON中有多余字段，不会抛异常
        String extraFieldJson = "{\"user_id\":999,\"full_name\":\"赵六\",\"extra_field\":\"ignored\",\"phone\":\"18800001234\"}";
        UserDTO withExtra = mapper.readValue(extraFieldJson, UserDTO.class);
        System.out.println("含多余字段的JSON解析成功: " + withExtra);
    }
}
