# Day 10: Apache PDFBox - PDF 处理库

## 工具简介

**Apache PDFBox** 是 Apache 软件基金会维护的开源 Java PDF 处理库，是 Java 生态中最成熟、最广泛使用的 PDF 操作框架之一。

| 属性 | 信息 |
|------|------|
| **GitHub** | https://github.com/apache/pdfbox |
| **官方文档** | https://pdfbox.apache.org/ |
| **当前稳定版** | 3.0.3 (Java 11+) / **2.0.31** (Java 8 兼容) |
| **Maven 仓库** | https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox |
| **Stars** | 2.6k+ |
| **License** | Apache 2.0 |

### 核心能力

- 📄 **创建 PDF**：从零创建文档，支持文字、图片、图形绘制
- 🔍 **提取文本**：从 PDF 中提取纯文本、带坐标的文本位置
- 🔗 **合并/拆分**：多个 PDF 合并，或按页拆分
- 💧 **水印/注释**：叠加水印、添加高亮/文字注释
- 📋 **表单操作**：填写 AcroForm 表单字段
- 🖨️ **PDF 打印**：通过 Java Print API 打印 PDF

### 2.x vs 3.x 版本选择

| | 2.0.x（推荐 Java 8） | 3.0.x |
|--|--|--|
| JDK 要求 | Java 8+ | Java 11+ |
| API 变化 | 稳定成熟 | 部分 API 重构 |
| 字体处理 | 需手动嵌入 | 改善的字体管理 |
| 线程安全 | 非线程安全 | 非线程安全 |
| **推荐场景** | **企业生产环境** | 新项目可考虑 |

> **本 Demo 使用 2.0.31 版本，兼容 Java 8，适合企业级应用。**

---

## Maven 依赖配置

```xml
<!-- pom.xml -->
<properties>
    <pdfbox.version>2.0.31</pdfbox.version>
</properties>

<dependencies>
    <!-- PDFBox 核心库 -->
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>${pdfbox.version}</version>
    </dependency>

    <!-- PDFBox 工具包（可选，包含命令行工具类） -->
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox-tools</artifactId>
        <version>${pdfbox.version}</version>
    </dependency>

    <!-- FontBox（字体处理，通常作为 pdfbox 传递依赖自动引入） -->
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>fontbox</artifactId>
        <version>${pdfbox.version}</version>
    </dependency>

    <!-- SLF4J（PDFBox 内部日志，需提供实现） -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>1.7.36</version>
    </dependency>
</dependencies>
```

---

## 核心 API 速查

### 1. 创建 PDF 文档

```java
// 创建新文档
try (PDDocument document = new PDDocument()) {
    // 添加页面（A4）
    PDPage page = new PDPage(PDRectangle.A4);
    document.addPage(page);

    // 获取内容流（用于绘制内容）
    try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
        // 写入文字
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
        cs.newLineAtOffset(50, 750);  // (x, y) 坐标原点在左下角
        cs.showText("Hello PDFBox!");
        cs.endText();

        // 绘制线条
        cs.moveTo(50, 700);
        cs.lineTo(500, 700);
        cs.stroke();

        // 填充矩形
        cs.setNonStrokingColor(0.9f, 0.9f, 0.9f); // 灰色
        cs.addRect(50, 600, 200, 50);  // (x, y, width, height)
        cs.fill();
    }

    document.save("output.pdf");
}
```

### 2. 读取 PDF 文本

```java
// 提取全部文本
try (PDDocument document = PDDocument.load(new File("input.pdf"))) {
    PDFTextStripper stripper = new PDFTextStripper();
    
    // 提取全部页面
    String fullText = stripper.getText(document);
    
    // 提取指定页面（从1开始）
    stripper.setStartPage(2);
    stripper.setEndPage(3);
    String partText = stripper.getText(document);
}
```

### 3. 合并 PDF

```java
PDFMergerUtility merger = new PDFMergerUtility();
merger.addSource("file1.pdf");
merger.addSource("file2.pdf");
merger.setDestinationFileName("merged.pdf");
merger.mergeDocuments(null);
```

### 4. 拆分 PDF

