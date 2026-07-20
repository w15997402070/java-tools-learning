package com.example.jsoup;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Jsoup 实战演示：综合应用场景
 *
 * 场景覆盖：
 * 1. 模拟网页抓取 —— 从页面提取结构化数据
 * 2. HTML 表格解析 —— 将 HTML 表格转为 List<Map>
 * 3. HTML 转纯文本 —— 提取可读文本
 * 4. 连接配置 —— User-Agent、超时、Cookie、代理
 * 5. Spring Boot 集成指南（注释说明）
 *
 * @author java-tools-learning
 */
public class JsoupPracticalDemo {

    // 模拟一个文章列表页面
    private static final String BLOG_HTML = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head><title>技术博客</title></head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <article class=\"post\">\n" +
            "            <h2 class=\"post-title\"><a href=\"/post/1\">深入理解 Java GC</a></h2>\n" +
            "            <div class=\"post-meta\">\n" +
            "                <span class=\"author\">张三</span>\n" +
            "                <span class=\"date\">2024-06-15</span>\n" +
            "                <span class=\"tags\">Java, JVM, GC</span>\n" +
            "                <span class=\"views\">阅读 1,234</span>\n" +
            "            </div>\n" +
            "            <p class=\"excerpt\">本文深入探讨 Java 垃圾回收机制，包括 Serial、Parallel、CMS、G1 等收集器的工作原理和适用场景...</p>\n" +
            "        </article>\n" +
            "        <article class=\"post\">\n" +
            "            <h2 class=\"post-title\"><a href=\"/post/2\">Spring Boot 3 新特性全解析</a></h2>\n" +
            "            <div class=\"post-meta\">\n" +
            "                <span class=\"author\">李四</span>\n" +
            "                <span class=\"date\">2024-06-14</span>\n" +
            "                <span class=\"tags\">Spring Boot, Java 17</span>\n" +
            "                <span class=\"views\">阅读 2,567</span>\n" +
            "            </div>\n" +
            "            <p class=\"excerpt\">Spring Boot 3 带来了诸多改进，包括 GraalVM 原生镜像支持、新的 Observability API、问题详情支持等...</p>\n" +
            "        </article>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";

    // 模拟一个数据表格
    private static final String TABLE_HTML = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head><title>销售数据</title></head>\n" +
            "<body>\n" +
            "    <table id=\"salesTable\" class=\"data-table\">\n" +
            "        <thead>\n" +
            "            <tr>\n" +
            "                <th>月份</th>\n" +
            "                <th>销售额（万元）</th>\n" +
            "                <th>订单数</th>\n" +
            "                <th>客单价（元）</th>\n" +
            "                <th>增长率</th>\n" +
            "            </tr>\n" +
            "        </thead>\n" +
            "        <tbody>\n" +
            "            <tr>\n" +
            "                <td>2024-01</td>\n" +
            "                <td>520.5</td>\n" +
            "                <td>3,420</td>\n" +
            "                <td>152.2</td>\n" +
            "                <td class=\"positive\">+5.2%</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td>2024-02</td>\n" +
            "                <td>480.3</td>\n" +
            "                <td>3,150</td>\n" +
            "                <td>152.5</td>\n" +
            "                <td class=\"negative\">-7.7%</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td>2024-03</td>\n" +
            "                <td>610.8</td>\n" +
            "                <td>4,120</td>\n" +
            "                <td>148.3</td>\n" +
            "                <td class=\"positive\">+27.2%</td>\n" +
            "            </tr>\n" +
            "            <tr>\n" +
            "                <td>2024-04</td>\n" +
            "                <td>580.2</td>\n" +
            "                <td>3,890</td>\n" +
            "                <td>149.2</td>\n" +
            "                <td class=\"negative\">-5.0%</td>\n" +
            "            </tr>\n" +
            "        </tbody>\n" +
            "    </table>\n" +
            "</body>\n" +
            "</html>";

