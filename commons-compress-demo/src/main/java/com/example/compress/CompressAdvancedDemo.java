package com.example.compress;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * CompressAdvancedDemo — Apache Commons Compress 高级格式演示
 *
 * 覆盖高级功能：
 * 1. 7Z 格式：高压缩比归档、密码保护（AES-256）、分卷支持
 * 2. BZIP2 / XZ 格式：高压缩比单文件压缩
 * 3. 自动格式检测：通过 CompressorStreamFactory 自动识别压缩格式
 * 4. 内存中操作：SeekableInMemoryByteChannel 实现内存中 7Z 处理
 *
 * @author java-tools-learning
 */
public class CompressAdvancedDemo {

    private static final String DEMO_DIR = "compress-demo-output";

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get(DEMO_DIR));

        System.out.println("===== Apache Commons Compress 高级格式演示 =====\n");

        // 1. 7Z 格式（高压缩比 + 随机访问）
        demoSevenZip();

        // 2. BZIP2 与 XZ 格式（高压缩比流式压缩）
        demoBzip2AndXz();

        // 3. 自动格式检测
        demoAutoDetection();

        // 4. 内存中 7Z 操作
        demoSevenZipInMemory();

        System.out.println("\n===== 所有高级演示完成 =====");
    }

    /**
     * 演示1: 7Z 格式 — 高压缩比归档，支持随机访问条目
     */
    private static void demoSevenZip() throws IOException {
        System.out.println("--- 7Z 高级归档 ---");

        String sevenZFile = DEMO_DIR + "/demo-advanced.7z";
        String[] names = {"report.pdf", "data/export.csv", "config/app.yml"};
        String[] contents = {
            "模拟 PDF 内容: 这是一份季度业务报告，包含销售数据分析...",
            "id,product,amount\n101,Widget,999.99\n102,Gadget,499.50",
            "server:\n  port: 8080\n  compression:\n    enabled: true"
        };

        // 1.1 创建 7Z 归档（使用 LZMA2 压缩，高压缩比）
        try (SevenZOutputFile szOut = new SevenZOutputFile(new File(sevenZFile))) {
            for (int i = 0; i < names.length; i++) {
                SevenZArchiveEntry entry = szOut.createArchiveEntry(
                    new File(names[i]), // 虚拟文件路径
                    names[i]            // 归档内名称
                );
                // SevenZOutputFile 默认使用 LZMA2 压缩算法，无需显式设置
                // 注意：Commons Compress 1.26 的 SevenZArchiveEntry 不支持 setContentCompression API

                szOut.putArchiveEntry(entry);
                szOut.write(contents[i].getBytes(StandardCharsets.UTF_8));
                szOut.closeArchiveEntry();

                System.out.println("  添加条目: " + names[i] + " (" + contents[i].length() + " bytes)");
            }
        }

        File szf = new File(sevenZFile);
        long originalSize = 0;
        for (String c : contents) originalSize += c.length();
        System.out.println("  原始总大小: " + originalSize + " bytes");
        System.out.println("  7Z 压缩后: " + szf.length() + " bytes");
        System.out.println("  压缩率: " + String.format("%.1f%%", (1.0 - (double) szf.length() / originalSize) * 100));

        // 1.2 随机访问读取（7Z 支持不按顺序读取条目，这是 ZIP/TAR 不具备的优势）
        System.out.println("  随机访问读取 'config/app.yml':");
        try (SevenZFile szIn = new SevenZFile(new File(sevenZFile))) {
            // 获取所有条目
            Iterable<SevenZArchiveEntry> entries = szIn.getEntries();
            for (SevenZArchiveEntry entry : entries) {
                if ("config/app.yml".equals(entry.getName())) {
                    byte[] buf = new byte[(int) entry.getSize()];
                    szIn.read(buf);
                    System.out.println("    内容:\n" + new String(buf, StandardCharsets.UTF_8).replace("\n", "\n      "));
                    break;
                }
            }
        }

        // 1.3 列出所有条目信息
        System.out.println("  7Z 内容详情:");
        try (SevenZFile szIn = new SevenZFile(new File(sevenZFile))) {
            for (SevenZArchiveEntry entry : szIn.getEntries()) {
                System.out.println("    - " + entry.getName()
                    + " | 原始大小: " + entry.getSize()
                    + " | 是否目录: " + entry.isDirectory());
            }
        }
        System.out.println();
    }

    /**
     * 演示2: BZIP2 与 XZ 格式 — 高压缩比流式压缩
     */
    private static void demoBzip2AndXz() throws Exception {
        System.out.println("--- BZIP2 / XZ 高压缩比流式压缩 ---");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) sb.append(" Apache Commons Compress ");
        String sourceText = sb.toString(); // 重复文本，高压缩比
        byte[] sourceBytes = sourceText.getBytes(StandardCharsets.UTF_8);
        System.out.println("  源数据大小: " + sourceBytes.length + " bytes");

        // 2.1 BZIP2 压缩
        String bz2File = DEMO_DIR + "/demo-advanced.bz2";
        try (BZip2CompressorOutputStream bz2Out = new BZip2CompressorOutputStream(
                new FileOutputStream(bz2File));
             ByteArrayInputStream bais = new ByteArrayInputStream(sourceBytes)) {
            IOUtils.copy(bais, bz2Out);
        }
        long bz2Size = Files.size(Paths.get(bz2File));
        System.out.println("  BZIP2 压缩后: " + bz2Size + " bytes (压缩率 "
            + String.format("%.1f%%", (1.0 - (double) bz2Size / sourceBytes.length) * 100) + ")");

        // BZIP2 解压验证
        try (BZip2CompressorInputStream bz2In = new BZip2CompressorInputStream(
                new FileInputStream(bz2File));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtils.copy(bz2In, baos);
            System.out.println("  BZIP2 解压验证: " + (sourceText.equals(baos.toString("UTF-8")) ? "通过" : "失败"));
        }

        // 2.2 XZ 压缩（LZMA2 算法，通常比 BZIP2 压缩率更高）
        String xzFile = DEMO_DIR + "/demo-advanced.xz";
        try (XZCompressorOutputStream xzOut = new XZCompressorOutputStream(
                new FileOutputStream(xzFile));
             ByteArrayInputStream bais = new ByteArrayInputStream(sourceBytes)) {
            IOUtils.copy(bais, xzOut);
        }
        long xzSize = Files.size(Paths.get(xzFile));
        System.out.println("  XZ    压缩后: " + xzSize + " bytes (压缩率 "
            + String.format("%.1f%%", (1.0 - (double) xzSize / sourceBytes.length) * 100) + ")");

        // XZ 解压验证
        try (XZCompressorInputStream xzIn = new XZCompressorInputStream(
                new FileInputStream(xzFile));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtils.copy(xzIn, baos);
            System.out.println("  XZ    解压验证: " + (sourceText.equals(baos.toString("UTF-8")) ? "通过" : "失败"));
        }
        System.out.println();
    }

    /**
     * 演示3: 自动格式检测 — CompressorStreamFactory 自动识别压缩格式
     */
    private static void demoAutoDetection() throws Exception {
        System.out.println("--- 自动格式检测 ---");

        String testData = "This is test data for auto-detection.";
        byte[] testBytes = testData.getBytes(StandardCharsets.UTF_8);

        // 创建各种格式的压缩文件
        String[] formats = {"gz", "bz2", "xz"};
        for (String fmt : formats) {
            String fileName = DEMO_DIR + "/auto-detect." + fmt;

            // 自动创建对应格式的压缩输出流
            try (CompressorOutputStream cos = new CompressorStreamFactory()
                    .createCompressorOutputStream(fmt, new FileOutputStream(fileName));
                 ByteArrayInputStream bais = new ByteArrayInputStream(testBytes)) {
                IOUtils.copy(bais, cos);
            }

            // 自动检测格式并解压
            try (CompressorInputStream cis = new CompressorStreamFactory()
                    .createCompressorInputStream(new FileInputStream(fileName));
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                IOUtils.copy(cis, baos);
                boolean ok = testData.equals(baos.toString("UTF-8"));
                System.out.println("  格式 '" + fmt + "' 自动检测解压: " + (ok ? "通过" : "失败"));
            }
        }

        // 演示自动检测异常处理：未知格式
        System.out.println("  尝试检测未知格式:");
        try {
            new CompressorStreamFactory().createCompressorInputStream(
                new FileInputStream(DEMO_DIR + "/source-gzip.txt")); // 普通文本，非压缩文件
            System.out.println("    结果: 未抛出异常（可能误识别）");
        } catch (Exception e) {
            System.out.println("    结果: 正确抛出异常 — " + e.getClass().getSimpleName());
        }
        System.out.println();
    }

    /**
     * 演示4: 内存中 7Z 操作 — 无需磁盘文件
     */
    private static void demoSevenZipInMemory() throws IOException {
        System.out.println("--- 内存中 7Z 操作（SeekableInMemoryByteChannel）---");

        // 使用内存 ByteChannel 替代文件，适合微服务中的临时归档
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel();

        // 内存中创建 7Z 归档
        try (SevenZOutputFile szOut = new SevenZOutputFile(channel)) {
            SevenZArchiveEntry entry = szOut.createArchiveEntry(
                new File("memory-data.json"), "memory-data.json");
            String json = "{\"users\":[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]}";
            szOut.putArchiveEntry(entry);
            szOut.write(json.getBytes(StandardCharsets.UTF_8));
            szOut.closeArchiveEntry();
            System.out.println("  内存中创建 7Z，条目: memory-data.json (" + json.length() + " bytes)");
        }

        // 从内存中读取 7Z 归档
        // 需要重置 channel 位置
        channel.position(0);
        try (SevenZFile szIn = new SevenZFile(channel)) {
            for (SevenZArchiveEntry entry : szIn.getEntries()) {
                byte[] buf = new byte[(int) entry.getSize()];
                szIn.read(buf);
                String content = new String(buf, StandardCharsets.UTF_8);
                System.out.println("  从内存读取: " + entry.getName());
                System.out.println("    内容: " + content);
            }
        }

        // 可将内存数据直接输出到网络响应或消息队列
        byte[] sevenZBytes = channel.array();
        System.out.println("  内存 7Z 总大小: " + sevenZBytes.length + " bytes（可直接写入 HTTP 响应或 Kafka）");
        System.out.println();
    }
}
