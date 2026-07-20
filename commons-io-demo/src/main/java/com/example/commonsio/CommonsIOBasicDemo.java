package com.example.commonsio;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Apache Commons IO — 基础演示
 *
 * <p>覆盖最常用的 IO 操作：
 * <ul>
 *   <li>FileUtils：文件读写、复制、移动、删除</li>
 *   <li>FilenameUtils：跨平台路径/扩展名处理</li>
 *   <li>IOUtils：流/Reader/Writer 工具方法</li>
 *   <li>LineIterator：按行迭代大文件（低内存）</li>
 * </ul>
 */
public class CommonsIOBasicDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("========== Apache Commons IO 基础演示 ==========\n");

        demoFileUtils();
        demoFilenameUtils();
        demoIOUtils();
        demoLineIterator();
    }

    // -----------------------------------------------------------------------
    // 1. FileUtils —— 文件读写与操作
    // -----------------------------------------------------------------------
    private static void demoFileUtils() throws IOException {
        System.out.println("【1. FileUtils — 文件读写与操作】");

        // 创建临时目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "commons-io-demo");
        FileUtils.forceMkdir(tempDir);                        // 递归创建目录，目录已存在不报错
        System.out.println("临时目录已创建: " + tempDir.getAbsolutePath());

        // 写文件（UTF-8）
        File textFile = new File(tempDir, "hello.txt");
        String content = "Hello, Apache Commons IO!\n第二行中文内容\n第三行结束";
        FileUtils.writeStringToFile(textFile, content, StandardCharsets.UTF_8);
        System.out.println("写入文件: " + textFile.getName() + "，大小: " + textFile.length() + " bytes");

        // 读整个文件为 String
        String readBack = FileUtils.readFileToString(textFile, StandardCharsets.UTF_8);
        System.out.println("读回内容（前50字符）: " + readBack.substring(0, Math.min(50, readBack.length())));

        // 读文件为行列表
        List<String> lines = FileUtils.readLines(textFile, StandardCharsets.UTF_8);
        System.out.println("共 " + lines.size() + " 行，第一行: " + lines.get(0));

        // 追加写入（append=true）
        FileUtils.writeStringToFile(textFile, "\n追加第四行", StandardCharsets.UTF_8, true);
        System.out.println("追加后行数: " + FileUtils.readLines(textFile, StandardCharsets.UTF_8).size());

        // 复制文件
        File copiedFile = new File(tempDir, "hello_copy.txt");
        FileUtils.copyFile(textFile, copiedFile);
        System.out.println("文件复制成功，副本大小: " + copiedFile.length() + " bytes");

        // 比较两文件内容是否相同
        boolean sameContent = FileUtils.contentEquals(textFile, copiedFile);
        System.out.println("两文件内容相同: " + sameContent);

        // 文件大小格式化显示
        System.out.println("文件大小(人类可读): " + FileUtils.byteCountToDisplaySize(textFile.length()));

        // 复制整个目录
        File subDir = new File(tempDir, "subdir");
        FileUtils.forceMkdir(subDir);
        FileUtils.writeStringToFile(new File(subDir, "a.txt"), "子目录文件", StandardCharsets.UTF_8);
        File destDir = new File(tempDir, "subdir_copy");
        FileUtils.copyDirectory(subDir, destDir);
        System.out.println("目录复制成功，目标文件数: " + destDir.listFiles().length);

        // 移动文件
        File movedFile = new File(tempDir, "hello_moved.txt");
        FileUtils.moveFile(copiedFile, movedFile);
        System.out.println("文件移动成功，原文件存在: " + copiedFile.exists()
                + "，新文件存在: " + movedFile.exists());

        // 清理（删除目录树）
        FileUtils.deleteDirectory(tempDir);
        System.out.println("临时目录已删除: " + !tempDir.exists());
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 2. FilenameUtils —— 路径与文件名工具
    // -----------------------------------------------------------------------
    private static void demoFilenameUtils() {
        System.out.println("【2. FilenameUtils — 路径与文件名】");

        String path = "/home/user/documents/report.final.pdf";
        String winPath = "C:\\Users\\Admin\\Documents\\report.final.pdf";

        // 提取各部分
        System.out.println("原始路径          : " + path);
        System.out.println("文件名(含扩展名)   : " + FilenameUtils.getName(path));
        System.out.println("文件名(不含扩展名) : " + FilenameUtils.getBaseName(path));
        System.out.println("扩展名             : " + FilenameUtils.getExtension(path));
        System.out.println("目录路径           : " + FilenameUtils.getFullPath(path));
        System.out.println("前缀（驱动器/根）  : " + FilenameUtils.getPrefix(path));

        // 跨平台分隔符规范化
        String normalized = FilenameUtils.normalize(winPath);
        System.out.println("Windows路径规范化  : " + normalized);

        // 拼接路径（自动处理多余分隔符）
        String joined = FilenameUtils.concat("/home/user/", "docs/file.txt");
        System.out.println("路径拼接           : " + joined);

        // 通配符匹配（支持 * 和 ?）
        System.out.println("*.pdf 匹配 report.pdf : " + FilenameUtils.wildcardMatch("report.pdf", "*.pdf"));
        System.out.println("*.txt 匹配 report.pdf : " + FilenameUtils.wildcardMatch("report.pdf", "*.txt"));
        System.out.println("rep??t.pdf 匹配      : " + FilenameUtils.wildcardMatch("report.pdf", "rep??t.pdf"));

        // 修改扩展名
        String renamed = FilenameUtils.removeExtension(path) + ".docx";
        System.out.println("修改扩展名后       : " + renamed);

        // 判断路径前缀相同（Windows/Unix 兼容）
        boolean eq = FilenameUtils.equalsNormalizedOnSystem(
                "C:/Users/Admin/file.txt", "C:\\Users\\Admin\\file.txt");
        System.out.println("路径等效（Win）    : " + eq);
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 3. IOUtils —— 流操作工具
    // -----------------------------------------------------------------------
    private static void demoIOUtils() throws IOException {
        System.out.println("【3. IOUtils — 流操作工具】");

        // InputStream → String
        String originalText = "Apache Commons IO makes I/O operations simpler!";
        try (InputStream in = IOUtils.toInputStream(originalText, StandardCharsets.UTF_8)) {
            String result = IOUtils.toString(in, StandardCharsets.UTF_8);
            System.out.println("流转字符串: " + result);
        }

        // 字节数组 → InputStream
        byte[] bytes = "Hello Bytes!".getBytes(StandardCharsets.UTF_8);
        try (InputStream in = IOUtils.toInputStream(new String(bytes, StandardCharsets.UTF_8),
                StandardCharsets.UTF_8)) {
            byte[] readBytes = IOUtils.toByteArray(in);
            System.out.println("读取字节数: " + readBytes.length);
        }

        // Reader → 行列表
        String multiLine = "line 1\nline 2\nline 3\n";
        try (StringReader reader = new StringReader(multiLine)) {
            List<String> lines = IOUtils.readLines(reader);
            System.out.println("从Reader读取行数: " + lines.size() + "，内容: " + lines);
        }

        // 计算内容大小
        try (InputStream in = IOUtils.toInputStream("count me", StandardCharsets.UTF_8)) {
            long count = IOUtils.consume(in);     // 消费流并返回字节数
            System.out.println("流字节数(consume): " + count);
        }

        // 静态常量
        System.out.println("系统行分隔符     : " + IOUtils.LINE_SEPARATOR.replace("\r", "\\r").replace("\n", "\\n"));
        System.out.println("目录分隔符       : " + IOUtils.DIR_SEPARATOR);
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 4. LineIterator —— 大文件按行迭代
    // -----------------------------------------------------------------------
    private static void demoLineIterator() throws IOException {
        System.out.println("【4. LineIterator — 大文件按行迭代（低内存）】");

        // 准备测试文件
        File tempFile = new File(System.getProperty("java.io.tmpdir"), "big-file.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            sb.append("第").append(i).append("行: 这是一段模拟大文件内容，包含一些中文和数字 ").append(i * 100).append("\n");
        }
        FileUtils.writeStringToFile(tempFile, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("写入测试文件: " + tempFile.getName() + "，大小: "
                + FileUtils.byteCountToDisplaySize(tempFile.length()));

        // LineIterator 按行迭代，不会一次性加载全部内容到内存
        int lineCount = 0;
        LineIterator it = FileUtils.lineIterator(tempFile, "UTF-8");
        try {
            while (it.hasNext()) {
                String line = it.nextLine();
                lineCount++;
                if (lineCount <= 3) {
                    System.out.println("  行" + lineCount + ": " + line);
                }
            }
        } finally {
            LineIterator.closeQuietly(it);   // 必须关闭，释放底层文件句柄
        }
        System.out.println("  ... 共迭代 " + lineCount + " 行");

        // 清理
        FileUtils.forceDeleteOnExit(tempFile);
        System.out.println("LineIterator 演示完成\n");
    }
}
