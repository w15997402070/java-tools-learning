package com.example.tika;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Tika 基础演示：文本提取、元数据读取、MIME 类型检测。
 *
 * <p>本类展示 Apache Tika 最核心的三个能力：
 * <ol>
 *   <li>使用 {@link Tika#detect} 检测文件/字节流的 MIME 类型；</li>
 *   <li>使用 {@link Tika#parseToString} 一键提取文本内容；</li>
 *   <li>使用 {@link AutoDetectParser} + {@link Metadata} 同时获取内容与元数据。</li>
 * </ol>
 */
public class TikaBasicDemo {

    public static void main(String[] args) throws Exception {
        // 1. 使用 Tika 门面类快速检测 MIME 类型
        detectMimeTypes();

        // 2. 使用 Tika 门面类快速提取文本
        extractTextQuickly();

        // 3. 使用 Parser + Metadata 提取内容与元数据
        extractWithMetadata();
    }

    /**
     * 演示：检测不同文件的 MIME 类型。
     * Tika 会根据文件扩展名、Magic Number（文件头）综合判断，结果比单纯看后缀更可靠。
     */
    private static void detectMimeTypes() throws Exception {
        System.out.println("===== 1. MIME 类型检测 =====");
        Tika tika = new Tika();

        // 通过类路径资源检测（基于文件扩展名 + 内容）
        String[] resources = { "sample/hello.txt", "sample/meeting.html", "sample/report.csv" };
        for (String res : resources) {
            try (InputStream is = getResource(res)) {
                String mime = tika.detect(is);
                System.out.printf("资源: %-25s -> MIME: %s%n", res, mime);
            }
        }

        // 通过字节数组检测（常用于上传文件校验）
        byte[] htmlBytes = "<html><body>Hello Tika</body></html>".getBytes(StandardCharsets.UTF_8);
        System.out.println("字节流检测: " + tika.detect(htmlBytes));
        System.out.println();
    }

    /**
     * 演示：使用 Tika 门面方法快速提取纯文本。
     * 适合“只要文本，不关心元数据”的场景。
     */
    private static void extractTextQuickly() throws Exception {
        System.out.println("===== 2. 快速文本提取 =====");
        Tika tika = new Tika();

        for (String res : new String[]{ "sample/hello.txt", "sample/meeting.html", "sample/report.csv" }) {
            try (InputStream is = getResource(res)) {
                String text = tika.parseToString(is);
                System.out.printf("--- %s ---%n%s%n%n", res, text.trim());
            }
        }
    }

    /**
     * 演示：使用底层 Parser 提取文本 + 元数据。
     * 可获取作者、标题、创建时间、页面数等结构化信息。
     */
    private static void extractWithMetadata() throws Exception {
        System.out.println("===== 3. 文本 + 元数据提取 =====");

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        try (InputStream is = getResource("sample/meeting.html")) {
            parser.parse(is, handler, metadata, context);

            System.out.println("提取文本:");
            System.out.println(handler.toString().trim());

            System.out.println("\n元数据:");
            for (String name : metadata.names()) {
                System.out.printf("  %s = %s%n", name, metadata.get(name));
            }
        }
        System.out.println();
    }

    private static InputStream getResource(String path) {
        InputStream is = TikaBasicDemo.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("类路径资源不存在: " + path);
        }
        return is;
    }
}
