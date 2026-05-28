package com.example.jackson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Jackson 实战演示
 *
 * 覆盖内容：
 * 1. 统一 API 响应体封装（泛型 Result<T>）
 * 2. 流式 API（JsonParser/JsonGenerator）— 超大 JSON 文件解析
 * 3. CSV 格式处理（CsvMapper）— 读写 CSV 数据
 * 4. 字段脱敏序列化（手机号/邮箱/身份证）
 * 5. Jackson 与 Spring Boot 集成指南（代码注释形式）
 * 6. 常见陷阱与最佳实践
 *
 * 依赖：jackson-databind / jackson-dataformat-csv / jackson-datatype-jsr310
 */
public class JacksonPracticalDemo {

    // ============================================================
    //  1. 统一 API 响应体（泛型）
    // ============================================================
    static class Result<T> {
        private int code;
        private String message;
        private T data;

        @JsonIgnore
        private boolean success;

        public Result() {}

        public static <T> Result<T> ok(T data) {
            Result<T> r = new Result<>();
            r.code = 200; r.message = "success"; r.data = data; r.success = true;
            return r;
        }

        public static <T> Result<T> fail(int code, String msg) {
            Result<T> r = new Result<>();
            r.code = code; r.message = msg; r.success = false;
            return r;
        }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        @Override public String toString() {
            return "Result{code=" + code + ", message='" + message + "', data=" + data + "}";
        }
    }

    static class UserProfile {
        private Long userId;
        private String username;
        private String email;
        private String phone;

        public UserProfile() {}
        public UserProfile(Long userId, String username, String email, String phone) {
            this.userId = userId; this.username = username;
            this.email = email; this.phone = phone;
        }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        @Override public String toString() {
            return "UserProfile{userId=" + userId + ", username='" + username + "', email='" + email + "'}";
        }
    }

