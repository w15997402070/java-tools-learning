package com.example.tika;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Tika 进阶演示：复杂文档解析、结构化输出、解析器配置、语言检测。
 *
 * <p>涵盖：
 * <ul>
 *   <li>在内存中生成 Office/PDF 测试文件并交给 Tika 解析；</li>
 *   <li>通过 {@link ParseContext} 控制解析行为（如 PDF 是否 OCR、电子表格是否提取公式）；</li>
 *   <li>使用 {@link ToXMLContentHandler} 输出带标签的 XHTML；</li>
 *   <li>使用 Tika 语言检测器识别文本语言。</li>
 * </ul>
 */
public class TikaAdvancedDemo {

    public static void main(String[] args) throws Exception {
        // 1. 生成并解析 Excel
        parseGeneratedExcel();

        // 2. 生成并解析 PDF
        parseGeneratedPdf();

        // 3. 输出结构化 XHTML
        extractAsXhtml();

        // 4. 语言检测
        detectLanguage();
    }

    /**
     * 演示：在内存中生成一个 XSSF Excel，再用 Tika 提取其内容与元数据。
     * Tika-parsers-standard-package 已传递依赖 Apache POI。
     */
    private static void parseGeneratedExcel() throws Exception {
        System.out.println("===== 1. 生成并解析 Excel =====");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("订单统计");
            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("日期");
            header.createCell(1).setCellValue("订单量");
            header.createCell(2).setCellValue("销售额");

            XSSFRow row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026-07-31");
            row.createCell(1).setCellValue(256);
            XSSFCell amountCell = row.createCell(2);
            amountCell.setCellValue(76800.00);
            amountCell.getCellStyle(); // 占位，保持样式对象存在

            wb.write(out);
        }

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();

        try (InputStream is = new ByteArrayInputStream(out.toByteArray())) {
            parser.parse(is, handler, metadata);
            System.out.println("提取文本:");
            System.out.println(handler.toString().trim());
            System.out.println("\n元数据:");
            System.out.println("  content-type = " + metadata.get(Metadata.CONTENT_TYPE));
            System.out.println("  application-name = " + metadata.get("Application-Name"));
        }
        System.out.println();
    }

    /**
     * 演示：在内存中生成一个 PDF，再用 Tika 提取其内容。
     * 通过 {@link PDFParserConfig} 设置解析策略。
     */
    private static void parseGeneratedPdf() throws Exception {
        System.out.println("===== 2. 生成并解析 PDF =====");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.newLineAtOffset(50, 700);
                stream.showText("Apache Tika PDF Parsing Demo");
                stream.endText();

                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 670);
                stream.showText("Page 1: Introduction to content extraction.");
                stream.endText();
            }

            document.save(out);
        }

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        // 配置：按顺序提取文本（对复杂排版更稳定）
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setSortByPosition(true);
        context.set(PDFParserConfig.class, pdfConfig);

        try (InputStream is = new ByteArrayInputStream(out.toByteArray())) {
            parser.parse(is, handler, metadata, context);
            System.out.println("提取文本:");
            System.out.println(handler.toString().trim());
            System.out.println("\n元数据:");
            System.out.println("  content-type = " + metadata.get(Metadata.CONTENT_TYPE));
            System.out.println("  x-parsed-by = " + metadata.get("X-Parsed-By"));
            System.out.println("  page-count = " + metadata.get("xmpTPg:NPages"));
        }
        System.out.println();
    }

    /**
     * 演示：使用 ToXMLContentHandler 输出结构化 XHTML，便于后续按标签做进一步处理。
     */
    private static void extractAsXhtml() throws Exception {
        System.out.println("===== 3. 结构化 XHTML 输出 =====");

        String html = "<html><body><h1>标题</h1><p>第一段内容。</p><p>第二段内容。</p></body></html>";
        AutoDetectParser parser = new AutoDetectParser();
        ToXMLContentHandler handler = new ToXMLContentHandler();
        Metadata metadata = new Metadata();

        try (InputStream is = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8))) {
            parser.parse(is, handler, metadata);
            System.out.println(handler.toString());
        }
        System.out.println();
    }

    /**
     * 演示：使用 Tika 内置语言检测器识别文本语言。
     * 适合对未知文档进行语种分类。
     */
    private static void detectLanguage() throws Exception {
        System.out.println("===== 4. 语言检测 =====");

        String[] samples = {
            "Apache Tika is a content analysis toolkit.",
            "Apache Tika 是一个开源的内容分析工具包。",
            "Apache Tika est une boîte à outils d'analyse de contenu."
        };

        LanguageDetector detector = LanguageDetector.getDefaultLanguageDetector().loadModels();
        for (String text : samples) {
            LanguageResult result = detector.detect(text);
            System.out.printf("文本: %-45s -> 语言: %s (置信度: %.2f)%n",
                    text.substring(0, Math.min(45, text.length())),
                    result.getLanguage(),
                    result.getRawScore());
        }
    }
}
