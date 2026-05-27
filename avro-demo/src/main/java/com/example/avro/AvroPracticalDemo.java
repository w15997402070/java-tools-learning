package com.example.avro;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Apache Avro 实战演示
 *
 * <p>贴近生产环境的三个场景：
 * <ol>
 *   <li>Kafka 消息体设计：事件驱动架构中 Avro 消息的标准格式（含 envelope + payload）</li>
 *   <li>ETL 数据管道：大批量 Record 写入 .avro 文件，再分批读取处理（流式模拟）</li>
 *   <li>Spring Boot 集成指南：以代码注释形式展示 Avro + Kafka + Schema Registry 配置要点</li>
 * </ol>
 */
public class AvroPracticalDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== Apache Avro 实战演示 ==========\n");

        demo1_kafkaMessageDesign();
        demo2_etlPipeline();
        demo3_springBootIntegrationGuide();

        System.out.println("\n========== 实战演示完毕 ==========");
    }

    // ----------------------------------------------------------------
    // 实战1：Kafka 消息体设计
    //   标准做法：外层 Envelope（type + version + traceId）+ 内层 payload（动态类型）
    // ----------------------------------------------------------------
    static void demo1_kafkaMessageDesign() throws Exception {
        System.out.println("--- 实战1：Kafka 消息体设计（Envelope模式）---");

        // ① 定义事件 Envelope Schema
        Schema orderEventSchema = SchemaBuilder.record("OrderEvent")
                .namespace("com.example.avro.kafka")
                .doc("订单领域事件 - Kafka消息标准格式")
                .fields()
                // 消息信封
                .name("eventType").type().stringType().noDefault()          // 事件类型
                .name("eventVersion").type().stringType().stringDefault("1.0") // Schema版本
                .name("traceId").type().stringType().noDefault()             // 链路追踪ID
                .name("timestamp").type().longType().noDefault()             // 事件产生时间
                .name("producerService").type().stringType().noDefault()     // 生产者服务名
                // 业务 payload（嵌套 Record）
                .name("orderId").type().stringType().noDefault()
                .name("userId").type().longType().noDefault()
                .name("amount").type().doubleType().noDefault()
                .name("currency").type().stringType().stringDefault("CNY")
                .name("status").type().stringType().noDefault()
                // 扩展字段（Map，允许后续无破坏性扩展）
                .name("extra").type().map().values().stringType().noDefault()
                .endRecord();

        // ② 构造三种典型事件
        String[] eventTypes = {"ORDER_CREATED", "ORDER_PAID", "ORDER_SHIPPED"};
        String[] statuses = {"PENDING", "PAID", "SHIPPED"};

        List<byte[]> messages = new ArrayList<>();
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(orderEventSchema);

        System.out.println("生产 Kafka 消息:");
        for (int i = 0; i < 3; i++) {
            GenericRecord event = new GenericData.Record(orderEventSchema);
            event.put("eventType", eventTypes[i]);
            event.put("eventVersion", "1.0");
            event.put("traceId", "trace-" + UUID.randomUUID().toString().substring(0, 8));
            event.put("timestamp", System.currentTimeMillis());
            event.put("producerService", "order-service");
            event.put("orderId", "ORD-2026-" + String.format("%06d", 1000 + i));
            event.put("userId", 10086L + i);
            event.put("amount", 299.0 + i * 100);
            event.put("currency", "CNY");
            event.put("status", statuses[i]);
            Map<String, Object> extra = new HashMap<>();
            extra.put("channel", "APP");
            extra.put("region", "CN-BEIJING");
            event.put("extra", extra);

            // 序列化为 byte[]（模拟 Kafka Producer 发送）
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BinaryEncoder enc = EncoderFactory.get().binaryEncoder(baos, null);
            writer.write(event, enc);
            enc.flush();
            messages.add(baos.toByteArray());

            System.out.printf("  [%s] orderId=%s, amount=%.1f, bytes=%d%n",
                    eventTypes[i], event.get("orderId"), event.get("amount"), baos.size());
        }

        // ③ 消费端反序列化（模拟 Kafka Consumer）
        System.out.println("\n消费 Kafka 消息:");
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(orderEventSchema);
        for (byte[] msg : messages) {
            GenericRecord event = reader.read(null, DecoderFactory.get().binaryDecoder(msg, null));
            System.out.printf("  消费: eventType=%-16s traceId=%s%n",
                    event.get("eventType"), event.get("traceId"));
        }

        System.out.println("\n✓ Envelope 模式优势：统一消息格式、链路追踪、无破坏性扩展（extra Map）");
        System.out.println();
    }

    // ----------------------------------------------------------------
    // 实战2：ETL 数据管道 - 大批量写入 .avro + 流式分批读取
    // ----------------------------------------------------------------
    static void demo2_etlPipeline() throws Exception {
        System.out.println("--- 实战2：ETL数据管道（大批量写入+分批读取）---");

        Schema schema = SchemaBuilder.record("SalesRecord")
                .namespace("com.example.avro.etl")
                .fields()
                .name("recordId").type().longType().noDefault()
                .name("storeId").type().intType().noDefault()
                .name("productCode").type().stringType().noDefault()
                .name("quantity").type().intType().noDefault()
                .name("unitPrice").type().doubleType().noDefault()
                .name("saleDate").type().stringType().noDefault()
                .name("salesPersonId").type().intType().noDefault()
                .endRecord();

        // ① ETL 抽取阶段：写入 .avro 文件（模拟从数据库导出）
        File dataFile = File.createTempFile("sales_etl", ".avro");
        dataFile.deleteOnExit();

        int totalRecords = 5000;
        long writeStart = System.currentTimeMillis();

        DataFileWriter<GenericRecord> fileWriter = new DataFileWriter<>(
                new GenericDatumWriter<>(schema));
        // ETL场景通常选 deflate 压缩以减少存储
        fileWriter.setCodec(org.apache.avro.file.CodecFactory.deflateCodec(6));
        fileWriter.create(schema, dataFile);

        Random rand = new Random(42);
        String[] products = {"P001", "P002", "P003", "P004", "P005"};
        for (int i = 0; i < totalRecords; i++) {
            GenericRecord r = new GenericData.Record(schema);
            r.put("recordId", (long) (i + 1));
            r.put("storeId", rand.nextInt(10) + 1);
            r.put("productCode", products[rand.nextInt(products.length)]);
            r.put("quantity", rand.nextInt(20) + 1);
            r.put("unitPrice", 10.0 + rand.nextInt(490));
            r.put("saleDate", "2026-05-" + String.format("%02d", rand.nextInt(27) + 1));
            r.put("salesPersonId", rand.nextInt(50) + 1);
            fileWriter.append(r);
        }
        fileWriter.close();
        long writeEnd = System.currentTimeMillis();

        System.out.printf("写入 %,d 条记录，文件大小: %,d bytes，耗时: %d ms%n",
                totalRecords, dataFile.length(), writeEnd - writeStart);

        // ② ETL 转换阶段：分批读取、聚合统计（模拟按门店汇总销售额）
        Map<Integer, Double> storeSales = new HashMap<>();
        long readStart = System.currentTimeMillis();

        DataFileReader<GenericRecord> fileReader = new DataFileReader<>(dataFile,
                new GenericDatumReader<>());
        int processed = 0;
        while (fileReader.hasNext()) {
            GenericRecord r = fileReader.next();
            int storeId = (Integer) r.get("storeId");
            double amount = (Integer) r.get("quantity") * (Double) r.get("unitPrice");
            storeSales.merge(storeId, amount, Double::sum);
            processed++;
        }
        fileReader.close();
        long readEnd = System.currentTimeMillis();

        System.out.printf("读取并聚合 %,d 条记录，耗时: %d ms%n", processed, readEnd - readStart);

        // ③ ETL 加载阶段：输出聚合结果（模拟写入数仓）
        System.out.println("各门店销售额汇总（Top 5）:");
        storeSales.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> System.out.printf("  门店 #%02d: ¥%,.2f%n", e.getKey(), e.getValue()));

        System.out.println("\n✓ Avro + 压缩 是大数据 ETL 管道的标准选择（Hadoop、Hive、Spark均原生支持）");
        System.out.println();
    }

    // ----------------------------------------------------------------
    // 实战3：Spring Boot 集成指南（Avro + Kafka + Confluent Schema Registry）
    // ----------------------------------------------------------------
    static void demo3_springBootIntegrationGuide() {
        System.out.println("--- 实战3：Spring Boot 集成指南 ---\n");

        System.out.println("【1】Maven 依赖（pom.xml）：");
        System.out.println("  <!-- Avro 核心 -->");
        System.out.println("  <dependency>");
        System.out.println("    <groupId>org.apache.avro</groupId>");
        System.out.println("    <artifactId>avro</artifactId>");
        System.out.println("    <version>1.11.3</version>");
        System.out.println("  </dependency>");
        System.out.println("  <!-- Kafka + Avro Serializer（需要 Confluent 仓库）-->");
        System.out.println("  <dependency>");
        System.out.println("    <groupId>io.confluent</groupId>");
        System.out.println("    <artifactId>kafka-avro-serializer</artifactId>");
        System.out.println("    <version>7.6.0</version>");
        System.out.println("  </dependency>");
        System.out.println("  <!-- Spring Boot Kafka Starter -->");
        System.out.println("  <dependency>");
        System.out.println("    <groupId>org.springframework.kafka</groupId>");
        System.out.println("    <artifactId>spring-kafka</artifactId>");
        System.out.println("  </dependency>");
        System.out.println();

        System.out.println("【2】application.yml 配置：");
        System.out.println("  spring:");
        System.out.println("    kafka:");
        System.out.println("      bootstrap-servers: localhost:9092");
        System.out.println("      producer:");
        System.out.println("        key-serializer: org.apache.kafka.common.serialization.StringSerializer");
        System.out.println("        value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer");
        System.out.println("        properties:");
        System.out.println("          schema.registry.url: http://localhost:8081");
        System.out.println("      consumer:");
        System.out.println("        key-deserializer: org.apache.kafka.common.serialization.StringDeserializer");
        System.out.println("        value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer");
        System.out.println("        properties:");
        System.out.println("          schema.registry.url: http://localhost:8081");
        System.out.println("          specific.avro.reader: true  # 使用 Specific API（代码生成类）");
        System.out.println();

        System.out.println("【3】Avro Maven 插件（从 .avsc 生成 Java 类）：");
        System.out.println("  <plugin>");
        System.out.println("    <groupId>org.apache.avro</groupId>");
        System.out.println("    <artifactId>avro-maven-plugin</artifactId>");
        System.out.println("    <version>1.11.3</version>");
        System.out.println("    <executions><execution>");
        System.out.println("      <phase>generate-sources</phase>");
        System.out.println("      <goals><goal>schema</goal></goals>");
        System.out.println("      <configuration>");
        System.out.println("        <sourceDirectory>src/main/resources/avro</sourceDirectory>");
        System.out.println("        <outputDirectory>src/main/java</outputDirectory>");
        System.out.println("      </configuration>");
        System.out.println("    </execution></executions>");
        System.out.println("  </plugin>");
        System.out.println();

        System.out.println("【4】Producer 代码示例（Specific API，使用生成的 OrderEvent 类）：");
        System.out.println("  @Autowired KafkaTemplate<String, OrderEvent> kafkaTemplate;");
        System.out.println();
        System.out.println("  public void publishOrderEvent(Order order) {");
        System.out.println("    OrderEvent event = OrderEvent.newBuilder()");
        System.out.println("      .setEventType(\"ORDER_CREATED\")");
        System.out.println("      .setOrderId(order.getId())");
        System.out.println("      .setTimestamp(System.currentTimeMillis())");
        System.out.println("      .build();");
        System.out.println("    kafkaTemplate.send(\"order-events\", order.getId(), event);");
        System.out.println("  }");
        System.out.println();

        System.out.println("【5】Consumer 代码示例：");
        System.out.println("  @KafkaListener(topics = \"order-events\", groupId = \"inventory-group\")");
        System.out.println("  public void handleOrderEvent(@Payload OrderEvent event,");
        System.out.println("                               @Header KafkaHeaders headers) {");
        System.out.println("    log.info(\"收到事件: type={}, orderId={}\",");
        System.out.println("             event.getEventType(), event.getOrderId());");
        System.out.println("    // 业务处理...");
        System.out.println("  }");
        System.out.println();

        System.out.println("【6】关键注意事项：");
        System.out.println("  ★ Schema Registry 模式：生产环境必须用 Confluent Schema Registry");
        System.out.println("    避免将 Schema 内嵌在每条消息中（极大浪费带宽）");
        System.out.println("  ★ Schema 兼容性检查：Schema Registry 支持 BACKWARD/FORWARD/FULL 兼容模式");
        System.out.println("    推荐使用 BACKWARD（新版Reader能读旧版数据），禁止删除无默认值字段");
        System.out.println("  ★ 枚举演化：新增枚举值需要 FORWARD 兼容，删除枚举值会破坏 BACKWARD 兼容");
        System.out.println("  ★ null 联合类型顺序：[\"null\",\"string\"] 默认值为null；[\"string\",\"null\"] 默认无null");
        System.out.println("    Kafka Schema Registry 强制要求 null 排在 union 第一位");
        System.out.println("  ★ 不要直接向生产 Schema Registry 注册新 Schema，先在测试环境验证兼容性");

        System.out.println("\n✓ Avro + Schema Registry + Kafka 是大型微服务架构的数据契约标准");
    }
}
