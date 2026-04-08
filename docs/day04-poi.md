# Day 04 - Apache POI：Java 操作 Excel/Word 的事实标准

## 工具简介

**Apache POI** 是 Apache 软件基金会开发的开源 Java 库，提供了操作 Microsoft Office 文档格式（Excel、Word、PowerPoint）的能力。它是 Java 生态中处理 Office 文档的事实标准，广泛应用于数据报表导出、文档自动生成、批量数据导入等场景。

- **GitHub**: https://github.com/apache/poi
- **官方网站**: https://poi.apache.org/
- **星标**: 3.8k+
- **最新版本**: 5.2.5（2024年）
- **License**: Apache License 2.0

### 核心模块说明

| 模块 | 格式 | 说明 |
|------|------|------|
| `poi` | `.xls` | 旧版 Excel（HSSF API） |
| `poi-ooxml` | `.xlsx` `.docx` | 新版 Excel/Word（XSSF/XWPF API） |
| `poi-scratchpad` | `.doc` `.ppt` | 旧版 Word/PowerPoint（HWPF/HSLF API） |

> 💡 **建议**：实际开发中统一使用 `.xlsx` / `.docx` 格式（OOXML），使用 `poi-ooxml` 依赖即可满足绝大多数需求。

---

## Maven 依赖配置

```xml
<!-- Apache POI - 核心库（包含 XSSF/XWPF，支持 .xlsx/.docx） -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- 如需操作旧版 .doc/.xls 格式，额外引入 -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-scratchpad</artifactId>
    <version>5.2.5</version>
</dependency>
```

> ⚠️ **注意**：`poi-ooxml` 已包含 `poi` 基础包，不需要再单独声明 `poi` 依赖。引入旧版 `poi` + `poi-ooxml` 可能导致类冲突。

---

## 核心 API 速查

### Excel 操作接口

```
Workbook (接口)
├── XSSFWorkbook  → .xlsx 格式 (>= Office 2007, 最常用)
├── HSSFWorkbook  → .xls  格式 (Old format, 最多 65536 行)
└── SXSSFWorkbook → 流式写入 .xlsx (处理大数据量，推荐 10w+ 行时使用)
```

### 常用 Excel 代码示例

```java
// =========== 创建 Excel ===========
Workbook workbook = new XSSFWorkbook();
Sheet sheet = workbook.createSheet("Sheet1");
Row row = sheet.createRow(0);
Cell cell = row.createCell(0);
cell.setCellValue("Hello POI");

// 写入文件
try (FileOutputStream out = new FileOutputStream("output.xlsx")) {
    workbook.write(out);
}
workbook.close();

// =========== 读取 Excel ===========
try (FileInputStream fis = new FileInputStream("input.xlsx");
     Workbook wb = new XSSFWorkbook(fis)) {
    Sheet sheet = wb.getSheetAt(0);
    for (Row row : sheet) {
        for (Cell cell : row) {
            // 推荐用 DataFormatter 读取，自动处理类型
            DataFormatter formatter = new DataFormatter();
            System.out.print(formatter.formatCellValue(cell) + " | ");
        }
        System.out.println();
    }
}

// =========== 单元格样式 ===========
CellStyle style = workbook.createCellStyle();
Font font = workbook.createFont();
font.setBold(true);
font.setColor(IndexedColors.RED.getIndex());
style.setFont(font);
style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
style.setAlignment(HorizontalAlignment.CENTER);
cell.setCellStyle(style);

// =========== 公式 ===========
cell.setCellFormula("SUM(A1:A10)");
workbook.setForceFormulaRecalculation(true); // 强制重算

// =========== 合并单元格 ===========
// addMergedRegion(firstRow, lastRow, firstCol, lastCol)
sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4)); // 合并第1行的A到E

// =========== 冻结行列 ===========
sheet.createFreezePane(2, 1); // 冻结前2列、前1行
```

### Word 操作示例

