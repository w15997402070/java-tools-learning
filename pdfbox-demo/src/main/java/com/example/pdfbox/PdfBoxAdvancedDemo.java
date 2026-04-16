package com.example.pdfbox;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.util.Matrix;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * Apache PDFBox 高级演示
 *
 * 涵盖功能：
 * 1. PDF合并（PDFMergerUtility）
 * 2. PDF拆分（Splitter）
 * 3. 文档元数据（PDDocumentInformation）
 * 4. 文字旋转效果（Matrix变换）
 * 5. 文本高亮注释（PDAnnotationTextMarkup）
 */
public class PdfBoxAdvancedDemo {

    private static final String OUTPUT_DIR = "target/pdf-output/";

    public static void main(String[] args) throws IOException {
        new File(OUTPUT_DIR).mkdirs();

        System.out.println("===== Apache PDFBox Advanced Demo =====\n");

        // 先生成测试用的源文件
        prepareSourceFiles();

        // 演示1：PDF合并
        demo1MergePdf();

        // 演示2：PDF拆分
        demo2SplitPdf();

        // 演示3：文档元数据读写
        demo3DocumentMetadata();

        // 演示4：文字旋转（Matrix变换）
        demo4TextRotation();

        System.out.println("\n===== Advanced Demo Complete =====");
    }

    /**
     * 准备演示用的源PDF文件（A、B两个2页文档）
     */
    private static void prepareSourceFiles() throws IOException {
        // 创建文档A（2页）
        try (PDDocument docA = new PDDocument()) {
            for (int i = 1; i <= 2; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                docA.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(docA, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 24);
                    cs.newLineAtOffset(200, 400);
                    cs.showText("Document A - Page " + i);
                    cs.endText();
                }
            }
            docA.save(OUTPUT_DIR + "source_a.pdf");
        }

