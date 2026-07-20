package com.example.commonsio;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Apache Commons IO — 实战演示
 *
 * <p>覆盖三个典型业务场景：
 * <ol>
 *   <li>配置文件安全读取工具（带默认值、类型转换、统一 close 保护）</li>
 *   <li>多租户文件归档工具（按租户 ID 组织目录、批量迁移、磁盘占用统计）</li>
 *   <li>日志文件清理工具（扫描超期文件、安全删除、操作报告生成）</li>
 * </ol>
 */
public class CommonsIOPracticalDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== Apache Commons IO 实战演示 ==========\n");

        File baseDir = new File(System.getProperty("java.io.tmpdir"), "commons-io-practical");
        FileUtils.forceMkdir(baseDir);

        demoConfigFileUtil(baseDir);
        demoMultiTenantArchive(baseDir);
        demoLogFileCleaner(baseDir);

        FileUtils.deleteDirectory(baseDir);
        System.out.println("所有演示完成，临时目录已清理。");
    }

    // -----------------------------------------------------------------------
    // 场景1：配置文件安全读取工具
    // -----------------------------------------------------------------------
    private static void demoConfigFileUtil(File baseDir) throws IOException {
        System.out.println("【场景1：配置文件安全读取工具】");

        // 准备配置文件
        File configFile = new File(baseDir, "app.properties");
        String configContent =
                "# 应用配置\n" +
                "app.name=MyService\n" +
                "app.port=8080\n" +
                "app.debug=true\n" +
                "app.timeout=3000\n" +
                "app.description=这是一个示例服务\n";
        FileUtils.writeStringToFile(configFile, configContent, StandardCharsets.UTF_8);

        // 读取并解析配置（使用 IOUtils + Properties 风格）
        Map<String, String> config = parsePropertiesFile(configFile);

        System.out.println("配置文件: " + configFile.getName() + "（"
                + FileUtils.byteCountToDisplaySize(configFile.length()) + "）");
        System.out.println("app.name    = " + getConfig(config, "app.name", "unknown"));
        System.out.println("app.port    = " + getConfigInt(config, "app.port", 9090));
        System.out.println("app.debug   = " + getConfigBool(config, "app.debug", false));
        System.out.println("app.timeout = " + getConfigInt(config, "app.timeout", 5000));
        System.out.println("app.missing = " + getConfig(config, "app.missing", "DEFAULT_VALUE"));

        // CloseShieldInputStream：包装流，防止外部代码意外关闭它
        // 场景：把 InputStream 传给第三方库，又不希望第三方关闭它
        try (InputStream raw = IOUtils.toInputStream(configContent, StandardCharsets.UTF_8)) {
            InputStream shielded = new CloseShieldInputStream(raw);
            String data = IOUtils.toString(shielded, StandardCharsets.UTF_8);
            shielded.close();   // 调用 close，但底层 raw 不受影响
            // raw 在这里仍然可用
            System.out.println("CloseShieldInputStream 演示：原流未被关闭，可继续读取");
        }

        // StringBuilderWriter：内存 Writer，替代 StringWriter，不抛 IOException
        Writer writer = new StringBuilderWriter();
        writer.write("写入配置摘要: ");
        writer.write(config.size() + " 个有效配置项");
        System.out.println("StringBuilderWriter 结果: " + writer.toString());
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 场景2：多租户文件归档工具
    // -----------------------------------------------------------------------
    private static void demoMultiTenantArchive(File baseDir) throws IOException {
        System.out.println("【场景2：多租户文件归档工具】");

        // 模拟上传文件存放在一个混合目录
        File uploadDir = new File(baseDir, "uploads");
        FileUtils.forceMkdir(uploadDir);

        // 模拟 6 个租户的文件（文件名格式：tenant_文件名.扩展名）
        String[] uploads = {
                "tenant_A_invoice_001.pdf", "tenant_B_report_2026.xlsx",
                "tenant_A_photo.jpg",       "tenant_C_contract.pdf",
                "tenant_B_logo.png",        "tenant_A_backup.zip",
                "tenant_C_data.csv",        "tenant_B_readme.txt"
        };
        for (String name : uploads) {
            File f = new File(uploadDir, name);
            FileUtils.writeStringToFile(f,
                    "content of " + name + " ".repeat((int)(Math.random() * 200)),
                    StandardCharsets.UTF_8);
        }

        // 按租户 ID 分类归档
        File archiveDir = new File(baseDir, "archive");
        Collection<File> allFiles = FileUtils.listFiles(uploadDir, TrueFileFilter.INSTANCE, null);
        int moved = 0;
        for (File file : allFiles) {
            String fname = file.getName();
            if (!fname.startsWith("tenant_")) continue;
            // 提取租户 ID
            String tenantId = fname.substring("tenant_".length(), fname.indexOf('_', "tenant_".length()));
            File tenantDir = new File(archiveDir, tenantId);
            FileUtils.forceMkdir(tenantDir);
            FileUtils.moveFileToDirectory(file, tenantDir, false);
            moved++;
        }
        System.out.println("归档文件数: " + moved);

        // 统计每个租户磁盘占用
        File[] tenantDirs = archiveDir.listFiles(File::isDirectory);
        System.out.println("各租户磁盘占用统计:");
        if (tenantDirs != null) {
            for (File td : tenantDirs) {
                long size = FileUtils.sizeOfDirectory(td);
                int count = FileUtils.listFiles(td, TrueFileFilter.INSTANCE, null).size();
                System.out.printf("  租户 %-4s：%d 个文件，%s%n",
                        td.getName(), count, FileUtils.byteCountToDisplaySize(size));
            }
        }

        // 按文件类型统计（PDF / 图片 / 其他）
        System.out.println("按文件类型统计（从 archive 目录递归）:");
        for (String suffix : new String[]{".pdf", ".xlsx", ".jpg", ".png", ".csv", ".zip", ".txt"}) {
            Collection<File> matched = FileUtils.listFiles(archiveDir,
                    new SuffixFileFilter(suffix), TrueFileFilter.INSTANCE);
            if (!matched.isEmpty()) {
                System.out.printf("  %-6s：%d 个文件%n", suffix, matched.size());
            }
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 场景3：日志文件清理工具
    // -----------------------------------------------------------------------
    private static void demoLogFileCleaner(File baseDir) throws IOException {
        System.out.println("【场景3：日志文件清理工具】");

        File logDir = new File(baseDir, "logs");
        FileUtils.forceMkdir(logDir);

        // 模拟创建不同时间的日志文件（用 lastModified 欺骗时间）
        long now = System.currentTimeMillis();
        long day = 86400_000L;

        String[] logNames = {
                "app-2026-05-01.log", "app-2026-05-10.log",
                "app-2026-05-15.log", "app-2026-05-20.log",
                "app-2026-05-28.log", "app-2026-05-29.log"
        };
        long[] ageDays = {28, 19, 14, 9, 1, 0};   // 距今天数

        for (int i = 0; i < logNames.length; i++) {
            File f = new File(logDir, logNames[i]);
            FileUtils.writeStringToFile(f,
                    "=== " + logNames[i] + " ===\n模拟日志内容\n", StandardCharsets.UTF_8);
            f.setLastModified(now - ageDays[i] * day);
        }

        System.out.println("准备了 " + logNames.length + " 个日志文件");

        // 清理策略：删除 7 天前的日志，保留最近 7 天
        long cutoffTime = now - 7 * day;
        int deleted = 0, kept = 0;
        long freedBytes = 0;

        // 生成报告到 StringBuilderWriter
        Writer report = new StringBuilderWriter();
        report.write("日志清理报告\n");
        report.write("切割时间: " + new java.util.Date(cutoffTime) + "\n");
        report.write("--------------------\n");

        Collection<File> logFiles = FileUtils.listFiles(logDir,
                new SuffixFileFilter(".log"), null);
        for (File f : logFiles) {
            long age = (now - f.lastModified()) / day;
            if (f.lastModified() < cutoffTime) {
                freedBytes += f.length();
                FileUtils.deleteQuietly(f);
                report.write("DELETED: " + f.getName() + " (" + age + "天前)\n");
                deleted++;
            } else {
                report.write("KEPT   : " + f.getName() + " (" + age + "天前)\n");
                kept++;
            }
        }
        report.write("--------------------\n");
        report.write("删除: " + deleted + " 个，保留: " + kept + " 个，释放: "
                + FileUtils.byteCountToDisplaySize(freedBytes) + "\n");

        // 打印报告
        System.out.println(report.toString());

        // 将报告写入磁盘
        File reportFile = new File(logDir, "cleanup-report.txt");
        FileUtils.writeStringToFile(reportFile, report.toString(), StandardCharsets.UTF_8);
        System.out.println("报告已写入: " + reportFile.getName()
                + " (" + FileUtils.byteCountToDisplaySize(reportFile.length()) + ")");
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // 辅助方法
    // -----------------------------------------------------------------------
    private static Map<String, String> parsePropertiesFile(File file) throws IOException {
        Map<String, String> map = new HashMap<>();
        for (String line : FileUtils.readLines(file, StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.startsWith("#") || !line.contains("=")) continue;
            int idx = line.indexOf('=');
            map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
        }
        return map;
    }

    private static String getConfig(Map<String, String> cfg, String key, String defaultVal) {
        return cfg.getOrDefault(key, defaultVal);
    }

    private static int getConfigInt(Map<String, String> cfg, String key, int defaultVal) {
        try {
            return Integer.parseInt(cfg.getOrDefault(key, String.valueOf(defaultVal)));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static boolean getConfigBool(Map<String, String> cfg, String key, boolean defaultVal) {
        String val = cfg.get(key);
        return val == null ? defaultVal : Boolean.parseBoolean(val);
    }
}