```java
// =========== 创建 Word 文档 ===========
XWPFDocument document = new XWPFDocument();

// 添加段落
XWPFParagraph para = document.createParagraph();
para.setAlignment(ParagraphAlignment.CENTER);
XWPFRun run = para.createRun();
run.setText("报告标题");
run.setBold(true);
run.setFontSize(18);

// 添加表格
XWPFTable table = document.createTable(3, 4); // 3行4列
table.getRow(0).getCell(0).setText("姓名");
table.getRow(0).getCell(1).setText("部门");

// 保存
try (FileOutputStream out = new FileOutputStream("output.docx")) {
    document.write(out);
}
document.close();
```

---

## Spring Boot 集成方式

### 1. 添加依赖（无需特别的 Starter）

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

### 2. 封装通用 Excel 导出工具类

```java
@Component
public class ExcelExportService {

    /**
     * 通用 Excel 导出方法
     * @param headers    表头数组
     * @param dataList   数据列表（每行为一个 Object[]）
     * @param sheetName  工作表名
     * @return           Excel 文件的字节数组
     */
    public byte[] exportToExcel(String[] headers, List<Object[]> dataList, String sheetName)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 写入数据
            for (int rowIdx = 0; rowIdx < dataList.size(); rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                Object[] rowData = dataList.get(rowIdx);
                for (int colIdx = 0; colIdx < rowData.length; colIdx++) {
                    setCellValue(row.createCell(colIdx), rowData[colIdx]);
                }
            }

            // 自动列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
```

### 3. Controller 层提供下载接口

```java
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 导出员工数据为 Excel
     */
    @GetMapping("/employees")
    public void exportEmployees(HttpServletResponse response) throws IOException {
        // 设置响应头，告知浏览器这是一个文件下载
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" +
                URLEncoder.encode("员工列表.xlsx", "UTF-8"));

        // 查询数据
        List<Employee> employees = employeeService.getAllEmployees();

        // 转换为 Object[][] 格式
        String[] headers = {"工号", "姓名", "部门", "月薪"};
        List<Object[]> dataList = employees.stream()
                .map(e -> new Object[]{e.getId(), e.getName(), e.getDept(), e.getSalary()})
                .collect(Collectors.toList());

        // 生成 Excel
        byte[] excelBytes = excelExportService.exportToExcel(headers, dataList, "员工信息");

        // 写入响应流
        try (OutputStream out = response.getOutputStream()) {
            out.write(excelBytes);
            out.flush();
        }
    }
}
```

### 4. 大数据量导出（流式 + 分批查询）

```java
/**
 * 大数据量导出：使用 SXSSFWorkbook + 分批查询，防止 OOM
 */
@GetMapping("/employees/large")
public void exportLargeData(HttpServletResponse response) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=large_export.xlsx");

    // 内存中只保留100行
    try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
         OutputStream out = response.getOutputStream()) {

        SXSSFSheet sheet = workbook.createSheet("数据导出");

        // 写表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("序号");
        headerRow.createCell(1).setCellValue("数据");

        int rowNum = 1;
        int page = 0;
        int pageSize = 5000;
        List<Employee> batch;

        // 分批查询写入
        do {
            batch = employeeService.queryByPage(page++, pageSize);
            for (Employee emp : batch) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(emp.getName());
            }
        } while (batch.size() == pageSize);

        workbook.write(out);
        out.flush();

        // 重要：清理临时文件
        workbook.dispose();
    }
}
```

---

## 注意事项

### 🔴 Bug 风险

1. **内存溢出 (OOM)**
   - 使用 `XSSFWorkbook` 操作大量数据（10万+行）时，会将全部数据加载到内存
   - **解决**: 导出时使用 `SXSSFWorkbook`，导入时考虑 SAX 解析模式

