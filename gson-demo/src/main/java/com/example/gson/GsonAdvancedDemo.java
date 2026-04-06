package com.example.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Gson高级特性演示
 *
 * <p>涵盖：
 * <ul>
 *   <li>@SerializedName - 字段名映射（JSON字段名与Java字段名不同）</li>
 *   <li>@Expose - 精确控制哪些字段参与序列化/反序列化</li>
 *   <li>FieldNamingPolicy - 命名策略（驼峰/下划线/大写等）</li>
 *   <li>自定义TypeAdapter - 处理特殊类型（如Date）</li>
 *   <li>ExclusionStrategy - 自定义排除策略（按注解/字段名）</li>
 *   <li>null值处理策略</li>
 * </ul>
 */
public class GsonAdvancedDemo {

    // ====================================================
    // 自定义注解：用于演示ExclusionStrategy
    // ====================================================

    /**
     * 自定义注解：标注此注解的字段在序列化时会被忽略
     * 类似于 @JsonIgnore (Jackson) 的自定义版本
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Sensitive {
        // 标记为敏感数据，序列化时排除
    }

    // ====================================================
    // 模型类
    // ====================================================

    /**
     * 演示 @SerializedName：当JSON字段名与Java命名规范不同时使用
     * 常见场景：后端返回 user_name, create_time 等下划线风格
     */
    static class ApiUser {
        @SerializedName("user_id")           // JSON中是 user_id，Java中是 userId
        private int userId;

        @SerializedName(value = "user_name", alternate = {"userName", "name"})  // 支持多个备用名
        private String userName;

        @SerializedName("create_time")
        private String createTime;

        @SerializedName("is_active")
        private boolean isActive;

        public ApiUser(int userId, String userName, String createTime, boolean isActive) {
            this.userId = userId;
            this.userName = userName;
            this.createTime = createTime;
            this.isActive = isActive;
        }

        @Override
        public String toString() {
            return "ApiUser{userId=" + userId + ", userName='" + userName
                    + "', createTime='" + createTime + "', isActive=" + isActive + "}";
        }
    }

    /**
     * 演示 @Expose：精确控制序列化行为
     * 必须配合 GsonBuilder.excludeFieldsWithoutExposeAnnotation() 使用
     */
    static class SecureUser {
        @Expose(serialize = true, deserialize = true)   // 序列化+反序列化都参与
        private int id;

        @Expose(serialize = true, deserialize = true)
        private String name;

        @Expose(serialize = false, deserialize = true)  // 只参与反序列化，不参与序列化（如密码）
        private String password;

        @Expose(serialize = true, deserialize = false)  // 只参与序列化，不参与反序列化
        private String displayName;

        // 没有 @Expose 注解：完全被忽略
        private String internalCode;

        @Sensitive  // 自定义注解，用于ExclusionStrategy演示
        private String secretKey;

        public SecureUser(int id, String name, String password) {
            this.id = id;
            this.name = name;
            this.password = password;
            this.displayName = "展示名: " + name;
            this.internalCode = "INTERNAL-001";
            this.secretKey = "TOP-SECRET-KEY";
        }

        @Override
        public String toString() {
            return "SecureUser{id=" + id + ", name='" + name + "', password='" + password + "'}";
        }
    }

    /**
     * 演示自定义Date类型适配器
     */
    static class EventRecord {
        private String eventName;
        private Date eventDate;
        private Date createdAt;

        public EventRecord(String eventName, Date eventDate, Date createdAt) {
            this.eventName = eventName;
            this.eventDate = eventDate;
            this.createdAt = createdAt;
        }

        @Override
        public String toString() {
            return "EventRecord{eventName='" + eventName + "', eventDate=" + eventDate + "}";
        }
    }

    // ====================================================
    // 自定义TypeAdapter：Date类型处理
    // ====================================================