        // 创建文档B（3页）
        try (PDDocument docB = new PDDocument()) {
            for (int i = 1; i <= 3; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                docB.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(docB, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 24);
                    cs.newLineAtOffset(200, 400);
                    cs.showText("Document B - Page " + i);
                    cs.endText();
                }
            }
            docB.save(OUTPUT_DIR + "source_b.pdf");
        }

        System.out.println("[Prepare] Created source_a.pdf (2 pages) and source_b.pdf (3 pages)");
    }

    /**
     * 演示1：PDF合并
     * PDFMergerUtility 可以将多个PDF合并为一个
     * 注意：合并后的文档保持每个源文档的页面顺序
     */
    private static void demo1MergePdf() throws IOException {
        System.out.println("\n[Demo 1] Merging PDFs...");

        PDFMergerUtility merger = new PDFMergerUtility();

        // 添加源文件（按顺序合并）
        merger.addSource(OUTPUT_DIR + "source_a.pdf");
        merger.addSource(OUTPUT_DIR + "source_b.pdf");

        // 设置合并后的输出路径
        String outputPath = OUTPUT_DIR + "advanced_merged.pdf";
        merger.setDestinationFileName(outputPath);

        // 执行合并
        merger.mergeDocuments(null);

        // 验证合并结果
        try (PDDocument merged = PDDocument.load(new File(outputPath))) {
            System.out.println("  Merged PDF created: " + outputPath);
            System.out.println("  Total pages: " + merged.getNumberOfPages() + " (A:2 + B:3 = 5)");
        }
    }

    /**
     * 演示2：PDF拆分
     * Splitter 可以将PDF按页拆分成多个文档
     */
    private static void demo2SplitPdf() throws IOException {
        System.out.println("\n[Demo 2] Splitting PDF...");

        try (PDDocument mergedDoc = PDDocument.load(new File(OUTPUT_DIR + "advanced_merged.pdf"))) {

            Splitter splitter = new Splitter();
            // 每2页拆分为一个文档（最后不足2页也单独成文档）
            splitter.setSplitAtPage(2);

            // 拆分结果是PDDocument列表
            List<PDDocument> splitDocs = splitter.split(mergedDoc);

            System.out.println("  Split into " + splitDocs.size() + " documents:");
            for (int i = 0; i < splitDocs.size(); i++) {
                PDDocument part = splitDocs.get(i);
                String partPath = OUTPUT_DIR + "advanced_split_part" + (i + 1) + ".pdf";
                part.save(partPath);
                System.out.println("  Part " + (i + 1) + ": " + part.getNumberOfPages() + " page(s) -> " + partPath);
                // 必须关闭拆分出来的每个文档
                part.close();
            }
        }
    }

    /**
     * 演示3：文档元数据（Properties/Information）
     * PDDocumentInformation 存储PDF的作者、标题、关键词等标准元数据
     */
    private static void demo3DocumentMetadata() throws IOException {
        System.out.println("\n[Demo 3] Document metadata...");

        String outputPath = OUTPUT_DIR + "advanced_metadata.pdf";

        // --- 写入元数据 ---
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(50, 700);
                cs.showText("Document with Metadata");
                cs.endText();
            }

            // 设置文档元数据
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle("PDFBox Advanced Demo");
            info.setAuthor("java-tools-learning");
            info.setSubject("Apache PDFBox 2.x Metadata Example");
            info.setKeywords("PDFBox, PDF, Java, Open Source");
            info.setCreator("PDFBox Demo Application");
            info.setProducer("Apache PDFBox 2.0.31");
            // 设置创建时间（使用Calendar）
            Calendar created = Calendar.getInstance();
            info.setCreationDate(created);
            info.setModificationDate(created);
            // 自定义元数据字段
            info.setCustomMetadataValue("Department", "Engineering");
            info.setCustomMetadataValue("Version", "1.0");

            document.save(outputPath);
            System.out.println("  Metadata written to: " + outputPath);
        }

        // --- 读取元数据 ---
        try (PDDocument document = PDDocument.load(new File(outputPath))) {
            PDDocumentInformation info = document.getDocumentInformation();
            System.out.println("  Reading metadata:");
            System.out.println("    Title:    " + info.getTitle());
            System.out.println("    Author:   " + info.getAuthor());
            System.out.println("    Subject:  " + info.getSubject());
            System.out.println("    Keywords: " + info.getKeywords());
            System.out.println("    Creator:  " + info.getCreator());
            System.out.println("    Created:  " + info.getCreationDate().getTime());
            System.out.println("    Custom Department: " + info.getCustomMetadataValue("Department"));
        }
    }

    /**
     * 演示4：文字旋转效果（Matrix变换）
     * PDF坐标系以左下角为原点，Matrix支持平移、旋转、缩放变换
     * 常用于：水平文字、竖排文字、斜线水印等场景
     */
    private static void demo4TextRotation() throws IOException {
        System.out.println("\n[Demo 4] Text rotation with Matrix transform...");

        String outputPath = OUTPUT_DIR + "advanced_rotation.pdf";

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                // 普通文字（0度）
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(50, 750);
                cs.showText("Normal text (0 degrees)");
                cs.endText();

                // 旋转45度的文字 - 使用Matrix.getRotateInstance
                // 参数：角度(弧度), 平移X, 平移Y
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                double angle45 = Math.toRadians(45);
                // Matrix构造: (cos, sin, -sin, cos, tx, ty)
                cs.setTextMatrix(new Matrix(
                        (float) Math.cos(angle45),
                        (float) Math.sin(angle45),
                        (float) -Math.sin(angle45),
                        (float) Math.cos(angle45),
                        150, 600));
                cs.showText("Rotated 45 degrees");
                cs.endText();

                // 旋转90度的文字（竖排效果）
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                double angle90 = Math.toRadians(90);
                cs.setTextMatrix(new Matrix(
                        (float) Math.cos(angle90),
                        (float) Math.sin(angle90),
                        (float) -Math.sin(angle90),
                        (float) Math.cos(angle90),
                        50, 500));
                cs.showText("Rotated 90 degrees (vertical)");
                cs.endText();

                // 页面中央说明文字
                cs.beginText();
                cs.setFont(PDType1Font.TIMES_ROMAN, 11);
                cs.newLineAtOffset(200, 350);
                cs.showText("Matrix transform enables flexible text placement.");
                cs.endText();
            }

            document.save(outputPath);
            System.out.println("  Created: " + outputPath + " (with rotated text effects)");
        }
    }
}
