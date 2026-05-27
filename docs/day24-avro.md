# Day 24 - Apache Avro 1.11.3

## 工具简介

**Apache Avro** 是 Apache Hadoop 项目的子项目，一个高效的**数据序列化框架**，原生为大数据生态设计，是 Apache Kafka + Confluent Schema Registry 体系中事实上的**消息格式标准**。

| 属性 | 说明 |
|------|------|
| **GitHub** | https://github.com/apache/avro |
| **官方文档** | https://avro.apache.org/docs/ |
| **当前版本** | 1.11.3（本文档版本）|
| **Stars** | 2.8k+（Apache 官方维护，实际使用量极大）|
| **License** | Apache License 2.0 |

### 核心优势

1. **Schema 驱动**：数据结构由 JSON Schema（`.avsc` 文件）定义，Schema 随数据传输，双方无需提前约定
2. **二进制高效**：比 JSON 体积小 30-80%，序列化/反序列化速度更快
3. **Schema 演化**：支持 BACKWARD / FORWARD / FULL 兼容性演化，字段可以新增、删除（有限制）
4. **跨语言**：Java、Python、Go、C++、Ruby、C# 均有官方支持
5. **大数据生态**：Hadoop、Hive、Spark、Kafka 原生支持 Avro 格式

### 与其他序列化框架对比

| 特性 | Avro | Protobuf | JSON | Thrift |
|------|------|----------|------|--------|
| 格式 | 二进制 | 二进制 | 文本 | 二进制 |
| Schema 随消息 | ✅（文件头）| ❌（需提前共享）| N/A | ❌ |
| Schema 演化 | ✅ 一等公民 | ✅ 字段编号 | 手动 | ✅ |
| 可读性 | ❌（二进制）| ❌ | ✅ | ❌ |
| Kafka 生态 | ⭐⭐⭐ 首选 | ⭐⭐ | ⭐ | ⭐ |
| Schema Registry | ✅ 官方支持 | ✅ 支持 | ❌ | ❌ |

---

## Maven 依赖配置

```xml
<!-- Apache Avro 核心库 -->
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.11.3</version>
</dependency>

<!-- Avro 代码生成插件（从 .avsc 生成 Java 类，在 build/plugins 中配置）-->
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.11.3</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals><goal>schema</goal></goals>
            <configuration>
                <sourceDirectory>src/main/resources/avro</sourceDirectory>
                <outputDirectory>src/main/java</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Schema 定义（.avsc 文件）

Avro Schema 使用 JSON 格式定义，放在 `src/main/resources/avro/` 目录下：

```json
{
  "type": "record",
  "name": "User",
  "namespace": "com.example.avro",
  "doc": "用户信息",
  "fields": [
    { "name": "id",     "type": "long"                        },
    { "name": "name",   "type": "string"                      },
    { "name": "email",  "type": ["null", "string"], "default": null },
    { "name": "age",    "type": "int"                         },
    { "name": "active", "type": "boolean", "default": true    }
  ]
}
```

### 支持的字段类型

| Avro 类型 | Java 类型 | 说明 |
|-----------|-----------|------|
| `null` | null | 空类型 |
| `boolean` | Boolean | |
| `int` | Integer | 32位有符号整数 |
| `long` | Long | 64位有符号整数 |
| `float` | Float | |
| `double` | Double | |
| `bytes` | ByteBuffer | 任意字节序列 |
| `string` | String | UTF-8 字符串 |
| `record` | 生成的 Java 类 | 嵌套对象 |
| `enum` | 生成的枚举类 | |
| `array` | List<T> | |
| `map` | Map<String, T> | 键只能是 string |
| `union` | 联合类型 | `["null","string"]` 表示可空 |
| `fixed` | byte[] | 固定长度字节 |

---

## 两种 API 模式

### Generic API（动态，不需要代码生成）

```java
// 用 JSON 定义 Schema
Schema schema = new Schema.Parser().parse(schemaJson);

// 创建 Record
GenericRecord user = new GenericData.Record(schema);
user.put("id", 1L);
user.put("name", "张三");

// 序列化
ByteArrayOutputStream baos = new ByteArrayOutputStream();
BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);
new GenericDatumWriter<GenericRecord>(schema).write(user, encoder);
encoder.flush();
byte[] bytes = baos.toByteArray();

