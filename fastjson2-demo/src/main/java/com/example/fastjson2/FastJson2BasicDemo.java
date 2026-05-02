package com.example.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.TypeReference;

import java.util.*;

/**
 * Fastjson2 基础功能演示
 * 涵盖：基础序列化/反序列化、集合处理、JSONPath 查询
 */
public class FastJson2BasicDemo {

    // 定义内部实体类用于演示
    static class User {
        private String name;
        private Integer age;
        private String email;

        // 必须有无参构造函数（Fastjson2 可以通过字节码技术跳过，但建议保留）
        public User() {}

        public User(String name, Integer age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        // getter 和 setter（Fastjson2 支持无需 getter/setter，但这里演示标准用法）
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        @Override
        public String toString() {
            return "User{name='" + name + "', age=" + age + ", email='" + email + "'}";
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Fastjson2 基础功能演示 ===\n");

        demoBasicSerialize();
        demoBasicDeserialize();
        demoCollectionProcess();
        demoJsonPath();
    }

    /**
     * 演示 1：基础序列化（Java 对象 → JSON 字符串）
     */
    static void demoBasicSerialize() {
        System.out.println("--- 1. 基础序列化（对象 → JSON） ---");

        User user = new User("张三", 28, "zhangsan@example.com");

        // 最基础用法：toJSONString
        String jsonString = JSON.toJSONString(user);
        System.out.println("基础序列化结果：");
        System.out.println(jsonString);

        // 格式化输出（pretty print）
        String prettyJson = JSON.toJSONString(user, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
        System.out.println("\n格式化输出：");
        System.out.println(prettyJson);

        // 序列化集合
        List<User> userList = Arrays.asList(
                new User("张三", 28, "zhangsan@example.com"),
                new User("李四", 32, "lisi@example.com")
        );
        String listJson = JSON.toJSONString(userList);
        System.out.println("\n集合序列化结果：");
        System.out.println(listJson);

        System.out.println();
    }

    /**
     * 演示 2：基础反序列化（JSON 字符串 → Java 对象）
     */
    static void demoBasicDeserialize() {
        System.out.println("--- 2. 基础反序列化（JSON → 对象） ---");

        String json = "{\"name\":\"张三\",\"age\":28,\"email\":\"zhangsan@example.com\"}";

        // 反序列化为 JavaBean
        User user = JSON.parseObject(json, User.class);
        System.out.println("反序列化为 JavaBean：");
        System.out.println(user);

        // 反序列化为 JSONObject（类似 Map）
        JSONObject jsonObject = JSON.parseObject(json);
        System.out.println("\n反序列化为 JSONObject：");
        System.out.println("name: " + jsonObject.getString("name"));
        System.out.println("age: " + jsonObject.getInteger("age"));

        // 反序列化为 List
        String listJson = "[{\"name\":\"张三\",\"age\":28},{\"name\":\"李四\",\"age\":32}]";
        List<User> userList = JSON.parseArray(listJson, User.class);
        System.out.println("\n反序列化为 List：");
        userList.forEach(System.out::println);

        // 使用 TypeReference 处理复杂泛型
        String mapJson = "{\"user1\":{\"name\":\"张三\",\"age\":28}}";
        Map<String, User> map = JSON.parseObject(mapJson, new TypeReference<Map<String, User>>() {});
        System.out.println("\n反序列化为 Map（使用 TypeReference）：");
        System.out.println(map.get("user1"));

        System.out.println();
    }

    /**
     * 演示 3：集合和数组处理
     */
    static void demoCollectionProcess() {
        System.out.println("--- 3. 集合和数组处理 ---");

        // JSONArray 操作
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(new User("张三", 28, "zhangsan@example.com"));
        jsonArray.add(new User("李四", 32, "lisi@example.com"));

        System.out.println("JSONArray 序列化：");
        System.out.println(jsonArray.toJSONString());

        // 从 JSON 字符串解析为 JSONArray
        String json = "[{\"name\":\"张三\"},{\"name\":\"李四\"}]";
        JSONArray parsedArray = JSON.parseArray(json);
        System.out.println("\n解析 JSONArray 并遍历：");
        for (int i = 0; i < parsedArray.size(); i++) {
            JSONObject obj = parsedArray.getJSONObject(i);
            System.out.println("第 " + (i + 1) + " 个元素: " + obj.getString("name"));
        }

        System.out.println();
    }

    /**
     * 演示 4：JSONPath 查询（类似 XPath）
     */
    static void demoJsonPath() {
        System.out.println("--- 4. JSONPath 查询 ---");

        String json = "{" +
                "\"store\":{" +
                "  \"name\":\"书店\"," +
                "  \"books\":[" +
                "    {\"title\":\"Java编程思想\",\"price\":88.0,\"category\":\"编程\"}," +
                "    {\"title\":\"算法导论\",\"price\":128.0,\"category\":\"计算机\"}" +
                "  ]," +
                "  \"location\":\"北京\"" +
                "}}";

        JSONObject root = JSON.parseObject(json);

        // JSONPath 查询：获取所有书名
        List<Object> titles = (List<Object>) JSONPath.eval(root, "$.store.books[*].title");
        System.out.println("所有书名：");
        titles.forEach(t -> System.out.println("  " + t));

        // JSONPath 查询：获取价格大于 100 的书籍
        List<Object> expensiveBooks = (List<Object>) JSONPath.eval(root, "$.store.books[?(@.price > 100)]");
        System.out.println("\n价格大于 100 的书籍：");
        expensiveBooks.forEach(b -> System.out.println("  " + b));

        // JSONPath 查询：获取书店名称
        Object storeName = JSONPath.eval(root, "$.store.name");
        System.out.println("\n书店名称：" + storeName);

        System.out.println();
    }
}
