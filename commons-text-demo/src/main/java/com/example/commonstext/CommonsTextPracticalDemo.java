package com.example.commonstext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.WordUtils;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Apache Commons Text 实战演示。
 *
 * 涵盖：
 * 1. 企业姓名/地址 fuzzy match（去重/合并）
 * 2. 敏感信息脱敏（手机号/身份证/银行卡）
 * 3. 配置文件/邮件模板渲染
 * 4. 随机密码/验证码生成策略
 * 5. 简单的搜索推荐排序
 *
 * <p>本类演示 Commons Text 如何与 Commons Lang3 配合使用，解决真实业务中的文本处理问题。</p>
 */
public class CommonsTextPracticalDemo {

    private static final Logger log = LoggerFactory.getLogger(CommonsTextPracticalDemo.class);

    public static void main(String[] args) {
        demoCustomerNameDeduplication();
        demoSensitiveDataMasking();
        demoConfigTemplateRender();
        demoPasswordGenerator();
        demoSearchSuggestionRanking();
    }

    /**
     * 1. 客户姓名 fuzzy match 去重。
     *
     * <p>业务场景：CRM 系统中存在大量重复录入的客户，需要自动识别可能的重复记录。
     * 综合 Levenshtein 距离、Jaro-Winkler 相似度、字符级 Fuzzy Score 进行排序。</p>
     */
    private static void demoCustomerNameDeduplication() {
        log.info("===== 1. 客户姓名 fuzzy match 去重 =====");

        String input = "北京阿里巴巴科技有限公司";
        List<String> candidates = new ArrayList<String>();
        candidates.add("北京阿里巴巴科技有限公司");
        candidates.add("北京阿里吧吧科技有限公司");
        candidates.add("上海阿里巴巴网络技术有限公司");
        candidates.add("北京阿狸科技有限公司");
        candidates.add("深圳腾讯科技有限公司");

        LevenshteinDistance levenshtein = new LevenshteinDistance();
        JaroWinklerDistance jaroWinkler = new JaroWinklerDistance();
        FuzzyScore fuzzyScore = new FuzzyScore(Locale.SIMPLIFIED_CHINESE);

        List<MatchResult> results = new ArrayList<MatchResult>();
        for (String candidate : candidates) {
            int ld = levenshtein.apply(input, candidate);
            double jw = jaroWinkler.apply(input, candidate);
            int fs = fuzzyScore.fuzzyScore(input, candidate);
            // 简单加权得分：Levenshtein 越小越好，Jaro-Winkler 越大越好，Fuzzy 越大越好
            double score = jw * 100 + fs - ld * 0.5;
            results.add(new MatchResult(candidate, ld, jw, fs, score));
        }

        Collections.sort(results, new Comparator<MatchResult>() {
            @Override
            public int compare(MatchResult o1, MatchResult o2) {
                return Double.compare(o2.score, o1.score);
            }
        });

        log.info("输入: {}", input);
        for (MatchResult result : results) {
            log.info("候选: {} | Levenshtein={} | Jaro-Winkler={} | Fuzzy={} | 综合分={}",
                    result.candidate, result.levenshtein,
                    String.format("%.4f", result.jaroWinkler),
                    result.fuzzy,
                    String.format("%.2f", result.score));
        }
    }

    /**
     * 2. 敏感信息脱敏。
     *
     * <p>利用 {@link StringUtils#overlay(String, String, int, int)} 与 {@link StringUtils#leftPad(String, int, char)}
     * 实现手机号、身份证、银行卡号的星号遮蔽。Commons Text 的转义工具可进一步防止输出被解析。</p>
     */
    private static void demoSensitiveDataMasking() {
        log.info("===== 2. 敏感信息脱敏 =====");

        String phone = "13812345678";
        String idCard = "110101199001011234";
        String bankCard = "6222021234567890123";

        String maskedPhone = StringUtils.overlay(phone, "****", 3, 7);
        String maskedIdCard = StringUtils.left(idCard, 4) + "**********" + StringUtils.right(idCard, 4);
        String maskedBankCard = StringUtils.left(bankCard, 4) + " **** **** " + StringUtils.right(bankCard, 4);

        log.info("手机号: {} -> {}", phone, maskedPhone);
        log.info("身份证: {} -> {}", idCard, maskedIdCard);
        log.info("银行卡: {} -> {}", bankCard, maskedBankCard);

        // 对脱敏后的文本再做 JSON 转义，避免日志注入
        String logLine = "userPhone=" + maskedPhone + ", remark=Line1\nLine2";
        log.info("JSON 安全日志: {}", StringEscapeUtils.escapeJson(logLine));
    }