2. **自动列宽中文不准确**
   - `sheet.autoSizeColumn(i)` 对中文字符宽度计算不准确
   - **解决**: 手动设置列宽 `sheet.setColumnWidth(i, 20 * 256)`（单位是字符的 1/256）

3. **公式不自动重算**
   - 写入公式后用 Java 读取，`getNumericCellValue()` 可能返回 0
   - **解决**: 在写出前调用 `workbook.setForceFormulaRecalculation(true)`，或使用 `FormulaEvaluator` 显式计算

4. **单元格类型陷阱**
   - 直接调用 `cell.getStringCellValue()` 但单元格实际是数值类型，会抛出异常
   - **解决**: 使用 `DataFormatter formatter = new DataFormatter(); formatter.formatCellValue(cell)` 统一读取

5. **SXSSFWorkbook 临时文件泄漏**
   - 忘记调用 `workbook.dispose()` 会导致磁盘上的临时文件不被清理
   - **解决**: 使用 try-with-resources 后，在 `close()` 之前手动调用 `dispose()`

### 🟡 性能注意

1. **样式复用**: 同类样式应创建一个对象反复使用，不要在循环中 `workbook.createCellStyle()`，一个 Workbook 最多支持约 6.5万种样式

2. **字体复用**: 同上，`workbook.createFont()` 应在循环外创建并复用

3. **autoSizeColumn 代价高**: 每列调用一次会遍历所有行，数据量大时谨慎使用

4. **SXSSFWorkbook 窗口大小**: `new SXSSFWorkbook(rowAccessWindowSize)` 的窗口越小内存越小，但不能回溯修改已刷出的行

### 🟢 使用限制

| 格式 | 最大行数 | 最大列数 |
|------|---------|---------|
| HSSF (.xls) | 65,536 | 256 |
| XSSF (.xlsx) | 1,048,576 | 16,384 |
| SXSSF (.xlsx) | 1,048,576 | 16,384（流式） |

---

## 运行方法

### 前置要求

- JDK 8+
- Maven 3.6+

### 编译和运行

```bash
cd java-tools-learning/poi-demo

# 编译
mvn clean package -DskipTests

# 运行基础演示（创建 Excel 文件）
mvn exec:java -Dexec.mainClass="com.example.poi.PoiExcelBasicDemo"

# 运行高级演示（公式、合并单元格、冻结）
mvn exec:java -Dexec.mainClass="com.example.poi.PoiExcelAdvancedDemo"

# 运行实战演示（数据导出、Word文档、大数据量）
mvn exec:java -Dexec.mainClass="com.example.poi.PoiPracticalDemo"
```

### 生成文件位置

运行后，文件会生成到 `src/main/resources/` 目录下：

- `students_basic.xlsx` - 基础 Excel 演示
- `multi_sheets.xlsx` - 多工作表演示
- `styled_report.xlsx` - 格式化报表
- `formula_demo.xlsx` - 公式演示
- `merge_cells.xlsx` - 合并单元格演示
- `freeze_pane.xlsx` - 冻结面板演示
- `employees_export.xlsx` - 员工数据导出
- `performance_report.docx` - Word 绩效报告
- `large_data_export.xlsx` - 5万行大数据导出
- `salary_slip.xlsx` - 工资单模板填充

---

## 常见替代方案对比

| 库 | 优点 | 缺点 |
|----|------|------|
| Apache POI | 功能全面，支持所有 Office 格式，社区成熟 | API 繁琐，大数据需用 SXSSF |
| EasyExcel（阿里） | API 简洁，大数据性能优秀，注解驱动 | 仅支持 Excel，不支持 Word |
| Hutool-poi | 封装 POI，链式 API，上手快 | 底层还是 POI，无性能提升 |
| FastExcel | 纯 Java，高性能，读写均快 | 功能相对简单，社区小 |

> 💡 **推荐**：如果主要需求是 Excel 数据导入导出（10万+行），使用 **EasyExcel**；如需要同时操作 Excel + Word + PPT，用 **Apache POI**。