// 反序列化
GenericRecord restored = new GenericDatumReader<GenericRecord>(schema)
    .read(null, DecoderFactory.get().binaryDecoder(bytes, null));
```

### Specific API（生成代码，类型安全，推荐生产使用）

```java
// 先用 avro-maven-plugin 从 User.avsc 生成 User.java

// 构建（Builder 模式，null 安全）
User user = User.newBuilder()
    .setId(1L)
    .setName("张三")
    .setEmail("zhangsan@example.com")
    .setAge(25)
    .setActive(true)
    .build();

// 序列化（与 Generic API 相同接口）
DatumWriter<User> writer = new SpecificDatumWriter<>(User.class);
// ...
```

### SchemaBuilder API（流式构建 Schema，避免拼 JSON 字符串）

```java
Schema schema = SchemaBuilder.record("Order")
    .namespace("com.example.avro")
    .doc("订单Schema")
    .fields()
    .name("orderId").type().stringType().noDefault()
    .name("amount").type().doubleType().noDefault()
    .name("remark").type().nullable().stringType().stringDefault(null)
    .name("status").type().enumeration("OrderStatus")
        .symbols("PENDING", "PAID", "SHIPPED").noDefault()
    .endRecord();
```

---

## Avro 数据文件（.avro）

DataFileWriter / DataFileReader 支持**自包含**的 .avro 文件（文件头内嵌 Schema）：

```java
// 写入
DatumWriter<GenericRecord> dw = new GenericDatumWriter<>(schema);
DataFileWriter<GenericRecord> fw = new DataFileWriter<>(dw);
fw.setCodec(CodecFactory.deflateCodec(6));  // 可选压缩
fw.create(schema, new File("data.avro"));
fw.append(record1);
fw.append(record2);
fw.close();

// 读取（无需提前知道 Schema，从文件头自动解析）
DataFileReader<GenericRecord> fr = new DataFileReader<>(
    new File("data.avro"), new GenericDatumReader<>());
while (fr.hasNext()) {
    GenericRecord r = fr.next();
    // 处理...
}
fr.close();
```

### 支持的压缩编解码器

| 编解码器 | 方法 | 特点 |
|---------|------|------|
| 无压缩 | `CodecFactory.nullCodec()` | 默认 |
| Deflate | `CodecFactory.deflateCodec(level)` | 通用，level 1-9 |
| Snappy | `CodecFactory.snappyCodec()` | 速度快，需要依赖 |
| Bzip2 | `CodecFactory.bzip2Codec()` | 压缩率高，较慢 |
| Zstandard | `CodecFactory.zstandardCodec(level)` | 现代推荐，速度+压缩率平衡 |

---

## Schema 演化（核心特性）

Schema 演化允许新旧版本的 Producer/Consumer 共存：

```java
// ❌ 旧 Schema 写入的数据
// {id: long, name: string}

// ✅ 新 Schema 读取（新增字段必须有 default 值）
// {id: long, name: string, price: double(default=0.0), desc: ["null","string"](default=null)}

GenericRecord restored = new GenericDatumReader<GenericRecord>(writerSchema, readerSchema)
    .read(null, decoder);
// price 字段自动填充默认值 0.0
// desc 字段自动填充默认值 null
```

### Schema 演化规则

| 操作 | 兼容性 | 说明 |
|------|--------|------|
| 新增字段（有 default） | BACKWARD ✅ | 推荐方式 |
| 新增字段（无 default） | ❌ 破坏性 | 禁止 |
| 删除字段（有 default） | FORWARD ✅ | 旧 Reader 使用 default |
| 删除字段（无 default） | ❌ 破坏性 | 禁止 |
| 修改字段类型 | ❌ 破坏性 | 大多数情况禁止 |
| 新增枚举值 | FORWARD ✅ | |
| 删除枚举值 | BACKWARD ❌ | 慎重 |

---

## Spring Boot 集成方式

### 1. 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.11.3</version>
</dependency>

<!-- Kafka + Schema Registry（需要 Confluent 仓库）-->
<dependency>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-avro-serializer</artifactId>
    <version>7.6.0</version>
</dependency>

<!-- Confluent 仓库 -->
<repositories>
    <repository>
        <id>confluent</id>
        <url>https://packages.confluent.io/maven/</url>
    </repository>
</repositories>
```

### 2. application.yml 配置

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      properties:
        schema.registry.url: http://localhost:8081
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: http://localhost:8081
        specific.avro.reader: true  # 使用 Specific API（代码生成类）
