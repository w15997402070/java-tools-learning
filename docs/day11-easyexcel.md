# Day 11: EasyExcel - 阿里巴巴高性能 Excel 读写库

## 工具简介

**EasyExcel** 是阿里巴巴开源的一款基于 Apache POI 封装的 Java Excel 读写库。  
它解决了 POI 原生 API 在处理大文件时的最大痛点——内存溢出（OOM）问题。

### 核心优势

| 特性 | Apache POI（原生） | EasyExcel |
|------|----------|-----------|
| 大文件内存 | 百万行可能 OOM | 流式处理，极低内存 |
| API 复杂度 | 复杂、冗长 | 注解 + 一行代码 |
| 读写速度 | 一般 | 更快（底层优化） |
| 学习成本 | 高 | 低（注解驱动） |
| 适合场景 | 精细格式控制 | 数据导入导出 |

### 内存对比（官方数据）

- **POI 读取 75M Excel**：约占用 **700M 内存**
- **EasyExcel 读取 75M Excel**：约占用 **64M 内存**（相差约 10 倍）

### GitHub 地址

🔗 https://github.com/alibaba/easyexcel  
⭐ Stars: 32k+  
📦 当前使用版本: **3.3.4**（Java 8 兼容）

---

## Maven 依赖配置

```xml
<!-- EasyExcel 核心依赖（已内置 POI，无需单独引入 POI） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>3.3.4</version>
</dependency>

<!-- 推荐配合 SLF4J 使用日志 -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.12</version>
</dependency>
```

> **⚠️ 注意**：EasyExcel 已内置了指定版本的 POI，**不要在同一项目中再引入其他版本的 POI**，否则会出现依赖冲突。

---

## 核心注解速览

```java
// 映射 Excel 列（支持 index 指定顺序，或 value 匹配列标题）
@ExcelProperty(value = "用户名", index = 1)

// 列宽（字符数）
@ColumnWidth(20)

// 表头行高 / 内容行高（磅值）
@HeadRowHeight(24)
@ContentRowHeight(18)

// 日期格式化（写入时自动转为字符串）
@DateTimeFormat("yyyy-MM-dd HH:mm:ss")

// 数字格式化
@NumberFormat("#,##0.00")

// 忽略该字段（不读写）
@ExcelIgnore
```

---

## 基础用法

### 1. 定义数据模型

```java
@HeadRowHeight(20)
@ContentRowHeight(18)
@ColumnWidth(20)
public class UserModel {

    @ExcelProperty(value = "用户ID", index = 0)
    @ColumnWidth(10)
    private Integer userId;

    @ExcelProperty(value = "用户名", index = 1)
    private String username;

    @ExcelProperty(value = "注册时间", index = 2)
    @DateTimeFormat("yyyy-MM-dd")
    private Date registerTime;

    @ExcelIgnore
    private String password;   // 不写入 Excel

    // getter/setter...
}
```

### 2. 写 Excel（最简单写法）

```java
// 一行代码写入 Excel
EasyExcel.write("output.xlsx", UserModel.class)
         .sheet("用户列表")
         .doWrite(dataList);
```

### 3. 读 Excel（同步，小数据量）

```java
// doReadSync() 直接返回 List，适合 < 5000 行
List<UserModel> list = EasyExcel.read("output.xlsx")
                                .head(UserModel.class)
                                .sheet()
                                .doReadSync();
```

### 4. 读 Excel（监听器，推荐！大数据量必用）

```java
EasyExcel.read("output.xlsx", UserModel.class, new ReadListener<UserModel>() {
    @Override
    public void invoke(UserModel data, AnalysisContext context) {
        // 每读取一行调用一次，可在此批量入库
        processList.add(data);
        if (processList.size() >= 500) {
            saveToDatabase(processList);  // 批量插入
            processList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 处理最后不足一批的数据
        if (!processList.isEmpty()) {
            saveToDatabase(processList);
        }
    }
}).sheet().doRead();
```

### 5. 多 Sheet 写入

```java
ExcelWriter excelWriter = EasyExcel.write("multi.xlsx", UserModel.class).build();
try {
    WriteSheet sheet1 = EasyExcel.writerSheet(0, "用户A").build();
    excelWriter.write(listA, sheet1);

    WriteSheet sheet2 = EasyExcel.writerSheet(1, "用户B").build();
    excelWriter.write(listB, sheet2);
} finally {
    excelWriter.finish();  // 必须调用！
}
```

