package com.example.commonscsv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Apache Commons CSV 进阶演示。
 *
 * 涵盖：
 * 1. 自定义分隔符（分号 / 竖线 / 脱字符）
 * 2. 自定义引号策略 QuoteMode（ALL / NON_NUMERIC / NONE / MINIMAL）
 * 3. 含 BOM 头的 UTF-8 文件处理（首行多了 \uFEFF）
 * 4. 跳过注释行（#）与空行
 * 5. 宽松解析（setIgnoreSurroundingSpaces）
 * 6. 多值字段解析（按 | 分隔写入 List）
 * 7. CSV → Map / List<Map> 转换
 * 8. 大文件流式处理（iterator，不一次性加载）
 */
public class CommonsCsvAdvancedDemo {

    private static final Logger log = LoggerFactory.getLogger(CommonsCsvAdvancedDemo.class);

    public static void main(String[] args) throws IOException {
        log.info("===== 进阶 1. 自定义分隔符：分号 =====");
        demoCustomDelimiter();

        log.info("===== 进阶 2. 自定义引号策略 QuoteMode =====");
        demoQuoteMode();

        log.info("===== 进阶 3. 处理 UTF-8 BOM 头 =====");
        demoBomHandling();

        log.info("===== 进阶 4. 跳过注释行 + 空行 =====");
        demoSkipComments();

        log.info("===== 进阶 5. 大文件流式迭代 =====");
        demoStreamLargeFile();

        log.info("===== 进阶 6. CSV → List<Map<String,String>> =====");
        demoCsvToMapList();

        log.info("进阶演示完成");
    }

