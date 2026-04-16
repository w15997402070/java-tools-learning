package com.example.pdfbox;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Apache PDFBox 实战演示
 *
 * 涵盖功能：
 * 1. PDF水印添加（透明度+旋转文字水印）
 * 2. 表格绘制（手动绘制线条构建表格）
 * 3. 批量PDF文本提取（模拟批量处理场景）
 * 4. 报告生成（综合运用多个PDFBox功能，生成排版精美的报告）
 */
public class PdfBoxPracticalDemo {

    private static final String OUTPUT_DIR = "target/pdf-output/";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static void main(String[] args) throws IOException {
        new File(OUTPUT_DIR).mkdirs();

        System.out.println("===== Apache PDFBox Practical Demo =====\n");

        // 演示1：添加文字水印
        demo1AddWatermark();

        // 演示2：绘制表格
        demo2DrawTable();

        // 演示3：批量提取PDF文本
        demo3BatchTextExtraction();

        // 演示4：生成业务报告
        demo4GenerateBusinessReport();

        System.out.println("\n===== Practical Demo Complete =====");
    }

    /**
     * 演示1：在PDF上添加对角线文字水印
     * 核心技巧：
     * - APPEND模式的PDPageContentStream（不覆盖原内容）
     * - PDExtendedGraphicsState 设置透明度
     * - Matrix旋转文字
     */
    private static void demo1AddWatermark() throws IOException {
        System.out.println("[Demo 1] Adding watermark to PDF...");

        // 先创建一个包含内容的源文件
        String sourcePath = OUTPUT_DIR + "practical_source.pdf";
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.TIMES_ROMAN, 12);
                cs.setLeading(18.0f);
                cs.newLineAtOffset(50, 750);
                cs.showText("This is a confidential document.");
                cs.newLine();
                cs.showText("It contains sensitive business information.");
                cs.newLine();
                cs.showText("Please handle with care.");
                cs.endText();
            }
            doc.save(sourcePath);
        }

        // 在已有PDF上叠加水印
        String outputPath = OUTPUT_DIR + "practical_watermarked.pdf";
        try (PDDocument document = PDDocument.load(new File(sourcePath))) {

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();

                // APPEND模式：在原内容之上叠加，不清除原内容
                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // 设置透明度（通过扩展图形状态）
                    PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                    // 非描边透明度（文字填充透明度）0.0=完全透明，1.0=不透明
                    graphicsState.setNonStrokingAlphaConstant(0.3f);
                    graphicsState.setAlphaSourceFlag(true);
                    cs.setGraphicsStateParameters(graphicsState);

                    // 设置水印文字颜色（浅灰色）
                    cs.setNonStrokingColor(0.7f, 0.7f, 0.7f);

                    // 在页面对角线方向写水印（旋转45度，居中）
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 60);
                    // 计算旋转后的居中位置
                    double angle = Math.toRadians(45);
                    float centerX = pageWidth / 2;
                    float centerY = pageHeight / 2;
                    cs.setTextMatrix(new org.apache.pdfbox.util.Matrix(
                            (float) Math.cos(angle),
                            (float) Math.sin(angle),
                            (float) -Math.sin(angle),
                            (float) Math.cos(angle),
                            centerX - 100, centerY));
                    cs.showText("CONFIDENTIAL");
                    cs.endText();
                }
            }

            document.save(outputPath);
            System.out.println("  Watermarked PDF saved: " + outputPath);
        }
    }

    /**
     * 演示2：手动绘制表格
     * PDFBox没有内置表格组件，需要手动用drawLine绘制线条，再填充文字
     * 这是企业报表生成中最常用的实现方式
     */
    private static void demo2DrawTable() throws IOException {
        System.out.println("\n[Demo 2] Drawing a table in PDF...");

        String outputPath = OUTPUT_DIR + "practical_table.pdf";

        // 表格数据
        String[] headers = {"ID", "Product Name", "Category", "Price", "Stock"};
        String[][] data = {
            {"001", "Laptop Pro 15", "Electronics", "$1,299.00", "50"},
            {"002", "Wireless Mouse", "Accessories", "$29.99", "200"},
            {"003", "USB-C Hub 7in1", "Accessories", "$49.99", "150"},
            {"004", "4K Monitor 27\"", "Electronics", "$599.00", "30"},
            {"005", "Mechanical Keyboard", "Accessories", "$89.99", "80"},
            {"006", "Webcam HD 1080P", "Electronics", "$79.99", "120"},
            {"007", "Laptop Stand", "Accessories", "$39.99", "200"},
        };

        // 表格布局参数
        float startX = 50;          // 表格左边距
        float startY = 700;         // 表格顶部Y坐标
        float rowHeight = 25;       // 行高
        float[] colWidths = {40, 150, 100, 80, 60};  // 每列宽度

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                // 标题
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(startX, 740);
                cs.showText("Product Inventory Report");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(startX, 720);
                cs.showText("Generated: " + new SimpleDateFormat(DATE_FORMAT).format(new Date()));
                cs.endText();

                // 绘制表头背景（浅灰色填充）
                cs.setNonStrokingColor(0.85f, 0.85f, 0.85f);
                cs.addRect(startX, startY - rowHeight, getTotalWidth(colWidths), rowHeight);
                cs.fill();
                cs.setNonStrokingColor(0, 0, 0); // 恢复黑色

                // 绘制表头文字
                float x = startX;
                for (int col = 0; col < headers.length; col++) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                    cs.newLineAtOffset(x + 5, startY - rowHeight + 8);
                    cs.showText(headers[col]);
                    cs.endText();
                    x += colWidths[col];
                }

                // 绘制数据行
                for (int row = 0; row < data.length; row++) {
                    float rowY = startY - (row + 2) * rowHeight;

                    // 奇偶行交替背景色
                    if (row % 2 == 0) {
                        cs.setNonStrokingColor(0.95f, 0.95f, 1.0f); // 浅蓝
                        cs.addRect(startX, rowY, getTotalWidth(colWidths), rowHeight);
                        cs.fill();
                        cs.setNonStrokingColor(0, 0, 0);
                    }

                    x = startX;
                    for (int col = 0; col < data[row].length; col++) {
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 9);
                        cs.newLineAtOffset(x + 5, rowY + 8);
                        cs.showText(data[row][col]);
                        cs.endText();
                        x += colWidths[col];
                    }
                }

                // 绘制表格边框（外框 + 竖分割线 + 横分割线）
                int totalRows = data.length + 1; // 含表头
                float tableBottom = startY - (totalRows + 1) * rowHeight;
                float tableRight = startX + getTotalWidth(colWidths);

                cs.setStrokingColor(0.3f, 0.3f, 0.3f);
                cs.setLineWidth(0.5f);

                // 横线
                for (int row = 0; row <= totalRows; row++) {
                    float lineY = startY - row * rowHeight;
                    cs.moveTo(startX, lineY);
                    cs.lineTo(tableRight, lineY);
                    cs.stroke();
                }

                // 竖线
                x = startX;
                for (int col = 0; col <= colWidths.length; col++) {
                    cs.moveTo(x, startY);
                    cs.lineTo(x, tableBottom + rowHeight);
                    cs.stroke();
                    if (col < colWidths.length) x += colWidths[col];
                }
            }

            document.save(outputPath);
            System.out.println("  Table PDF created: " + outputPath);
            System.out.println("  Rows: " + data.length + ", Columns: " + headers.length);
        }
    }

    /**
     * 计算所有列的总宽度
     */
    private static float getTotalWidth(float[] colWidths) {
        float total = 0;
        for (float w : colWidths) total += w;
        return total;
    }

    /**
     * 演示3：批量提取多个PDF的文本内容
     * 模拟从PDF报告目录批量提取摘要的业务场景
     */
    private static void demo3BatchTextExtraction() throws IOException {
        System.out.println("\n[Demo 3] Batch text extraction from multiple PDFs...");

        // 收集目录中所有PDF文件
        File outputDir = new File(OUTPUT_DIR);
        File[] pdfFiles = outputDir.listFiles((dir, name) -> name.endsWith(".pdf"));

        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("  No PDF files found in " + OUTPUT_DIR);
            return;
        }

        System.out.println("  Found " + pdfFiles.length + " PDF files:");
        PDFTextStripper stripper = new PDFTextStripper();

        Map<String, Integer> extractionResults = new LinkedHashMap<>();

        for (File pdfFile : pdfFiles) {
            try (PDDocument doc = PDDocument.load(pdfFile)) {
                String text = stripper.getText(doc);
                int charCount = text.trim().length();
                extractionResults.put(pdfFile.getName(), charCount);
                System.out.printf("    %-40s %d pages, %d chars%n",
                        pdfFile.getName(), doc.getNumberOfPages(), charCount);
            } catch (IOException e) {
                System.out.println("    ERROR reading " + pdfFile.getName() + ": " + e.getMessage());
            }
        }

        // 统计摘要
        System.out.println("\n  Extraction Summary:");
        System.out.println("    Total files processed: " + extractionResults.size());
        int totalChars = extractionResults.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("    Total characters extracted: " + totalChars);
        OptionalInt maxChars = extractionResults.values().stream().mapToInt(Integer::intValue).max();
        if (maxChars.isPresent()) {
            System.out.println("    Largest file chars: " + maxChars.getAsInt());
        }
    }

    /**
     * 演示4：生成结构化业务报告
     * 综合运用多个PDFBox功能，生成包含封面、目录区、内容页的报告
     */
    private static void demo4GenerateBusinessReport() throws IOException {
        System.out.println("\n[Demo 4] Generating business report PDF...");

        String outputPath = OUTPUT_DIR + "practical_business_report.pdf";

        try (PDDocument document = new PDDocument()) {

            // === 封面页 ===
            PDPage coverPage = new PDPage(PDRectangle.A4);
            document.addPage(coverPage);
            try (PDPageContentStream cs = new PDPageContentStream(document, coverPage)) {

                // 封面顶部色块（深蓝色）
                cs.setNonStrokingColor(0.1f, 0.2f, 0.5f);
                cs.addRect(0, 700, PDRectangle.A4.getWidth(), 142);
                cs.fill();

                // 报告标题（白色文字）
                cs.setNonStrokingColor(1, 1, 1);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 28);
                cs.newLineAtOffset(50, 790);
                cs.showText("Annual Technology Report");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 14);
                cs.newLineAtOffset(50, 755);
                cs.showText("Java Open Source Tools Learning Series");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 725);
                cs.showText("Generated: " + new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH).format(new Date()));
                cs.endText();

                // 报告正文区（黑色）
                cs.setNonStrokingColor(0, 0, 0);

                // Executive Summary
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(50, 650);
                cs.showText("Executive Summary");
                cs.endText();

                cs.moveTo(50, 640);
                cs.lineTo(400, 640);
                cs.setStrokingColor(0.1f, 0.2f, 0.5f);
                cs.setLineWidth(2.0f);
                cs.stroke();

                cs.beginText();
                cs.setFont(PDType1Font.TIMES_ROMAN, 11);
                cs.setLeading(18.0f);
                cs.newLineAtOffset(50, 620);
                cs.showText("This report summarizes the team's progress in learning and evaluating");
                cs.newLine();
                cs.showText("open source Java libraries. Each library was assessed for production");
                cs.newLine();
                cs.showText("readiness, performance characteristics, and integration complexity.");
                cs.endText();

                // 工具清单
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(50, 545);
                cs.showText("Tools Evaluated (Day 1 - Day 10)");
                cs.endText();

                String[] tools = {
                    "Day 01: Picocli 4.7.7 - CLI Parsing",
                    "Day 02: Gson 2.10.1 - JSON Processing",
                    "Day 03: OkHttp 4.12.0 - HTTP Client",
                    "Day 04: Apache POI 5.2.5 - Office Documents",
                    "Day 05: Guava 33.1.0 - Utility Library",
                    "Day 06: ZXing 3.5.3 - QR/Barcode",
                    "Day 07: Quartz 2.3.2 - Job Scheduling",
                    "Day 08: Hibernate Validator - Bean Validation",
                    "Day 09: Thumbnailator 0.4.20 - Image Processing",
                    "Day 10: Apache PDFBox 2.0.31 - PDF Processing"
                };

                float toolY = 520;
                for (String tool : tools) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(70, toolY);
                    cs.showText("- " + tool);
                    cs.endText();
                    toolY -= 18;
                }

                // 页脚
                cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.newLineAtOffset(50, 30);
                cs.showText("java-tools-learning | Apache PDFBox Demo | Page 1 of 1");
                cs.endText();
            }

            document.save(outputPath);
            System.out.println("  Business report created: " + outputPath);
            System.out.println("  Pages: " + document.getNumberOfPages());
        }
    }
}