### 6. 动态表头（无 POJO 模型）

```java
// 表头：List<List<String>>，每个子 List 代表一列（支持多级）
List<List<String>> head = new ArrayList<>();
head.add(Arrays.asList("报表", "月份"));
head.add(Arrays.asList("报表", "销售额"));

// 数据：List<List<Object>>
List<List<Object>> data = new ArrayList<>();
data.add(Arrays.asList("1月", 120000.0));

EasyExcel.write("report.xlsx").head(head).sheet("报表").doWrite(data);
```

---

## Spring Boot 集成方式

### 1. 添加依赖（Boot 项目已自动管理版本）

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>3.3.4</version>
</dependency>
```

### 2. 导出接口（HTTP 下载）

```java
@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    @Autowired
    private UserService userService;

    /**
     * 导出用户列表为 Excel 文件
     * GET /api/excel/export/users
     */
    @GetMapping("/export/users")
    public void exportUsers(HttpServletResponse response) throws IOException {
        // 设置响应头，触发浏览器下载
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户列表", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 从数据库查询
        List<UserModel> users = userService.findAll();

        // 写入响应流
        EasyExcel.write(response.getOutputStream(), UserModel.class)
                 .sheet("用户列表")
                 .doWrite(users);
    }
}
```

### 3. 导入接口（HTTP 上传）

```java
/**
 * 接收用户上传的 Excel 文件并导入
 * POST /api/excel/import/users
 */
@PostMapping("/import/users")
public ResponseEntity<String> importUsers(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("文件为空");
    }

    List<UserModel> successList = new ArrayList<>();
    List<String> errorMessages = new ArrayList<>();

    try {
        EasyExcel.read(file.getInputStream(), UserModel.class,
                new ReadListener<UserModel>() {
                    private final List<UserModel> batch = new ArrayList<>(500);

                    @Override
                    public void invoke(UserModel data, AnalysisContext context) {
                        batch.add(data);
                        if (batch.size() >= 500) {
                            userService.batchSave(batch);
                            successList.addAll(batch);
                            batch.clear();
                        }
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {
                        if (!batch.isEmpty()) {
                            userService.batchSave(batch);
                            successList.addAll(batch);
                            batch.clear();
                        }
                    }

                    @Override
                    public void onException(Exception e, AnalysisContext context) {
                        errorMessages.add("第 " + context.readRowHolder().getRowIndex()
                                + " 行：" + e.getMessage());
                    }
                }).sheet().doRead();
    } catch (IOException e) {
        return ResponseEntity.internalServerError().body("读取文件失败：" + e.getMessage());
    }

    return ResponseEntity.ok("导入成功 " + successList.size() + " 条"
            + (errorMessages.isEmpty() ? "" : "，错误 " + errorMessages.size() + " 条"));
}
```

### 4. 自定义样式（HorizontalCellStyleStrategy）

```java
// 表头样式
WriteCellStyle headStyle = new WriteCellStyle();
headStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
headStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
WriteFont headFont = new WriteFont();
headFont.setBold(true);
headFont.setColor(IndexedColors.WHITE.getIndex());
headStyle.setWriteFont(headFont);
headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

// 内容样式
WriteCellStyle contentStyle = new WriteCellStyle();
contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

HorizontalCellStyleStrategy strategy =
    new HorizontalCellStyleStrategy(headStyle, contentStyle);

EasyExcel.write(filePath, UserModel.class)
         .registerWriteHandler(strategy)
         .sheet("数据")
         .doWrite(data);
```

### 5. 添加冻结行 + 自动筛选器

```java
EasyExcel.write(filePath, UserModel.class)
         .registerWriteHandler(new SheetWriteHandler() {
             @Override
             public void afterSheetCreate(SheetWriteHandlerContext context) {
                 Sheet sheet = context.getWriteSheetHolder().getSheet();
                 sheet.createFreezePane(0, 1);  // 冻结首行
                 sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, 5)); // 自动筛选器
             }
         })
         .sheet("数据")
         .doWrite(data);
