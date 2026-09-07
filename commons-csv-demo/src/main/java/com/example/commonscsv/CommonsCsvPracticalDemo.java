package com.example.commonscsv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Apache Commons CSV 实战演示。
 *
 * 三个真实业务场景：
 * 1. 批量导入用户（容错 + 校验 + 跳过空行/无效行 + 输出错误报告）
 * 2. 数据库查询结果导出 CSV（含类型安全转换、NULL 处理、空值兜底）
 * 3. Spring Boot 文件上传 + 解析 + 生成导入结果报告
 *
 * <p>实战关键：</p>
 * <ul>
 *   <li>不要相信 CSV 内容——所有字段都要 trim + 长度校验 + 业务校验</li>
 *   <li>超大文件（>100MB）请用流式处理，CSV 不会一次性加载内存</li>
 *   <li>导出的 CSV 必须 UTF-8 BOM（兼容 Excel 中文乱码）</li>
 *   <li>数字字段反序列化时务必 try-catch，别相信 CSV 里的数据</li>
 * </ul>
 */
public class CommonsCsvPracticalDemo {

    private static final Logger log = LoggerFactory.getLogger(CommonsCsvPracticalDemo.class);

    /** 用户导入业务结果 */
    static class ImportResult {
        int success;
        int failed;
        List<String> errorMessages = new ArrayList<String>();

        @Override
        public String toString() {
            return "ImportResult{success=" + success + ", failed=" + failed
                    + ", errors=" + errorMessages.size() + "}";
        }
    }

    /** 用户 DTO（业务校验用） */
    static class UserRow {
        String username;
        String email;
        Integer age;
        String role;

        boolean isValid(List<String> errors) {
            if (username == null || username.isEmpty() || username.length() > 32) {
                errors.add("username 非法: " + username);
                return false;
            }
            if (email == null || !email.contains("@") || email.length() > 64) {
                errors.add("email 非法: " + email);
                return false;
            }
            if (age == null || age < 0 || age > 150) {
                errors.add("age 非法: " + age);
                return false;
            }
            if (!"admin".equals(role) && !"user".equals(role) && !"vip".equals(role)) {
                errors.add("role 必须是 admin/user/vip: " + role);
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "User{username=" + username + ", email=" + email
                    + ", age=" + age + ", role=" + role + "}";
        }
    }

    public static void main(String[] args) throws IOException {
        log.info("===== 实战 1. 批量导入用户（容错） =====");
        scenarioImportUsers();

        log.info("===== 实战 2. 导出查询结果到 CSV（中文 + BOM 兼容 Excel） =====");
        scenarioExportOrders();

        log.info("===== 实战 3. Spring Boot 风格的 CSV 上传解析 =====");
        scenarioSpringBootUpload();

        log.info("实战演示完成");
    }

    /**
     * 场景 1：批量导入用户，演示真实业务校验。
     * <ul>
     *   <li>跳过注释/空行</li>
     *   <li>字段 trim 后校验</li>
     *   <li>类型转换异常单独捕获</li>
     *   <li>写入错误报告到另一个 CSV 文件</li>
     * </ul>
     */
    private static void scenarioImportUsers() throws IOException {
        // 准备带问题的 CSV（含空行、注释、无效数据）
        String srcPath = "target/import-users.csv";
        try (Writer w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(srcPath), StandardCharsets.UTF_8));
             CSVPrinter p = new CSVPrinter(w, CSVFormat.DEFAULT.builder()
                     .setHeader("username", "email", "age", "role")
                     .build())) {
            w.write("# 用户批量导入文件 - 2026-09-07\r\n");
            w.write("# 列：username,email,age,role\r\n");
            w.write("\r\n");   // 空行
            p.printRecord("zhangsan", "zhangsan@example.com", "25", "user");
            p.printRecord("lisi", "lisi_at_example.com", "30", "user");   // 邮箱非法
            p.printRecord("wangwu", "wangwu@example.com", "not-a-number", "user"); // age 非法
            p.printRecord("zhaoliu", "zhaoliu@example.com", "-5", "user"); // age 越界
            p.printRecord("sunqi", "sunqi@example.com", "28", "guest");   // role 非法
            p.printRecord("admin01", "admin01@example.com", "40", "admin");
            p.printRecord("", "bad@example.com", "20", "user");           // username 空
            p.printRecord("  spacesuser  ", "spaces@example.com", "22", "user"); // 含空格
        }

