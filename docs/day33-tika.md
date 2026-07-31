# Day 33: Apache Tika — 文档内容提取与元数据分析库

## 1. 工具简介

**Apache Tika** 是一个开源的内容分析工具包，能够检测、提取和解析上千种文件格式的文本与元数据。

- **GitHub**: https://github.com/apache/tika
- **官网**: https://tika.apache.org
- **版本**: 2.9.1（Java 8 兼容）
- **星标**: 2.3k+（Apache 官方项目，广泛应用于搜索引擎、内容管理、数据治理）

### 核心能力

| 能力 | 说明 |
|------|------|
| MIME 检测 | 基于文件扩展名、Magic Number、内容综合判断文件类型 |
| 文本提取 | 从 PDF、Word、Excel、PPT、HTML、图片等文件中抽取纯文本 |
| 元数据读取 | 提取作者、标题、创建时间、页数、软件版本等结构化信息 |
| 结构化输出 | 支持输出 XHTML，便于保留段落、表格、标题等结构 |
| 语言检测 | 内置多种语言检测模型，可识别文本语种 |

## 2. Maven 依赖

```xml
<properties>
    <tika.version>2.9.1</tika.version>
</properties>

<dependencies>
    <!-- Tika 核心 -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>${tika.version}</version>
    </dependency>

    <!-- 标准解析包：包含 PDF、Office、HTML、XML、图片、压缩包等解析器 -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-parsers-standard-package</artifactId>
        <version>${tika.version}</version>
    </dependency>

    <!-- 日志实现（可选，用于减少启动时的 SLF4J 警告） -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

> **注意**：`tika-parsers-standard-package` 已经传递依赖了 `pdfbox`、`poi-ooxml` 等常用解析库，通常无需单独引入。

## 3. 快速开始

### 3.1 检测 MIME 类型

```java
Tika tika = new Tika();
try (InputStream is = Files.newInputStream(Paths.get("report.pdf"))) {
    String mime = tika.detect(is);
    System.out.println(mime); // application/pdf
}
```

### 3.2 一键提取文本

```java
Tika tika = new Tika();
try (InputStream is = Files.newInputStream(Paths.get("contract.docx"))) {
    String text = tika.parseToString(is);
    System.out.println(text);
}
```

### 3.3 提取文本 + 元数据

```java
AutoDetectParser parser = new AutoDetectParser();
BodyContentHandler handler = new BodyContentHandler();
Metadata metadata = new Metadata();

try (InputStream is = Files.newInputStream(Paths.get("report.pdf"))) {
    parser.parse(is, handler, metadata);
    System.out.println(handler.toString());
    for (String name : metadata.names()) {
        System.out.println(name + " = " + metadata.get(name));
    }
}
```

## 4. Spring Boot 集成

### 4.1 添加依赖

与上述 Maven 依赖相同，直接加入 `pom.xml` 即可。

### 4.2 注册 Tika Bean

```java
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TikaConfig {

    @Bean
    public Tika tika() {
        return new Tika();
    }
}
```

### 4.3 上传文件解析 Service

```java
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentParseService {

    private final Tika tika;

    public DocumentParseService(Tika tika) {
        this.tika = tika;
    }

    public DocumentParseResult parse(MultipartFile file) throws Exception {
        String mimeType = tika.detect(file.getInputStream());
        String content = tika.parseToString(file.getInputStream());
        return new DocumentParseResult(mimeType, content);
    }
}
```

### 4.4 控制器示例

```java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentParseService parseService;

    public DocumentController(DocumentParseService parseService) {
        this.parseService = parseService;
    }

    @PostMapping("/parse")
    public ResponseEntity<DocumentParseResult> parse(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(parseService.parse(file));
    }
}
```

## 5. 注意事项

### 5.1 内存与性能

- **大文件风险**：`Tika#parseToString` 和 `BodyContentHandler()` 默认不限制输出长度，解析大 PDF/Office 文件可能导致 OOM。
  - 解决方案：使用 `new BodyContentHandler(int writeLimit)` 限制最大字符数。
- **解析耗时**：复杂 PDF、扫描件、大型 PPT 解析可能耗时数秒甚至更长，建议异步处理。
- **临时文件**：部分解析器会在 `%TEMP%` 目录生成临时文件，注意磁盘空间与清理。

### 5.2 安全风险

- **文件类型校验**：上传场景不能仅凭扩展名判断文件类型，应使用 `Tika#detect` 检测 MIME。
- **不可信文件解析**：Tika 依赖大量第三方解析库，历史上曾出现解析器漏洞。建议在沙箱或低权限环境中解析外部上传的文件。
- **XXE 风险**：早期版本的 Office/XML 解析器存在 XXE 漏洞，使用 2.9.x 并关注官方安全公告。

### 5.3 依赖冲突

- `tika-parsers-standard-package` 会引入大量传递依赖（如 POI、PDFBox、BouncyCastle 等）。
- 如果项目已有 `poi` 或 `pdfbox`，注意版本冲突，可通过 `<exclusions>` 排除。
- 推荐生产环境按需引入解析器模块，而非直接使用 `standard-package`。

### 5.4 语言检测

- 语言检测依赖统计模型，短文本或混合语种准确率会下降。
- 模型文件默认从 classpath 加载，首次调用可能稍慢。

### 5.5 OCR 能力

- Tika 支持通过 Tesseract 对图片/扫描 PDF 进行 OCR，但需要额外安装 Tesseract 引擎并配置环境变量，默认不启用。

## 6. 运行方法

### 6.1 编译项目

```bash
cd d:/ai/workbuddy/java-tools-learning/tika-demo
mvn clean package -DskipTests
```

### 6.2 运行单个 Demo

```bash
# 基础演示
mvn exec:java -Dexec.mainClass="com.example.tika.TikaBasicDemo"

# 或使用打包后的 fat-jar
java -jar target/tika-demo-1.0-SNAPSHOT.jar
```

### 6.3 运行其他演示类

```bash
java -cp target/tika-demo-1.0-SNAPSHOT.jar com.example.tika.TikaAdvancedDemo
java -cp target/tika-demo-1.0-SNAPSHOT.jar com.example.tika.TikaPracticalDemo
```

> **提示**：`TikaPracticalDemo` 中使用了相对路径 `tika-demo/src/main/resources/sample`，请在项目根目录 `d:/ai/workbuddy/java-tools-learning/` 下运行，或在 IDE 中配置工作目录。

## 7. 总结

Apache Tika 是 Java 生态中处理异构文档内容提取的首选工具。它屏蔽了不同文件格式的解析细节，提供统一的文本、元数据和 MIME 检测接口。在搜索索引、知识库、文档审核、数据迁移等场景中都非常实用。

与已学过的 **Apache POI（Day 4）**、**EasyExcel（Day 11）**、**Apache PDFBox（Day 10）** 形成互补：POI/PDFBox 面向“生成和精细操作文档”，而 Tika 面向“统一解析和提取内容”。
