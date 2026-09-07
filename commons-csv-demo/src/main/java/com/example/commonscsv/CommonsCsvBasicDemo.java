package com.example.commonscsv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

/**
 * Apache Commons CSV 基础演示。
 *
 * 涵盖：
 * 1. CSVFormat 预定义格式（DEFAULT / RFC4180 / EXCEL / TDF / MYSQL / POSTGRESQL 等）
 * 2. CSVPrinter 写出 CSV（带表头、控制引号、转义）
 * 3. CSVParser 解析 CSV（按 header、按列号、迭代记录）
 * 4. 最常用的 API：builder()、withHeader()、withDelimiter()、withQuote()、withIgnoreEmptyLines()
 *
 * <p>Apache Commons CSV 的设计哲学：所有可变配置都通过 {@link CSVFormat} 表达，
 * CSVParser/CSVPrinter 仅负责 IO 与语法解析，不持有可变状态，线程安全。</p>
 */
public class CommonsCsvBasicDemo {

    private static final Logger log = LoggerFactory.getLogger(CommonsCsvBasicDemo.class);

    public static void main(String[] args) throws IOException {
        String outCsv = "target/basic-output.csv";
        String inCsv = "target/basic-input.csv";

        // ---------- 1. 演示 6 种预定义 CSVFormat ----------
        log.info("===== 1. 预定义 CSVFormat 一览 =====");
        showPredefinedFormats();

        // ---------- 2. CSVPrinter 写出 CSV ----------
        log.info("===== 2. 使用 CSVPrinter 写出 CSV（RFC4180 标准） =====");
        writeCsv(outCsv);

        // ---------- 3. CSVParser 解析 CSV（按 Header） ----------
        log.info("===== 3. 使用 CSVParser 按 Header 解析 CSV =====");
        readCsvByHeader(inCsv.replace("basic-input", "basic-output"));

        // ---------- 4. CSVParser 解析 CSV（按列号 + 不带头） ----------
        log.info("===== 4. 不带 Header 时按列号访问 =====");
        readCsvByIndex();

        log.info("基础演示完成，输出文件：{}", outCsv);
    }

    /** 演示 Commons CSV 内置的 6 种 CSVFormat（含分号/制表符/MySQL 等常见方言） */
    private static void showPredefinedFormats() {
        log.info("DEFAULT     分隔符=','   引号='\"'   适用：通用");
        log.info("RFC4180     分隔符=','   引号='\"'   适用：HTTP/邮件/标准协议");
        log.info("EXCEL       分隔符=','   引号='\"'   适用：Excel 导出/导入");
        log.info("TDF         分隔符='\\t'  适用：Tab 分隔（MySQL outfile）");
        log.info("MYSQL       分隔符='\\t'  适用：MySQL INTO OUTFILE");
        log.info("POSTGRESQL  分隔符='\\t'  适用：PostgreSQL COPY");
        log.info("POSTGRESQL_TEXT 同上，header 行为更宽松");
    }

    /**
     * 使用 RFC4180 格式写出 CSV。
     * <p>注意：</p>
     * <ul>
     *   <li>withHeader() 会自动写入表头行，且确保表头被正确引号包裹</li>
     *   <li>含逗号/换行/引号 的字段会被自动加双引号，并把字段内的双引号转义为两个双引号</li>
     *   <li>记得 close() CSVPrinter（或 try-with-resources），否则缓冲不会刷盘</li>
     * </ul>
     */
    private static void writeCsv(String path) throws IOException {
        // builder() + setHeader() + build() 是 1.10+ 推荐写法
        CSVFormat format = CSVFormat.RFC4180.builder()
                .setHeader("id", "name", "email", "city", "comment")
                .build();

        try (Writer writer = new BufferedWriter(new FileWriter(path));
             CSVPrinter printer = new CSVPrinter(writer, format)) {

            // printRecord 接收 Object... 可变参数（重载），支持任意 toString() 的对象
            printer.printRecord(1, "张三", "zhangsan@example.com", "北京",
                    "普通用户,无特殊备注");
            printer.printRecord(2, "Li, Si", "lisi@example.com", "上海",
                    "字段里含逗号");   // 自动加引号 -> "字段里含逗号"
            printer.printRecord(3, "Wang Wu", "wangwu@example.com", "深圳",
                    "He said \"Hello\"\nAnd goodbye.");   // 字段内引号转义为 ""，换行保留在引号内
            printer.printRecord(4, "赵六", "zhaoliu@example.com", "广州", "");
            printer.printRecord(5, "孙七", "sunqi@example.com", "杭州", null);

            printer.flush();
            log.info("已写入 5 条数据到 {}", path);
        }
    }

    /**
     * 按 Header 名解析 CSV（推荐写法：字段名更稳定，免受列顺序影响）。
     */
    private static void readCsvByHeader(String path) throws IOException {
        CSVFormat format = CSVFormat.RFC4180.builder()
                .setHeader()  // 空参：使用第一行作为 header
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        int count = 0;
        try (Reader reader = new BufferedReader(new FileReader(path));
             CSVParser parser = new CSVParser(reader, format)) {

            // 通过 header 名取值（推荐，可读性强）
            for (CSVRecord record : parser) {
                long id = Long.parseLong(record.get("id"));
                String name = record.get("name");
                String email = record.get("email");
                String city = record.get("city");
                String comment = record.isMapped("comment") ? record.get("comment") : "";

                log.info("行 {}: id={}, name={}, email={}, city={}, comment={}",
                        record.getRecordNumber(), id, name, email, city, comment);
                count++;
            }

            log.info("解析完成，共 {} 行；CSV 总记录数 = {}, header 数 = {}",
                    count, parser.getRecordNumber(), parser.getHeaderNames());
        }
    }

    /**
     * 不带 header 的 CSV：按列号访问（不推荐用于业务代码，但日志/临时脚本常用）。
     */
    private static void readCsvByIndex() throws IOException {
        // 准备一个无 header 的 CSV
        String path = "target/no-header.csv";
        try (Writer w = new BufferedWriter(new FileWriter(path));
             CSVPrinter p = new CSVPrinter(w, CSVFormat.DEFAULT)) {
            p.printRecord(Arrays.asList("1001", "iPhone 15", "5999.00", "10"));
            p.printRecord(Arrays.asList("1002", "MacBook Pro", "14999.00", "5"));
        }

        // 解析：无 header
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .build();

        try (Reader reader = new BufferedReader(new FileReader(path));
             CSVParser parser = new CSVParser(reader, format)) {

            for (CSVRecord record : parser) {
                String sku = record.get(0);     // 按列号
                String name = record.get(1);
                String price = record.get(2);
                String stock = record.get(3);
                log.info("[按列号] sku={}, name={}, price={}, stock={}",
                        sku, name, price, stock);
            }
        }
    }
}
