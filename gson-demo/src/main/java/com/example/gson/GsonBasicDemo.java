package com.example.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gson基础用法演示
 *
 * <p>涵盖：
 * <ul>
 *   <li>对象序列化（Java Object → JSON字符串）</li>
 *   <li>对象反序列化（JSON字符串 → Java Object）</li>
 *   <li>集合/Map的序列化与反序列化</li>
 *   <li>JsonObject/JsonArray的手动解析</li>
 *   <li>格式化输出（Pretty Print）</li>
 * </ul>
 */
public class GsonBasicDemo {

    // ====================================================
    // 内部模型类 - 模拟用户信息
    // ====================================================

    /**
     * 用户实体类
     * Gson通过反射读取字段名，无需任何注解即可序列化
     */
    static class User {
        private int id;
        private String name;
        private String email;
        private int age;
        // transient 字段会被Gson自动忽略，适合存放密码等敏感信息
        private transient String password;

        public User() {}

        public User(int id, String name, String email, int age) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.age = age;
            this.password = "secret123"; // 演示transient效果
        }

        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "', email='" + email + "', age=" + age + "}";
        }
    }

    /**
     * 地址实体类，用于演示嵌套对象
     */
    static class Address {
        private String city;
        private String province;
        private String street;

        public Address(String city, String province, String street) {
            this.city = city;
            this.province = province;
            this.street = street;
        }

        @Override
        public String toString() {
            return "Address{city='" + city + "', province='" + province + "', street='" + street + "'}";
        }
    }

    /**
     * 用户详情（包含嵌套对象）
     */
    static class UserDetail {
        private User user;
        private Address address;
        private List<String> hobbies;

        public UserDetail(User user, Address address, List<String> hobbies) {
            this.user = user;
            this.address = address;
            this.hobbies = hobbies;
        }

        @Override
        public String toString() {
            return "UserDetail{user=" + user + ", address=" + address + ", hobbies=" + hobbies + "}";
        }
    }

    // ====================================================
    // 演示方法
    // ====================================================

    /**
     * 演示1：简单对象序列化和反序列化
     */
    static void demo01SimpleObjectConversion() {
        System.out.println("\n========== Demo01: 简单对象序列化/反序列化 ==========");

        Gson gson = new Gson();

        // 1.1 Java对象 → JSON字符串
        User user = new User(1, "张三", "zhangsan@example.com", 28);
        String json = gson.toJson(user);
        System.out.println("序列化结果: " + json);
        // 注意：password是transient，不会出现在JSON中

        // 1.2 JSON字符串 → Java对象
        String jsonStr = "{\"id\":2,\"name\":\"李四\",\"email\":\"lisi@example.com\",\"age\":25}";
        User deserializedUser = gson.fromJson(jsonStr, User.class);
        System.out.println("反序列化结果: " + deserializedUser);

        // 1.3 格式化输出（Pretty Print）
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = prettyGson.toJson(user);
        System.out.println("格式化输出:\n" + prettyJson);
    }

    /**
     * 演示2：嵌套对象和集合的处理
     */
    static void demo02NestedObjectAndCollection() {
        System.out.println("\n========== Demo02: 嵌套对象与集合 ==========");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // 2.1 嵌套对象序列化
        User user = new User(3, "王五", "wangwu@example.com", 32);
        Address address = new Address("深圳", "广东", "南山区科技园");
        List<String> hobbies = Arrays.asList("读书", "编程", "爬山");
        UserDetail detail = new UserDetail(user, address, hobbies);

        String json = gson.toJson(detail);
        System.out.println("嵌套对象序列化:\n" + json);

        // 2.2 List<User> 序列化
        List<User> userList = new ArrayList<>();
        userList.add(new User(1, "Alice", "alice@example.com", 24));
        userList.add(new User(2, "Bob", "bob@example.com", 30));
        String listJson = gson.toJson(userList);
        System.out.println("\nList序列化:\n" + listJson);

        // 2.3 List<User> 反序列化 - 必须使用TypeToken保留泛型信息
        // 直接用 List.class 会丢失泛型，得到 List<LinkedTreeMap>
        Type listType = new TypeToken<List<User>>() {}.getType();
        List<User> parsedList = gson.fromJson(listJson, listType);
        System.out.println("\nList反序列化结果:");
        for (User u : parsedList) {
            System.out.println("  " + u);
        }

        // 2.4 Map序列化
        Map<String, Object> config = new HashMap<>();
        config.put("appName", "MyApp");
        config.put("version", 1.5);
        config.put("enabled", true);
        config.put("maxConnections", 100);
        System.out.println("\nMap序列化: " + gson.toJson(config));
    }

    /**
     * 演示3：JsonObject/JsonArray 手动解析（无需POJO）
     */
    static void demo03ManualJsonParsing() {
        System.out.println("\n========== Demo03: JsonObject手动解析 ==========");

        // 模拟一个复杂的JSON响应（如API返回）
        String apiResponse = "{"
                + "\"code\": 200,"
                + "\"message\": \"success\","
                + "\"data\": {"
                + "  \"total\": 3,"
                + "  \"users\": ["
                + "    {\"id\": 1, \"name\": \"Alice\", \"vip\": true},"
                + "    {\"id\": 2, \"name\": \"Bob\", \"vip\": false},"
                + "    {\"id\": 3, \"name\": \"Charlie\", \"vip\": true}"
                + "  ]"
                + "}"
                + "}";

        // 解析根对象
        JsonObject root = JsonParser.parseString(apiResponse).getAsJsonObject();
        int code = root.get("code").getAsInt();
        String message = root.get("message").getAsString();
        System.out.println("状态码: " + code + ", 消息: " + message);

        // 获取嵌套data对象
        JsonObject data = root.getAsJsonObject("data");
        int total = data.get("total").getAsInt();
        System.out.println("总数: " + total);

        // 遍历数组
        JsonArray users = data.getAsJsonArray("users");
        System.out.println("用户列表:");
        for (JsonElement element : users) {
            JsonObject userObj = element.getAsJsonObject();
            int id = userObj.get("id").getAsInt();
            String name = userObj.get("name").getAsString();
            boolean isVip = userObj.get("vip").getAsBoolean();
            System.out.println("  ID=" + id + ", 名称=" + name + ", VIP=" + isVip);
        }

        // 动态构建JsonObject
        System.out.println("\n--- 动态构建JSON ---");
        JsonObject newUser = new JsonObject();
        newUser.addProperty("id", 100);
        newUser.addProperty("name", "新用户");
        newUser.addProperty("score", 99.5);
        newUser.addProperty("active", true);

        JsonArray tagArray = new JsonArray();
        tagArray.add("java");
        tagArray.add("spring");
        tagArray.add("gson");
        newUser.add("tags", tagArray);

        System.out.println("动态构建结果: " + new GsonBuilder().setPrettyPrinting().create().toJson(newUser));
    }

    public static void main(String[] args) {
        System.out.println("======================================");
        System.out.println("   Gson基础用法演示 (Day 02)");
        System.out.println("======================================");

        demo01SimpleObjectConversion();
        demo02NestedObjectAndCollection();
        demo03ManualJsonParsing();

        System.out.println("\n✅ GsonBasicDemo 运行完成！");
    }
}
