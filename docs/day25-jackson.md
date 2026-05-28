# Day 25: Jackson — Spring Boot 默认 JSON 引擎，支持 JSON/XML/YAML/CSV 多格式

## 工具简介

**Jackson** 是 Java 生态中最广泛使用的数据绑定库，也是 **Spring Boot 内置默认 JSON 序列化引擎**（自动配置，无需手动引入）。相比 Gson、Fastjson2，Jackson 功能最全面，支持 JSON/XML/YAML/CSV 四种格式，提供注解驱动、流式 API、树模型、多态处理等高级特性。

- **GitHub**: https://github.com/FasterXML/jackson
- **官网文档**: https://github.com/FasterXML/jackson-docs
- **版本**: 2.17.x（2024 年最新稳定版，Java 8 兼容）
- **Stars**: 8.6k+（核心库）+ 生态系列库均 1k~3k+
- **License**: Apache 2.0

### Jackson vs 同类库对比

| 特性 | Jackson | Gson | Fastjson2 |
|------|---------|------|-----------|
| Spring Boot 默认 | ✅ 内置 | ❌ 需引入 | ❌ 需引入 |
| XML 支持 | ✅ XmlMapper | ❌ | ❌ |
| YAML 支持 | ✅ YAMLMapper | ❌ | ❌ |
| CSV 支持 | ✅ CsvMapper | ❌ | ❌ |
| 流式 API | ✅ 低内存大文件 | ✅ | ✅ |
| 多态类型 | ✅ @JsonTypeInfo | 需自定义 | ✅ |
| 历史安全漏洞 | ✅ 记录较少 | ✅ 良好 | ⚠️ 有历史漏洞 |
| 性能（综合） | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

## Maven 依赖配置

```xml
<properties>
    <jackson.version>2.17.2</jackson.version>
</properties>

<dependencies>
    <!-- 核心：JSON 序列化/反序列化（包含 jackson-core + jackson-annotations） -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>${jackson.version}</version>
    </dependency>

    <!-- Java 8 日期时间支持（LocalDateTime/LocalDate） -->
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
        <version>${jackson.version}</version>
    </dependency>

    <!-- XML 格式支持 -->
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-xml</artifactId>
        <version>${jackson.version}</version>
    </dependency>

    <!-- YAML 格式支持 -->
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-yaml</artifactId>
        <version>${jackson.version}</version>
    </dependency>

    <!-- CSV 格式支持 -->
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-csv</artifactId>
        <version>${jackson.version}</version>
    </dependency>
</dependencies>
```

> **Spring Boot 用户注意**：Spring Boot 已通过 `spring-boot-starter-web` 自动传递 `jackson-databind`，无需额外声明。只需引入 `jackson-datatype-jsr310`（处理 Java 8 日期）和多格式扩展模块即可。

---

## 核心 API 速查

### ObjectMapper 初始化（推荐单例）

```java
// 全局单例 — 线程安全，禁止每次 new！
private static final ObjectMapper MAPPER = createObjectMapper();

private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    // 注册 Java 8 日期时间模块
    mapper.registerModule(new JavaTimeModule());
    // 日期输出为字符串而非时间戳
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    // 反序列化时忽略未知字段（向后兼容）
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    // null 字段不输出
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper;
}
```

### 基础序列化/反序列化

```java
// 对象 -> JSON 字符串
String json = mapper.writeValueAsString(obj);

// JSON 字符串 -> 对象
MyClass obj = mapper.readValue(json, MyClass.class);

// JSON 字符串 -> 泛型集合（必须用 TypeReference）
List<MyClass> list = mapper.readValue(json, new TypeReference<List<MyClass>>() {});
Map<String, Object> map = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

// 对象 -> 字节数组
byte[] bytes = mapper.writeValueAsBytes(obj);

// 写入文件/流
mapper.writeValue(new File("output.json"), obj);
mapper.writeValue(outputStream, obj);
```

---

## 常用注解汇总

```java
@JsonProperty("user_id")            // 指定 JSON 字段名（驼峰 <-> 下划线）
@JsonIgnore                          // 序列化/反序列化时完全忽略
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略 JSON 中多余字段（推荐加在类上）
@JsonAlias({"phone", "mobile"})      // 反序列化时支持多个别名
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")  // 日期格式
@JsonInclude(JsonInclude.Include.NON_NULL)     // 值为 null 时不序列化该字段
@JsonSerialize(using = MySerializer.class)    // 自定义序列化器
@JsonDeserialize(using = MyDeserializer.class) // 自定义反序列化器
@JsonValue                           // 枚举：序列化时使用该方法的返回值
@JsonCreator                         // 枚举/构造器：反序列化时使用该工厂方法
```

