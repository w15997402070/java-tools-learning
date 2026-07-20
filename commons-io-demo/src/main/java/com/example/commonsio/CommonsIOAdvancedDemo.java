package com.example.commonsio;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.comparator.SizeFileComparator;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Apache Commons IO — 进阶演示
 *
 * <p>覆盖以下进阶特性：
 * <ul>
 *   <li>FileFilter：灵活的文件过滤组合（后缀/通配符/时间/组合过滤器）</li>
 *   <li>Comparator：文件排序（按大小、修改时间）</li>
 *   <li>ReversedLinesFileReader：反向逐行读取文件（日志尾部读取）</li>
 *   <li>BoundedInputStream：限制读取字节数（防止内存溢出）</li>
 *   <li>TeeOutputStream：流复用（同时写多个输出流）</li>
 *   <li>FileAlterationMonitor：文件变更监控</li>
 * </ul>
 */
public class CommonsIOAdvancedDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== Apache Commons IO 进阶演示 ==========\n");

        // 准备临时工作目录
        File workDir = new File(System.getProperty("java.io.tmpdir"), "commons-io-advanced");
        FileUtils.forceMkdir(workDir);
        prepareTestFiles(workDir);

        demoFileFilter(workDir);
        demoFileComparator(workDir);
        demoReversedLinesReader(workDir);
        demoBoundedInputStream();
        demoTeeOutputStream();
        demoFileAlterationMonitor(workDir);

        // 清理
        FileUtils.deleteDirectory(workDir);
    }

    // -----------------------------------------------------------------------
    // 辅助：准备测试文件
    // -----------------------------------------------------------------------
    private static void prepareTestFiles(File dir) throws IOException {
        FileUtils.writeStringToFile(new File(dir, "report.pdf"),  "fake pdf", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(dir, "data.csv"),    "a,b,c\n1,2,3", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(dir, "config.xml"),  "<config/>", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(dir, "app.log"),     "ERROR: something\nINFO: ok\nWARN: slow", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(dir, "readme.txt"),  "read me", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(dir, "archive.zip"), "fake zip content bigger", StandardCharsets.UTF_8);
        // 子目录
        File sub = new File(dir, "subdir");
        FileUtils.forceMkdir(sub);
        FileUtils.writeStringToFile(new File(sub, "inner.txt"), "inner file", StandardCharsets.UTF_8);
        System.out.println("测试文件准备完毕: " + dir.getAbsolutePath() + "\n");
    }

    // -----------------------------------------------------------------------
    // 1. FileFilter —— 组合文件过滤
    // -----------------------------------------------------------------------
    private static void demoFileFilter(File dir) throws IOException {
        System.out.println("【1. FileFilter — 文件过滤】");

        // 按后缀过滤：只要 .txt 和 .log
        SuffixFileFilter textOrLog = new SuffixFileFilter(new String[]{".txt", ".log"});
        Collection<File> textAndLogFiles = FileUtils.listFiles(dir, textOrLog, null);
        System.out.println("后缀过滤(.txt/.log): " + fileNames(textAndLogFiles));

        // 通配符过滤：匹配 *.csv
        WildcardFileFilter csvFilter = new WildcardFileFilter("*.csv");
        Collection<File> csvFiles = FileUtils.listFiles(dir, csvFilter, null);
        System.out.println("通配符过滤(*.csv)  : " + fileNames(csvFiles));

        // 递归遍历：包含子目录中的所有 .txt
        Collection<File> allTxt = FileUtils.listFiles(dir, new SuffixFileFilter(".txt"), TrueFileFilter.INSTANCE);
        System.out.println("递归.txt文件       : " + fileNames(allTxt));

        // 只列目录
        File[] subDirs = dir.listFiles((java.io.FileFilter) DirectoryFileFilter.DIRECTORY);
        System.out.println("子目录列表         : " + (subDirs != null ? Arrays.toString(subDirs) : "[]"));

        // 组合过滤：后缀 .txt 并且最近 1 小时内修改过（AgeFileFilter cutoff=1h ago）
        Date oneHourAgo = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
        AgeFileFilter recentFilter = new AgeFileFilter(oneHourAgo, false); // false = newer than cutoff
        AndFileFilter combined = new AndFileFilter(new SuffixFileFilter(".txt"), recentFilter);
        Collection<File> recentTxt = FileUtils.listFiles(dir, combined, null);
        System.out.println("近1小时内的.txt    : " + fileNames(recentTxt));
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 2. Comparator —— 文件排序
    // -----------------------------------------------------------------------
    private static void demoFileComparator(File dir) {
        System.out.println("【2. Comparator — 文件排序】");

        File[] files = dir.listFiles(File::isFile);
        if (files == null) return;

        // 按文件大小升序
        Arrays.sort(files, SizeFileComparator.SIZE_COMPARATOR);
        System.out.println("按大小升序: ");
        for (File f : files) {
            System.out.printf("  %-20s  %d bytes%n", f.getName(), f.length());
        }

        // 按修改时间降序（最新的在前）
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        System.out.println("按修改时间降序: ");
        for (File f : files) {
            System.out.printf("  %-20s  %tF %<tT%n", f.getName(), f.lastModified());
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 3. ReversedLinesFileReader —— 反向读取文件（适合读日志尾部）
    // -----------------------------------------------------------------------
    private static void demoReversedLinesReader(File dir) throws IOException {
        System.out.println("【3. ReversedLinesFileReader — 反向逐行读取】");

        // 准备多行日志文件
        File logFile = new File(dir, "app.log");
        FileUtils.writeStringToFile(logFile,
                "2026-05-29 08:00:00 INFO  服务启动\n" +
                "2026-05-29 08:01:05 DEBUG 加载配置完成\n" +
                "2026-05-29 08:02:10 WARN  连接池接近上限\n" +
                "2026-05-29 08:03:15 ERROR 数据库连接超时\n" +
                "2026-05-29 08:04:20 INFO  自动重连成功\n",
                StandardCharsets.UTF_8);

        // 从文件末尾往前读取最后 3 行（日志监控常用模式）
        System.out.println("日志文件最后 3 行（反向读取）:");
        try (ReversedLinesFileReader reader =
                     new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
            for (int i = 0; i < 3; i++) {
                String line = reader.readLine();
                if (line == null) break;
                System.out.println("  " + line);
            }
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 4. BoundedInputStream —— 限制读取字节数
    // -----------------------------------------------------------------------
    private static void demoBoundedInputStream() throws IOException {
        System.out.println("【4. BoundedInputStream — 限制最大读取字节数】");

        String bigContent = "0123456789ABCDEFGHIJ".repeat(10); // 200 字符
        byte[] rawBytes = bigContent.getBytes(StandardCharsets.UTF_8);

        // 只读前 50 字节，防止恶意/超大输入导致 OOM
        try (BoundedInputStream bounded = new BoundedInputStream(
                IOUtils.toInputStream(bigContent, StandardCharsets.UTF_8), 50)) {
            byte[] limited = IOUtils.toByteArray(bounded);
            System.out.println("原始长度: " + rawBytes.length + " bytes");
            System.out.println("读取限制: 50 bytes，实际读取: " + limited.length + " bytes");
            System.out.println("内容前50: " + new String(limited, StandardCharsets.UTF_8));
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 5. TeeOutputStream —— 流复用（同时写多个输出流）
    // -----------------------------------------------------------------------
    private static void demoTeeOutputStream() throws IOException {
        System.out.println("【5. TeeOutputStream — 流复用（同时写多个目标）】");

        // 模拟：同时写到控制台和内存缓冲区（实际项目：同时写文件和发送到远程）
        ByteArrayOutputStream memBuffer = new ByteArrayOutputStream();
        // TeeOutputStream: 写一次 → 分发到 branch1(stdout) 和 branch2(memBuffer)
        try (TeeOutputStream tee = new TeeOutputStream(System.out, memBuffer)) {
            PrintStream ps = new PrintStream(tee, true, "UTF-8");
            ps.println("  >>> 这条消息同时写到控制台和内存缓冲区 <<<");
        }
        System.out.println("内存缓冲区内容: " + memBuffer.toString("UTF-8").trim());
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 6. FileAlterationMonitor —— 文件变更监控
    // -----------------------------------------------------------------------
    private static void demoFileAlterationMonitor(File watchDir) throws Exception {
        System.out.println("【6. FileAlterationMonitor — 文件变更监控】");

        // 只监控 .txt 文件（通过 filter 传入 observer）
        FileAlterationObserver observer = new FileAlterationObserver(
                watchDir, new WildcardFileFilter("*.txt"));

        // 注册监听器
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                System.out.println("  [事件] 文件创建: " + file.getName());
            }
            @Override
            public void onFileChange(File file) {
                System.out.println("  [事件] 文件修改: " + file.getName());
            }
            @Override
            public void onFileDelete(File file) {
                System.out.println("  [事件] 文件删除: " + file.getName());
            }
        });

        // 每 500ms 轮询一次
        FileAlterationMonitor monitor = new FileAlterationMonitor(500, observer);
        monitor.start();
        System.out.println("监控启动，目录: " + watchDir.getAbsolutePath());

        // 模拟文件操作
        Thread.sleep(600);
        File newFile = new File(watchDir, "newfile.txt");
        FileUtils.writeStringToFile(newFile, "created", StandardCharsets.UTF_8);

        Thread.sleep(600);
        FileUtils.writeStringToFile(newFile, "modified", StandardCharsets.UTF_8);

        Thread.sleep(600);
        FileUtils.deleteQuietly(newFile);

        Thread.sleep(600);
        monitor.stop();
        System.out.println("监控已停止\n");
    }

    // -----------------------------------------------------------------------
    // 辅助：提取文件名列表
    // -----------------------------------------------------------------------
    private static List<String> fileNames(Collection<File> files) {
        List<String> names = new java.util.ArrayList<>();
        for (File f : files) {
            names.add(f.getName());
        }
        java.util.Collections.sort(names);
        return names;
    }
}