```

### 3. 发送 Avro 消息

```java
@Service
public class OrderEventProducer {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(Order order) {
        OrderEvent event = OrderEvent.newBuilder()
            .setEventType("ORDER_CREATED")
            .setOrderId(order.getId())
            .setTimestamp(System.currentTimeMillis())
            .build();
        kafkaTemplate.send("order-events", order.getId(), event);
    }
}
```

### 4. 消费 Avro 消息

```java
@KafkaListener(topics = "order-events", groupId = "inventory-group")
public void handle(@Payload OrderEvent event) {
    log.info("收到事件: type={}, orderId={}", event.getEventType(), event.getOrderId());
    // 业务处理...
}
```

### 5. 不使用 Schema Registry 的轻量集成（嵌入式 Schema）

```java
@Bean
public ProducerFactory<String, byte[]> producerFactory() {
    return new DefaultKafkaProducerFactory<>(Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class
    ));
}

// 手动序列化
public void send(GenericRecord record, Schema schema) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    BinaryEncoder enc = EncoderFactory.get().binaryEncoder(baos, null);
    new GenericDatumWriter<>(schema).write(record, enc);
    enc.flush();
    kafkaTemplate.send("topic", baos.toByteArray());
}
```

---

## 注意事项

### ⚠️ Bug 风险

1. **union 类型 null 顺序问题**
   ```json
   // ✅ 正确：null 在第一位（Confluent Schema Registry 强制要求）
   "type": ["null", "string"], "default": null

   // ❌ 错误：null 不在第一位时，default 不能为 null
   "type": ["string", "null"]
   ```

2. **编码器 flush 必须调用**
   ```java
   encoder.flush();  // ❗ 忘记 flush 会导致数据截断，序列化字节不完整
   ```

3. **Schema 解析顺序**：包含嵌套类型的多 .avsc 文件必须按依赖顺序解析，否则报 `SchemaParseException: Undefined name`

### ⚠️ 性能问题

1. **BinaryEncoder/Decoder 复用**：高频序列化场景应复用 Encoder/Decoder 对象（通过 `binaryEncoder(out, existing)` 第二参数传入已有实例），避免每次创建新对象

2. **Schema 缓存**：`Schema.Parser` 每次 `parse()` 都会重新解析 JSON，应将 Schema 对象缓存为静态变量或单例

3. **DataFileWriter 批量同步**：DataFileWriter 内部有同步块，高并发写入同一文件需加锁或使用独立写入流

### ⚠️ 使用限制

1. **Map 键必须是 string**：Avro Map 类型的键只能是字符串，不支持其他类型

2. **不支持继承/多态**：Avro 没有面向对象的继承概念，多态需通过 union 类型实现

3. **枚举值不能重命名**：重命名枚举值会破坏所有兼容性级别

4. **Schema Registry 部署依赖**：使用 `kafka-avro-serializer` 必须运行 Confluent Schema Registry 服务，增加运维成本。小型项目可以考虑使用 `apicurio-registry`（开源替代方案）

5. **Java 8 注意**：Avro 1.11.x 完全兼容 Java 8，但需要 `avro-maven-plugin` 同版本，否则代码生成可能报兼容性警告

---

## 运行方法

```bash
# 进入项目目录
cd java-tools-learning/avro-demo

# 编译打包
mvn clean package -DskipTests

# 运行基础演示
java -cp target/avro-demo-1.0-SNAPSHOT.jar com.example.avro.AvroBasicDemo

# 运行进阶演示
java -cp target/avro-demo-1.0-SNAPSHOT.jar com.example.avro.AvroAdvancedDemo

# 运行实战演示
java -cp target/avro-demo-1.0-SNAPSHOT.jar com.example.avro.AvroPracticalDemo
```

---

## 学习要点总结

| 概念 | 要点 |
|------|------|
| Generic API | 动态 Schema，无需代码生成，适合框架层/工具开发 |
| Specific API | 代码生成，类型安全，适合业务代码 |
| Schema Evolution | 新增字段必须有 default；修改类型会破坏兼容 |
| Union 类型 | null 联合体是 Avro 实现可空字段的唯一方式 |
| .avro 文件 | 自包含（Schema 内嵌文件头），大数据管道首选 |
| Kafka 集成 | 推荐 Specific API + Schema Registry，避免 Schema 随消息传输 |
