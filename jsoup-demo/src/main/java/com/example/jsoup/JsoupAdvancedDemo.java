package com.example.jsoup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.safety.Safelist;

import java.io.File;
import java.io.IOException;

/**
 * Jsoup 高级演示：DOM 操作、HTML 清洗、高级选择器、表单解析
 *
 * 主要内容：
 * 1. 从 URL 加载 HTML 文档（HTTP 请求）
 * 2. DOM 修改：添加/删除/修改元素
 * 3. HTML 清洗 (Clean) —— 防止 XSS 攻击
 * 4. 高级选择器：nth-child、正则匹配、组合选择器
 * 5. 表单数据解析
 *
 * @author java-tools-learning
 */
public class JsoupAdvancedDemo {

    // 模拟一个包含表单的 HTML 页面
    private static final String FORM_HTML = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head><title>用户注册</title></head>\n" +
            "<body>\n" +
            "    <form id=\"registerForm\" action=\"/register\" method=\"post\">\n" +
            "        <input type=\"text\" name=\"username\" value=\"john_doe\" placeholder=\"用户名\">\n" +
            "        <input type=\"email\" name=\"email\" value=\"john@example.com\">\n" +
            "        <input type=\"password\" name=\"password\" value=\"secret123\">\n" +
            "        <select name=\"role\">\n" +
            "            <option value=\"user\" selected>普通用户</option>\n" +
            "            <option value=\"admin\">管理员</option>\n" +
            "            <option value=\"vip\">VIP会员</option>\n" +
            "        </select>\n" +
            "        <input type=\"checkbox\" name=\"agree\" value=\"yes\" checked> 同意协议\n" +
            "        <button type=\"submit\">注册</button>\n" +
            "    </form>\n" +
            "    <div class=\"product-list\">\n" +
            "        <div class=\"product\" data-id=\"1001\">\n" +
            "            <h3>商品 A</h3>\n" +
            "            <span class=\"price\">¥99.00</span>\n" +
            "            <span class=\"stock in-stock\">有货</span>\n" +
            "        </div>\n" +
            "        <div class=\"product\" data-id=\"1002\">\n" +
            "            <h3>商品 B</h3>\n" +
            "            <span class=\"price\">¥199.00</span>\n" +
            "            <span class=\"stock out-of-stock\">缺货</span>\n" +
            "        </div>\n" +
            "        <div class=\"product\" data-id=\"1003\">\n" +
            "            <h3>商品 C</h3>\n" +
            "            <span class=\"price\">¥299.00</span>\n" +
            "            <span class=\"stock in-stock\">有货</span>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";

    // 一个包含不安全内容的 HTML（模拟 XSS 攻击场景）
    private static final String UNSAFE_HTML = "<div>\n" +
            "    <h1>用户评论</h1>\n" +
            "    <p>这是一个正常的评论。</p>\n" +
            "    <script>alert('XSS攻击!');</script>\n" +
            "    <p onclick=\"stealCookies()\">点击这里可能有危险</p>\n" +
            "    <a href=\"javascript:void(0)\" onclick=\"hack()\">危险链接</a>\n" +
            "    <img src=\"x\" onerror=\"alert('img XSS')\">\n" +
            "</div>";

    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.parse(FORM_HTML);

        // ========== 1. DOM 修改操作 ==========
        System.out.println("========== 1. DOM 修改操作 ==========");

        // 1.1 修改元素文本
        Element heading = doc.select("h1").first();
        if (heading == null) {
            // 如果不存在 h1 则创建一个
            heading = doc.select("head").first().appendElement("h1");
        }
        String originalText = heading.text();
        heading.text("欢迎来到在线商城");
        System.out.println("修改标题: \"" + originalText + "\" → \"" + heading.text() + "\"");

        // 1.2 添加属性
        Element firstProduct = doc.select(".product").first();
        if (firstProduct != null) {
            firstProduct.attr("data-promotion", "true");
            firstProduct.addClass("hot-sale");
            System.out.println("添加属性后: " + firstProduct.outerHtml().substring(0, Math.min(80, firstProduct.outerHtml().length())) + "...");
        }

        // 1.3 添加新元素
        Element productList = doc.select(".product-list").first();
        if (productList != null) {
            productList.append("<div class=\"product\" data-id=\"1004\">" +
                    "<h3>商品 D (新品)</h3>" +
                    "<span class=\"price\">¥399.00</span>" +
                    "<span class=\"stock in-stock\">有货</span>" +
                    "</div>");
            System.out.println("添加新商品后，商品总数: " + doc.select(".product").size());
        }

        // 1.4 删除元素
        Elements outOfStock = doc.select(".out-of-stock").parents();
        for (Element el : outOfStock) {
            if (el.hasClass("product")) {
                el.remove();
                System.out.println("删除缺货商品: " + el.select("h3").text());
            }
        }

        // ========== 2. 高级 CSS 选择器 ==========
        System.out.println("\n========== 2. 高级 CSS 选择器 ==========");

        // 重新解析避免已被修改
        Document doc2 = Jsoup.parse(FORM_HTML);

        // 2.1 :nth-child 伪类
        System.out.println("--- nth-child 选择器 ---");
        Elements allProducts = doc2.select(".product-list .product");
        for (int i = 0; i < allProducts.size(); i++) {
            Element nthProduct = doc2.select(".product-list .product:nth-child(" + (i + 1) + ")").first();
            if (nthProduct != null) {
                System.out.println("  第" + (i + 1) + "个商品: " + nthProduct.select("h3").text());
            }
        }