---

## Spring Boot 集成方式

### 1. 自动配置（默认可用，无需手动配置）

Spring Boot 自动装配 `JacksonAutoConfiguration`，创建全局 `ObjectMapper` Bean。

### 2. `application.yml` 常用配置

```yaml
spring:
  jackson:
    # 美化输出（生产环境关闭以节省带宽）
    serialization:
      indent-output: false
      write-dates-as-timestamps: false   # 日期输出为字符串
    deserialization:
      fail-on-unknown-properties: false  # 忽略未知字段（推荐）
    # 全局不输出 null 字段
    default-property-inclusion: non_null
    time-zone: Asia/Shanghai
    date-format: "yyyy-MM-dd HH:mm:ss"
```

### 3. 自定义 ObjectMapper Bean

```java
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        // 注册 Java 8 日期时间
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
```

### 4. Controller 中使用

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private ObjectMapper objectMapper;   // 注入全局单例

    // Spring MVC 自动使用 Jackson 序列化返回值
    @GetMapping("/{id}")
    public Result<UserProfile> getUser(@PathVariable Long id) {
        UserProfile profile = userService.findById(id);
        return Result.ok(profile);
    }

    // 手动解析 JSON 请求体（特殊场景）
    @PostMapping("/dynamic")
    public ResponseEntity<?> handleDynamic(@RequestBody String rawJson) throws Exception {
        JsonNode node = objectMapper.readTree(rawJson);
        String type = node.path("type").asText();
        // 根据 type 动态处理...
        return ResponseEntity.ok("ok");
    }
}
```

### 5. 多态序列化完整示例

```java
// 父类
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = EmailNotification.class, name = "email"),
    @JsonSubTypes.Type(value = SmsNotification.class, name = "sms")
})
public abstract class Notification {
    protected String content;
    protected String recipient;
}

// 子类
public class EmailNotification extends Notification {
    private String subject;
    private String fromAddress;
}

// 使用时：序列化自动加 "type" 字段，反序列化自动还原正确子类
```

---

## 代码示例

### 场景一：API 网关统一响应体

```java
// 泛型响应封装，支持任意数据类型
Result<List<UserDTO>> response = Result.ok(userList);
String json = mapper.writeValueAsString(response);
// {"code":200,"message":"success","data":[...]}

// 反序列化时保留泛型信息
Result<List<UserDTO>> parsed = mapper.readValue(json,
    new TypeReference<Result<List<UserDTO>>>() {});
```

### 场景二：处理超大 JSON 文件（流式 API）

```java
// 低内存流式解析（适合百万条记录的 JSON 数组）
try (JsonParser parser = mapper.getFactory().createParser(inputStream)) {
    parser.nextToken(); // START_ARRAY
    while (parser.nextToken() == JsonToken.START_OBJECT) {
        // 逐条反序列化，不在内存中积累
        Record record = mapper.readValue(parser, Record.class);
        processRecord(record);
    }
}
```

### 场景三：CSV 数据导入导出

```java
CsvMapper csvMapper = new CsvMapper();
CsvSchema schema = csvMapper.schemaFor(SalesRecord.class).withHeader();

// 导出 CSV
String csv = csvMapper.writer(schema).writeValueAsString(records);

// 导入 CSV
List<SalesRecord> imported = csvMapper.reader(SalesRecord.class)
    .with(schema).<SalesRecord>readValues(csvFile).readAll();
```

### 场景四：读写 YAML 配置文件

```java
ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

// 读取 YAML 配置
AppConfig config = yamlMapper.readValue(
    new File("config.yml"), AppConfig.class);

// 写入 YAML
yamlMapper.writeValue(new File("output.yml"), config);
```

---

## 注意事项（Bug 风险 / 性能问题 / 使用限制）

### ⚠️ 高风险：不要重复创建 ObjectMapper

```java
// ❌ 严重性能问题：每次请求创建 ObjectMapper，初始化成本高
public String toJson(Object obj) {
    return new ObjectMapper().writeValueAsString(obj);  // 极度错误！
}

// ✅ 正确：声明为 static final 或 Spring Bean（单例）
private static final ObjectMapper MAPPER = new ObjectMapper();
```

### ⚠️ LocalDateTime 必须注册 JavaTimeModule

```java
// ❌ 不注册会抛 InvalidDefinitionException
ObjectMapper mapper = new ObjectMapper();
mapper.writeValueAsString(LocalDateTime.now()); // 报错！