    /**
     * 自定义Date序列化/反序列化适配器
     * Gson默认对Date的处理不够友好，通常需要自定义
     */
    static class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            // Date → "yyyy-MM-dd HH:mm:ss" 格式字符串
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            return new JsonPrimitive(sdf.format(src));
        }

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            // "yyyy-MM-dd HH:mm:ss" 字符串 → Date
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                return sdf.parse(json.getAsString());
            } catch (ParseException e) {
                throw new JsonParseException("无法解析日期: " + json.getAsString(), e);
            }
        }
    }

    // ====================================================
    // 演示方法
    // ====================================================

    /**
     * 演示1：@SerializedName 字段名映射
     */
    static void demo01SerializedName() {
        System.out.println("\n========== Demo01: @SerializedName 字段名映射 ==========");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // 1.1 序列化：Java驼峰 → JSON下划线
        ApiUser user = new ApiUser(101, "张三", "2024-01-15 10:30:00", true);
        String json = gson.toJson(user);
        System.out.println("序列化（驼峰→下划线）:\n" + json);

        // 1.2 反序列化：支持多个备用字段名（alternate）
        String json1 = "{\"user_id\":102,\"user_name\":\"李四\",\"create_time\":\"2024-02-20\",\"is_active\":false}";
        String json2 = "{\"user_id\":103,\"userName\":\"王五\",\"create_time\":\"2024-03-10\",\"is_active\":true}";
        String json3 = "{\"user_id\":104,\"name\":\"赵六\",\"create_time\":\"2024-04-01\",\"is_active\":true}";

        System.out.println("\n反序列化 user_name: " + gson.fromJson(json1, ApiUser.class));
        System.out.println("反序列化 userName: " + gson.fromJson(json2, ApiUser.class));
        System.out.println("反序列化 name: " + gson.fromJson(json3, ApiUser.class));
    }

    /**
     * 演示2：FieldNamingPolicy 命名策略
     */
    static void demo02FieldNamingPolicy() {
        System.out.println("\n========== Demo02: FieldNamingPolicy 命名策略 ==========");

        // Java字段名为驼峰，用不同策略输出JSON
        class Product {
            String productName = "商品名称";
            double unitPrice = 99.9;
            int stockCount = 100;
            boolean isAvailable = true;
        }

        Product product = new Product();

        // 策略1：下划线分隔（LOWER_CASE_WITH_UNDERSCORES）
        Gson gsonUnderscore = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        System.out.println("下划线策略: " + gsonUnderscore.toJson(product));

        // 策略2：连字符分隔（LOWER_CASE_WITH_DASHES）
        Gson gsonDash = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .create();
        System.out.println("连字符策略: " + gsonDash.toJson(product));

        // 策略3：首字母大写（UPPER_CAMEL_CASE）
        Gson gsonUpper = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .create();
        System.out.println("首字母大写策略: " + gsonUpper.toJson(product));
    }

    /**
     * 演示3：@Expose 控制字段参与序列化/反序列化
     */
    static void demo03ExposeAnnotation() {
        System.out.println("\n========== Demo03: @Expose 字段控制 ==========");

        // 必须使用 excludeFieldsWithoutExposeAnnotation() 才能激活 @Expose
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();

        SecureUser user = new SecureUser(1, "admin", "P@ssw0rd");

        // 序列化：password不出现（serialize=false），internalCode/secretKey不出现（无@Expose）
        String serialized = gson.toJson(user);
        System.out.println("序列化结果（密码不可见）:\n" + serialized);

        // 反序列化：password会被读取
        String json = "{\"id\":2,\"name\":\"test\",\"password\":\"newPassword123\",\"displayName\":\"忽略此字段\"}";
        SecureUser deserialized = gson.fromJson(json, SecureUser.class);
        System.out.println("\n反序列化结果: " + deserialized);
        // displayName 的 deserialize=false，所以即使JSON中有也会被忽略
    }

    /**
     * 演示4：自定义TypeAdapter处理Date类型 + null值策略
     */
    static void demo04DateAdapterAndNullHandling() throws ParseException {
        System.out.println("\n========== Demo04: 自定义Date适配器 + Null处理 ==========");

        // 注册自定义Date适配器
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeNulls()           // 默认null字段不输出，加此选项强制输出null
                .setPrettyPrinting()
                .create();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        EventRecord event = new EventRecord(
                "技术分享大会",
                sdf.parse("2024-06-15 14:00:00"),
                sdf.parse("2024-05-01 09:30:00")
        );

        // 4.1 序列化：Date自动转为格式化字符串
        String json = gson.toJson(event);
        System.out.println("带自定义日期格式的序列化:\n" + json);

        // 4.2 反序列化：格式化字符串自动转回Date
        String jsonStr = "{\"eventName\":\"年度会议\",\"eventDate\":\"2024-12-31 18:00:00\",\"createdAt\":\"2024-11-01 08:00:00\"}";
        EventRecord parsed = gson.fromJson(jsonStr, EventRecord.class);
        System.out.println("\n反序列化结果: " + parsed);

        // 4.3 演示null处理差异
        EventRecord nullEvent = new EventRecord("空日期测试", null, null);
        Gson gsonNoNull = new GsonBuilder().registerTypeAdapter(Date.class, new DateTypeAdapter()).create();
        Gson gsonWithNull = new GsonBuilder().registerTypeAdapter(Date.class, new DateTypeAdapter()).serializeNulls().create();

        System.out.println("\n不序列化null: " + gsonNoNull.toJson(nullEvent));
        System.out.println("序列化null:   " + gsonWithNull.toJson(nullEvent));
    }

    /**
     * 演示5：ExclusionStrategy 自定义排除策略
     */
    static void demo05ExclusionStrategy() {
        System.out.println("\n========== Demo05: ExclusionStrategy 自定义排除 ==========");

        // 排除所有带 @Sensitive 注解的字段
        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        // 排除带有 @Sensitive 注解的字段
                        return f.getAnnotation(Sensitive.class) != null;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false; // 不排除任何类
                    }
                })
                .setPrettyPrinting()
                .create();

        SecureUser user = new SecureUser(1, "admin", "P@ssw0rd");
        System.out.println("排除@Sensitive字段后:\n" + gson.toJson(user));
    }

    public static void main(String[] args) throws ParseException {
        System.out.println("======================================");
        System.out.println("   Gson高级特性演示 (Day 02)");
        System.out.println("======================================");

        demo01SerializedName();
        demo02FieldNamingPolicy();
        demo03ExposeAnnotation();
        demo04DateAdapterAndNullHandling();
        demo05ExclusionStrategy();

        System.out.println("\n✅ GsonAdvancedDemo 运行完成！");
    }
}
