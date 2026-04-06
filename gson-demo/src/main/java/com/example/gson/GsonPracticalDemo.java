package com.example.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gson实战场景演示
 *
 * <p>涵盖：
 * <ul>
 *   <li>泛型ApiResponse包装类的正确处理（TypeToken用法）</li>
 *   <li>Streaming API（JsonReader/JsonWriter）- 处理超大JSON文件，节省内存</li>
 *   <li>动态类型解析（同一字段可能是字符串/数字/布尔/对象/数组）</li>
 *   <li>JSON深度合并（两个配置对象合并，后者覆盖前者）</li>
 * </ul>
 */
public class GsonPracticalDemo {

    // ====================================================
    // 模型类
    // ====================================================

    /**
     * 通用API响应包装类（泛型）
     * 实际项目中几乎所有API都会有这样的包装结构
     */
    static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;

        public ApiResponse() {}

        public ApiResponse(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public int getCode() { return code; }
        public String getMessage() { return message; }
        public T getData() { return data; }

        @Override
        public String toString() {
            return "ApiResponse{code=" + code + ", message='" + message + "', data=" + data + "}";
        }
    }

    /**
     * 商品信息
     */
    static class Product {
        int id;
        String name;
        double price;
        String category;

        public Product() {}

        public Product(int id, String name, double price, String category) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.category = category;
        }