    public static void main(String[] args) throws IOException {
        // ========== 场景 1: 博客文章抓取 ==========
        System.out.println("========== 场景 1: 博客文章列表抓取 ==========");
        List<Map<String, String>> articles = scrapeBlogArticles(BLOG_HTML);
        for (int i = 0; i < articles.size(); i++) {
            Map<String, String> article = articles.get(i);
            System.out.println("\n文章 #" + (i + 1) + ":");
            System.out.println("  标题: " + article.get("title"));
            System.out.println("  作者: " + article.get("author"));
            System.out.println("  日期: " + article.get("date"));
            System.out.println("  标签: " + article.get("tags"));
            System.out.println("  阅读: " + article.get("views"));
            System.out.println("  摘要: " + article.get("excerpt"));
            System.out.println("  链接: " + article.get("url"));
        }

        // ========== 场景 2: HTML 表格解析 ==========
        System.out.println("\n========== 场景 2: HTML 表格数据提取 ==========");
        List<Map<String, String>> tableData = parseHtmlTable(TABLE_HTML, "#salesTable");
        System.out.println("表格行数: " + tableData.size());
        System.out.println("列名: " + (tableData.isEmpty() ? "无" : tableData.get(0).keySet()));
        for (Map<String, String> row : tableData) {
            System.out.println("  " + row);
        }

        // 简单统计分析
        double totalSales = 0;
        int totalOrders = 0;
        for (Map<String, String> row : tableData) {
            totalSales += Double.parseDouble(row.get("销售额（万元）"));
            totalOrders += Integer.parseInt(row.get("订单数").replace(",", ""));
        }
        System.out.println("\n统计:");
        System.out.println("  总销售额: " + String.format("%.1f", totalSales) + " 万元");
        System.out.println("  总订单数: " + String.format("%,d", totalOrders));
        System.out.println("  月均销售额: " + String.format("%.1f", totalSales / tableData.size()) + " 万元");

        // ========== 场景 3: HTML 转纯文本 ==========
        System.out.println("\n========== 场景 3: HTML 转纯文本 ==========");
        String pureText = htmlToPlainText(BLOG_HTML);
        System.out.println(pureText);

        // ========== 场景 4: 连接配置说明 ==========
        System.out.println("\n========== 场景 4: HTTP 连接配置示例 ==========");
        demonstrateConnectionConfig();

        // ========== 场景 5: 批量处理 ==========
        System.out.println("\n========== 场景 5: 批量文档处理 ==========");
        batchProcessExample();

        System.out.println("\n✅ Jsoup 实战演示完成！");
    }

    /**
     * 从博客页面提取文章列表
     */
    private static List<Map<String, String>> scrapeBlogArticles(String html) {
        List<Map<String, String>> articles = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        Elements posts = doc.select("article.post");
        for (Element post : posts) {
            Map<String, String> article = new LinkedHashMap<>();
            article.put("title", post.select(".post-title a").text());
            article.put("url", post.select(".post-title a").attr("href"));
            article.put("author", post.select(".author").text());
            article.put("date", post.select(".date").text());
            article.put("tags", post.select(".tags").text());
            article.put("views", post.select(".views").text());
            article.put("excerpt", post.select(".excerpt").text());
            articles.add(article);
        }
        return articles;
    }

    /**
     * 将 HTML 表格解析为 List<Map<String, String>> 结构
     *
     * @param html      HTML 内容
     * @param tableSelector 表格的 CSS 选择器
     * @return 表格数据
     */
    private static List<Map<String, String>> parseHtmlTable(String html, String tableSelector) {
        List<Map<String, String>> result = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        Element table = doc.select(tableSelector).first();

        if (table == null) {
            return result;
        }

        // 提取表头作为 Key
        Elements headers = table.select("thead th");
        List<String> keys = new ArrayList<>();
        for (Element th : headers) {
            keys.add(th.text().trim());
        }

        // 提取数据行
        Elements rows = table.select("tbody tr");
        for (Element row : rows) {
            Map<String, String> rowData = new LinkedHashMap<>();
            Elements cells = row.select("td");
            for (int i = 0; i < cells.size() && i < keys.size(); i++) {
                rowData.put(keys.get(i), cells.get(i).text().trim());
            }
            result.add(rowData);
        }

        return result;
    }

    /**
     * 将 HTML 转为纯文本（保留基本格式）
     */
    private static String htmlToPlainText(String html) {
        Document doc = Jsoup.parse(html);
        // wholeText() 返回整个文档的纯文本，包括脚本和样式内容
        // text() 返回文档正文纯文本，排除脚本和样式
        return doc.body().text();
    }