```java
try (PDDocument document = PDDocument.load(new File("input.pdf"))) {
    Splitter splitter = new Splitter();
    splitter.setSplitAtPage(1);  // 每页拆分为独立文档
    
    List<PDDocument> pages = splitter.split(document);
    for (int i = 0; i < pages.size(); i++) {
        try (PDDocument page = pages.get(i)) {
            page.save("page_" + (i+1) + ".pdf");
        }
    }
}
```

### 5. 添加水印

```java
try (PDDocument document = PDDocument.load(new File("source.pdf"))) {
    for (PDPage page : document.getPages()) {
        try (PDPageContentStream cs = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            // 设置透明度
            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant(0.25f);
            cs.setGraphicsStateParameters(gs);
            
            // 旋转45度绘制水印
            cs.setNonStrokingColor(0.8f, 0.8f, 0.8f);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 60);
            double angle = Math.toRadians(45);
            cs.setTextMatrix(new Matrix(
                (float)Math.cos(angle), (float)Math.sin(angle),
                (float)-Math.sin(angle), (float)Math.cos(angle),
                150, 300));
            cs.showText("DRAFT");
            cs.endText();
        }
    }
    document.save("watermarked.pdf");
}
```

---

## Spring Boot 集成方式

### 依赖配置（Spring Boot 项目）

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- PDFBox（Spring Boot 无默认 Starter，直接引入） -->
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>2.0.31</version>
    </dependency>
</dependencies>
```

### PDF 服务封装示例

```java
@Service
public class PdfService {

    /**
     * 生成 PDF 并返回字节数组（适用于 HTTP 下载）
     */
    public byte[] generateReport(ReportData data) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 750);
                cs.showText(data.getTitle());
                cs.endText();
                // ... 更多内容绘制
            }
            
            document.save(baos);
            return baos.toByteArray();
        }
    }
    
    /**
     * 从上传的 PDF 提取文本
     */
    public String extractText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(document);
        }
    }
}
```

### Controller 下载接口示例

```java
@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    /**
     * 生成并下载 PDF 报告
     * GET /api/pdf/report?title=xxx
     */
    @GetMapping("/report")
    public ResponseEntity<byte[]> downloadReport(@RequestParam String title) throws IOException {
        ReportData data = new ReportData(title);
        byte[] pdfBytes = pdfService.generateReport(data);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
            ContentDisposition.attachment().filename("report.pdf").build()
        );
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
    
    /**
     * 上传 PDF 并提取文本
     * POST /api/pdf/extract
     */
    @PostMapping("/extract")
    public ResponseEntity<String> extractText(@RequestParam MultipartFile file) throws IOException {
        String text = pdfService.extractText(file);
        return ResponseEntity.ok(text);
    }
}
```

---

## 注意事项

### ⚠️ 中文字体支持

**PDFBox 内置的 PDType1Font（如 HELVETICA、TIMES_ROMAN）只支持 Latin 字符集，无法显示中文。**

解决方案：使用 `PDType0Font` 加载 TrueType 字体文件（.ttf）：

```java
// 从系统字体或项目资源加载 TrueType 字体
File fontFile = new File("C:/Windows/Fonts/simhei.ttf"); // 黑体
PDType0Font font = PDType0Font.load(document, fontFile);

cs.beginText();
cs.setFont(font, 14);
cs.newLineAtOffset(50, 700);
cs.showText("你好，世界！");  // 现在可以显示中文
cs.endText();
```

> **注意**：从 classpath 加载字体需要确保字体文件已嵌入 JAR，且加载后字体数据会保存在 PDF 文件中（增大文件体积）。

### ⚠️ 资源泄漏风险

PDDocument 和 PDPageContentStream **必须关闭**，否则会导致临时文件无法释放：

```java
// 推荐：try-with-resources（Java 7+）
try (PDDocument document = new PDDocument()) {
    try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
        // ...
    } // cs 自动关闭
} // document 自动关闭