```

---

## 注意事项

### ⚠️ Bug 风险

1. **ExcelWriter 必须调用 finish()**  
   忘记调用 `excelWriter.finish()` 会导致 Excel 文件损坏（无法打开）。  
   推荐使用 `try-finally` 确保调用：
   ```java
   ExcelWriter excelWriter = EasyExcel.write(...).build();
   try {
       // 写入...
   } finally {
       if (excelWriter != null) excelWriter.finish();
   }
   ```

2. **@DateTimeFormat 仅对写入生效**  
   读取 Excel 时，如果 Excel 中日期列是数字（序列号格式），需额外处理转换，  
   不能依赖 `@DateTimeFormat` 自动转换。

3. **同一 POI 版本冲突**  
   EasyExcel 内置了特定版本 POI，如果项目中还引入了其他版本 POI 或使用 Apache PDFBox（POI 同源）时，  
   可能出现类冲突。解决方式：统一排除旧版依赖，使用 EasyExcel 内置版本。

4. **`doReadSync()` 不适合大文件**  
   `doReadSync()` 会将所有数据加载到内存中，超过 5000 行务必改用监听器模式。

### ⚠️ 性能注意

1. **写入大文件请分批**  
   单次 `doWrite()` 传入超过 10 万行数据会造成内存峰值过高。  
   推荐通过 `ExcelWriter` + 分批 `write()` 控制每批 2000~5000 行。

2. **不要在监听器 invoke() 里做重查询**  
   每行一次 DB 查询会导致性能极差（N+1 问题），应攒到 500~1000 行再批量操作。

3. **EasyExcel 不支持 XLS（97-2003 格式）写入**  
   只支持 `.xlsx`（OOXML 格式）。读取可以兼容 `.xls`。

### ⚠️ 使用限制

1. **不支持公式计算**  
   EasyExcel 写入的公式单元格，打开 Excel 后需手动刷新才能计算结果。

2. **合并单元格写入需要自定义 WriteHandler**  
   EasyExcel 本身不提供高级的合并单元格 API，需要实现 `CellWriteHandler` 手动操作 POI。

3. **复杂样式控制不如原生 POI**  
   EasyExcel 适合数据导入导出，对于高度定制化的 Excel 报表（多级表头合并、复杂图表等），  
   建议使用原生 Apache POI 或 EasyExcel + WriteHandler 组合。

---

## 运行方法

### 前置条件

- Java 8+
- Maven 3.6+

### 编译打包

```bash
cd easyexcel-demo
mvn clean package -DskipTests
```

### 运行演示类

```bash
# 基础演示（写 Excel / 读 Excel / 多 Sheet）
java -cp target/easyexcel-demo-1.0-SNAPSHOT.jar com.example.easyexcel.EasyExcelBasicDemo

# 高级演示（自定义样式 / 动态表头 / 冻结行）
java -cp target/easyexcel-demo-1.0-SNAPSHOT.jar com.example.easyexcel.EasyExcelAdvancedDemo

# 实战演示（大数据量写入 50000 行 / 分批读取导入 / 按部门分 Sheet）
java -cp target/easyexcel-demo-1.0-SNAPSHOT.jar com.example.easyexcel.EasyExcelPracticalDemo
```

> 生成的 Excel 文件保存在当前目录的 `output/` 文件夹下。

### 依赖冲突排查

如果出现 `NoSuchMethodError` 或 `ClassNotFoundException`，检查是否有其他 POI 依赖冲突：

```bash
mvn dependency:tree | grep poi
```

---

## 与 Apache POI 的选型建议

| 场景 | 推荐 |
|------|------|
| 数据导入（用户上传 Excel） | ✅ EasyExcel |
| 数据导出（大量行数） | ✅ EasyExcel |
| 简单报表（表格样式） | ✅ EasyExcel + WriteHandler |
| 复杂报表（多级合并、图表） | ✅ Apache POI 原生 |
| 操作已有 Excel 模板 | ✅ Apache POI 原生 |
| Spring Boot 批量导入导出 | ✅ EasyExcel |

---

## 参考资料

- 官方文档：https://easyexcel.opensource.alibaba.com/
- GitHub：https://github.com/alibaba/easyexcel
- 官方 Demo：https://github.com/alibaba/easyexcel/tree/master/easyexcel-test/src/test/java/com/alibaba/easyexcel/test/demo