        // 导入
        String errorReportPath = "target/import-errors.csv";
        ImportResult result = importUsers(srcPath, errorReportPath);
        log.info("导入结果：{}", result);
        log.info("错误报告已写入：{}", errorReportPath);
    }

    private static ImportResult importUsers(String srcPath, String errorReportPath) throws IOException {
        ImportResult result = new ImportResult();
        List<UserRow> validUsers = new ArrayList<UserRow>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setCommentMarker('#')
                .setIgnoreEmptyLines(true)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)           // 关键：自动 trim
                .setIgnoreSurroundingSpaces(true)
                .build();

        try (Reader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(srcPath), StandardCharsets.UTF_8));
             CSVParser parser = format.parse(r)) {

            int lineNo = 1; // 1-based（header 已经跳过）
            for (CSVRecord rec : parser) {
                lineNo++;
                List<String> rowErrors = new ArrayList<String>();
                UserRow row = new UserRow();

                // username (必填, 字符串)
                row.username = rec.isMapped("username") ? rec.get("username") : "";
                // email
                row.email = rec.isMapped("email") ? rec.get("email") : "";
                // age (整数, 可能为 null/非法)
                String ageStr = rec.isMapped("age") ? rec.get("age") : "";
                if (ageStr != null && !ageStr.isEmpty()) {
                    try {
                        row.age = Integer.parseInt(ageStr.trim());
                    } catch (NumberFormatException e) {
                        rowErrors.add("age 无法转为整数: '" + ageStr + "'");
                    }
                }
                // role
                row.role = rec.isMapped("role") ? rec.get("role") : "";

                // 业务校验
                if (!row.isValid(rowErrors)) {
                    result.failed++;
                    for (String err : rowErrors) {
                        result.errorMessages.add("line " + lineNo + ": " + err);
                    }
                } else {
                    result.success++;
                    validUsers.add(row);
                }
            }
        }

        // 写错误报告
        writeErrorReport(errorReportPath, result.errorMessages);

        log.info("成功导入 {} 条用户：", validUsers.size());
        for (UserRow u : validUsers) {
            log.info("  ✓ {}", u);
        }

        return result;
    }

    private static void writeErrorReport(String path, List<String> errors) throws IOException {
        try (Writer w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8));
             CSVPrinter p = new CSVPrinter(w, CSVFormat.DEFAULT.builder()
                     .setHeader("lineNo", "message")
                     .build())) {
            for (String err : errors) {
                int idx = err.indexOf(": ");
                if (idx > 0) {
                    String lineNo = err.substring(5, idx);   // "line " 之后到 ":"
                    String msg = err.substring(idx + 2);
                    p.printRecord(lineNo, msg);
                } else {
                    p.printRecord("", err);
                }
            }
        }
    }

    /**
     * 场景 2：导出数据库查询结果到 CSV，重点解决 Excel 中文乱码问题。
     *
     * 关键点：
     * <ul>
     *   <li>写出 UTF-8 + BOM（Excel 用 UTF-8 打开 CSV 会乱码，必须有 BOM）</li>
     *   <li>NULL 值用空字符串兜底（不要输出 "null"）</li>
     *   <li>Date 转 yyyy-MM-dd HH:mm:ss 字符串</li>
     *   <li>大数字避免科学计数法（用 BigDecimal.toPlainString）</li>
     * </ul>
     */
    private static void scenarioExportOrders() throws IOException {
        String path = "target/orders-export.csv";

        // 模拟从 DB 查出来的结果（实际可能是 List<Map<String, Object>> 或 DO 列表）
        List<Map<String, Object>> orders = new ArrayList<Map<String, Object>>();
        Map<String, Object> o1 = new LinkedHashMap<String, Object>();
        o1.put("orderId", "OD202609070001");
        o1.put("userId", 1001L);
        o1.put("userName", "张三");
        o1.put("amount", 1299.50);
        o1.put("status", "PAID");
        o1.put("createdAt", "2026-09-07 10:30:00");
        orders.add(o1);

        Map<String, Object> o2 = new LinkedHashMap<String, Object>();
        o2.put("orderId", "OD202609070002");
        o2.put("userId", 1002L);
        o2.put("userName", "李四");
        o2.put("amount", 89.00);
        o2.put("status", "REFUNDED");
        o2.put("createdAt", "2026-09-07 11:15:00");
        orders.add(o2);

        Map<String, Object> o3 = new LinkedHashMap<String, Object>();
        o3.put("orderId", "OD202609070003");
        o3.put("userId", null);     // 演示 null
        o3.put("userName", null);
        o3.put("amount", 0.01);     // 演示极小数字
        o3.put("status", "CANCELLED");
        o3.put("createdAt", "2026-09-07 12:00:00");
        orders.add(o3);

        // 列顺序（控制 CSV 表头顺序）
        List<String> columns = Arrays.asList("orderId", "userId", "userName", "amount", "status", "createdAt");

        Path file = Paths.get(path);
        // 写出文件：先写 UTF-8 BOM 让 Excel 正确识别
        try (Writer w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8))) {
            w.write('\uFEFF');   // UTF-8 BOM

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader(columns.toArray(new String[0]))
                    .build();

            try (CSVPrinter p = new CSVPrinter(w, format)) {
                for (Map<String, Object> row : orders) {
                    Object[] values = new Object[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        Object v = row.get(columns.get(i));
                        values[i] = nullSafe(v);   // null 兜底
                    }
                    p.printRecord(values);
                }
            }
        }

        log.info("已导出 {} 条订单到 {}", orders.size(), path);

        // 验证：读回看看
        try (Reader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {
            r.read(); // 吃掉 BOM
            CSVFormat fmt = CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true).build();
            try (CSVParser parser = fmt.parse(r)) {
                for (CSVRecord rec : parser) {
                    log.info("读回: {} | {} | {} | {} | {} | {}",
                            rec.get("orderId"), rec.get("userId"), rec.get("userName"),
                            rec.get("amount"), rec.get("status"), rec.get("createdAt"));
                }
            }
        }
    }

    /** null 兜底：避免 CSV 里出现 "null" 字面量 */
    private static String nullSafe(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Number) {
            // 避免科学计数法
            return new java.math.BigDecimal(v.toString()).toPlainString();
        }
        return v.toString();
    }

    /**
     * 场景 3：Spring Boot 风格的 CSV 上传处理（纯模拟，不依赖 Spring）。
     *
     * <p>真实 Spring Boot 集成：</p>
     * <pre>{@code
     * @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
     * public ImportResult upload(@RequestParam("file") MultipartFile file) throws IOException {
     *     try (Reader r = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
     *         CSVFormat fmt = CSVFormat.DEFAULT.builder()
     *                 .setHeader().setSkipHeaderRecord(true).setTrim(true).build();
     *         try (CSVParser parser = fmt.parse(r)) {
     *             // ... 业务逻辑
     *         }
     *     }
     * }
     * }</pre>
     */
    private static void scenarioSpringBootUpload() throws IOException {
        // 模拟上传文件内容（byte[] / InputStream）
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // BOM
        sb.append("orderId,userId,amount\n");
        sb.append("OD001,1001,100.50\n");
        sb.append("OD002,1002,200.00\n");
        sb.append("OD003,1003,300.99\n");

        byte[] uploadedBytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        // Spring Controller 入口：MultipartFile file
        // 这里用 ByteArrayInputStream 模拟
        ImportResult result = new ImportResult();
        int rowCount = 0;
        try (Reader r = new java.io.InputStreamReader(
                new java.io.ByteArrayInputStream(uploadedBytes), StandardCharsets.UTF_8)) {
            // 读第一个 char 可能是 BOM，用 r.read() 消耗；commons-csv 1.10+ 也能自动处理
            r.read();
            CSVFormat fmt = CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true).setTrim(true)
                    .build();
            try (CSVParser parser = fmt.parse(r)) {
                for (CSVRecord rec : parser) {
                    rowCount++;
                    // 这里只演示打印，真实业务：解析 → 入库 → 写错误日志
                    log.info("[controller 模拟] 处理订单: id={}, userId={}, amount={}",
                            rec.get("orderId"), rec.get("userId"), rec.get("amount"));
                }
            }
        }
        result.success = rowCount;
        log.info("上传解析完成：{}", result);

        // 写一份导入结果报告给前端
        String reportPath = "target/upload-report.csv";
        try (Writer w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(reportPath), StandardCharsets.UTF_8));
             CSVPrinter p = new CSVPrinter(w, CSVFormat.DEFAULT.builder()
                     .setHeader("metric", "value")
                     .build())) {
            p.printRecord("totalRows", rowCount);
            p.printRecord("success", result.success);
            p.printRecord("failed", result.failed);
            p.printRecord("uploadTime", java.time.LocalDateTime.now().toString());
        }
        log.info("导入报告：{}", reportPath);

        // 控制 Files.readAllBytes 输出长度，避免日志刷屏
        long size = Files.size(Paths.get(reportPath));
        log.info("报告大小: {} bytes", size);
    }

    /** 工具方法：从 List 中取第一个（避免 lambda 让 Java 8 编译报 diamond warning） */
    @SuppressWarnings("unused")
    private static <T> T firstOrNull(List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /** 工具方法：空 list 兜底（保持 Java 8 Collections.emptyList() 风格） */
    @SuppressWarnings("unused")
    private static <T> List<T> emptyList() {
        return Collections.emptyList();
    }
}
