# Day 42: Apache Commons CSV — Java CSV 读写事实标准库

## 一、工具简介

**Apache Commons CSV** 是 Apache 官方出品的 CSV 读写库，用于在 Java 中处理 CSV（Comma-Separated Values）格式的数据。

- **GitHub**: https://github.com/apache/commons-csv
- **官网文档**: https://commons.apache.org/proper/commons-csv/
- **Maven Central**: https://central.sonatype.com/artifact/org.apache.commons/commons-csv
- **星标**: 700+（Apache 官方项目，Java CSV 读写事实标准）
- **当前最新稳定版**: **1.10.0**（Java 8 兼容；1.11.x 起部分功能要求 Java 11）
- **License**: Apache License 2.0
- **维护团队**: Apache Commons Team
- **首次发布**: 2005 年（从 Apache Commons Sandbox 毕业），历史悠久

### 为什么选择 Commons CSV？

| 维度 | 手写 String.split | OpenCSV | Commons CSV | FastCSV |
| --- | --- | --- | --- | --- |
| Apache 官方维护 | ❌ | ❌ | ✅ | ❌ |
| Java 8 兼容 | ✅ | ✅ | ✅ | ✅ |
| 预定义方言（Excel/MySQL/TDF） | ❌ | 极少 | ✅ 6+ | ❌ |
| 处理 RFC 4180 边界（引号/换行/转义） | 自己造 | ✅ | ✅ 严谨 | ✅ |
| 0 依赖 | ✅ | ❌ | ✅ | ✅ |
| 流式大文件 | ✅ | ✅ | ✅ | ✅ |
| API 设计 | — | 老式 | Builder | 简单 |
| 体积 | — | 中 | 小 | 小 |

**结论**：在 Java 后端场景下，Apache Commons CSV 是最稳妥的选择 —— Apache 官方维护、0 依赖、API 现代、覆盖所有 CSV 方言。

### 核心设计哲学

Commons CSV 把 CSV 的所有"可变配置"封装在 `CSVFormat` 中（不可变对象，线程安全），把 IO 和语法解析放在 `CSVParser` / `CSVPrinter` 中。这样：
- **一份 CSVFormat 可以安全地在多线程间共享**
- 解析/写出逻辑只关注 IO，无需关心引号/分隔符细节
- 配置和执行彻底解耦

## 二、Maven 依赖配置

```xml
<properties>
    <commons.csv.version>1.10.0</commons.csv.version>
</properties>

<dependencies>
    <!-- Apache Commons CSV 核心库（0 传递依赖） -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-csv</artifactId>
        <version>${commons.csv.version}</version>
    </dependency>

    <!-- 日志门面 + 实现（推荐，但非必需） -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.36</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.13</version>
    </dependency>

    <!-- 可选：commons-lang3（字符串工具） -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
        <version>3.14.0</version>
    </dependency>
</dependencies>
```

**注意**：
- commons-csv 1.10.x 是 **0 依赖** 的（不依赖 commons-lang3 / commons-io）
- 1.10.0 之前的版本（如 1.9.0）需要 commons-lang3
- 1.11+ 版本要求 Java 11+，且默认开启更严格的检查
- Java 8 项目请锁定到 **1.10.0**（最后一个官方支持 Java 8 的稳定版本）

## 三、核心 API 速览

### 3.1 CSVFormat：CSV 配置（不可变）

Commons CSV 提供 **6 种预定义格式**，覆盖所有主流 CSV 方言：

| 预定义常量 | 分隔符 | 引号 | 适用场景 |
| --- | --- | --- | --- |
| `DEFAULT` | `,` | `"` | 通用 |
| `RFC4180` | `,` | `"` | 标准协议、HTTP、邮件附件 |
| `EXCEL` | `,` | `"` | Excel 导入/导出 |
| `TDF` | `\t` | `"` | Tab 分隔 |
| `MYSQL` | `\t` | `"` | MySQL `SELECT ... INTO OUTFILE` |
| `POSTGRESQL` / `POSTGRESQL_TEXT` | `\t` | `"` | PostgreSQL `COPY ... TO STDOUT` |

