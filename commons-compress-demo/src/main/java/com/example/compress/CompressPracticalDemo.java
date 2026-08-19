package com.example.compress;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * CompressPracticalDemo — Apache Commons Compress 实战场景演示
 *
 * 覆盖实战功能：
 * 1. 目录递归归档：将整个目录（含子目录）打包为 ZIP，保留目录结构
 * 2. 批量解压与格式转换：自动识别格式并解压到指定目录
 * 3. 压缩包内容预览：不解压直接列出 ZIP 内所有条目及元数据
 * 4. 大文件流式处理：使用流式 API 避免内存溢出，适合 GB 级文件
 * 5. 压缩包完整性校验：检测损坏条目、CRC 校验
 *
 * @author java-tools-learning
 */
public class CompressPracticalDemo {

    private static final String DEMO_DIR = "compress-demo-output";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get(DEMO_DIR));

        System.out.println("===== Apache Commons Compress 实战场景演示 =====\n");

        // 1. 目录递归归档
        demoDirectoryArchiving();

        // 2. 批量解压与格式自动识别
        demoBatchExtract();

        // 3. 压缩包内容预览
        demoArchivePreview();

        // 4. 大文件流式处理
        demoStreamingLargeFile();

        // 5. 压缩包完整性校验
        demoIntegrityCheck();

        System.out.println("\n===== 所有实战演示完成 =====");
    }

    /**
     * 实战1: 目录递归归档 — 将整个目录树打包为 ZIP
     */
    private static void demoDirectoryArchiving() throws IOException {
        System.out.println("--- 实战: 目录递归归档 ---");

        // 创建演示目录结构
        String sourceDir = DEMO_DIR + "/archive-source";
        createSampleDirectoryTree(sourceDir);

        String zipFile = DEMO_DIR + "/archive-directory.zip";

        // 递归归档
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(new FileOutputStream(zipFile))) {
            zos.setEncoding("UTF-8");
            zos.setLevel(6); // 压缩级别

            Path sourcePath = Paths.get(sourceDir);
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 计算相对路径作为 ZIP 内名称
                    String zipEntryName = sourcePath.relativize(file).toString().replace("\\", "/");

                    ZipArchiveEntry entry = new ZipArchiveEntry(file.toFile(), zipEntryName);
                    zos.putArchiveEntry(entry);

                    // 流式写入文件内容（适合大文件）
                    try (FileInputStream fis = new FileInputStream(file.toFile())) {
                        IOUtils.copy(fis, zos);
                    }
                    zos.closeArchiveEntry();

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // 为目录创建条目（保留空目录）
                    if (!dir.equals(sourcePath)) {
                        String dirName = sourcePath.relativize(dir).toString().replace("\\", "/") + "/";
                        ZipArchiveEntry entry = new ZipArchiveEntry(dirName);
                        zos.putArchiveEntry(entry);
                        zos.closeArchiveEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        // 统计归档结果
        long fileCount = Files.walk(Paths.get(sourceDir))
            .filter(Files::isRegularFile).count();
        long dirCount = Files.walk(Paths.get(sourceDir))
            .filter(Files::isDirectory).count() - 1; // 减去根目录
        long zipSize = Files.size(Paths.get(zipFile));

        System.out.println("  源目录: " + sourceDir);
        System.out.println("  文件数: " + fileCount + ", 子目录数: " + dirCount);
        System.out.println("  生成 ZIP: " + zipFile + " (" + zipSize + " bytes)");
        System.out.println();
    }

    /**
     * 实战2: 批量解压 — 自动识别 ZIP/TAR/7Z 等格式并解压
     */
    private static void demoBatchExtract() throws Exception {
        System.out.println("--- 实战: 批量解压与格式自动识别 ---");

        String[] archiveFiles = {
            DEMO_DIR + "/demo-basic.zip",
            DEMO_DIR + "/demo-basic.tar"
        };

        for (String archivePath : archiveFiles) {
            if (!Files.exists(Paths.get(archivePath))) {
                System.out.println("  跳过（文件不存在）: " + archivePath);
                continue;
            }

            String extractDir = DEMO_DIR + "/extracted-" + Paths.get(archivePath).getFileName().toString();
            Files.createDirectories(Paths.get(extractDir));

            // 使用 ArchiveStreamFactory 自动检测归档格式
            try (ArchiveInputStream ais = new ArchiveStreamFactory()
                    .createArchiveInputStream(new BufferedInputStream(new FileInputStream(archivePath)))) {

                ArchiveEntry entry;
                int count = 0;
                while ((entry = ais.getNextEntry()) != null) {
                    File outFile = new File(extractDir, entry.getName());
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            IOUtils.copy(ais, fos);
                        }
                    }
                    count++;
                }
                System.out.println("  解压: " + Paths.get(archivePath).getFileName()
                    + " -> " + extractDir + " (" + count + " 个条目)");
            }
        }
        System.out.println();
    }

    /**
     * 实战3: 压缩包内容预览 — 不解压直接查看元数据
     */
    private static void demoArchivePreview() throws Exception {
        System.out.println("--- 实战: 压缩包内容预览 ---");

        String zipFile = DEMO_DIR + "/archive-directory.zip";
        if (!Files.exists(Paths.get(zipFile))) {
            System.out.println("  跳过（文件不存在）: " + zipFile);
            System.out.println();
            return;
        }

        System.out.println("  ZIP 内容预览（不读取内容，仅元数据）:");
        System.out.println(String.format("    %-30s %10s %12s %s",
            "名称", "原始大小", "压缩后", "修改时间"));
        System.out.println("    " + repeatChar('-', 80));

        // 使用 java.util.zip.ZipFile 进行随机访问预览（比流式更快）
        try (ZipFile zf = new ZipFile(zipFile, StandardCharsets.UTF_8)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            long totalOriginal = 0;
            long totalCompressed = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String type = entry.isDirectory() ? "[D] " : "[F] ";
                String name = entry.getName();
                if (name.length() > 28) name = "..." + name.substring(name.length() - 25);

                String modified = entry.getTime() > 0
                    ? DATE_FMT.format(new Date(entry.getTime()))
                    : "N/A";

                System.out.println(String.format("    %s%-28s %10d %12d %s",
                    type, name, entry.getSize(), entry.getCompressedSize(), modified));

                totalOriginal += entry.getSize();
                totalCompressed += entry.getCompressedSize();
            }

            System.out.println("    " + repeatChar('-', 80));
            System.out.println(String.format("    %-30s %10d %12d",
                "总计", totalOriginal, totalCompressed));
            if (totalOriginal > 0) {
                System.out.println(String.format("    压缩率: %.1f%%",
                    (1.0 - (double) totalCompressed / totalOriginal) * 100));
            }
        }
        System.out.println();
    }

    /**
     * 实战4: 大文件流式处理 — 避免将整个文件加载到内存
     */
    private static void demoStreamingLargeFile() throws Exception {
        System.out.println("--- 实战: 大文件流式处理 ---");

        // 模拟一个大文件（这里用 5MB 演示，实际可处理 GB 级）
        String largeFile = DEMO_DIR + "/large-file.bin";
        int sizeMB = 5;
        byte[] chunk = new byte[1024 * 1024]; // 1MB 缓冲区
        System.out.println("  生成模拟大文件: " + sizeMB + " MB");

        try (FileOutputStream fos = new FileOutputStream(largeFile)) {
            for (int i = 0; i < sizeMB; i++) {
                // 填充可识别的数据模式
                for (int j = 0; j < chunk.length; j++) {
                    chunk[j] = (byte) ((i * 7 + j * 13) % 256);
                }
                fos.write(chunk);
            }
        }
        System.out.println("  源文件大小: " + Files.size(Paths.get(largeFile)) + " bytes");

        // 流式压缩：逐块读取、压缩、写入，内存占用固定（1MB 缓冲区）
        String gzippedFile = DEMO_DIR + "/large-file.bin.gz";
        long startTime = System.currentTimeMillis();

        CompressorStreamFactory factory = new CompressorStreamFactory();
        try (OutputStream gzos = factory.createCompressorOutputStream(
                 CompressorStreamFactory.GZIP, new FileOutputStream(gzippedFile));
             FileInputStream fis = new FileInputStream(largeFile)) {

            // IOUtils.copy 内部使用 8KB 缓冲区，对于大文件应自定义缓冲区
            byte[] buffer = new byte[64 * 1024]; // 64KB 缓冲区
            int len;
            long copied = 0;
            while ((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
                copied += len;
                // 每 1MB 打印进度
                if (copied % (1024 * 1024) == 0) {
                    System.out.println("  压缩进度: " + (copied / (1024 * 1024)) + " MB / " + sizeMB + " MB");
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        long gzSize = Files.size(Paths.get(gzippedFile));
        System.out.println("  压缩完成: " + gzSize + " bytes, 耗时 " + elapsed + " ms");
        System.out.println("  内存占用: 恒定 ~64KB 缓冲区（与文件大小无关）");

        // 流式解压验证
        String decompressedFile = DEMO_DIR + "/large-file-decompressed.bin";
        CompressorStreamFactory factory2 = new CompressorStreamFactory();
        try (InputStream gzis = factory2.createCompressorInputStream(
                 CompressorStreamFactory.GZIP, new FileInputStream(gzippedFile));
             FileOutputStream fos = new FileOutputStream(decompressedFile)) {
            IOUtils.copy(gzis, fos);
        }

        // 验证完整性（比较文件大小）
        long originalSize = Files.size(Paths.get(largeFile));
        long decompressedSize = Files.size(Paths.get(decompressedFile));
        System.out.println("  解压验证: " + (originalSize == decompressedSize ? "通过" : "失败")
            + " (" + decompressedSize + " bytes)");
        System.out.println();
    }

    /**
     * 实战5: 压缩包完整性校验 — 检测损坏或 CRC 错误
     */
    private static void demoIntegrityCheck() throws IOException {
        System.out.println("--- 实战: 压缩包完整性校验 ---");

        String zipFile = DEMO_DIR + "/demo-basic.zip";
        if (!Files.exists(Paths.get(zipFile))) {
            System.out.println("  跳过（文件不存在）");
            System.out.println();
            return;
        }

        System.out.println("  校验 ZIP 文件: " + zipFile);

        boolean hasError = false;
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new FileInputStream(zipFile), "UTF-8", true, true)) { // 启用 CRC 校验

            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                // 读取完整内容以触发 CRC 校验
                byte[] buf = new byte[4096];
                long totalRead = 0;
                int len;
                while ((len = zis.read(buf)) != -1) {
                    totalRead += len;
                }

                // Commons Compress 的 ZipArchiveInputStream 会在读取完成后校验 CRC
                // 如果 CRC 不匹配，会抛出 IOException
                System.out.println("    [OK] " + entry.getName()
                    + " | 读取 " + totalRead + " bytes | CRC: " + Long.toHexString(entry.getCrc()));
            }
        } catch (IOException e) {
            hasError = true;
            System.out.println("    [ERROR] CRC 校验失败或文件损坏: " + e.getMessage());
        }

        if (!hasError) {
            System.out.println("  校验结果: 所有条目 CRC 校验通过");
        }

        // 演示检测损坏文件：修改 ZIP 中一个字节
        System.out.println("  模拟文件损坏检测:");
        byte[] zipBytes = Files.readAllBytes(Paths.get(zipFile));
        if (zipBytes.length > 100) {
            zipBytes[50] = (byte) (zipBytes[50] ^ 0xFF); // 翻转某个字节
            Files.write(Paths.get(DEMO_DIR + "/corrupted.zip"), zipBytes);

            try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                    new FileInputStream(DEMO_DIR + "/corrupted.zip"), "UTF-8", true, true)) {
                ZipArchiveEntry entry;
                while ((entry = zis.getNextZipEntry()) != null) {
                    byte[] buf = new byte[4096];
                    while (zis.read(buf) != -1) {
                        // 读取全部内容
                    }
                }
                System.out.println("    损坏检测: 未捕获异常（可能未触及 CRC 区域）");
            } catch (IOException e) {
                System.out.println("    损坏检测: 正确捕获异常 — " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
            }
        }
        System.out.println();
    }

    /**
     * 辅助方法：重复字符 n 次（Java 8 兼容，替代 String.repeat）
     */
    private static String repeatChar(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    /**
     * 辅助方法：创建演示目录树
     */
    private static void createSampleDirectoryTree(String rootDir) throws IOException {
        // 创建目录结构
        Files.createDirectories(Paths.get(rootDir, "src", "main", "java", "com", "example"));
        Files.createDirectories(Paths.get(rootDir, "src", "main", "resources"));
        Files.createDirectories(Paths.get(rootDir, "src", "test", "java"));
        Files.createDirectories(Paths.get(rootDir, "docs"));

        // 创建文件
        Files.write(Paths.get(rootDir, "pom.xml"),
            "<project><modelVersion>4.0.0</modelVersion></project>".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(rootDir, "src", "main", "java", "com", "example", "App.java"),
            "package com.example; public class App {}".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(rootDir, "src", "main", "resources", "application.yml"),
            "server:\n  port: 8080".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(rootDir, "src", "test", "java", "AppTest.java"),
            "public class AppTest {}".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(rootDir, "docs", "README.md"),
            "# Project README\nThis is a sample project.".getBytes(StandardCharsets.UTF_8));
    }
}
