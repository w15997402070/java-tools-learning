package com.example.commonstext;

import org.apache.commons.text.RandomStringGenerator;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.TextStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Apache Commons Text 进阶演示。
 *
 * 涵盖：
 * 1. 字符串转义（HTML / XML / JSON / CSV / Java）
 * 2. 模板替换（StringSubstitutor / ${} / ${prefix:}）
 * 3. 随机字符串生成（RandomStringGenerator）
 * 4. 可复用的文本构建器（TextStringBuilder）
 *
 * <p>本类演示 Commons Text 在数据清洗、配置渲染、安全编码等场景的核心能力。</p>
 */
public class CommonsTextAdvancedDemo {

    private static final Logger log = LoggerFactory.getLogger(CommonsTextAdvancedDemo.class);

    public static void main(String[] args) {
        demoStringEscape();
        demoStringSubstitutor();
        demoRandomStringGenerator();
        demoTextStringBuilder();
    }

    /**
     * 1. 字符串转义。
     *
     * <p>{@link StringEscapeUtils} 支持 HTML、XML、JSON、CSV、Java、JavaScript、EcmaScript 等
     * 多种格式转义/反转义，避免手动拼接转义表，减少 XSS 与注入风险。</p>
     */
    private static void demoStringEscape() {
        log.info("===== 1. 字符串转义 =====");

        String rawHtml = "<div class=\"title\">Hello & 你好 > 世界</div>";
        String escapedHtml = StringEscapeUtils.escapeHtml4(rawHtml);
        String unescapedHtml = StringEscapeUtils.unescapeHtml4(escapedHtml);

        String rawXml = "<user><name>Tom & Jerry</name></user>";
        String escapedXml = StringEscapeUtils.escapeXml11(rawXml);

        String rawJson = "{\"name\":\"张三\",\"desc\":\"Line1\\nLine2\"}";
        String escapedJson = StringEscapeUtils.escapeJson(rawJson);
        String unescapedJson = StringEscapeUtils.unescapeJson(escapedJson);

        String rawJava = "路径: C:\\Users\\admin\\file\tname\"quote\"";
        String escapedJava = StringEscapeUtils.escapeJava(rawJava);

        String rawCsv = "Tom, Jerry, \"mouse\"";
        String escapedCsv = StringEscapeUtils.escapeCsv(rawCsv);

        log.info("原始 HTML: {}", rawHtml);
        log.info("转义 HTML: {}", escapedHtml);
        log.info("还原 HTML: {}", unescapedHtml);
        log.info("原始 XML: {}", rawXml);
        log.info("转义 XML11: {}", escapedXml);
        log.info("原始 JSON: {}", rawJson);
        log.info("转义 JSON: {}", escapedJson);
        log.info("还原 JSON: {}", unescapedJson);
        log.info("原始 Java: {}", rawJava);
        log.info("转义 Java: {}", escapedJava);
        log.info("原始 CSV: {}", rawCsv);
        log.info("转义 CSV: {}", escapedCsv);
    }

    /**
     * 2. 模板替换。
     *
     * <p>{@link StringSubstitutor} 是 Commons Text 提供的轻量级模板引擎，
     * 支持 ${key}、$simple、${prefix:key} 等变量语法，可自定义前缀/后缀与默认值，
     * 适合配置文件、SQL 模板、邮件/短信模板等场景。</p>
     */
    private static void demoStringSubstitutor() {
        log.info("===== 2. 模板替换 =====");

        Map<String, String> params = new HashMap<String, String>();
        params.put("userName", "张三");
        params.put("orderId", "ORD-20260909-001");
        params.put("amount", "199.00");

        String template = "尊敬的 ${userName}，您的订单 ${orderId} 已支付成功，金额 ¥${amount}。";
        StringSubstitutor substitutor = new StringSubstitutor(params);
        String result = substitutor.replace(template);
        log.info("模板: {}", template);
        log.info("渲染结果: {}", result);

        // 默认值：变量不存在时使用默认值
        String templateWithDefault = "欢迎 ${userName:${defaultName}}，当前时间 ${now:--}。";
        Map<String, String> params2 = new HashMap<String, String>();
        params2.put("defaultName", "访客");
        StringSubstitutor withDefault = new StringSubstitutor(params2);
        String result2 = withDefault.replace(templateWithDefault);
        log.info("带默认值模板: {}", templateWithDefault);
        log.info("带默认值结果: {}", result2);

        // 使用自定义前缀/后缀，避免与 SQL/MyBatis 的 #{} 冲突
        String myBatisStyle = "SELECT * FROM user WHERE name = @{userName} AND status = @{status};";
        Map<String, String> sqlParams = new HashMap<String, String>();
        sqlParams.put("userName", "admin");
        sqlParams.put("status", "1");
        StringSubstitutor custom = new StringSubstitutor(sqlParams, "@{", "}");
        String sqlResult = custom.replace(myBatisStyle);
        log.info("SQL 模板: {}", myBatisStyle);
        log.info("SQL 渲染: {}", sqlResult);
    }

    /**
     * 3. 随机字符串生成。
     *
     * <p>{@link RandomStringGenerator} 提供可配置的随机字符生成能力，
     * 支持指定字符范围、长度范围、是否包含字母/数字/特殊字符，适合生成邀请码、验证码、测试数据。</p>
     */
    private static void demoRandomStringGenerator() {
        log.info("===== 3. 随机字符串生成 =====");

        // 8-12 位大小写字母+数字的随机字符串
        RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange('0', 'z')
                .filteredBy(Character::isLetterOrDigit)
                .build();
        for (int i = 0; i < 3; i++) {
            log.info("随机码[{}]: {}", i, generator.generate(10));
        }

        // 纯数字 6 位验证码
        RandomStringGenerator numeric = new RandomStringGenerator.Builder()
                .withinRange('0', '9')
                .build();
        log.info("6 位数字验证码: {}", numeric.generate(6));

        // 仅小写字母，固定 8 位，适合短链/邀请码
        RandomStringGenerator lowercase = new RandomStringGenerator.Builder()
                .withinRange('a', 'z')
                .build();
        log.info("8 位小写邀请码: {}", lowercase.generate(8));
    }

    /**
     * 4. 可复用文本构建器。
     *
     * <p>{@link TextStringBuilder} 是对 {@link StringBuilder} 的增强，支持
     * 替换、删除、插入、清空、自动扩容等操作，并提供可读性更好的 API。</p>
     */
    private static void demoTextStringBuilder() {
        log.info("===== 4. 可复用文本构建器 =====");

        TextStringBuilder builder = new TextStringBuilder();
        builder.append("Hello, ")
                .append("Commons Text! ")
                .append("We love Java.");
        log.info("初始内容: {}", builder);

        // 替换所有匹配文本
        builder.replaceAll("Java", "Text Processing");
        log.info("替换后: {}", builder);

        // 删除子串
        builder.deleteFirst("We love ");
        log.info("删除后: {}", builder);

        // 清空并复用
        builder.clear();
        builder.appendln("第一行").appendln("第二行").appendln("第三行");
        log.info("复用后内容:\n{}", builder);
    }
}