**自定义格式**（Builder 模式，1.10+ 推荐写法）：

```java
CSVFormat format = CSVFormat.DEFAULT.builder()
    .setHeader("id", "name", "email")   // 表头
    .setDelimiter(';')                  // 自定义分隔符（欧洲 CSV）
    .setQuote('"')
    .setRecordSeparator("\r\n")          // Windows 行尾
    .setIgnoreEmptyLines(true)          // 忽略空行
    .setTrim(true)                      // 自动 trim
    .setCommentMarker('#')              // # 开头的行视为注释
    .setQuoteMode(QuoteMode.NON_NUMERIC) // 数字不加引号
    .setNullString("")                  // null 输出为空字符串
    .build();                            // build() 产生不可变 CSVFormat
```

**4 种引号策略（QuoteMode）**：

| 模式 | 行为 | 典型用途 |
| --- | --- | --- |
| `MINIMAL`（默认） | 仅在字段含分隔符/引号/换行时加引号 | 99% 场景 |
| `ALL` | 所有字段都加引号 | 数据审计、人工可读 |
| `NON_NUMERIC` | 仅非数字字段加引号 | 数据库导入 |
| `NONE` | 永不加引号 | 数据本身无歧义（极致压缩） |

### 3.2 CSVPrinter：写出 CSV

```java
try (Writer w = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream("out.csv"), StandardCharsets.UTF_8));
     CSVPrinter p = new CSVPrinter(w, format)) {
    p.printRecord(1, "张三", "zhangsan@example.com");      // 变长 Object...
    p.printRecord(Arrays.asList(2, "李四", "lisi@x.com")); // 或 Collection
    p.printRecord("标题");                                  // 单独打印单个字段
    p.printComment("这是一行注释");                         // 仅当 setCommentMarker 设置后生效
    p.flush();
}
```

### 3.3 CSVParser：解析 CSV

```java
try (Reader r = new BufferedReader(
        new InputStreamReader(new FileInputStream("in.csv"), StandardCharsets.UTF_8));
     CSVParser parser = format.parse(r)) {

    // 1. 按 header 名取值（推荐，可读性强）
    for (CSVRecord rec : parser) {
        String id = rec.get("id");
        String name = rec.get("name");
    }

    // 2. 按列号取值
    for (CSVRecord rec : parser) {
        String sku = rec.get(0);
    }

    // 3. 取整行作为 Map（不可变 LinkedHashMap）
    for (CSVRecord rec : parser) {
        Map<String, String> row = rec.toMap();
    }

    // 4. 元数据
    int total = parser.getRecordNumber();
    List<String> headers = parser.getHeaderNames();
}
```

## 四、Spring Boot 集成

### 4.1 导出 CSV 接口

```java
@RestController
@RequestMapping("/api/export")
public class CsvExportController {

    @Autowired
    private OrderService orderService;

    /**
     * 导出订单列表为 CSV
     * Content-Disposition 让浏览器直接下载
     * BOM 让 Excel 正确识别 UTF-8 编码（否则中文乱码）
     */
    @GetMapping(value = "/orders", produces = "text/csv")
    public void exportOrders(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=orders-" + LocalDate.now() + ".csv");

        // 写 UTF-8 BOM 让 Excel 不乱码
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        CSVFormat format = CSVFormat.EXCEL.builder()
                .setHeader("orderId", "userId", "userName", "amount", "status", "createdAt")
                .setQuoteMode(QuoteMode.MINIMAL)
                .build();

        try (CSVPrinter printer = new CSVPrinter(
                new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)),
                format)) {

            List<Order> orders = orderService.findAll();   // 你的业务方法
            for (Order o : orders) {
                printer.printRecord(
                        nullSafe(o.getOrderId()),
                        nullSafe(o.getUserId()),
                        nullSafe(o.getUserName()),
                        o.getAmount() == null ? "" : o.getAmount().toPlainString(),
                        nullSafe(o.getStatus()),
                        o.getCreatedAt() == null ? "" :
                                o.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );
            }
        }
    }

    private static String nullSafe(Object v) {
        return v == null ? "" : v.toString();
    }
}
```

