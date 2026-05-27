package com.example.avro;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Apache Avro 进阶演示
 *
 * <p>覆盖内容：
 * <ol>
 *   <li>Avro 数据文件（.avro）的读写：DataFileWriter / DataFileReader</li>
 *   <li>Schema 演化（Schema Evolution）：Reader Schema 与 Writer Schema 不同时的兼容处理</li>
 *   <li>Union 类型（可空字段）和 Map 类型的使用</li>
 *   <li>压缩编解码器：Snappy / Deflate 压缩写文件</li>
 *   <li>序列化性能对比：Avro 二进制 vs JSON</li>
 * </ol>
 */
public class AvroAdvancedDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== Apache Avro 进阶演示 ==========\n");

        demo1_dataFile();
        demo2_schemaEvolution();
        demo3_unionAndMap();
        demo4_compression();
        demo5_performanceComparison();

        System.out.println("\n========== 进阶演示完毕 ==========");
    }

    // ----------------------------------------------------------------
    // 演示1：DataFileWriter / DataFileReader（自包含 .avro 文件）
    // ----------------------------------------------------------------
    static void demo1_dataFile() throws Exception {
        System.out.println("--- 演示1：Avro数据文件读写（.avro格式）---");

        Schema schema = SchemaBuilder.record("Log")
                .namespace("com.example.avro")
                .fields()
                .name("level").type().stringType().noDefault()
                .name("message").type().stringType().noDefault()
                .name("timestamp").type().longType().noDefault()
                .name("threadId").type().intType().noDefault()
                .endRecord();

        // ① 写入 .avro 文件
        File avroFile = File.createTempFile("avro_logs", ".avro");
        avroFile.deleteOnExit();

        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
        DataFileWriter<GenericRecord> fileWriter = new DataFileWriter<>(datumWriter);
        fileWriter.create(schema, avroFile);

        String[] levels = {"INFO", "WARN", "ERROR", "DEBUG"};
        for (int i = 0; i < 10; i++) {
            GenericRecord log = new GenericData.Record(schema);
            log.put("level", levels[i % levels.length]);
            log.put("message", "系统日志消息 #" + i);
            log.put("timestamp", System.currentTimeMillis() + i * 1000L);
            log.put("threadId", i % 4 + 1);
            fileWriter.append(log);
        }
        fileWriter.close();
        System.out.println("写入 .avro 文件成功，文件大小: " + avroFile.length() + " bytes");
        System.out.println("文件路径: " + avroFile.getAbsolutePath());

        // ② 读取 .avro 文件（Schema 内嵌在文件头）
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
        DataFileReader<GenericRecord> fileReader = new DataFileReader<>(avroFile, datumReader);

        System.out.println("\n文件内嵌 Schema: " + fileReader.getSchema().getName());
        System.out.println("读取所有记录:");
        int count = 0;
        while (fileReader.hasNext()) {
            GenericRecord record = fileReader.next();
            if (count < 3) { // 只打印前3条
                System.out.println("  [" + count + "] " + record.get("level") + " - " + record.get("message"));
            }
            count++;
        }
        System.out.println("  ... 共读取 " + count + " 条记录");
        fileReader.close();
        System.out.println();
    }

    // ----------------------------------------------------------------
    // 演示2：Schema 演化（向后/向前兼容）
    //   Writer Schema（旧）写入数据，Reader Schema（新）来读取
    // ----------------------------------------------------------------
    static void demo2_schemaEvolution() throws Exception {
        System.out.println("--- 演示2：Schema演化（向后兼容）---");

        // 旧版 Schema（写入时使用）：只有 id 和 name
        Schema writerSchema = new Schema.Parser().parse("{"
                + "\"type\":\"record\","
                + "\"name\":\"Product\","
                + "\"namespace\":\"com.example.avro\","
                + "\"fields\":["
                + "  {\"name\":\"id\",\"type\":\"long\"},"
                + "  {\"name\":\"name\",\"type\":\"string\"}"
                + "]}");

        // 新版 Schema（读取时使用）：新增 price（有默认值）+ description（可空）
        Schema readerSchema = new Schema.Parser().parse("{"
                + "\"type\":\"record\","
                + "\"name\":\"Product\","
                + "\"namespace\":\"com.example.avro\","
                + "\"fields\":["
                + "  {\"name\":\"id\",\"type\":\"long\"},"
                + "  {\"name\":\"name\",\"type\":\"string\"},"
                + "  {\"name\":\"price\",\"type\":\"double\",\"default\":0.0},"
                + "  {\"name\":\"description\",\"type\":[\"null\",\"string\"],\"default\":null}"
                + "]}");

        // 用旧版 Schema 写入数据
        GenericRecord oldProduct = new GenericData.Record(writerSchema);
        oldProduct.put("id", 100L);
        oldProduct.put("name", "旧版商品");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);
        new GenericDatumWriter<GenericRecord>(writerSchema).write(oldProduct, encoder);
        encoder.flush();

        // 用新版 Schema 读取：新增字段使用默认值填充
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(baos.toByteArray(), null);
        // GenericDatumReader(writerSchema, readerSchema) 支持 Schema 演化
        GenericRecord newProduct = new GenericDatumReader<GenericRecord>(writerSchema, readerSchema)
                .read(null, decoder);

        System.out.println("Writer Schema 字段: " + writerSchema.getFields().stream()
                .map(Schema.Field::name).collect(java.util.stream.Collectors.joining(", ")));
        System.out.println("Reader Schema 字段: " + readerSchema.getFields().stream()
                .map(Schema.Field::name).collect(java.util.stream.Collectors.joining(", ")));
        System.out.println("读取结果 id=" + newProduct.get("id")
                + ", name=" + newProduct.get("name")
                + ", price=" + newProduct.get("price")           // 使用默认值 0.0
                + ", description=" + newProduct.get("description")); // 使用默认值 null
        System.out.println("Schema演化（向后兼容）验证成功！新增字段使用默认值填充。");
        System.out.println();
    }

    // ----------------------------------------------------------------
    // 演示3：Union 类型 + Map 类型
    // ----------------------------------------------------------------
    static void demo3_unionAndMap() throws Exception {
        System.out.println("--- 演示3：Union类型 + Map类型 ---");

        // Union 类型：表示"可以是 null 或 string"，是 Avro 实现可空字段的方式
        Schema schema = SchemaBuilder.record("Config")
                .namespace("com.example.avro")
                .fields()
                .name("serviceName").type().stringType().noDefault()
                // Union：null | string（可空字段，Kafka Schema Registry 推荐写法：null放第一位）
                .name("description").type().nullable().stringType().stringDefault(null)
                // Map 类型：String -> String 的键值对
                .name("properties").type().map().values().stringType().noDefault()
                // Union：null | int（可空整型）
                .name("timeout").type().nullable().intType().intDefault(30)
                .endRecord();

        System.out.println("Schema 结构:");
        schema.getFields().forEach(f ->
                System.out.println("  " + f.name() + ": " + f.schema()));

        // 创建包含 Map 的 Record
        GenericRecord config = new GenericData.Record(schema);
        config.put("serviceName", "order-service");
        config.put("description", "订单微服务配置");  // 有值
        Map<String, Object> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "8080");
        props.put("maxConnections", "100");
        config.put("properties", props);
        config.put("timeout", null);  // null 值

        System.out.println("\n创建 Config Record: " + config);

        // 序列化 + 反序列化验证
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryEncoder enc = EncoderFactory.get().binaryEncoder(baos, null);
        new GenericDatumWriter<GenericRecord>(schema).write(config, enc);
        enc.flush();

        GenericRecord restored = new GenericDatumReader<GenericRecord>(schema)
                .read(null, DecoderFactory.get().binaryDecoder(baos.toByteArray(), null));

        @SuppressWarnings("unchecked")
        Map<?, ?> restoredProps = (Map<?, ?>) restored.get("properties");
        System.out.println("Map properties.host = " + restoredProps.get("host"));
        System.out.println("Union timeout = " + restored.get("timeout") + " (null值正常)");
        System.out.println();
    }

    // ----------------------------------------------------------------
    // 演示4：压缩编解码（Deflate）
    // ----------------------------------------------------------------
    static void demo4_compression() throws Exception {
        System.out.println("--- 演示4：压缩编解码（Deflate）---");

        Schema schema = SchemaBuilder.record("TextDoc")
                .namespace("com.example.avro")
                .fields()
                .name("docId").type().longType().noDefault()
                .name("content").type().stringType().noDefault()
                .endRecord();

        // 生成测试数据：模拟高重复率的日志内容
        String baseContent = "2026-05-27 INFO [com.example.service.OrderService] 处理订单请求 orderId=";
        int recordCount = 100;

        // 无压缩写入
        File noCompressFile = File.createTempFile("avro_nocompress", ".avro");
        noCompressFile.deleteOnExit();
        DataFileWriter<GenericRecord> w1 = new DataFileWriter<>(new GenericDatumWriter<>(schema));
        w1.create(schema, noCompressFile);
        for (int i = 0; i < recordCount; i++) {
            GenericRecord r = new GenericData.Record(schema);
            r.put("docId", (long) i);
            r.put("content", baseContent + String.format("%08d", i));
            w1.append(r);
        }
        w1.close();

        // Deflate 压缩写入
        File compressFile = File.createTempFile("avro_deflate", ".avro");
        compressFile.deleteOnExit();
        DataFileWriter<GenericRecord> w2 = new DataFileWriter<>(new GenericDatumWriter<>(schema));
        w2.setCodec(CodecFactory.deflateCodec(6)); // 压缩级别 6
        w2.create(schema, compressFile);
        for (int i = 0; i < recordCount; i++) {
            GenericRecord r = new GenericData.Record(schema);
            r.put("docId", (long) i);
            r.put("content", baseContent + String.format("%08d", i));
            w2.append(r);
        }
        w2.close();

        System.out.println("写入 " + recordCount + " 条记录：");
        System.out.println("  无压缩文件大小:  " + noCompressFile.length() + " bytes");
        System.out.println("  Deflate压缩大小: " + compressFile.length() + " bytes");
        System.out.printf("  压缩比: %.1f%%（压缩后体积约为原来的%.1f%%）%n",
                (1.0 - (double) compressFile.length() / noCompressFile.length()) * 100,
                (double) compressFile.length() / noCompressFile.length() * 100);

        // 读取压缩文件（DataFileReader 自动识别编解码器）
        DataFileReader<GenericRecord> reader = new DataFileReader<>(compressFile,
                new GenericDatumReader<>());
        System.out.println("压缩文件编解码器: " + reader.getMetaString("avro.codec"));
        int cnt = 0;
        while (reader.hasNext()) { reader.next(); cnt++; }
        reader.close();
        System.out.println("读取验证：共读取 " + cnt + " 条，数据完整。");
        System.out.println();
    }

    // ----------------------------------------------------------------
    // 演示5：Avro 二进制 vs JSON 序列化性能对比
    // ----------------------------------------------------------------
    static void demo5_performanceComparison() throws Exception {
        System.out.println("--- 演示5：Avro 二进制 vs JSON 性能对比 ---");

        Schema schema = SchemaBuilder.record("Metric")
                .namespace("com.example.avro")
                .fields()
                .name("metricName").type().stringType().noDefault()
                .name("value").type().doubleType().noDefault()
                .name("timestamp").type().longType().noDefault()
                .name("host").type().stringType().noDefault()
                .endRecord();

        int rounds = 5000;

        // 准备数据
        GenericRecord metric = new GenericData.Record(schema);
        metric.put("metricName", "cpu.usage.percent");
        metric.put("value", 72.5);
        metric.put("timestamp", 1748282415000L);
        metric.put("host", "prod-server-01");

        DatumWriter<GenericRecord> avroWriter = new GenericDatumWriter<>(schema);
        DatumReader<GenericRecord> avroReader = new GenericDatumReader<>(schema);

        // Avro 二进制
        long t1 = System.nanoTime();
        byte[] lastAvroBytes = null;
        for (int i = 0; i < rounds; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
            BinaryEncoder enc = EncoderFactory.get().binaryEncoder(baos, null);
            avroWriter.write(metric, enc);
            enc.flush();
            lastAvroBytes = baos.toByteArray();
            avroReader.read(null, DecoderFactory.get().binaryDecoder(lastAvroBytes, null));
        }
        long avroTime = System.nanoTime() - t1;

        // JSON（Avro JSON Encoder）
        long t2 = System.nanoTime();
        byte[] lastJsonBytes = null;
        for (int i = 0; i < rounds; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
            JsonEncoder je = EncoderFactory.get().jsonEncoder(schema, baos);
            avroWriter.write(metric, je);
            je.flush();
            lastJsonBytes = baos.toByteArray();
            new GenericDatumReader<GenericRecord>(schema)
                    .read(null, DecoderFactory.get().jsonDecoder(schema,
                            new ByteArrayInputStream(lastJsonBytes)));
        }
        long jsonTime = System.nanoTime() - t2;

        System.out.printf("序列化+反序列化 %d 次：%n", rounds);
        System.out.printf("  Avro 二进制: %6.2f ms  单条大小: %d bytes%n",
                avroTime / 1_000_000.0, lastAvroBytes.length);
        System.out.printf("  Avro JSON  : %6.2f ms  单条大小: %d bytes%n",
                jsonTime / 1_000_000.0, lastJsonBytes.length);
        System.out.printf("  二进制比JSON快: %.1fx，单条体积减少: %.1f%%%n",
                (double) jsonTime / avroTime,
                (1.0 - (double) lastAvroBytes.length / lastJsonBytes.length) * 100);
        System.out.println("  → 高吞吐场景（Kafka消息）推荐使用二进制编码");
    }
}