    // ============================================================
    //  2. 手机号脱敏序列化器
    // ============================================================
    static class PhoneMaskSerializer extends com.fasterxml.jackson.databind.ser.std.StdSerializer<String> {
        public PhoneMaskSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider p) throws IOException {
            if (value == null) { gen.writeNull(); return; }
            // 手机号格式：138****8000
            if (value.length() == 11) {
                gen.writeString(value.substring(0, 3) + "****" + value.substring(7));
            } else {
                gen.writeString(value);
            }
        }
    }

    // 脱敏后的用户信息（用于对外接口）
    static class MaskedUser {
        private Long userId;
        private String username;

        @JsonSerialize(using = PhoneMaskSerializer.class)
        private String phone;

        // 邮箱脱敏：u***@example.com
        @JsonSerialize(using = EmailMaskSerializer.class)
        private String email;

        public MaskedUser() {}
        public MaskedUser(Long userId, String username, String phone, String email) {
            this.userId = userId; this.username = username;
            this.phone = phone; this.email = email;
        }
        public Long getUserId() { return userId; }
        public void setUserId(Long u) { this.userId = u; }
        public String getUsername() { return username; }
        public void setUsername(String u) { this.username = u; }
        public String getPhone() { return phone; }
        public void setPhone(String p) { this.phone = p; }
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
    }

    static class EmailMaskSerializer extends com.fasterxml.jackson.databind.ser.std.StdSerializer<String> {
        public EmailMaskSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider p) throws IOException {
            if (value == null) { gen.writeNull(); return; }
            int atIndex = value.indexOf('@');
            if (atIndex > 1) {
                // zhangsan@example.com -> z*****n@example.com
                String prefix = value.substring(0, 1) + "*****" + value.charAt(atIndex - 1);
                gen.writeString(prefix + value.substring(atIndex));
            } else {
                gen.writeString(value);
            }
        }
    }

    // ============================================================
    //  3. CSV 行记录 POJO
    // ============================================================
    static class SalesRecord {
        private String date;
        private String productName;
        private int quantity;
        private double unitPrice;
        private double totalAmount;

        public SalesRecord() {}
        public SalesRecord(String date, String productName, int quantity, double unitPrice, double totalAmount) {
            this.date = date; this.productName = productName;
            this.quantity = quantity; this.unitPrice = unitPrice;
            this.totalAmount = totalAmount;
        }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
        public double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

        @Override public String toString() {
            return "Sales{date=" + date + ", product=" + productName + ", qty=" + quantity
                    + ", price=" + unitPrice + ", total=" + totalAmount + "}";
        }
    }

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        System.out.println("========== 1. 统一 API 响应体（泛型 Result<T>） ==========");
        UserProfile profile = new UserProfile(1001L, "zhangsan", "zhangsan@example.com", "13812345678");
        Result<UserProfile> okResult = Result.ok(profile);
        String okJson = mapper.writeValueAsString(okResult);
        System.out.println("成功响应:\n" + okJson);

        Result<Object> failResult = Result.fail(404, "用户不存在");
        System.out.println("失败响应:\n" + mapper.writeValueAsString(failResult));

        // 反序列化带泛型的响应
        Result<UserProfile> parsedResult = mapper.readValue(okJson,
                new TypeReference<Result<UserProfile>>() {});
        System.out.println("反序列化泛型结果: " + parsedResult);

        System.out.println("\n========== 2. 手机号 & 邮箱脱敏序列化 ==========");
        MaskedUser maskedUser = new MaskedUser(
                1002L, "lisi", "13900139000", "lisi_longname@company.com"
        );
        String maskedJson = mapper.writeValueAsString(maskedUser);
        System.out.println("脱敏后 JSON:\n" + maskedJson);

        System.out.println("\n========== 3. 流式 API 生成大 JSON ==========");
        // JsonGenerator：写入大量数据，内存友好（不构建完整对象树）
        StringWriter sw = new StringWriter();
        JsonFactory factory = mapper.getFactory();
        try (JsonGenerator gen = factory.createGenerator(sw)) {
            gen.useDefaultPrettyPrinter();
            gen.writeStartObject();
            gen.writeStringField("reportType", "monthly_sales");
            gen.writeNumberField("totalCount", 10000);
            gen.writeArrayFieldStart("items");
            for (int i = 1; i <= 5; i++) {
                gen.writeStartObject();
                gen.writeNumberField("id", i);
                gen.writeStringField("name", "产品" + i);
                gen.writeNumberField("sales", i * 1000);
                gen.writeEndObject();
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
        System.out.println("流式生成 JSON（示例5条）:\n" + sw.toString().substring(0, Math.min(sw.toString().length(), 300)) + "...");

        System.out.println("\n========== 4. 流式 API 解析大 JSON ==========");
        // JsonParser：逐 token 解析，适合 GB 级 JSON 文件
        String bigJson = "[{\"id\":1,\"name\":\"item1\",\"price\":100.0},"
                + "{\"id\":2,\"name\":\"item2\",\"price\":200.0},"
                + "{\"id\":3,\"name\":\"item3\",\"price\":300.0}]";

        try (JsonParser parser = factory.createParser(bigJson)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("预期是数组");
            }
            int count = 0;
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                // 读取单个对象
                Map<String, Object> item = mapper.readValue(parser, new TypeReference<Map<String, Object>>() {});
                System.out.println("  流式读取: " + item);
                count++;
            }
            System.out.println("共解析 " + count + " 条记录");
        }

        System.out.println("\n========== 5. CSV 格式处理（CsvMapper） ==========");
        CsvMapper csvMapper = new CsvMapper();

        // 定义 CSV Schema（列顺序）
        CsvSchema schema = CsvSchema.builder()
                .addColumn("date")
                .addColumn("productName")
                .addNumberColumn("quantity")
                .addNumberColumn("unitPrice")
                .addNumberColumn("totalAmount")
                .build()
                .withHeader();  // 第一行为表头

        List<SalesRecord> records = Arrays.asList(
                new SalesRecord("2026-05-01", "iPhone 15", 10, 7999.0, 79990.0),
                new SalesRecord("2026-05-01", "MacBook Pro", 3, 14999.0, 44997.0),
                new SalesRecord("2026-05-02", "iPad Air", 5, 5299.0, 26495.0)
        );

        // POJO List -> CSV 字符串
        String csvOutput = csvMapper
                .writerFor(new TypeReference<List<SalesRecord>>() {})
                .with(schema)
                .writeValueAsString(records);
        System.out.println("CSV 输出:\n" + csvOutput);

        // CSV 字符串 -> POJO List
        List<SalesRecord> parsedRecords = csvMapper
                .readerFor(SalesRecord.class)
                .with(schema)
                .<SalesRecord>readValues(csvOutput)
                .readAll();
        System.out.println("从CSV解析 " + parsedRecords.size() + " 条记录:");
        for (SalesRecord rec : parsedRecords) {
            System.out.println("  " + rec);
        }

        System.out.println("\n========== 6. 动态JSON构建（ObjectNode） ==========");
        // 不依赖POJO，动态拼 JSON（适合数据库查询结果等动态结构）
        ObjectMapper nodeMapper = new ObjectMapper();
        List<Map<String, Object>> rows = Arrays.asList(
                new LinkedHashMap<String, Object>() {{ put("dept", "研发部"); put("count", 20); put("avgSalary", 22000); }},
                new LinkedHashMap<String, Object>() {{ put("dept", "产品部"); put("count", 8);  put("avgSalary", 18000); }},
                new LinkedHashMap<String, Object>() {{ put("dept", "运营部"); put("count", 15); put("avgSalary", 15000); }}
        );
        ObjectNode report = nodeMapper.createObjectNode();
        report.put("reportName", "部门人员统计");
        report.put("generatedAt", LocalDateTime.now().toString());
        report.set("departments", nodeMapper.valueToTree(rows));
        System.out.println("动态构建部门报表:\n" + nodeMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));

        System.out.println("\n========== 7. Spring Boot 集成要点（注释说明）==========");
        System.out.println("Spring Boot 默认已内置 Jackson，无需手动引入 jackson-databind。");
        System.out.println("常用配置（application.yml）：");
        System.out.println("  spring.jackson.serialization.indent-output: true");
        System.out.println("  spring.jackson.serialization.write-dates-as-timestamps: false");
        System.out.println("  spring.jackson.deserialization.fail-on-unknown-properties: false");
        System.out.println("  spring.jackson.default-property-inclusion: non_null");
        System.out.println("  spring.jackson.time-zone: Asia/Shanghai");
        System.out.println("  spring.jackson.date-format: yyyy-MM-dd HH:mm:ss");
        System.out.println("\n自定义 Jackson Bean 配置：");
        System.out.println("  @Bean public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {");
        System.out.println("      return builder.build().registerModule(new JavaTimeModule());");
        System.out.println("  }");

        System.out.println("\n========== 8. 常见陷阱提示 ==========");
        System.out.println("✅ ObjectMapper 是线程安全的，应声明为单例（Bean/static final）");
        System.out.println("❌ 不要在每次请求中 new ObjectMapper()，非常耗资源");
        System.out.println("✅ 反序列化时加 @JsonIgnoreProperties(ignoreUnknown=true)，防版本兼容问题");
        System.out.println("❌ LocalDateTime 必须注册 JavaTimeModule，否则抛 InvalidDefinitionException");
        System.out.println("✅ 泛型集合反序列化必须用 TypeReference，不能用 List.class");
        System.out.println("❌ @JsonInclude(NON_NULL) 在类级别才能全量生效，字段级只影响单个字段");
    }
}