    /**
     * 3. 配置/邮件模板渲染。
     *
     * <p>使用 {@link StringSubstitutor} 渲染带默认值的配置模板，
     * 可用于短信、邮件、推送文案、SQL 片段等需要动态填充内容的场景。</p>
     */
    private static void demoConfigTemplateRender() {
        log.info("===== 3. 配置/邮件模板渲染 =====");

        Map<String, String> values = new HashMap<String, String>();
        values.put("appName", "智能客服平台");
        values.put("env", "生产环境");
        values.put("alertLevel", "P1");
        values.put("threshold", "95%");
        // time 未提供，使用默认值

        String smsTemplate = "【${appName}】${env}告警：${alertLevel} 级别，指标超过阈值 ${threshold}，发生时间 ${now:--}，请及时处理。";
        StringSubstitutor substitutor = new StringSubstitutor(values);
        String sms = substitutor.replace(smsTemplate);
        log.info("短信模板: {}", sms);

        String emailTemplate = "<p>尊敬的运维同学，</p>"
                + "<p>${env} 的 <b>${appName}</b> 触发 ${alertLevel} 告警，当前值 ${threshold}。</p>"
                + "<p>时间：${now:--}</p>";
        String email = substitutor.replace(emailTemplate);
        log.info("邮件 HTML 渲染:\n{}", email);
    }

    /**
     * 4. 随机密码与验证码生成策略。
     *
     * <p>使用 {@link RandomStringGenerator} 生成不同安全级别的随机字符串，
     * 可指定是否包含特殊字符、长度、字符集，适合一次性 Token、重置密码、邀请码。</p>
     */
    private static void demoPasswordGenerator() {
        log.info("===== 4. 随机密码与验证码生成策略 =====");

        // 6 位数字验证码
        RandomStringGenerator numeric = new RandomStringGenerator.Builder()
                .withinRange('0', '9')
                .build();
        log.info("6 位数字验证码: {}", numeric.generate(6));

        // 8 位字母+数字（不含特殊字符，易读）
        RandomStringGenerator readable = new RandomStringGenerator.Builder()
                .withinRange('0', 'z')
                .filteredBy(ch -> Character.isLetterOrDigit(ch) && !isAmbiguous((char) ch))
                .build();
        log.info("8 位易读随机码: {}", readable.generate(8));

        // 16 位强密码：大小写字母+数字+特殊字符
        RandomStringGenerator strong = new RandomStringGenerator.Builder()
                .withinRange(33, 122)
                .filteredBy(ch -> Character.isLetterOrDigit(ch) || "!@#$%^&*()_+-=".indexOf((char) ch) >= 0)
                .build();
        log.info("16 位强密码: {}", strong.generate(16));
    }

    /**
     * 5. 搜索推荐排序。
     *
     * <p>根据用户输入对候选词进行多维度相似度排序，选出最相关的推荐项。
     * 可替代简单的 SQL LIKE 前缀匹配，提升搜索体验。</p>
     */
    private static void demoSearchSuggestionRanking() {
        log.info("===== 5. 搜索推荐排序 =====");

        String keyword = "commons";
        List<String> suggestions = new ArrayList<String>();
        suggestions.add("Apache Commons Text");
        suggestions.add("Apache Commons Codec");
        suggestions.add("Apache Commons IO");
        suggestions.add("Google Guava");
        suggestions.add("Apache Commons Collections");

        JaroWinklerDistance jaroWinkler = new JaroWinklerDistance();
        FuzzyScore fuzzyScore = new FuzzyScore(Locale.ROOT);

        List<SuggestResult> ranked = new ArrayList<SuggestResult>();
        for (String suggestion : suggestions) {
            double jw = jaroWinkler.apply(keyword.toLowerCase(Locale.ROOT), suggestion.toLowerCase(Locale.ROOT));
            int fs = fuzzyScore.fuzzyScore(suggestion, keyword);
            ranked.add(new SuggestResult(suggestion, jw, fs));
        }

        Collections.sort(ranked, new Comparator<SuggestResult>() {
            @Override
            public int compare(SuggestResult o1, SuggestResult o2) {
                int cmp = Double.compare(o2.jaroWinkler, o1.jaroWinkler);
                if (cmp != 0) {
                    return cmp;
                }
                return Integer.compare(o2.fuzzy, o1.fuzzy);
            }
        });

        log.info("关键词: {}", keyword);
        for (SuggestResult result : ranked) {
            log.info("推荐: {} | Jaro-Winkler={} | Fuzzy={}",
                    result.suggestion, String.format("%.4f", result.jaroWinkler), result.fuzzy);
        }
    }

    private static boolean isAmbiguous(char ch) {
        return "0O1lI".indexOf(ch) >= 0;
    }

    private static class MatchResult {
        final String candidate;
        final int levenshtein;
        final double jaroWinkler;
        final int fuzzy;
        final double score;

        MatchResult(String candidate, int levenshtein, double jaroWinkler, int fuzzy, double score) {
            this.candidate = candidate;
            this.levenshtein = levenshtein;
            this.jaroWinkler = jaroWinkler;
            this.fuzzy = fuzzy;
            this.score = score;
        }
    }

    private static class SuggestResult {
        final String suggestion;
        final double jaroWinkler;
        final int fuzzy;

        SuggestResult(String suggestion, double jaroWinkler, int fuzzy) {
            this.suggestion = suggestion;
            this.jaroWinkler = jaroWinkler;
            this.fuzzy = fuzzy;
        }
    }
}