        // 2.2 属性前缀、包含匹配
        System.out.println("--- 属性匹配选择器 ---");
        // [attr^=value] 属性值以 value 开头
        Elements productsWithData = doc2.select("[data-id]");
        System.out.println("有 data-id 属性的元素: " + productsWithData.size() + " 个");

        // [attr$=value] 属性值以 value 结尾
        Elements gmailInputs = doc2.select("input[value$=example.com]");
        System.out.println("邮箱为 example.com 的输入框: " + gmailInputs.size() + " 个");

        // [attr*=value] 属性值包含 value
        Elements stockSpans = doc2.select("span[class*=stock]");
        System.out.println("包含 stock 类的 span: " + stockSpans.size() + " 个");

        // 2.3 组合选择器
        System.out.println("--- 组合选择器 ---");
        Elements inStockProducts = doc2.select(".product:has(.in-stock)");
        System.out.println("有货商品数量: " + inStockProducts.size());

        Elements prices = doc2.select(".product .price");
        for (Element price : prices) {
            System.out.println("  价格: " + price.text());
        }

        // ========== 3. 表单数据解析 ==========
        System.out.println("\n========== 3. 表单数据解析 ==========");
        Document formDoc = Jsoup.parse(FORM_HTML);
        Element form = formDoc.select("#registerForm").first();
        if (form != null) {
            System.out.println("表单 action: " + form.attr("action"));
            System.out.println("表单 method: " + form.attr("method"));

            // 解析所有 input 元素
            Elements inputs = form.select("input");
            for (Element input : inputs) {
                String name = input.attr("name");
                String type = input.attr("type");
                String value = input.attr("value");
                String placeholder = input.attr("placeholder");
                if (!name.isEmpty()) {
                    System.out.println("  字段: " + name + " (类型=" + type + ", 值=" + value +
                            (placeholder.isEmpty() ? "" : ", 占位=" + placeholder) + ")");
                }
            }

            // 解析 select 元素
            Element select = form.select("select[name=role]").first();
            if (select != null) {
                Elements options = select.select("option");
                for (Element option : options) {
                    boolean selected = option.hasAttr("selected");
                    System.out.println("  选项: " + option.text() + " (值=" + option.attr("value") +
                            (selected ? ", 已选中" : "") + ")");
                }
            }

            // 解析 checkbox
            Elements checkboxes = form.select("input[type=checkbox]");
            for (Element cb : checkboxes) {
                System.out.println("  复选框: " + cb.attr("name") + " = " + cb.attr("value") +
                        (cb.hasAttr("checked") ? " (已勾选)" : ""));
            }
        }

        // ========== 4. HTML 清洗 (防 XSS) ==========
        System.out.println("\n========== 4. HTML 清洗 (防 XSS) ==========");
        System.out.println("--- 原始不安全 HTML ---");
        System.out.println(UNSAFE_HTML.trim());

        // 4.1 使用基本白名单清洗（只保留基本文本格式标签）
        String basicClean = Jsoup.clean(UNSAFE_HTML, Safelist.basic());
        System.out.println("\n--- basic() 清洗后 ---");
        System.out.println(basicClean);

        // 4.2 使用简单文本清洗（只保留文本，去掉所有标签）
        String simpleClean = Jsoup.clean(UNSAFE_HTML, Safelist.simpleText());
        System.out.println("\n--- simpleText() 清洗后 ---");
        System.out.println(simpleClean);

        // 4.3 自定义白名单
        Safelist customSafelist = new Safelist()
                .addTags("div", "h1", "p", "a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http", "https");
        String customClean = Jsoup.clean(UNSAFE_HTML, customSafelist);
        System.out.println("\n--- 自定义白名单清洗后 (只保留 div/h1/p/a) ---");
        System.out.println(customClean);

        // 4.4 验证清洗是否有效
        boolean hasScript = basicClean.contains("<script");
        boolean hasOnclick = basicClean.contains("onclick");
        boolean hasJavascriptHref = basicClean.contains("javascript:");
        System.out.println("\n清洗验证:");
        System.out.println("  残留 <script> 标签: " + hasScript);
        System.out.println("  残留 onclick 属性: " + hasOnclick);
        System.out.println("  残留 javascript: 协议: " + hasJavascriptHref);
        System.out.println("  ✅ XSS 攻击代码已全部清除！");

        // ========== 5. DOM 遍历 ==========
        System.out.println("\n========== 5. DOM 树遍历 ==========");
        Document doc3 = Jsoup.parse(FORM_HTML);
        Element productDiv = doc3.select(".product").first();
        if (productDiv != null) {
            System.out.println("当前元素: " + productDiv.tagName() + " (class=" + productDiv.className() + ")");

            // 父节点
            Element parent = productDiv.parent();
            System.out.println("  父节点: " + parent.tagName() + " (class=" + parent.className() + ")");

            // 兄弟节点
            Element nextSibling = productDiv.nextElementSibling();
            if (nextSibling != null) {
                System.out.println("  下一个兄弟: " + nextSibling.tagName() + " (class=" + nextSibling.className() + ")");
            }

            // 子节点遍历
            System.out.println("  子节点列表:");
            for (Node child : productDiv.childNodes()) {
                if (child instanceof Element) {
                    Element childEl = (Element) child;
                    System.out.println("    <" + childEl.tagName() + ">" + childEl.text() + "</" + childEl.tagName() + ">");
                } else if (child instanceof TextNode) {
                    String text = ((TextNode) child).text().trim();
                    if (!text.isEmpty()) {
                        System.out.println("    [文本] " + text);
                    }
                }
            }
        }

        System.out.println("\n✅ Jsoup 高级功能演示完成！");
    }
}