    /** 演示 1：自定义分隔符（欧洲 CSV 常用分号） */
    private static void demoCustomDelimiter() throws IOException {
        String path = "target/euro.csv";
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("id", "name", "price")
                .setDelimiter(';')          // 分号分隔（德语/法语 Excel 常见）
                .setQuote('"')
                .setRecordSeparator("\r\n")  // Windows 行尾
                .build();

        try (Writer w = new BufferedWriter(new FileWriter(path));
             CSVPrinter p = new CSVPrinter(w, format)) {
            p.printRecord(1, "Müller", "9,99");   // 9,99 中含分号? 这里是逗号，但含 . 分号字段值
            p.printRecord(2, "Schäfer", "12,50");
            p.printRecord(3, "Bäcker", "3,20");
        }
        log.info("已生成欧洲格式 CSV（分号分隔）：{}", path);

        // 解析时也用同样的分隔符
        try (Reader r = new BufferedReader(new FileReader(path));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true)
                     .setDelimiter(';')
                     .build()
                     .parse(r)) {

            for (CSVRecord rec : parser) {
                log.info("[欧式] id={}, name={}, price={}",
                        rec.get("id"), rec.get("name"), rec.get("price"));
            }
        }
    }

    /** 演示 2：4 种引号策略 QuoteMode */
    private static void demoQuoteMode() throws IOException {
        String path = "target/quote-modes.csv";

        // QuoteMode.ALL：所有字段都加引号
        // QuoteMode.MINIMAL（默认）：只在必要时加引号（含分隔符/引号/换行）
        // QuoteMode.NON_NUMERIC：仅非数字字段加引号
        // QuoteMode.NONE：从不加引号（需要数据本身无歧义）

        try (Writer w = new BufferedWriter(new FileWriter(path));
             CSVPrinter p = new CSVPrinter(w, CSVFormat.DEFAULT.builder()
                     .setHeader("id", "name", "price")
                     .setQuoteMode(QuoteMode.NON_NUMERIC)  // 数字不加引号
                     .build())) {
            p.printRecord(1, "Apple", 5.50);
            p.printRecord(2, "Banana", 3.20);
            p.printRecord(3, "Cherry", 7.80);
        }

        // 读出来验证
        try (Reader r = new BufferedReader(new FileReader(path));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).build().parse(r)) {
            for (CSVRecord rec : parser) {
                log.info("[QuoteMode.NON_NUMERIC] id={}, name={}, price={}",
                        rec.get("id"), rec.get("name"), rec.get("price"));
            }
        }
        log.info("NON_NUMERIC 输出 -> 数字不带引号，字符串带引号");
    }

    /**
     * 演示 3：处理 Windows Excel 保存的 UTF-8 CSV 文件（BOM 头 \uFEFF）。
     * 经典坑：直接解析时第一列 header 会变成 "\uFEFFid"，所有按 name 取值都会失败。
     */
    private static void demoBomHandling() throws IOException {
        String path = "target/bom.csv";

        // 1. 模拟 Excel 写出（手工写一个 BOM 头）
        try (Writer w = new BufferedWriter(new FileWriter(path))) {
            w.write('\uFEFF');    // BOM
            w.write("id,name,email\r\n");
            w.write("1,Alice,alice@example.com\r\n");
            w.write("2,Bob,bob@example.com\r\n");
        }

        // 2. 错误做法：直接按 header 名取 — 第一列会被识别为 "\uFEFFid"
        log.info("-- 错误做法：未处理 BOM --");
        try (Reader r = new BufferedReader(new FileReader(path));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).build().parse(r)) {
            log.info("header names = {}", parser.getHeaderNames());
            // 这里取 "id" 会抛 IllegalArgumentException: Mapping for id not found
            try {
                log.info("尝试取 id：{}", parser.getRecords().get(0).get("id"));
            } catch (IllegalArgumentException e) {
                log.warn("抛异常：{}", e.getMessage());
            }
        }

        // 3. 正确做法：使用 UnicodeInputStream 或手工吃掉 BOM
        log.info("-- 正确做法：读取时跳过 BOM --");
        try (Reader r = new BufferedReader(new FileReader(path))) {
            r.read(); // 吃掉 BOM（\uFEFF 占一个 char）
            CSVFormat fmt = CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true).build();
            try (CSVParser parser = fmt.parse(r)) {
                log.info("修复后 header = {}", parser.getHeaderNames());
                for (CSVRecord rec : parser) {
                    log.info("OK id={}, name={}, email={}",
                            rec.get("id"), rec.get("name"), rec.get("email"));
                }
            }
        }

        // 4. 更通用的做法：用 commons-csv 自带的 UnicodeBOMInputStream（需引入 commons-io）
        log.info("提示：生产环境推荐用 commons-io 的 UnicodeBOMInputStream 自动剥 BOM");
    }

    /** 演示 4：跳过注释行（#开头）和空行 */
    private static void demoSkipComments() throws IOException {
        String path = "target/comments.csv";

        try (Writer w = new BufferedWriter(new FileWriter(path));
             CSVPrinter p = new CSVPrinter(w, CSVFormat.DEFAULT)) {
            // 注意：用 # 作为注释前缀必须显式配置 setCommentMarker('#')
            // 普通 builder() 默认是没有注释前缀的
            w.write("# 这是注释：用户列表（2026-09-07 导出）\r\n");
            w.write("\r\n");  // 空行
            w.write("id,name,role\r\n");
            p.printRecord(1, "Alice", "admin");
            w.write("# 中间也可以插入注释\r\n");
            p.printRecord(2, "Bob", "user");
        }

        // 解析时同时开启：注释标记 + 跳过空行 + 第一行为 header
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setCommentMarker('#')
                .setIgnoreEmptyLines(true)
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader r = new BufferedReader(new FileReader(path));
             CSVParser parser = format.parse(r)) {
            log.info("实际解析的 header = {}", parser.getHeaderNames());
            for (CSVRecord rec : parser) {
                log.info("[skip-comments] id={}, name={}, role={}",
                        rec.get("id"), rec.get("name"), rec.get("role"));
            }
        }
    }

    /**
     * 演示 5：大文件流式迭代（不一次性加载到内存）。
     * <p>CSVParser 实现了 Iterable<CSVRecord>，可安全遍历百万级记录。</p>
     */
    private static void demoStreamLargeFile() throws IOException {
        String path = "target/large.csv";
        int total = 100_000;
        log.info("生成测试文件 {} 行 ...", total);

        try (Writer w = new BufferedWriter(new FileWriter(path));
             CSVPrinter p = new CSVPrinter(w, CSVFormat.DEFAULT.builder()
                     .setHeader("orderId", "userId", "amount", "status")
                     .build())) {
            for (int i = 1; i <= total; i++) {
                p.printRecord(i, 1000 + (i % 500), (i % 1000) + 0.99,
                        i % 7 == 0 ? "REFUND" : "PAID");
            }
        }

        // 流式遍历：只统计 PAID 金额大于 500 的订单
        long start = System.currentTimeMillis();
        int matchCount = 0;
        double totalAmount = 0;
        try (Reader r = new BufferedReader(new FileReader(path));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true).build().parse(r)) {

            for (CSVRecord rec : parser) {
                double amount = Double.parseDouble(rec.get("amount"));
                if ("PAID".equals(rec.get("status")) && amount > 500) {
                    matchCount++;
                    totalAmount += amount;
                }
                // 不需要 break 时千万不能把全部记录 add 到 List（会 OOM）
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("大文件 {} 行扫描完成，匹配 PAID 且 amount>500 的 {} 条，合计金额={}，耗时 {} ms",
                total, matchCount, totalAmount, elapsed);
    }

    /**
     * 演示 6：CSV → List<Map<String,String>>。
     * <p>常用于上传文件预览，但要注意：</p>
     * <ul>
     *   <li>返回的 Map 是 LinkedHashMap 保持顺序</li>
     *   <li>所有 value 都是 String，需要再自行做类型转换</li>
     *   <li>如果业务上是强类型 POJO，建议自定义 Bean 绑定（不要 mapToBean 库）</li>
     * </ul>
     */
    private static void demoCsvToMapList() throws IOException {
        String path = "target/products.csv";

        // 1. 准备一份产品 CSV
        try (Writer w = new BufferedWriter(new FileWriter(path));
             CSVPrinter p = new CSVPrinter(w, CSVFormat.DEFAULT.builder()
                     .setHeader("sku", "name", "price", "tags")
                     .build())) {
            p.printRecord("P001", "iPhone 15", "5999.00", "phone|apple|5g");
            p.printRecord("P002", "MacBook Pro", "14999.00", "laptop|apple|m3");
            p.printRecord("P003", "AirPods Pro", "1899.00", "audio|apple|anc");
        }

        // 2. 读成 List<Map>
        List<Map<String, String>> rows = new ArrayList<>();
        try (Reader r = new BufferedReader(new FileReader(path));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build().parse(r)) {

            for (CSVRecord rec : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                // toMap() 直接拿到一个不可变 Map（基于 LinkedHashMap）
                Map<String, String> immutable = rec.toMap();
                row.putAll(immutable);
                rows.add(row);
            }
        }

        log.info("共解析 {} 行：", rows.size());
        for (Map<String, String> row : rows) {
            // 自定义解析：把 tags 按 | 拆成 List
            String[] tagArr = StringUtils.split(row.get("tags"), "|");
            List<String> tags = tagArr == null ? new ArrayList<String>() : Arrays.asList(tagArr);
            log.info("  sku={}, name={}, price={}, tags={}",
                    row.get("sku"), row.get("name"), row.get("price"), tags);
        }
    }
}
