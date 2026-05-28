package com.example.jackson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Jackson 进阶演示
 *
 * 覆盖内容：
 * 1. 自定义序列化器/反序列化器（BigDecimal精度/脱敏处理）
 * 2. 多态类型处理（@JsonTypeInfo + @JsonSubTypes）
 * 3. 枚举序列化策略（@JsonValue + @JsonCreator）
 * 4. XML 格式处理（XmlMapper）
 * 5. YAML 格式处理（YAMLMapper）
 * 6. 对象 Merge（readerForUpdating）
 * 7. ObjectMapper 高级配置
 *
 * 依赖：jackson-databind / jackson-dataformat-xml / jackson-dataformat-yaml
 */
public class JacksonAdvancedDemo {

    // ============================================================
    //  1. 自定义序列化器：BigDecimal 金额保留2位小数
    // ============================================================
    static class MoneySerializer extends StdSerializer<BigDecimal> {
        public MoneySerializer() { super(BigDecimal.class); }

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider p) throws IOException {
            // 统一输出两位小数字符串，避免科学计数法和精度丢失
            gen.writeString(value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        }
    }

    // 自定义反序列化器：字符串金额 -> BigDecimal
    static class MoneyDeserializer extends StdDeserializer<BigDecimal> {
        public MoneyDeserializer() { super(BigDecimal.class); }

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String text = p.getText();
            return new BigDecimal(text.replaceAll(",", "")); // 处理千分位逗号
        }
    }

    // 使用自定义序列化器的 POJO
    static class Invoice {
        private Long invoiceNo;
        private String buyer;

        @JsonSerialize(using = MoneySerializer.class)
        @JsonDeserialize(using = MoneyDeserializer.class)
        private BigDecimal amount;

        @JsonSerialize(using = MoneySerializer.class)
        @JsonDeserialize(using = MoneyDeserializer.class)
        private BigDecimal tax;

        public Invoice() {}
        public Invoice(Long invoiceNo, String buyer, BigDecimal amount, BigDecimal tax) {
            this.invoiceNo = invoiceNo; this.buyer = buyer;
            this.amount = amount; this.tax = tax;
        }

        public Long getInvoiceNo() { return invoiceNo; }
        public void setInvoiceNo(Long invoiceNo) { this.invoiceNo = invoiceNo; }
        public String getBuyer() { return buyer; }
        public void setBuyer(String buyer) { this.buyer = buyer; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getTax() { return tax; }
        public void setTax(BigDecimal tax) { this.tax = tax; }

        @Override
        public String toString() {
            return "Invoice{no=" + invoiceNo + ", buyer='" + buyer + "', amount=" + amount + ", tax=" + tax + "}";
        }
    }

    // ============================================================
    //  2. 多态类型：支付方式（父类 + 多子类）
    //     @JsonTypeInfo 在JSON中写入类型标识字段
    //     @JsonSubTypes 映射子类
    // ============================================================
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,      // 使用名称（非类名）
        include = JsonTypeInfo.As.PROPERTY,  // 作为额外JSON属性
        property = "type"               // 属性名
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = JacksonAdvancedDemo.AlipayPayment.class, name = "alipay"),
        @JsonSubTypes.Type(value = JacksonAdvancedDemo.WechatPayment.class, name = "wechat"),
        @JsonSubTypes.Type(value = JacksonAdvancedDemo.CardPayment.class, name = "card")
    })
    abstract static class Payment {
        protected double amount;
        protected String currency;

        public Payment() {}
        public Payment(double amount, String currency) {
            this.amount = amount; this.currency = currency;
        }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public abstract String describe();
    }

    static class AlipayPayment extends Payment {
        private String alipayAccount;
        public AlipayPayment() {}
        public AlipayPayment(double amount, String currency, String account) {
            super(amount, currency); this.alipayAccount = account;
        }
        public String getAlipayAccount() { return alipayAccount; }
        public void setAlipayAccount(String a) { this.alipayAccount = a; }
        @Override public String describe() { return "支付宝: " + alipayAccount + " ¥" + amount; }
    }

    static class WechatPayment extends Payment {
        private String openId;
        public WechatPayment() {}
        public WechatPayment(double amount, String currency, String openId) {
            super(amount, currency); this.openId = openId;
        }
        public String getOpenId() { return openId; }
        public void setOpenId(String o) { this.openId = o; }
        @Override public String describe() { return "微信支付: " + openId + " ¥" + amount; }
    }

    static class CardPayment extends Payment {
        private String cardLast4;
        private String bank;
        public CardPayment() {}
        public CardPayment(double amount, String currency, String cardLast4, String bank) {
            super(amount, currency); this.cardLast4 = cardLast4; this.bank = bank;
        }
        public String getCardLast4() { return cardLast4; }
        public void setCardLast4(String c) { this.cardLast4 = c; }
        public String getBank() { return bank; }
        public void setBank(String b) { this.bank = b; }
        @Override public String describe() { return "银行卡 *" + cardLast4 + "(" + bank + ") ¥" + amount; }
    }

    // ============================================================
    //  3. 枚举序列化策略
    // ============================================================
    enum OrderStatus {
        PENDING("待支付", 0),
        PAID("已支付", 1),
        SHIPPED("已发货", 2),
        COMPLETED("已完成", 3),
        CANCELLED("已取消", -1);

        private final String label;
        private final int code;

        OrderStatus(String label, int code) {
            this.label = label; this.code = code;
        }

        @JsonValue  // 序列化时输出 code 而非枚举名
        public int getCode() { return code; }

        // 反序列化时根据 code 还原枚举
        @JsonCreator
        public static OrderStatus fromCode(int code) {
            for (OrderStatus s : values()) {
                if (s.code == code) return s;
            }
            throw new IllegalArgumentException("Unknown code: " + code);
        }

        public String getLabel() { return label; }
    }

    static class Order {
        private Long orderId;
        private OrderStatus status;

        public Order() {}
        public Order(Long orderId, OrderStatus status) {
            this.orderId = orderId; this.status = status;
        }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long o) { this.orderId = o; }
        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus s) { this.status = s; }

        @Override public String toString() {
            return "Order{id=" + orderId + ", status=" + status + "(" + status.getLabel() + ")}";
        }
    }

    // ============================================================
    //  4. XML 格式 POJO
    // ============================================================
    @JacksonXmlRootElement(localName = "Employee")
    static class Employee {
        @JacksonXmlProperty(isAttribute = true)  // 作为XML属性而非子元素
        private Long id;

        private String name;
        private String department;
        private double salary;

        public Employee() {}
        public Employee(Long id, String name, String department, double salary) {
            this.id = id; this.name = name;
            this.department = department; this.salary = salary;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String n) { this.name = n; }
        public String getDepartment() { return department; }
        public void setDepartment(String d) { this.department = d; }
        public double getSalary() { return salary; }
        public void setSalary(double s) { this.salary = s; }

        @Override public String toString() {
            return "Employee{id=" + id + ", name='" + name + "', dept='" + department + "', salary=" + salary + "}";
        }
    }

    // ============================================================
    //  5. YAML 格式 POJO（用于配置文件解析）
    // ============================================================
    static class AppConfig {
        private String appName;
        private String version;
        private DatabaseConfig database;
        private List<String> features;

        public AppConfig() {}

        public String getAppName() { return appName; }
        public void setAppName(String a) { this.appName = a; }
        public String getVersion() { return version; }
        public void setVersion(String v) { this.version = v; }
        public DatabaseConfig getDatabase() { return database; }
        public void setDatabase(DatabaseConfig d) { this.database = d; }
        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> f) { this.features = f; }

        @Override public String toString() {
            return "AppConfig{app=" + appName + ", version=" + version + ", db=" + database + ", features=" + features + "}";
        }
    }

    static class DatabaseConfig {
        private String host;
        private int port;
        private String name;

        public DatabaseConfig() {}
        public String getHost() { return host; }
        public void setHost(String h) { this.host = h; }
        public int getPort() { return port; }
        public void setPort(int p) { this.port = p; }
        public String getName() { return name; }
        public void setName(String n) { this.name = n; }

        @Override public String toString() {
            return "DB{host=" + host + ", port=" + port + ", name=" + name + "}";
        }
    }

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        System.out.println("========== 1. 自定义序列化器（金额精度保护） ==========");
        Invoice invoice = new Invoice(
                20001L, "上海科技有限公司",
                new BigDecimal("9876.5432"),
                new BigDecimal("1284.94")
        );
        String invoiceJson = mapper.writeValueAsString(invoice);
        System.out.println("Invoice JSON（金额格式化为2位小数）:\n" + invoiceJson);
        Invoice parsedInvoice = mapper.readValue(invoiceJson, Invoice.class);
        System.out.println("反序列化: " + parsedInvoice);

        System.out.println("\n========== 2. 多态类型处理（支付方式） ==========");
        List<Payment> payments = Arrays.asList(
                new AlipayPayment(199.0, "CNY", "user@alipay.com"),
                new WechatPayment(299.0, "CNY", "o-abc123xyz"),
                new CardPayment(499.0, "CNY", "6789", "工商银行")
        );

        // 序列化：每个对象自动加 "type" 字段
        String paymentsJson = mapper.writeValueAsString(payments);
        System.out.println("多态支付列表 JSON:\n" + paymentsJson);

        // 反序列化：根据 "type" 字段自动还原正确子类
        List<Payment> parsedPayments = mapper.readValue(paymentsJson,
                mapper.getTypeFactory().constructCollectionType(List.class, Payment.class));
        for (Payment p : parsedPayments) {
            // p 是正确的子类实例
            System.out.println("  解析: " + p.getClass().getSimpleName() + " -> " + p.describe());
        }

        System.out.println("\n========== 3. 枚举序列化（输出code，输入code）==========");
        Order order1 = new Order(3001L, OrderStatus.SHIPPED);
        Order order2 = new Order(3002L, OrderStatus.CANCELLED);
        System.out.println("Order1 JSON: " + mapper.writeValueAsString(order1));
        System.out.println("Order2 JSON: " + mapper.writeValueAsString(order2));

        // 反序列化时从 code 还原枚举
        String orderJson = "{\"orderId\":3003,\"status\":1}";
        Order parsedOrder = mapper.readValue(orderJson, Order.class);
        System.out.println("从code=1反序列化: " + parsedOrder);

        System.out.println("\n========== 4. XML 格式处理（XmlMapper） ==========");
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);

        Employee emp = new Employee(101L, "张伟", "后端研发部", 25000.0);
        String xmlStr = xmlMapper.writeValueAsString(emp);
        System.out.println("Employee XML:\n" + xmlStr);

        Employee parsedEmp = xmlMapper.readValue(xmlStr, Employee.class);
        System.out.println("从XML解析: " + parsedEmp);

        System.out.println("\n========== 5. YAML 格式处理（YAMLMapper） ==========");
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        // 构建一个配置对象，输出为 YAML
        AppConfig config = new AppConfig();
        config.setAppName("my-service");
        config.setVersion("1.2.3");
        DatabaseConfig db = new DatabaseConfig();
        db.setHost("localhost");
        db.setPort(3306);
        db.setName("prod_db");
        config.setDatabase(db);
        config.setFeatures(Arrays.asList("oauth2", "websocket", "rate-limit"));

        String yamlStr = yamlMapper.writeValueAsString(config);
        System.out.println("AppConfig YAML:\n" + yamlStr);

        // 从 YAML 反序列化
        AppConfig parsedConfig = yamlMapper.readValue(yamlStr, AppConfig.class);
        System.out.println("从YAML解析: " + parsedConfig);

        System.out.println("\n========== 6. 对象局部更新（readerForUpdating / Merge） ==========");
        // 场景：PATCH请求只更新部分字段，不覆盖其余字段
        Employee original = new Employee(201L, "李娜", "产品部", 18000.0);
        System.out.println("更新前: " + original);

        // 只传入要修改的字段
        String patchJson = "{\"salary\":20000.0}";
        ObjectReader updater = mapper.readerForUpdating(original);
        updater.readValue(patchJson);  // 直接修改 original 对象
        System.out.println("Merge后（仅salary更新，其余保留）: " + original);

        System.out.println("\n========== 7. ObjectMapper 高级配置示例 ==========");
        ObjectMapper strictMapper = new ObjectMapper();
        // 遇到未知字段时抛异常（默认是忽略）
        strictMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        // 允许JSON含注释（// 和 /* */）
        strictMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS, true);
        // 允许字段名不带引号（宽松模式）
        strictMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 空字符串反序列化为 null
        strictMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        System.out.println("高级配置已展示（未抛异常代表设置成功）");

        System.out.println("\n========== 8. 通过 SimpleModule 注册全局自定义序列化器 ==========");
        SimpleModule module = new SimpleModule("MoneyModule");
        module.addSerializer(BigDecimal.class, new MoneySerializer());
        module.addDeserializer(BigDecimal.class, new MoneyDeserializer());
        ObjectMapper moneyMapper = new ObjectMapper();
        moneyMapper.registerModule(module);

        BigDecimal amount = new BigDecimal("12345.678901");
        System.out.println("全局BigDecimal序列化: " + moneyMapper.writeValueAsString(amount));
    }
}