### 4.2 导入 CSV 接口（MultipartFile）

```java
@RestController
@RequestMapping("/api/import")
public class CsvImportController {

    private static final Logger log = LoggerFactory.getLogger(CsvImportController.class);

    @PostMapping(value = "/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setCommentMarker('#')
                .setIgnoreEmptyLines(true)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        ImportResult result = new ImportResult();
        List<User> successUsers = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            int lineNo = 1;
            for (CSVRecord rec : parser) {
                lineNo++;
                List<String> errors = new ArrayList<>();
                User user = parseRow(rec, errors);

                if (errors.isEmpty()) {
                    successUsers.add(user);
                    result.success++;
                } else {
                    result.failed++;
                    for (String err : errors) {
                        result.errorMessages.add("line " + lineNo + ": " + err);
                    }
                }
            }

            // 持久化
            if (!successUsers.isEmpty()) {
                userService.batchInsert(successUsers);
            }
        }

        log.info("导入完成：成功 {}，失败 {}", result.success, result.failed);
        return result;
    }

    private User parseRow(CSVRecord rec, List<String> errors) {
        User user = new User();
        user.setUsername(rec.get("username"));
        user.setEmail(rec.get("email"));

        try {
            user.setAge(Integer.parseInt(rec.get("age")));
        } catch (NumberFormatException e) {
            errors.add("age 字段无法解析为整数");
        }

        // 业务校验
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            errors.add("username 不能为空");
        }
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            errors.add("email 格式不正确");
        }

        return user;
    }

    public static class ImportResult {
        public int success;
        public int failed;
        public List<String> errorMessages = new ArrayList<>();
    }
}
```

### 4.3 Spring Boot application.yml（无特殊配置）

Commons CSV 不需要任何 Spring Boot 配置 —— 它是普通 Java 库，不像 Druid、Quartz、MyBatis 那样有 Spring 集成 starter。

如需 UTF-8 BOM 处理，可引入 commons-io 配合 `UnicodeBOMInputStream`：

```xml
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.15.1</version>
</dependency>
```

```java
// 自动剥离 UTF-8 BOM
try (Reader reader = new InputStreamReader(
        new UnicodeBOMInputStream(file.getInputStream()), StandardCharsets.UTF_8);
     CSVParser parser = format.parse(reader)) {
    // ...
}
```

## 五、注意事项（Bug 风险 / 性能问题 / 使用限制）

### 5.1 编码与 BOM

- **🔴 经典坑：Excel 打开 UTF-8 CSV 中文乱码**
  - 原因：Excel 用系统 ANSI 编码（Windows 上是 GBK）解析无 BOM 的 UTF-8 文件
  - **解决**：写出时手动在文件开头写 3 字节 `0xEF 0xBB 0xBF`（BOM）
  - 或用 `new OutputStreamWriter(out, StandardCharsets.UTF_8)` 后再 `out.write('\uFEFF')`

- **🔴 经典坑：解析 Excel 保存的 CSV 失败**
  - 原因：Excel 保存的 UTF-8 CSV 自带 BOM，第一列 header 变成 `"\uFEFFid"`
  - 表现：`record.get("id")` 抛 `IllegalArgumentException: Mapping for id not found`
  - **解决**：解析前先 `reader.read()` 吃掉 BOM，或用 commons-io 的 `UnicodeBOMInputStream`

### 5.2 类型转换

- **🟡 CSVRecord.get() 永远返回 String**
  - 所有值都是 String，需要自己 `Integer.parseInt`、`Double.parseDouble`、`LocalDate.parse`
  - **务必 try-catch NumberFormatException**，不要相信 CSV 里的数据

