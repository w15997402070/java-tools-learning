# Day 02: Gson - Google JSON处理库

## 📌 简介

**Gson** 是 Google 开源的 Java JSON 序列化/反序列化库，以**零依赖、零配置即用**著称。无需任何注解即可将 Java 对象转换为 JSON 字符串，也可将 JSON 字符串还原为 Java 对象。

| 属性 | 信息 |
|------|------|
| **GitHub** | https://github.com/google/gson |
| **Stars** | 23k+ |
| **最新版本** | 2.10.1 (2023-09) |
| **Java要求** | Java 7+ |
| **包大小** | ~250KB（零依赖） |
| **维护状态** | 活跃维护（Google官方） |

---

## 📦 Maven依赖

```xml
<!-- Gson核心依赖（无其他依赖） -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

---

## 🚀 核心用法速查

### 1. 基础序列化/反序列化

```java
Gson gson = new Gson();

// 对象 → JSON
User user = new User(1, "张三", "zhangsan@example.com");
String json = gson.toJson(user);
// 输出: {"id":1,"name":"张三","email":"zhangsan@example.com"}

// JSON → 对象
User parsed = gson.fromJson(json, User.class);

// 格式化输出（Pretty Print）
Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
String pretty = prettyGson.toJson(user);
```

### 2. 集合与Map

```java
// List序列化
List<User> list = Arrays.asList(user1, user2);
String listJson = gson.toJson(list);

// List反序列化（必须用TypeToken！）
Type listType = new TypeToken<List<User>>() {}.getType();
List<User> parsedList = gson.fromJson(listJson, listType);

// Map序列化
Map<String, Object> map = new HashMap<>();
map.put("key", "value");
String mapJson = gson.toJson(map);
```

> ⚠️ **陷阱**：反序列化带泛型的集合时，必须使用 `TypeToken`，否则 Gson 无法推断泛型类型，会返回 `LinkedTreeMap` 而非目标类。

### 3. 字段注解

```java
// @SerializedName：处理JSON字段名与Java字段名不同（如下划线风格）
public class ApiUser {
    @SerializedName("user_id")
    private int userId;

    @SerializedName(value = "user_name", alternate = {"userName", "name"})
    private String userName;  // 支持多个备用字段名
}

// @Expose：精确控制哪些字段参与序列化（配合 excludeFieldsWithoutExposeAnnotation）
public class SecureUser {
    @Expose(serialize = true, deserialize = true)
    private String name;

    @Expose(serialize = false, deserialize = true)  // 密码不输出到JSON
    private String password;

    private String internalCode;  // 无注解：完全忽略
}

// 使用 @Expose 时必须如此配置
Gson gson = new GsonBuilder()
    .excludeFieldsWithoutExposeAnnotation()
    .create();
```

### 4. 自定义TypeAdapter（处理Date等特殊类型）

```java
// 自定义Date格式适配器
class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    private static final String FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    public JsonElement serialize(Date src, Type type, JsonSerializationContext ctx) {
        return new JsonPrimitive(new SimpleDateFormat(FORMAT).format(src));
    }

    @Override
    public Date deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
        try {
            return new SimpleDateFormat(FORMAT).parse(json.getAsString());
        } catch (ParseException e) {
            throw new JsonParseException(e);
        }
    }
}

// 注册适配器
Gson gson = new GsonBuilder()
    .registerTypeAdapter(Date.class, new DateTypeAdapter())
    .create();
```

### 5. 命名策略（FieldNamingPolicy）

```java
// Java驼峰 → JSON下划线
Gson gson = new GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create();
// productName → product_name

// 其他策略
FieldNamingPolicy.LOWER_CASE_WITH_DASHES      // productName → product-name
FieldNamingPolicy.UPPER_CAMEL_CASE             // productName → ProductName
```

### 6. Streaming API（处理超大JSON）

```java
// 流式读取（节省内存，适合大文件）
JsonReader reader = new JsonReader(new FileReader("large.json"));
reader.beginArray();
while (reader.hasNext()) {
    reader.beginObject();
    while (reader.hasNext()) {
        String key = reader.nextName();
        // 根据key处理value
        switch (key) {
            case "id": int id = reader.nextInt(); break;
            case "name": String name = reader.nextString(); break;
            default: reader.skipValue(); // 忽略不需要的字段
        }
    }
    reader.endObject();
}
reader.endArray();
reader.close();
```

---

## 🔗 Spring Boot 集成

### 方式一：替换默认的Jackson（全局）

Spring Boot默认使用Jackson，如果整个项目都要用Gson，可以替换：

```xml
<!-- pom.xml：排除Jackson，引入Gson -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-json</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