        @Override
        public String toString() {
            return "Product{id=" + id + ", name='" + name + "', price=" + price + "}";
        }
    }

    // ====================================================
    // 演示方法
    // ====================================================

    /**
     * 演示1：泛型ApiResponse的正确反序列化方式
     *
     * <p><b>关键知识点</b>：Gson处理泛型时必须用TypeToken，
     * 直接用 ApiResponse.class 会导致 data 字段被解析为 LinkedTreeMap 而非目标类型
     */
    static void demo01GenericResponseDeserialization() {
        System.out.println("\n========== Demo01: 泛型ApiResponse反序列化 ==========");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // 场景1：data是单个对象
        String singleJson = "{\"code\":200,\"message\":\"success\","
                + "\"data\":{\"id\":1,\"name\":\"iPhone 15\",\"price\":7999.0,\"category\":\"手机\"}}";

        // 正确方式：使用 TypeToken 保留泛型信息
        Type singleType = new TypeToken<ApiResponse<Product>>() {}.getType();
        ApiResponse<Product> singleResp = gson.fromJson(singleJson, singleType);
        System.out.println("单对象响应: " + singleResp);
        System.out.println("商品名称: " + singleResp.getData().name);

        // 错误演示（仅提示，不运行）
        System.out.println("\n[提示] 错误用法：gson.fromJson(json, ApiResponse.class)");
        System.out.println("[提示] 会导致 data 被解析为 LinkedTreeMap，访问 .name 时抛出 ClassCastException");

        // 场景2：data是对象列表
        String listJson = "{\"code\":200,\"message\":\"success\","
                + "\"data\":[{\"id\":1,\"name\":\"MacBook\",\"price\":12999.0,\"category\":\"电脑\"},"
                + "{\"id\":2,\"name\":\"iPad\",\"price\":4999.0,\"category\":\"平板\"}]}";

        Type listType = new TypeToken<ApiResponse<List<Product>>>() {}.getType();
        ApiResponse<List<Product>> listResp = gson.fromJson(listJson, listType);
        System.out.println("\n列表响应: code=" + listResp.getCode() + ", 消息=" + listResp.getMessage());
        for (Product p : listResp.getData()) {
            System.out.println("  " + p);
        }
    }

    /**
     * 演示2：Streaming API处理大JSON
     *
     * <p>当JSON文件非常大（几十MB到几GB）时，不能一次性加载到内存，
     * 需要使用 JsonReader 流式逐条处理，内存占用极低
     */
    static void demo02StreamingAPI() throws IOException {
        System.out.println("\n========== Demo02: Streaming API流式处理 ==========");

        // 模拟一个大JSON数组（实际场景可能是文件输入流）
        StringBuilder sb = new StringBuilder("[");
        for (int i = 1; i <= 5; i++) {
            if (i > 1) sb.append(",");
            sb.append("{\"id\":").append(i)
              .append(",\"name\":\"Product-").append(i)
              .append("\",\"price\":").append(i * 100.0)
              .append(",\"category\":\"分类").append(i % 3 + 1).append("\"}");
        }
        sb.append("]");
        String largeJson = sb.toString();
        System.out.println("输入JSON: " + largeJson);

        // 使用JsonReader流式解析，逐条读取
        List<Product> products = new ArrayList<>();
        JsonReader reader = new JsonReader(new StringReader(largeJson));
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            int id = 0;
            String name = null;
            double price = 0;
            String category = null;

            while (reader.hasNext()) {
                String fieldName = reader.nextName();
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    continue;
                }
                switch (fieldName) {
                    case "id":       id = reader.nextInt(); break;
                    case "name":     name = reader.nextString(); break;
                    case "price":    price = reader.nextDouble(); break;
                    case "category": category = reader.nextString(); break;
                    default:         reader.skipValue(); break;
                }
            }
            reader.endObject();
            products.add(new Product(id, name, price, category));
        }
        reader.endArray();
        reader.close();

        System.out.println("流式解析结果（共" + products.size() + "条）:");
        for (Product p : products) {
            System.out.println("  " + p);
        }

        // 使用JsonWriter流式写出
        System.out.println("\n--- JsonWriter流式写出 ---");
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setIndent("  ");

        writer.beginArray();
        for (Product p : products) {
            writer.beginObject();
            writer.name("id").value(p.id);
            writer.name("productName").value(p.name);
            writer.name("price").value(p.price);
            writer.endObject();
        }
        writer.endArray();
        writer.close();

        System.out.println("流式写出结果:\n" + sw.toString());
    }

    /**
     * 演示3：动态类型解析（value可能是字符串或数字等多种类型）
     *
     * <p>实际API中经常遇到同一字段在不同情况下类型不同，
     * 需要用 JsonElement 先接收，再用 isJsonPrimitive/isJsonObject 判断
     */
    static void demo03DynamicTypeHandling() {
        System.out.println("\n========== Demo03: 动态类型JSON解析 ==========");

        String[] jsonSamples = {
            "{\"id\":\"USR-001\",\"score\":\"优秀\",\"extra\":{\"level\":3}}",
            "{\"id\":1001,\"score\":99.5,\"extra\":null}",
            "{\"id\":\"USR-003\",\"score\":true,\"extra\":[\"tag1\",\"tag2\"]}"
        };

        for (String json : jsonSamples) {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            // 安全处理id（可能是字符串或数字）
            JsonElement idElem = obj.get("id");
            String id = idElem.isJsonPrimitive() ? idElem.getAsString() : "unknown";

            // 安全处理score（类型不确定）
            JsonElement scoreElem = obj.get("score");
            String scoreDesc;
            if (scoreElem.isJsonPrimitive()) {
                if (scoreElem.getAsJsonPrimitive().isNumber()) {
                    scoreDesc = "数字:" + scoreElem.getAsDouble();
                } else if (scoreElem.getAsJsonPrimitive().isBoolean()) {
                    scoreDesc = "布尔:" + scoreElem.getAsBoolean();
                } else {
                    scoreDesc = "字符串:" + scoreElem.getAsString();
                }
            } else {
                scoreDesc = "非基本类型";
            }

            // 安全处理extra（可能是对象、数组或null）
            JsonElement extraElem = obj.get("extra");
            String extraDesc;
            if (extraElem == null || extraElem.isJsonNull()) {
                extraDesc = "null";
            } else if (extraElem.isJsonObject()) {
                extraDesc = "对象(" + extraElem.getAsJsonObject().entrySet().size() + "个字段)";
            } else if (extraElem.isJsonArray()) {
                extraDesc = "数组(" + extraElem.getAsJsonArray().size() + "个元素)";
            } else {
                extraDesc = "原始值";
            }

            System.out.println("id=" + id + ", score=" + scoreDesc + ", extra=" + extraDesc);
        }
    }

    /**
     * 演示4：JSON深度合并（配置覆盖场景）
     *
     * <p>常见于：默认配置 + 环境配置合并，后者覆盖前者的同名字段，但保留前者独有字段
     */
    static void demo04JsonDeepMerge() {
        System.out.println("\n========== Demo04: JSON深度合并 ==========");

        String baseConfig = "{\"host\":\"localhost\",\"port\":3306,\"database\":\"mydb\","
                + "\"pool\":{\"min\":5,\"max\":20,\"timeout\":30000}}";
        String overrideConfig = "{\"host\":\"prod-server\",\"port\":5432,"
                + "\"pool\":{\"max\":50},\"ssl\":true}";

        JsonObject base = JsonParser.parseString(baseConfig).getAsJsonObject();
        JsonObject override = JsonParser.parseString(overrideConfig).getAsJsonObject();

        JsonObject merged = deepMerge(base, override);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println("基础配置:\n" + gson.toJson(base));
        System.out.println("\n覆盖配置:\n" + gson.toJson(override));
        System.out.println("\n合并结果（pool.min保留，host/port/pool.max被覆盖，ssl新增）:\n" + gson.toJson(merged));
    }

    /**
     * 深度合并两个JsonObject，override中的值优先
     * 对于嵌套对象递归合并，对于基本类型/数组直接覆盖
     */
    static JsonObject deepMerge(JsonObject base, JsonObject override) {
        JsonObject result = base.deepCopy();
        for (java.util.Map.Entry<String, JsonElement> entry : override.entrySet()) {
            String key = entry.getKey();
            JsonElement overrideVal = entry.getValue();
            if (result.has(key) && result.get(key).isJsonObject() && overrideVal.isJsonObject()) {
                // 递归合并嵌套对象
                result.add(key, deepMerge(result.getAsJsonObject(key), overrideVal.getAsJsonObject()));
            } else {
                result.add(key, overrideVal);
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("======================================");
        System.out.println("   Gson实战场景演示 (Day 02)");
        System.out.println("======================================");

        demo01GenericResponseDeserialization();
        demo02StreamingAPI();
        demo03DynamicTypeHandling();
        demo04JsonDeepMerge();

        System.out.println("\n==> GsonPracticalDemo 运行完成！");
    }
}