// ✅ 注册 JavaTimeModule
mapper.registerModule(new JavaTimeModule());
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

### ⚠️ 泛型集合反序列化必须用 TypeReference

```java
// ❌ 会将 List<User> 变成 List<LinkedHashMap>，运行时 ClassCastException
List<User> list = mapper.readValue(json, List.class);

// ✅ TypeReference 保留泛型信息
List<User> list = mapper.readValue(json, new TypeReference<List<User>>() {});
```

### ⚠️ 无参构造器必须存在（或配置替代方案）

```java
// ❌ 没有无参构造器，Jackson 无法反序列化
public class User {
    public User(String name) { this.name = name; }
}

// ✅ 方案1：添加无参构造器
// ✅ 方案2：使用 @JsonCreator 标注带参构造器
@JsonCreator
public User(@JsonProperty("name") String name) { ... }

// ✅ 方案3：引入 jackson-module-parameter-names 模块（JDK 8+编译参数 -parameters）
```

### ⚠️ @JsonInclude 类级别 vs 字段级别

```java
// 字段级别：只影响该字段
@JsonInclude(JsonInclude.Include.NON_NULL)
private String remark;

// 类级别：影响所有字段（推荐全局设置）
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO { ... }

// 全局设置（最推荐）
mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
```

### ⚠️ XmlMapper 与 ObjectMapper 是不同实例

```java
// ❌ 用 ObjectMapper 处理 XML
ObjectMapper jsonMapper = new ObjectMapper();
jsonMapper.readValue(xmlStr, User.class); // 报错

// ✅ XML 必须用 XmlMapper
XmlMapper xmlMapper = new XmlMapper();
User user = xmlMapper.readValue(xmlStr, User.class);
```

### ⚠️ 多态序列化的安全注意事项

```java
// ❌ 危险：允许任意类型的多态（存在反序列化攻击）
mapper.enableDefaultTyping(); // 已废弃，禁止使用！

// ✅ 安全：使用白名单方式指定允许的子类
@JsonSubTypes({
    @JsonSubTypes.Type(value = EmailNotification.class, name = "email"),
    @JsonSubTypes.Type(value = SmsNotification.class, name = "sms")
})
```

### 性能对比参考（序列化 10 万对象）

| 库 | 序列化耗时 | 反序列化耗时 |
|----|-----------|------------|
| Fastjson2 | ~120ms | ~140ms |
| **Jackson** | **~180ms** | **~190ms** |
| Gson | ~350ms | ~380ms |

> Jackson 性能优于 Gson，略低于 Fastjson2，但功能最完整，Spring 生态首选。

---

## 运行方法

### 前提条件

```bash
# 确认 JAVA_HOME（需 JDK 8+，推荐 JDK 17）
echo $JAVA_HOME   # Linux/Mac
echo %JAVA_HOME%  # Windows

# 设置 JDK（Windows）
set JAVA_HOME=D:/jdk/jdk17
```

### 编译和运行

```bash
# 进入项目目录
cd D:/ai/workbuddy/java-tools-learning/jackson-demo

# 编译打包（跳过测试）
JAVA_HOME=D:/jdk/jdk17 mvn clean package -DskipTests

# 运行基础演示
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.jackson.JacksonBasicDemo"

# 运行进阶演示（XML/YAML/多态/枚举）
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.jackson.JacksonAdvancedDemo"

# 运行实战演示（CSV/流式API/脱敏/Spring集成说明）
JAVA_HOME=D:/jdk/jdk17 mvn exec:java -Dexec.mainClass="com.example.jackson.JacksonPracticalDemo"
```

### Windows 环境一键运行

```powershell
cd D:\ai\workbuddy\java-tools-learning\jackson-demo
$env:JAVA_HOME="D:\jdk\jdk17"
mvn clean package -DskipTests
mvn exec:java "-Dexec.mainClass=com.example.jackson.JacksonBasicDemo"
```

---

## 文件说明

```
jackson-demo/
├── pom.xml                          # Maven 配置（jackson-databind 2.17.2 + 多格式扩展）
└── src/main/java/com/example/jackson/
    ├── JacksonBasicDemo.java        # 基础：序列化/反序列化/常用注解/泛型/日期时间/树模型
    ├── JacksonAdvancedDemo.java     # 进阶：自定义序列化器/多态类型/枚举/XML/YAML/Merge
    └── JacksonPracticalDemo.java    # 实战：统一响应体/脱敏/流式API/CSV/动态JSON/Spring集成
```
