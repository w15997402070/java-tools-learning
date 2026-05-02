# Day 13: Fastjson2 - 阿里巴巴高性能 JSON 库

## 1. 工具简介

**Fastjson2** 是阿里巴巴开源的高性能 JSON 库，是 Fastjson 的彻底升级版本，修复了 Fastjson 1.x 中的大量安全和性能问题。

| 项目 | 信息 |
|------|------|
| GitHub | https://github.com/alibaba/fastjson2 |
| 星标 | 4.5k+ |
| 最新版本 | 2.0.48（Java 8 兼容） |
| License | Apache 2.0 |
| 文档 | https://github.com/alibaba/fastjson2/wiki |

### 为什么选择 Fastjson2？

- 🚀 **高性能**：比 Jackson/Fastjson1 更快的序列化/反序列化速度
- 🔒 **安全**：彻底重构，修复了 Fastjson1 的 AutoType 安全漏洞
- ✅ **Java 8+ 兼容**：支持 Java 8 及以上版本
- 🎯 **API 简洁**：toJSONString / parseObject 一行搞定
- 🔧 **功能丰富**：支持注解、JSONPath、流式 API、循环引用处理

---

## 2. Maven 依赖配置

```xml
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.48</version>
</dependency>
```

> ⚠️ **注意**：Fastjson2 的 groupId 和 artifactId 与 Fastjson1 不同！
> - Fastjson1：`com.alibaba` : `fastjson`
> - Fastjson2：`com.alibaba.fastjson2` : `fastjson2`

---

## 3. 核心功能演示

### 3.1 基础序列化/反序列化

```java
// 序列化：Java 对象 → JSON 字符串
User user = new User("张三", 28, "zhangsan@example.com");
String json = JSON.toJSONString(user);

// 反序列化：JSON 字符串 → Java 对象
User parsed = JSON.parseObject(json, User.class);

// 格式化输出
String pretty = JSON.toJSONString(user, JSONWriter.Feature.PrettyFormat);
```

### 3.2 集合和泛型处理

```java
// 序列化 List
List<User> users = Arrays.asList(user1, user2);
String listJson = JSON.toJSONString(users);

// 反序列化 List
List<User> parsedList = JSON.parseArray(listJson, User.class);

// 复杂泛型：必须使用 TypeReference
ApiResponse<User> resp = JSON.parseObject(
    json,
    new TypeReference<ApiResponse<User>>() {}
);
```

### 3.3 @JSONField 注解

```java
static class Product {
    // 指定序列化后的字段名
    @JSONField(name = "product_name")
    private String name;

    // 指定日期格式
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // 序列化时忽略该字段
    @JSONField(serialize = false)
    private String secretKey;

    // 指定字段顺序
    @JSONField(ordinal = 1)
    private Double price;
}
```

### 3.4 JSONPath 查询

```java
JSONObject root = JSON.parseObject(json);

// 获取所有书名
List<Object> titles = root.getJSONPath("$.store.books[*].title");

// 条件过滤：价格大于 100 的书籍
List<Object> expensive = root.getJSONPath("$.store.books[?(@.price > 100)]");
```

---

## 4. Spring Boot 集成方式

### 4.1 配置 HttpMessageConverter（推荐）

```java
@Configuration
public class JsonConfig {

    @Bean
    public HttpMessageConverter<?> fastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

        // 配置序列化特性
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(
                JSONWriter.Feature.PrettyFormat,
                JSONWriter.Feature.WriteMapNullValue,
                JSONWriter.Feature.WriteBigDecimalAsPlain
        );

        // 配置日期格式
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");

        converter.setFastJsonConfig(config);

        // 设置支持的 MediaType
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        converter.setSupportedMediaTypes(mediaTypes);

        return converter;
    }
}
```

### 4.2 直接使用（非 Spring 项目）