- **🟡 NULL 处理**
  - CSV 没有真正的 NULL 概念，Commons CSV 默认会把空字段返回为空字符串 `""` 而不是 `null`
  - 想让 null 输出为空字符串：用 `setNullString("")` + `setQuoteMode(QuoteMode.MINIMAL)`
  - 想让 null 输出为字面量 "NULL"：用 `setNullString("NULL")`

### 5.3 性能

- **🟢 大文件流式处理（百万行级）**
  - `CSVParser` 实现 `Iterable<CSVRecord>`，遍历时是惰性读取，不会一次性加载
  - 100 万行的 CSV 在 1GB 内存下完全可行（本 Demo 演示了 10 万行扫描耗时 144ms）

- **🟡 数字字段科学计数法**
  - 默认 `Object.toString()` 对极大/极小的 Double 会输出 `1.23E10`
  - **解决**：`new BigDecimal(value.toString()).toPlainString()`

- **🟢 0 依赖**
  - commons-csv 1.10.0 不依赖任何第三方库，可放心引入不会冲突

### 5.4 API 演进（1.10 之前 vs 1.10+）

| 旧 API（1.9 及之前） | 新 API（1.10+，推荐） |
| --- | --- |
| `CSVFormat.DEFAULT.withHeader(...)` | `CSVFormat.DEFAULT.builder().setHeader(...).build()` |
| `CSVFormat.EXCEL.withDelimiter(';')` | `CSVFormat.EXCEL.builder().setDelimiter(';').build()` |
| `new CSVParser(reader, format)` | `format.parse(reader)`（推荐） |

- 旧 API 仍然可用，但**官方强烈推荐 Builder 模式**
- Builder 模式返回的是不可变 `CSVFormat`，线程安全
- 旧 API 返回的是可变实例，多线程下可能踩坑

### 5.5 其他

- **🟡 Maven 仓库**
  - commons-csv 在 Maven Central，下载稳定（无区域问题）
  - 国内可配置阿里云镜像加速

- **🟡 不支持 Excel 的 `.xlsx` / `.xls`**
  - 只处理 CSV（纯文本）
  - 想要 Excel 二进制文件请用 Apache POI（已学 Day 4）或 EasyExcel（已学 Day 11）

- **🟡 列数不匹配会抛 IllegalArgumentException**
  - 默认严格模式：某行字段数比 header 少或多时抛异常
  - 宽松模式：用 `setAllowMissingColumnNames(true)` + `setIgnoreEmptyLines(true)`

## 六、运行方法

### 6.1 构建项目

```bash
cd commons-csv-demo
mvn clean package -DskipTests
```

成功后会在 `target/` 下生成：
- `commons-csv-demo.jar`（不含依赖）
- `dependency/` 目录（含全部依赖 JAR）

### 6.2 运行 Demo

```bash
# 复制依赖到 target/dependency
mvn dependency:copy-dependencies

# 运行基础 Demo（写 CSV、按 header 解析、按列号解析）
java -Dfile.encoding=UTF-8 -cp "target/commons-csv-demo.jar;target/dependency/*" \
     com.example.commonscsv.CommonsCsvBasicDemo

# 运行进阶 Demo（欧式分号、QuoteMode、BOM、注释、大文件、Map 转换）
java -Dfile.encoding=UTF-8 -cp "target/commons-csv-demo.jar;target/dependency/*" \
     com.example.commonscsv.CommonsCsvAdvancedDemo

# 运行实战 Demo（用户批量导入、订单导出、Spring 风格上传）
java -Dfile.encoding=UTF-8 -cp "target/commons-csv-demo.jar;target/dependency/*" \
     com.example.commonscsv.CommonsCsvPracticalDemo
```