```java
// application.yml 或配置类中设置
spring:
  mvc:
    converters:
      preferred-json-mapper: gson
```

### 方式二：注册Gson Bean（共存，按需使用）

```java
@Configuration
public class GsonConfig {

    @Bean
    public Gson gson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .registerTypeAdapter(Date.class, new DateTypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    }
}
```

```java
// Service中注入使用
@Service
public class JsonService {

    @Autowired
    private Gson gson;

    public <T> String toJson(T obj) {
        return gson.toJson(obj);
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    // 处理泛型
    public <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }
}
```

### 方式三：Spring Boot自动配置（推荐）

Spring Boot 2.x+ 提供了 `GsonAutoConfiguration`，只需添加依赖即可：

```java
// 在application.properties中配置Gson选项
spring.gson.serialize-nulls=true
spring.gson.pretty-printing=true
spring.gson.date-format=yyyy-MM-dd HH:mm:ss
spring.gson.field-naming-policy=LOWER_CASE_WITH_UNDERSCORES
```

---

## ⚠️ 注意事项

### 1. 泛型陷阱（最常见Bug）

```java
// ❌ 错误：data 会是 LinkedTreeMap，不是 User
ApiResponse<User> resp = gson.fromJson(json, ApiResponse.class);
User user = resp.getData(); // ClassCastException！

// ✅ 正确：使用 TypeToken 保留泛型信息
Type type = new TypeToken<ApiResponse<User>>() {}.getType();
ApiResponse<User> resp = gson.fromJson(json, type);
```

### 2. transient字段自动跳过

```java
// transient 修饰的字段默认不序列化，适合存放临时计算值或敏感数据
private transient String cachedResult;
private transient String password;
```

### 3. 循环引用会导致StackOverflow

```java
// 如果对象间有循环引用（A持有B，B又持有A），直接 toJson 会栈溢出
// 解决方案：使用 @Expose 手动控制，或在模型中避免循环引用
```

### 4. 数字精度问题

```java
// Gson默认将数字反序列化为 double，可能丢失精度
// JSON中的 {"price": 99.9} 反序列化为 Map 时，price 会是 Double
// 对于精确计算（如金额），建议使用 String 或 BigDecimal + 自定义适配器
Map<String, Object> map = gson.fromJson(json, Map.class);
Object price = map.get("price"); // Double，不是 BigDecimal！
```

### 5. 性能建议

```java
// ✅ 推荐：Gson实例是线程安全的，全局复用同一个实例
private static final Gson GSON = new GsonBuilder()
    .setDateFormat("yyyy-MM-dd")
    .create();

// ❌ 避免：每次都new Gson()，增加GC压力（虽然影响不大但是不必要的开销）
```

### 6. null处理

```java
// 默认：null字段不输出到JSON
// serializeNulls()：强制输出null
Gson gson = new GsonBuilder().serializeNulls().create();
// 输出: {"name":"张三","email":null}  而非  {"name":"张三"}
```

### 7. Gson vs Jackson 选型参考

| 维度 | Gson | Jackson |
|------|------|---------|
| 包大小 | ~250KB（零依赖） | ~1.5MB（多模块） |
| 配置复杂度 | 极简 | 丰富但复杂 |
| 性能 | 中等 | 更快 |
| 功能 | 够用 | 功能更全 |
| Spring Boot集成 | 需要手动配置 | 开箱即用 |
| 推荐场景 | 简单JSON处理、Android | 企业级项目 |

---

## 🏃 运行方法

```bash
# 进入项目目录
cd java-tools-learning/gson-demo

# 编译并打包
mvn clean package -DskipTests

# 运行基础演示
mvn exec:java -Dexec.mainClass="com.example.gson.GsonBasicDemo"

# 运行高级演示
mvn exec:java -Dexec.mainClass="com.example.gson.GsonAdvancedDemo"

# 运行实战演示
mvn exec:java -Dexec.mainClass="com.example.gson.GsonPracticalDemo"
```

---

## 📁 代码结构

```
gson-demo/
├── pom.xml
└── src/main/java/com/example/gson/
    ├── GsonBasicDemo.java      # 基础用法：序列化/反序列化/集合/JsonObject手动解析
    ├── GsonAdvancedDemo.java   # 高级特性：注解/命名策略/TypeAdapter/ExclusionStrategy
    └── GsonPracticalDemo.java  # 实战场景：泛型响应/流式API/动态类型/JSON合并
```
