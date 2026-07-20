package com.example.jsoup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Jsoup 基础演示：HTML 解析、CSS 选择器、元素提取
 *
 * Jsoup 是一个用于处理真实世界 HTML 的 Java 库。
 * 它提供了非常方便的 API 来提取和操作数据，使用 DOM、CSS 和类似 jQuery 的方法。
 *
 * 核心能力：
 * 1. 从 URL、文件或字符串解析 HTML
 * 2. 使用 CSS 选择器查找元素
 * 3. 提取属性、文本和 HTML
 *
 * @author java-tools-learning
 */
public class JsoupBasicDemo {

    // 模拟一个简单的 HTML 页面内容
    private static final String HTML = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <title>Jsoup 学习页面</title>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"description\" content=\"Jsoup HTML解析器学习\">\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"header\">\n" +
            "        <h1>欢迎来到 Jsoup 世界</h1>\n" +
            "        <p class=\"subtitle\">一个强大的 Java HTML 解析器</p>\n" +
            "    </div>\n" +
            "    <div id=\"content\">\n" +
            "        <h2>最新文章</h2>\n" +
            "        <ul class=\"article-list\">\n" +
            "            <li>\n" +
            "                <a href=\"/article/1.html\" class=\"article-link\">Jsoup 入门指南</a>\n" +
            "                <span class=\"date\">2024-01-15</span>\n" +
            "                <span class=\"author\">张三</span>\n" +
            "            </li>\n" +
            "            <li>\n" +
            "                <a href=\"/article/2.html\" class=\"article-link\">CSS 选择器详解</a>\n" +
            "                <span class=\"date\">2024-02-20</span>\n" +
            "                <span class=\"author\">李四</span>\n" +
            "            </li>\n" +
            "            <li>\n" +
            "                <a href=\"/article/3.html\" class=\"article-link\">实战：网页数据抓取</a>\n" +
            "                <span class=\"date\">2024-03-10</span>\n" +
            "                <span class=\"author\">王五</span>\n" +
            "            </li>\n" +
            "        </ul>\n" +
            "    </div>\n" +
            "    <div id=\"footer\">\n" +
            "        <p>© 2024 Jsoup 学习项目 | 版本 <span class=\"version\">1.22.2</span></p>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";

    public static void main(String[] args) {
        // ========== 1. 从字符串解析 HTML ==========
        System.out.println("========== 1. 解析 HTML 字符串 ==========");
        Document doc = Jsoup.parse(HTML);
        System.out.println("解析成功！文档标题: " + doc.title());

        // ========== 2. 获取页面标题 ==========
        System.out.println("\n========== 2. 获取页面标题 ==========");
        System.out.println("标题 (doc.title()): " + doc.title());
        System.out.println("标题 (选择器): " + doc.select("title").text());

        // ========== 3. 使用 CSS 选择器提取元素 ==========
        System.out.println("\n========== 3. CSS 选择器基础 ==========");

        // 3.1 通过标签名选择
        System.out.println("--- 标签名选择器 ---");
        Elements h1Elements = doc.select("h1");
        System.out.println("h1 内容: " + h1Elements.text());

        // 3.2 通过 ID 选择
        System.out.println("--- ID 选择器 ---");
        Element header = doc.select("#header").first();
        if (header != null) {
            System.out.println("header 区域内容: " + header.text());
        }

        // 3.3 通过 class 选择
        System.out.println("--- Class 选择器 ---");
        Elements subtitles = doc.select(".subtitle");
        System.out.println("subtitle 内容: " + subtitles.text());

        // 3.4 层级选择器
        System.out.println("--- 层级选择器 ---");
        Elements articleItems = doc.select("#content .article-list li");
        System.out.println("文章列表项数量: " + articleItems.size());

        // 3.5 属性选择器
        System.out.println("--- 属性选择器 ---");
        Elements links = doc.select("a[href]");
        System.out.println("所有链接数量: " + links.size());
        Elements specificLinks = doc.select("a[href^=/article/]");
        System.out.println("文章链接数量: " + specificLinks.size());

        // ========== 4. 提取元素属性 ==========
        System.out.println("\n========== 4. 提取元素属性 ==========");
        for (Element link : specificLinks) {
            String href = link.attr("href");
            String text = link.text();
            String absHref = link.absUrl("href");
            System.out.println("  文本: " + text + " | 相对路径: " + href + " | 绝对路径: " + absHref);
        }

        // ========== 5. 提取 meta 标签 ==========
        System.out.println("\n========== 5. Meta 标签提取 ==========");
        Elements metaTags = doc.select("meta[name]");
        for (Element meta : metaTags) {
            System.out.println("  " + meta.attr("name") + " = " + meta.attr("content"));
        }

        // ========== 6. 遍历与过滤 ==========
        System.out.println("\n========== 6. 遍历文章列表 ==========");
        for (Element item : articleItems) {
            String title = item.select(".article-link").text();
            String date = item.select(".date").text();
            String author = item.select(".author").text();
            System.out.println("  文章: " + title + " | 日期: " + date + " | 作者: " + author);
        }

        // ========== 7. 文本提取方式对比 ==========
        System.out.println("\n========== 7. 文本提取方式对比 ==========");
        Element contentDiv = doc.select("#content").first();
        if (contentDiv != null) {
            // text() 返回所有子元素的纯文本（带空格）
            System.out.println("text(): " + contentDiv.text());
            // ownText() 只返回自身的文本
            System.out.println("ownText(): " + contentDiv.ownText());
            // html() 返回内部 HTML
            System.out.println("html() 前50字符: " + contentDiv.html().substring(0, Math.min(50, contentDiv.html().length())));
        }

        System.out.println("\n✅ Jsoup 基础功能演示完成！");
    }
}