> **Windows bash 注意**：bash 中 `target/dependency/*` 不会展开为多个 JAR，需要：
> - 在 cmd/PowerShell 中运行（`*` 自动展开）
> - 或在 bash 中用 `find target/dependency -name '*.jar' | tr '\n' ':'` 构造 classpath

### 6.3 生成的演示文件

运行后会在 `target/` 下生成：
- `basic-output.csv` — 基础写出的 RFC4180 CSV（含中文、含引号、含换行）
- `euro.csv` — 欧洲格式 CSV（分号分隔）
- `quote-modes.csv` — NON_NUMERIC 引号策略
- `bom.csv` — 含 BOM 头的 CSV
- `comments.csv` — 含注释行的 CSV
- `large.csv` — 10 万行大文件
- `no-header.csv` — 无 header 的 CSV
- `import-users.csv` — 含问题的用户导入文件
- `import-errors.csv` — 错误报告
- `orders-export.csv` — 含 UTF-8 BOM 的订单导出
- `upload-report.csv` — Spring 上传报告
- `products.csv` — 产品 CSV（多值字段）

## 七、与其它 CSV 库的横向对比

| 库 | 体积 | API 风格 | 性能 | 兼容性 | 适用场景 |
| --- | --- | --- | --- | --- | --- |
| **Apache Commons CSV** | 50KB | Builder + Fluent | 高 | RFC4180/Excel/MySQL/PG | 后端首选 |
| OpenCSV | 80KB | 老式 | 中 | RFC4180 | 老项目、状态机解析 |
| FastCSV | 30KB | 简洁 | 高 | RFC4180 | 极简依赖场景 |
| Univocity Parsers | 250KB | 强大 | 极高 | 极多格式 | 复杂数据处理 |
| Jackson CSV | 模块化 | Jackson 风格 | 中 | Jackson 生态 | 已用 Jackson 的项目 |

**推荐选择**：
- **90% 后端场景 → Apache Commons CSV**（本 Demo）
- 已经在用 Jackson → 用 `jackson-dataformat-csv`
- 极致性能 + 复杂字段映射 → Univocity Parsers

## 八、最佳实践清单

✅ **DO**:
1. **写文件加 UTF-8 BOM**，避免 Excel 中文乱码
2. **解析时显式 `setTrim(true)`**，防止 Excel 偷偷加的空格
3. **读 CSV 前先 `reader.read()` 吃 BOM**，或用 `UnicodeBOMInputStream`
4. **类型转换必须 try-catch**，别相信 CSV 数据
5. **导出文件设置正确的 Content-Type 和 Content-Disposition**
6. **大文件用流式 for-each**，不要 `parser.getRecords().forEach()`
7. **业务校验与语法解析分离** —— Commons CSV 只负责语法，业务校验由你做
8. **NULL 字段用空字符串兜底**，不要输出 "null"

❌ **DON'T**:
1. 不要用 `String.split(",")` 自己解析 CSV —— 处理引号、转义、换行会让你怀疑人生
2. 不要把整个 CSV 读进内存再用 Stream 处理 —— 用 `Iterable<CSVRecord>` 即可流式
3. 不要把 NaN、Infinity 写到 CSV（不是 RFC 4180 标准，部分解析器会拒绝）
4. 不要混用 tab 和逗号在同一个 CSV 中 —— 选一种方言就用到底
5. 不要忘记 close CSVPrinter —— 否则缓冲区不会刷盘

## 九、参考资源

- [Apache Commons CSV User Guide](https://commons.apache.org/proper/commons-csv/user-guide.html)
- [RFC 4180 规范](https://datatracker.ietf.org/doc/html/rfc4180) — CSV 的"宪法"
- [Apache Commons CSV Javadoc](https://commons.apache.org/proper/commons-csv/apidocs/index.html)
- [Spring Boot File Upload 官方文档](https://spring.io/guides/gs/uploading-files/)

---

**完成日期**: 2026-09-07
**版本**: Apache Commons CSV 1.10.0
**Java 兼容性**: Java 8+