    /**
     * 演示 Jsoup HTTP 连接的各种配置选项
     */
    private static void demonstrateConnection() {
        System.out.println("Jsoup 连接配置说明（不实际执行以节省时间）:");
        System.out.println();
        System.out.println("// 基础配置示例:");
        System.out.println("Document doc = Jsoup.connect(\"https://example.com\")");
        System.out.println("    .userAgent(\"Mozilla/5.0 (Windows NT 10.0; Win64; x64)\")");
        System.out.println("    .header(\"Accept-Language\", \"zh-CN,zh;q=0.9\")");
        System.out.println("    .timeout(10000)           // 10 秒超时");
        System.out.println("    .cookie(\"sessionId\", \"abc123\")");
        System.out.println("    .referrer(\"https://google.com\")");
        System.out.println("    .followRedirects(true)     // 跟随重定向");
        System.out.println("    .ignoreHttpErrors(true)    // 忽略 HTTP 错误状态码");
        System.out.println("    .maxBodySize(2 * 1024 * 1024) // 2MB 最大响应体");
        System.out.println("    .get();");
        System.out.println();
        System.out.println("// POST 请求示例:");
        System.out.println("Document doc = Jsoup.connect(\"https://example.com/login\")");
        System.out.println("    .data(\"username\", \"admin\")");
        System.out.println("    .data(\"password\", \"secret\")");
        System.out.println("    .post();");
        System.out.println();
        System.out.println("// 代理配置:");
        System.out.println("Document doc = Jsoup.connect(\"https://example.com\")");
        System.out.println("    .proxy(\"127.0.0.1\", 8100)");
        System.out.println("    .get();");
    }

    /**
     * 演示连接配置
     */
    private static void demonstrateConnectionConfig() {
        demonstrateConnection();
    }

    /**
     * 批量处理多个 HTML 文档的示例
     */
    private static void batchProcessExample() {
        System.out.println("批量处理示例（概念演示）:");

        // 模拟要处理的 HTML 片段列表
        String[] htmlSnippets = {
                "<div><h2>标题A</h2><p>内容A</p></div>",
                "<div><h2>标题B</h2><p>内容B</p></div>",
                "<div><h2>标题C</h2><p>内容C</p></div>"
        };

        List<Map<String, String>> results = new ArrayList<>();
        for (String snippet : htmlSnippets) {
            Document doc = Jsoup.parse(snippet);
            Map<String, String> data = new LinkedHashMap<>();
            data.put("title", doc.select("h2").text());
            data.put("content", doc.select("p").text());
            results.add(data);
        }

        for (int i = 0; i < results.size(); i++) {
            System.out.println("  文档 #" + (i + 1) + ": " + results.get(i));
        }
        System.out.println("  共处理 " + results.size() + " 个文档片段");
    }

    /**
     * ============================================================
     * Spring Boot 集成指南
     * ============================================================
     *
     * 1. Maven 依赖（在 Spring Boot 项目中添加）:
     *
     *    <dependency>
     *        <groupId>org.jsoup</groupId>
     *        <artifactId>jsoup</artifactId>
     *        <version>1.22.2</version>
     *    </dependency>
     *
     * 2. 配置类示例（@Configuration）:
     *
     *    @Configuration
     *    public class JsoupConfig {
     *
     *        @Bean
     *        public Connection defaultConnection() {
     *            return Jsoup.newSession()
     *                .userAgent("Mozilla/5.0 ...")
     *                .timeout(10000)
     *                .cookie("sessionId", getSessionId());
     *        }
     *    }
     *
     * 3. Service 层使用示例:
     *
     *    @Service
     *    public class WebScraperService {
     *
     *        @Autowired
     *        private Connection defaultConnection;
     *
     *        public List<Article> fetchArticles(String url) throws IOException {
     *            Document doc = defaultConnection.newRequest()
     *                .url(url)
     *                .get();
     *
     *            return doc.select(".article-item").stream()
     *                .map(el -> new Article(
     *                    el.select(".title").text(),
     *                    el.select(".summary").text()
     *                ))
     *                .collect(Collectors.toList());
     *        }
     *    }
     *
     * 4. 注意事项:
     *    - Jsoup 不是线程安全的 Connection 对象，每次请求应创建新实例
     *    - 建议结合 @Async 注解实现异步抓取
     *    - 配合 RateLimiter 避免对目标网站造成压力
     *    - 使用 Jsoup.newSession() 可以共享 Cookie 和默认设置
     */
}
