package com.example.avro;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;

import java.io.*;
import java.util.Arrays;

/**
 * Apache Avro 基础演示
 *
 * <p>覆盖内容：
 * <ol>
 *   <li>两种使用方式概览：Generic API（动态Schema）vs Specific API（代码生成）</li>
 *   <li>用 Generic API 定义 Schema、创建 Record、序列化为二进制</li>
 *   <li>反序列化还原为 GenericRecord</li>
 *   <li>使用 JSON Encoder 查看可读格式</li>
 *   <li>使用 SchemaBuilder 以流式 API 创建 Schema</li>
 * </ol>
 *
 * <p>运行方式：直接执行 main 方法即可。
 */
public class AvroBasicDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== Apache Avro 基础演示 ==========\n");

        demo1_defineSchemaAndSerialize();
        demo2_deserialize();
        demo3_jsonEncoding();
        demo4_schemaBuilder();

        System.out.println("\n========== 基础演示完毕 ==========");
    }

    // ----------------------------------------------------------------
    // 演示1：定义 Schema + 创建 Record + 二进制序列化
    // ----------------------------------------------------------------
    static void demo1_defineSchemaAndSerialize() throws Exception {
        System.out.println("--- 演示1：定义Schema + 序列化为二进制 ---");

        // 1. 用 JSON 字符串定义 Schema（也可从 .avsc 文件加载）
        String schemaJson = "{"
                + "\"type\":\"record\","
                + "\"name\":\"Product\","
                + "\"namespace\":\"com.example.avro\","
                + "\"fields\":["
                + "  {\"name\":\"id\",\"type\":\"long\"},"
                + "  {\"name\":\"name\",\"type\":\"string\"},"
                + "  {\"name\":\"price\",\"type\":\"double\"},"
                + "  {\"name\":\"tags\",\"type\":{\"type\":\"array\",\"items\":\"string\"}}"
                + "]"
                + "}";
        Schema schema = new Schema.Parser().parse(schemaJson);
        System.out.println("Schema 名称: " + schema.getFullName());
        System.out.println("字段列表: " + schema.getFields());

        // 2. 创建 GenericRecord（Generic API，不需要生成代码）
        GenericRecord product = new GenericData.Record(schema);
        product.put("id", 1001L);
        product.put("name", "Java编程思想");
        product.put("price", 89.9);
        product.put("tags", Arrays.asList("Java", "编程", "经典"));
        System.out.println("\n创建 Record: " + product);

        // 3. 序列化为字节数组（二进制格式）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);
        writer.write(product, encoder);
        encoder.flush();

        byte[] avroBytes = baos.toByteArray();
        System.out.println("序列化后字节数: " + avroBytes.length + " bytes");
        // 对比 JSON 大小
        String jsonStr = product.toString();
        System.out.println("等价 JSON 字节数: " + jsonStr.getBytes("UTF-8").length + " bytes（Avro更紧凑）");

        System.out.println();
    }

    // ----------------------------------------------------------------
    // 演示2：从字节数组反序列化为 GenericRecord
    // ----------------------------------------------------------------
    static void demo2_deserialize() throws Exception {
        System.out.println("--- 演示2：二进制反序列化 ---");

        // 先序列化一个 User Record
        String schemaJson = "{"
                + "\"type\":\"record\","
                + "\"name\":\"UserDemo\","
                + "\"namespace\":\"com.example.avro\","
                + "\"fields\":["
                + "  {\"name\":\"userId\",\"type\":\"long\"},"
                + "  {\"name\":\"username\",\"type\":\"string\"},"
                + "  {\"name\":\"score\",\"type\":\"float\"},"
                + "  {\"name\":\"verified\",\"type\":\"boolean\",\"default\":false}"
                + "]"
                + "}";
        Schema schema = new Schema.Parser().parse(schemaJson);

        // 序列化
        GenericRecord user = new GenericData.Record(schema);
        user.put("userId", 42L);
        user.put("username", "张三");
        user.put("score", 98.5f);
        user.put("verified", true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new GenericDatumWriter<GenericRecord>(schema).write(
                user, EncoderFactory.get().binaryEncoder(baos, null));
        EncoderFactory.get().binaryEncoder(baos, null).flush();

        // 重新序列化确保 flush
        baos = new ByteArrayOutputStream();
        BinaryEncoder enc = EncoderFactory.get().binaryEncoder(baos, null);
        new GenericDatumWriter<GenericRecord>(schema).write(user, enc);
        enc.flush();

        // 反序列化
        byte[] bytes = baos.toByteArray();
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
        GenericRecord restored = reader.read(null, decoder);

        System.out.println("原始 Record : " + user);
        System.out.println("反序列化结果: " + restored);
        System.out.println("userId 字段: " + restored.get("userId"));
        System.out.println("username 字段: " + restored.get("username"));
        System.out.println("反序列化成功: " + (user.toString().equals(restored.toString())));
        System.out.println();
    }

    // ----------------------------------------------------------------
    // 演示3：使用 JsonEncoder 查看可读格式（调试利器）
    // ----------------------------------------------------------------
    static void demo3_jsonEncoding() throws Exception {
        System.out.println("--- 演示3：JSON Encoder（便于调试）---");

        Schema schema = SchemaBuilder.record("Event")
                .namespace("com.example.avro")
                .fields()
                .name("type").type().stringType().noDefault()
                .name("timestamp").type().longType().noDefault()
                .name("payload").type().nullable().stringType().stringDefault(null)
                .endRecord();

        GenericRecord event = new GenericData.Record(schema);
        event.put("type", "USER_LOGIN");
        event.put("timestamp", System.currentTimeMillis());
        event.put("payload", "{\"ip\":\"192.168.1.1\"}");

        // JSON 编码
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, baos, true);
        new GenericDatumWriter<GenericRecord>(schema).write(event, jsonEncoder);
        jsonEncoder.flush();

        System.out.println("JSON 编码结果（可读）：");
        System.out.println(baos.toString("UTF-8"));
        System.out.println();
    }

    // ----------------------------------------------------------------
    // 演示4：使用 SchemaBuilder 流式 API 构建 Schema
    // ----------------------------------------------------------------
    static void demo4_schemaBuilder() throws Exception {
        System.out.println("--- 演示4：SchemaBuilder 流式 API ---");

        // SchemaBuilder 是比 JSON 字符串拼接更安全的 Schema 构造方式
        Schema addressSchema = SchemaBuilder.record("Address")
                .namespace("com.example.avro")
                .doc("用户收货地址")
                .fields()
                .name("street").type().stringType().noDefault()
                .name("city").type().stringType().noDefault()
                .name("province").type().stringType().noDefault()
                .name("zipCode").type().nullable().stringType().stringDefault(null)
                .endRecord();

        System.out.println("Address Schema:");
        System.out.println(addressSchema.toString(true));

        // 嵌套另一个 Record
        Schema customerSchema = SchemaBuilder.record("Customer")
                .namespace("com.example.avro")
                .fields()
                .name("customerId").type().longType().noDefault()
                .name("name").type().stringType().noDefault()
                .name("address").type(addressSchema).noDefault()
                .name("vipLevel").type().enumeration("VipLevel")
                .symbols("REGULAR", "SILVER", "GOLD", "PLATINUM").noDefault()
                .endRecord();

        System.out.println("\nCustomer Schema 字段:");
        customerSchema.getFields().forEach(f ->
                System.out.println("  " + f.name() + " -> " + f.schema().getType()));

        // 创建嵌套 Record
        GenericRecord address = new GenericData.Record(addressSchema);
        address.put("street", "中关村大街1号");
        address.put("city", "北京");
        address.put("province", "北京市");
        address.put("zipCode", "100000");

        GenericRecord customer = new GenericData.Record(customerSchema);
        customer.put("customerId", 9527L);
        customer.put("name", "李四");
        customer.put("address", address);
        customer.put("vipLevel", new GenericData.EnumSymbol(
                customerSchema.getField("vipLevel").schema(), "GOLD"));

        System.out.println("\n创建 Customer Record: " + customer);
        System.out.println("嵌套 address.city: " + ((GenericRecord) customer.get("address")).get("city"));
    }
}
