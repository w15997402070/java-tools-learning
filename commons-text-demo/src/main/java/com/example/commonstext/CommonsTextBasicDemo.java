package com.example.commonstext;

import org.apache.commons.text.CaseUtils;
import org.apache.commons.text.WordUtils;
import org.apache.commons.text.similarity.CosineDistance;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.commons.text.similarity.HammingDistance;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Apache Commons Text 基础演示。
 *
 * 涵盖：
 * 1. 字符串相似度计算（Levenshtein / Jaro-Winkler / Hamming / Cosine / Fuzzy）
 * 2. 文本大小写转换（camelCase / snake_case / 首字母大写）
 * 3. 文本格式化（WordUtils 首字母大写、缩写、换行）
 *
 * <p>Commons Text 是 Apache 官方提供的文本处理工具箱，定位在 Commons Lang3 之上，
 * 专注于文本算法、转义、模板替换、随机字符串等高级文本操作，0 依赖，Java 8 兼容。</p>
 */
public class CommonsTextBasicDemo {

    private static final Logger log = LoggerFactory.getLogger(CommonsTextBasicDemo.class);

    public static void main(String[] args) {
        demoStringSimilarity();
        demoCaseUtils();
        demoWordUtils();
    }

    /**
     * 1. 字符串相似度计算。
     *
     * <p>Commons Text 提供多种经典相似度/距离算法，可用于：</p>
     * <ul>
     *   <li>搜索联想与拼写纠错</li>
     *   <li>数据去重与合并</li>
     *   <li>姓名/地址 fuzzy match</li>
     *   <li>测试数据相似性分析</li>
     * </ul>
     */
    private static void demoStringSimilarity() {
        log.info("===== 1. 字符串相似度计算 =====");

        String source = "Apache Commons Text";
        String candidate1 = "Apache Commons Test";
        String candidate2 = "commons-text apache";
        String candidate3 = "完全无关的字符串";

        // Levenshtein 距离：编辑距离，越小越相似
        LevenshteinDistance levenshtein = new LevenshteinDistance();
        Integer ld1 = levenshtein.apply(source, candidate1);
        Integer ld2 = levenshtein.apply(source, candidate2);
        Integer ld3 = levenshtein.apply(source, candidate3);

        // Jaro-Winkler 相似度：0.0 ~ 1.0，越大越相似，对前缀匹配友好
        JaroWinklerDistance jaroWinkler = new JaroWinklerDistance();
        Double jw1 = jaroWinkler.apply(source, candidate1);
        Double jw2 = jaroWinkler.apply(source, candidate2);
        Double jw3 = jaroWinkler.apply(source, candidate3);

        // Hamming 距离：仅适用于等长字符串，统计不同字符个数
        HammingDistance hamming = new HammingDistance();
        String a = "abcdef";
        String b = "abCdeF";
        Integer hammingDistance = hamming.apply(a, b);

        // Cosine 距离：基于词频向量，适合较长文本的语义相似度
        CosineDistance cosine = new CosineDistance();
        Double cd1 = cosine.apply(source.toLowerCase(Locale.ROOT), candidate1.toLowerCase(Locale.ROOT));
        Double cd2 = cosine.apply(source.toLowerCase(Locale.ROOT), candidate3);

        // Fuzzy Score：简单打分，字符顺序匹配程度
        FuzzyScore fuzzyScore = new FuzzyScore(Locale.ROOT);
        Integer fs1 = fuzzyScore.fuzzyScore(source, "act");
        Integer fs2 = fuzzyScore.fuzzyScore(source, "text");

        log.info("源字符串: {}", source);
        log.info("候选 1: {}", candidate1);
        log.info("  Levenshtein 距离: {}", ld1);
        log.info("  Jaro-Winkler 相似度: {}", jw1);
        log.info("  Cosine 距离: {}", cd1);
        log.info("候选 2: {}", candidate2);
        log.info("  Levenshtein 距离: {}", ld2);
        log.info("  Jaro-Winkler 相似度: {}", jw2);
        log.info("候选 3: {}", candidate3);
        log.info("  Levenshtein 距离: {}", ld3);
        log.info("  Jaro-Winkler 相似度: {}", jw3);
        log.info("  Cosine 距离: {}", cd2);
        log.info("Hamming 距离 ({} vs {}): {}", a, b, hammingDistance);
        log.info("Fuzzy Score '{}' in '{}': {}", "act", source, fs1);
        log.info("Fuzzy Score '{}' in '{}': {}", "text", source, fs2);
    }

    /**
     * 2. 文本大小写转换。
     *
     * <p>{@link CaseUtils} 提供从各种分隔文本到 camelCase 的转换，
     * 是处理配置项、数据库字段名、JSON 属性名的利器。</p>
     */
    private static void demoCaseUtils() {
        log.info("===== 2. 文本大小写转换 =====");

        // toCamelCase：将 snake_case / kebab-case / 空格分隔 转成 camelCase
        String camel1 = CaseUtils.toCamelCase("user_name", false, '_');
        String camel2 = CaseUtils.toCamelCase("order-item-id", true, '-');
        String camel3 = CaseUtils.toCamelCase("hello world commons text", false, ' ');

        // 手动首字母大写/小写，配合 WordUtils 使用
        String title = "hello world";
        String upperFirst = WordUtils.capitalize(title);
        String lowerFirst = WordUtils.uncapitalize("HelloWorld");

        // swapCase：大小写互换
        String swapped = WordUtils.swapCase("Apache Commons Text");

        log.info("snake_case -> camelCase: {}", camel1);
        log.info("kebab-case -> CamelCase: {}", camel2);
        log.info("空格分隔 -> camelCase: {}", camel3);
        log.info("首字母大写: {}", upperFirst);
        log.info("首字母小写: {}", lowerFirst);
        log.info("大小写互换: {}", swapped);
    }

    /**
     * 3. 文本格式化。
     *
     * <p>{@link WordUtils} 提供段落格式化、首字母大写、缩写、换行等功能，
     * 常用于生成报告标题、用户昵称格式化、长文本折叠展示。</p>
     */
    private static void demoWordUtils() {
        log.info("===== 3. 文本格式化 =====");

        String text = "apache commons text is a library focused on algorithms for processing strings and other text.";

        // 每个单词首字母大写
        String capitalized = WordUtils.capitalize(text);
        // 仅首句首字母大写
        String fully = WordUtils.capitalizeFully(text);
        // 初始化（首字母缩写）
        String initials = WordUtils.initials("John Fitzgerald Kennedy");
        // 按指定宽度换行
        String wrapped = WordUtils.wrap(text, 30, "\n", true);

        log.info("原始文本: {}", text);
        log.info("capitalize: {}", capitalized);
        log.info("capitalizeFully: {}", fully);
        log.info("initials: {}", initials);
        log.info("wrapped (width=30):\n{}", wrapped);
    }
}
