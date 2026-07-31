package com.example.tika;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tika 实战演示：文档索引、文件类型统计、敏感信息扫描、Spring Boot 集成思路。
 *
 * <p>场景贴近企业级内容管理：
 * <ul>
 *   <li><b>批量索引</b>：递归扫描目录，提取文本和元数据，为搜索引擎准备数据；</li>
 *   <li><b>文件类型统计</b>：基于 MIME 类型做存储分布分析；</li>
 *   <li><b>敏感词扫描</b>：从文档中提取文本后匹配敏感词库；</li>
 *   <li><b>Spring Boot 集成</b>：给出服务封装思路与代码骨架。</li>
 * </ul>
 */
public class TikaPracticalDemo {

    private static final List<String> SENSITIVE_WORDS = Arrays.asList("密码", "passwd", "机密", "confidential");

    public static void main(String[] args) throws Exception {
        // 1. 批量文档索引（使用项目自带的 sample 目录）
        indexDocuments(Paths.get("tika-demo/src/main/resources/sample"));

        // 2. 文件类型统计
        statisticsByMimeType(Paths.get("tika-demo/src/main/resources/sample"));

        // 3. 敏感词扫描
        scanSensitiveWords(Paths.get("tika-demo/src/main/resources/sample"));

        // 4. 打印 Spring Boot 集成要点
        printSpringBootIntegrationTips();
    }

    /**
     * 递归扫描目录，提取每个文件的文本与元数据，模拟构建搜索索引的过程。
     */
    private static void indexDocuments(Path root) throws Exception {
        System.out.println("===== 1. 批量文档索引 =====");
        AutoDetectParser parser = new AutoDetectParser();
        Tika tika = new Tika();

        if (!Files.exists(root)) {
            System.out.println("目录不存在（请在项目根目录运行）: " + root);
            System.out.println();
            return;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(Files::isRegularFile).collect(Collectors.toList());

            for (Path file : files) {
                BodyContentHandler handler = new BodyContentHandler(-1); // -1 表示不限制字符数
                Metadata metadata = new Metadata();
                metadata.set("resourceName", file.getFileName().toString());

                try (InputStream is = Files.newInputStream(file)) {
                    parser.parse(is, handler, metadata, new ParseContext());

                    String title = metadata.get("title");
                    String mime = tika.detect(file);
                    String contentPreview = handler.toString()
                            .replaceAll("\\s+", " ")
                            .trim();
                    if (contentPreview.length() > 80) {
                        contentPreview = contentPreview.substring(0, 80) + "...";
                    }

                    System.out.printf("文件: %-20s | MIME: %-25s | 标题: %-10s | 预览: %s%n",
                            file.getFileName(), mime, title == null ? "N/A" : title, contentPreview);
                } catch (Exception e) {
                    System.err.println("解析失败: " + file + " -> " + e.getMessage());
                }
            }
        }
        System.out.println();
    }

    /**
     * 统计目录下各类文件的 MIME 类型分布。
     */
    private static void statisticsByMimeType(Path root) throws IOException {
        System.out.println("===== 2. 文件类型统计 =====");
        Tika tika = new Tika();
        Map<String, Integer> stats = new HashMap<>();

        if (!Files.exists(root)) {
            System.out.println("目录不存在: " + root);
            System.out.println();
            return;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String mime = tika.detect(file);
                    stats.merge(mime, 1, Integer::sum);
                } catch (IOException e) {
                    System.err.println("检测失败: " + file);
                }
            });
        }

        stats.forEach((mime, count) -> System.out.printf("  %-35s : %d 个%n", mime, count));
        System.out.println();
    }

    /**
     * 扫描目录文档，检测是否包含预定义的敏感词。
     */
    private static void scanSensitiveWords(Path root) throws Exception {
        System.out.println("===== 3. 敏感词扫描 =====");
        AutoDetectParser parser = new AutoDetectParser();

        if (!Files.exists(root)) {
            System.out.println("目录不存在: " + root);
            System.out.println();
            return;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(Files::isRegularFile).collect(Collectors.toList());

            for (Path file : files) {
                BodyContentHandler handler = new BodyContentHandler();
                try (InputStream is = Files.newInputStream(file)) {
                    parser.parse(is, handler, new Metadata(), new ParseContext());
                    String content = handler.toString();

                    List<String> hits = SENSITIVE_WORDS.stream()
                            .filter(content::contains)
                            .collect(Collectors.toList());

                    if (!hits.isEmpty()) {
                        System.out.printf("⚠️ 文件 %s 命中敏感词: %s%n", file.getFileName(), hits);
                    } else {
                        System.out.printf("✅ 文件 %s 未命中敏感词%n", file.getFileName());
                    }
                } catch (Exception e) {
                    System.err.println("扫描失败: " + file + " -> " + e.getMessage());
                }
            }
        }
        System.out.println();
    }

    /**
     * 打印 Spring Boot 集成思路与代码骨架。
     */
    private static void printSpringBootIntegrationTips() {
        System.out.println("===== 4. Spring Boot 集成要点 =====");
        System.out.println("1. 添加依赖：");
        System.out.println("   <dependency>");
        System.out.println("       <groupId>org.apache.tika</groupId>");
        System.out.println("       <artifactId>tika-core</artifactId>");
        System.out.println("       <version>2.9.1</version>");
        System.out.println("   </dependency>");
        System.out.println("   <dependency>");
        System.out.println("       <groupId>org.apache.tika</groupId>");
        System.out.println("       <artifactId>tika-parsers-standard-package</artifactId>");
        System.out.println("       <version>2.9.1</version>");
        System.out.println("   </dependency>");
        System.out.println();
        System.out.println("2. 注册 Tika 单例 Bean：");
        System.out.println("   @Bean");
        System.out.println("   public Tika tika() {");
        System.out.println("       return new Tika();");
        System.out.println("   }");
        System.out.println();
        System.out.println("3. 上传文件解析 Service：");
        System.out.println("   public DocumentParseResult parse(MultipartFile file) throws Exception {");
        System.out.println("       Tika tika = ...;");
        System.out.println("       String mime = tika.detect(file.getInputStream());");
        System.out.println("       String text = tika.parseToString(file.getInputStream());");
        System.out.println("       return new DocumentParseResult(mime, text);");
        System.out.println("   }");
        System.out.println();
        System.out.println("4. 注意事项：");
        System.out.println("   - 大文件应使用 BodyContentHandler(int writeLimit) 限制字符数，防止 OOM。");
        System.out.println("   - 对不可信文件做 MIME 白名单校验，避免解析恶意构造的文件。");
        System.out.println("   - 解析耗时操作应放在线程池或消息队列中异步执行。");
    }
}