```java
@RestController
public class UserController {

    @PostMapping("/user")
    public String createUser(@RequestBody String body) {
        // 直接解析请求体
        User user = JSON.parseObject(body, User.class);
        // ...
        return JSON.toJSONString(ApiResponse.success(user));
    }
}
```

---

## 5. 注意事项（Bug 风险、性能问题、使用限制）

### ⚠️ 风险 1：类型擦除导致反序列化失败

```java
// ❌ 错误：data 字段会变成 JSONObject，而非目标类型
ApiResponse<User> resp = JSON.parseObject(json, ApiResponse.class);

// ✅ 正确：使用 TypeReference 保持泛型类型
ApiResponse<User> resp = JSON.parseObject(
    json,
    new TypeReference<ApiResponse<User>>() {}
);
```

### ⚠️ 风险 2：boolean 字段命名问题

```java
// ❌ 错误：字段名不要用 is 开头
private boolean isActive;  // 序列化后字段名会变成 "active"，而非 "isActive"

// ✅ 正确
private boolean active;
```

### ⚠️ 风险 3：BigDecimal 精度丢失

```java
// ❌ 默认可能导致精度问题
String json = JSON.toJSONString(new Amount(new BigDecimal("123.456789")));

// ✅ 使用 WriteBigDecimalAsPlain 保持精度
String json = JSON.toJSONString(amount, JSONWriter.Feature.WriteBigDecimalAsPlain);
```

### ⚠️ 风险 4：循环引用导致栈溢出

```java
// 父子节点互相引用时，默认会生成引用标识 $ref
// 如需禁用（不推荐）：JSONWriter.Feature.DisableCircularCheck
String json = JSON.toJSONString(root);
// 输出：{"name":"root","children":[{"name":"child1","parent":{"$ref":".."}}]}
```

### ⚠️ 风险 5：Fastjson1 和 Fastjson2 混用

- 两者包名不同，**可以同时存在**于项目中
- 建议新项目直接使用 Fastjson2，不要混用
- 迁移时需注意 API 差异（Fastjson2 的包名是 `com.alibaba.fastjson2.*`）

### 性能建议

1. **重复序列化同一类型时，缓存 Class 信息**（Fastjson2 已内置缓存，无需手动处理）
2. **大文件处理使用流式 API**（`JSONReader`），避免 OOM
3. **禁用不需要的 Feature**，可以提升性能
4. **生产环境建议开启 `JSONWriter.Feature.WriteBigDecimalAsPlain`** 保持精度

---

## 6. 运行方法

### 6.1 运行基础演示

```bash
cd fastjson2-demo
mvn clean compile exec:java -Dexec.mainClass="com.example.fastjson2.FastJson2BasicDemo"
```

### 6.2 运行高级功能演示

```bash
mvn exec:java -Dexec.mainClass="com.example.fastjson2.FastJson2AdvancedDemo"
```

### 6.3 运行实战场景演示

```bash
mvn exec:java -Dexec.mainClass="com.example.fastjson2.FastJson2PracticalDemo"
```

### 6.4 运行所有演示（Maven 方式）

在 `pom.xml` 中配置 `exec-maven-plugin` 后，可以直接运行：

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.example.fastjson2.FastJson2BasicDemo"
```

---

## 7. Fastjson2 vs Fastjson1 vs Jackson 对比

| 特性 | Fastjson2 | Fastjson1 | Jackson |
|------|-----------|-----------|---------|
| 性能 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| 安全性 | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| API 简洁性 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Spring 默认 | ❌ | ❌ | ✅ |
| 中文文档 | ✅ | ✅ | ❌ |

---

## 8. 参考资料

- 官方 GitHub：https://github.com/alibaba/fastjson2
- 官方 Wiki：https://github.com/alibaba/fastjson2/wiki
- 版本发布：https://github.com/alibaba/fastjson2/releases
- 迁移指南（Fastjson1 → Fastjson2）：https://github.com/alibaba/fastjson2/wiki/fastjson2_%E8%BF%81%E7%A7%BB