// 错误示范：不关闭会导致文件句柄泄漏
PDDocument doc = new PDDocument();
// ... 忘记调用 doc.close()
```

### ⚠️ 线程安全

**PDDocument 不是线程安全的**。多线程场景下，每个线程应使用独立的 PDDocument 实例：

```java
// 错误：多线程共享同一 PDDocument
// 正确：每个线程 new PDDocument()，处理完后 close()

// 在 Web 服务中，每次请求应创建新的 PDDocument 实例
@GetMapping("/pdf")
public byte[] generatePdf() throws IOException {
    try (PDDocument doc = new PDDocument()) {  // 请求级别，不共享
        // ...
    }
}
```

### ⚠️ 内存管理

处理大型 PDF（100MB+）时，PDFBox 会将文档加载到内存：

```java
// 使用 MemoryUsageSetting 控制内存策略（2.x API）
// 超过指定内存阈值后，自动溢出到临时文件
PDDocument document = PDDocument.load(
    new File("large.pdf"),
    MemoryUsageSetting.setupMixed(50 * 1024 * 1024) // 50MB 内存上限
);
```

### ⚠️ PDF/A 合规性

PDFBox 生成的 PDF 默认不是 PDF/A 格式。需要 PDF/A 存档格式时，需额外设置文档标识和颜色配置文件，或使用专业库（如 Apache Preflight 验证）。

### ⚠️ 性能建议

- **批量创建**：重用 `PDType1Font` 实例（字体对象可以跨页重用）
- **文本提取**：`PDFTextStripper` 是非线程安全的，并发提取时需每次新建实例
- **文件保存**：`document.save()` 前不要多次调用，每次 save 都会重写整个文件

---

## 运行方法

### 前提条件
- JDK 8+
- Maven 3.6+

### 编译并运行

```bash
# 进入 Demo 目录
cd pdfbox-demo

# 编译打包（跳过测试）
mvn clean package -DskipTests

# 运行基础演示（创建/读取/多页PDF）
mvn exec:java -Dexec.mainClass="com.example.pdfbox.PdfBoxBasicDemo"

# 运行高级演示（合并/拆分/元数据/文字旋转）
mvn exec:java -Dexec.mainClass="com.example.pdfbox.PdfBoxAdvancedDemo"

# 运行实战演示（水印/表格/批量提取/业务报告）
mvn exec:java -Dexec.mainClass="com.example.pdfbox.PdfBoxPracticalDemo"
```

### 输出文件

运行后，PDF 文件会生成在 `target/pdf-output/` 目录：

| 文件名 | 来源 | 说明 |
|--------|------|------|
| `basic_simple.pdf` | BasicDemo | 基础文字+图形PDF |
| `basic_multipage.pdf` | BasicDemo | 5页多页PDF |
| `advanced_merged.pdf` | AdvancedDemo | 合并后PDF（5页） |
| `advanced_split_part*.pdf` | AdvancedDemo | 拆分结果 |
| `advanced_metadata.pdf` | AdvancedDemo | 带自定义元数据的PDF |
| `advanced_rotation.pdf` | AdvancedDemo | 旋转文字效果 |
| `practical_watermarked.pdf` | PracticalDemo | 半透明水印PDF |
| `practical_table.pdf` | PracticalDemo | 表格报表PDF |
| `practical_business_report.pdf` | PracticalDemo | 完整业务报告 |

---

## 与其他 PDF 库对比

| 特性 | Apache PDFBox | iText 7 | OpenPDF |
|------|---------------|---------|---------|
| 许可证 | Apache 2.0（完全免费） | AGPL/商业双许可 | LGPL（免费） |
| 中文支持 | 需加载字体文件 | 内置良好 | 需加载字体文件 |
| 表格支持 | 手动绘制 | 内置 Table 组件 | 内置 PdfPTable |
| 文本提取 | 优秀 | 良好 | 良好 |
| 社区活跃度 | Apache 基金会维护，稳定 | 活跃但商业化 | 社区维护 |
| **推荐场景** | **开源项目/企业内部工具** | **复杂排版商业系统** | **替代旧 iText 2.x** |

> **结论**：Apache PDFBox 是开源项目的首选，免费无限制；若需要复杂表格布局和样式，可考虑 OpenPDF（兼容 iText API）。
