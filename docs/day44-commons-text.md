# Day 44: Apache Commons Text — Java 文本处理工具箱

## 一、工具简介

**Apache Commons Text** 是 Apache 官方出品的文本处理工具库，专注于字符串算法、转义、模板替换、随机字符串生成、文本格式化等高级文本操作。它与 Commons Lang3 形成互补：Lang3 负责通用字符串工具，Commons Text 负责更专业的文本算法与处理。

- **GitHub**: https://github.com/apache/commons-text
- **官网文档**: https://commons.apache.org/proper/commons-text/
- **Maven Central**: https://central.sonatype.com/artifact/org.apache.commons/commons-text
- **星标**: 700+（Apache 官方项目，Commons 系列重要成员）
- **当前最新稳定版**: **1.11.0**（Java 8 兼容；1.12+ 起可能要求 Java 11+）
- **License**: Apache License 2.0
- **维护团队**: Apache Commons Team
- **首次发布**: 2016 年

### 为什么选择 Commons Text？

| 场景 | JDK 内置 | 手写工具 | Commons Text |
| --- | --- | --- | --- |
| 字符串相似度 | 无 | 复杂易错 | ✅ Levenshtein / Jaro-Winkler / Cosine 等 |
| HTML/XML/JSON 转义 | 无通用工具 | 需维护映射表 | ✅ 一行方法，覆盖多种格式 |
| 模板变量替换 | 无 | 需正则/自研引擎 | ✅ StringSubstitutor，轻量模板 |
| 随机字符串生成 | `Random` + 手写 | 繁琐 | ✅ RandomStringGenerator |
| 文本格式化（首字母/换行） | 无 | 繁琐 | ✅ WordUtils / CaseUtils |
| 0 依赖 | — | ✅ | ✅（自身 0 依赖） |

**结论**：Commons Text 是 Java 后端处理文本算法、数据清洗、配置渲染、安全转义的利器，尤其适合搜索推荐、CRM 去重、日志脱敏、模板消息等场景。

### 核心能力图谱

```
字符串相似度
├── LevenshteinDistance（编辑距离）
├── JaroWinklerDistance（适合短字符串/人名）
├── HammingDistance（等长字符串差异）
├── CosineDistance（词频向量相似度）
└── FuzzyScore（字符顺序匹配打分）

转义与编码
├── StringEscapeUtils.escapeHtml4 / unescapeHtml4
├── StringEscapeUtils.escapeXml10 / escapeXml11
├── StringEscapeUtils.escapeJson / unescapeJson
├── StringEscapeUtils.escapeJava / escapeCsv
└── StringEscapeUtils.escapeEcmaScript / escapeJavaScript

模板与替换
├── StringSubstitutor（${key} / ${key:default} / 自定义前缀后缀）
├── TextStringBuilder（增强版 StringBuilder）
└── 与 Commons Lang3 StrSubstitutor 的增强版

生成与格式化
├── RandomStringGenerator（密码/验证码/邀请码）
├── WordUtils（capitalize / wrap / initials / swapCase）
└── CaseUtils（toCamelCase）
```

## 二、Maven 依赖配置

```xml
<properties>
    <commons.text.version>1.11.0</commons.text.version>
</properties>

<dependencies>
    <!-- Apache Commons Text 核心库（0 传递依赖） -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-text</artifactId>
        <version>${commons.text.version}</version>
    </dependency>
</dependencies>
```

**注意**：
- commons-text 1.11.x 是 **0 依赖** 的，不依赖 commons-lang3 / commons-io
- 1.11.x 要求 Java 8+，1.12 之后可能要求 Java 11+
- Java 8 项目请锁定到 **1.11.0**
- 实际项目中通常会同时引入 `commons-lang3` 配合使用

## 三、核心 API 速览

### 3.1 字符串相似度

```java
import org.apache.commons.text.similarity.*;

String s1 = "Apache Commons Text";
String s2 = "Apache Commons Test";

// Levenshtein 编辑距离：越小越相似
LevenshteinDistance ld = new LevenshteinDistance();
int distance = ld.apply(s1, s2); // 1

// Jaro-Winkler 相似度：0.0 ~ 1.0，越大越相似
JaroWinklerDistance jw = new JaroWinklerDistance();
double similarity = jw.apply(s1, s2); // 接近 1.0

// Hamming 距离：仅适用于等长字符串
HammingDistance hamming = new HammingDistance();
int hammingDistance = hamming.apply("abcdef", "abCdeF"); // 2

// Cosine 距离：基于词频向量，适合较长文本
CosineDistance cosine = new CosineDistance();
double cosineDistance = cosine.apply(s1.toLowerCase(), s2.toLowerCase());

// Fuzzy Score：字符顺序匹配打分
FuzzyScore fuzzy = new FuzzyScore(Locale.ROOT);
int score = fuzzy.fuzzyScore(s1, "act");
```

### 3.2 字符串转义

```java
import org.apache.commons.text.StringEscapeUtils;

// HTML 转义
String html = "<div>Hello & 你好</div>";
String escaped = StringEscapeUtils.escapeHtml4(html);
String restored = StringEscapeUtils.unescapeHtml4(escaped);

// JSON 转义
String json = "{\"name\":\"张三\"}";
String escapedJson = StringEscapeUtils.escapeJson(json);

// Java 字符串转义
String java = "C:\\Users\\admin\\file\"quote\"";
String escapedJava = StringEscapeUtils.escapeJava(java);

// CSV 转义
String csv = "Tom, Jerry, \"mouse\"";
String escapedCsv = StringEscapeUtils.escapeCsv(csv);
```

### 3.3 模板变量替换

```java
import org.apache.commons.text.StringSubstitutor;
import java.util.HashMap;
import java.util.Map;

Map<String, String> params = new HashMap<>();
params.put("userName", "张三");
params.put("orderId", "ORD-20260909-001");
params.put("amount", "199.00");

String template = "尊敬的 ${userName}，您的订单 ${orderId} 已支付 ¥${amount}。";
StringSubstitutor substitutor = new StringSubstitutor(params);
String result = substitutor.replace(template);

// 带默认值
String withDefault = "时间：${now:--}";
substitutor.replace(withDefault); // 时间：--

// 自定义前缀/后缀，避免与 MyBatis #{} 冲突
StringSubstitutor custom = new StringSubstitutor(params, "@{", "}");
custom.replace("SELECT * FROM user WHERE name = @{userName}");
```

### 3.4 随机字符串生成

```java
import org.apache.commons.text.RandomStringGenerator;

// 10 位大小写字母+数字
RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withinRange('0', 'z')
        .filteredBy(Character::isLetterOrDigit)
        .build();
String code = generator.generate(10);

// 6 位纯数字验证码
RandomStringGenerator numeric = new RandomStringGenerator.Builder()
        .withinRange('0', '9')
        .build();
String verifyCode = numeric.generate(6);

// 8 位小写字母邀请码
RandomStringGenerator lowercase = new RandomStringGenerator.Builder()
        .withinRange('a', 'z')
        .build();
String inviteCode = lowercase.generate(8);
```

### 3.5 文本格式化

```java
import org.apache.commons.text.CaseUtils;
import org.apache.commons.text.WordUtils;

// camelCase 转换
String camel = CaseUtils.toCamelCase("user_name", false, '_'); // userName
String camelUpper = CaseUtils.toCamelCase("order-item-id", true, '-'); // OrderItemId

// 首字母大写/小写/互换
String title = WordUtils.capitalize("hello world");       // "Hello World"
String lower = WordUtils.uncapitalize("HelloWorld");       // "helloWorld"
String swapped = WordUtils.swapCase("Apache Commons Text"); // "aPACHE cOMMONS tEXT"

// 首字母缩写
String initials = WordUtils.initials("John Fitzgerald Kennedy"); // "JFK"

// 按宽度换行
String wrapped = WordUtils.wrap(longText, 80, "\n", true);
```

## 四、Spring Boot 集成

### 4.1 添加依赖

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-text</artifactId>
    <version>1.11.0</version>
</dependency>

<!-- 通常与 commons-lang3 一起使用 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.14.0</version>
</dependency>
```

Commons Text 没有 Spring Boot starter，也不需要任何自动配置 —— 直接在 Service/Util 中调用工具类即可。

### 4.2 封装文本处理工具类（推荐）

```java
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TextToolkit {

    private final JaroWinklerDistance jaroWinkler = new JaroWinklerDistance();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    /** 生成指定长度随机验证码/邀请码 */
    public String randomCode(int length, boolean numericOnly) {
        RandomStringGenerator generator = numericOnly
                ? new RandomStringGenerator.Builder().withinRange('0', '9').build()
                : new RandomStringGenerator.Builder()
                        .withinRange('0', 'z')
                        .filteredBy(Character::isLetterOrDigit)
                        .build();
        return generator.generate(length);
    }

    /** 渲染文本模板，支持默认值 */
    public String renderTemplate(String template, Map<String, String> params) {
        return new StringSubstitutor(params).replace(template);
    }

    /** 计算两个字符串的 Jaro-Winkler 相似度 */
    public double similarity(String a, String b) {
        return jaroWinkler.apply(a, b);
    }

    /** 手机号脱敏：138****5678 */
    public String maskPhone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() != 11) {
            return phone;
        }
        return StringUtils.overlay(phone, "****", 3, 7);
    }

    /** 输出安全的 JSON 日志字段 */
    public String safeJsonLog(String raw) {
        return StringEscapeUtils.escapeJson(raw);
    }
}
```

### 4.3 搜索推荐与 Fuzzy Match 示例

```java
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.commons.text.similarity.JaroWinklerDistance;

import java.util.*;

@Service
public class SuggestService {

    private final JaroWinklerDistance jw = new JaroWinklerDistance();
    private final FuzzyScore fuzzy = new FuzzyScore(Locale.ROOT);

    public List<String> suggest(String keyword, List<String> candidates, int topN) {
        List<Scored> scored = new ArrayList<>();
        for (String candidate : candidates) {
            double jwScore = jw.apply(keyword.toLowerCase(), candidate.toLowerCase());
            int fuzzyScore = fuzzy.fuzzyScore(candidate, keyword);
            scored.add(new Scored(candidate, jwScore * 100 + fuzzyScore));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, scored.size()); i++) {
            result.add(scored.get(i).text);
        }
        return result;
    }

    private static class Scored {
        final String text;
        final double score;
        Scored(String text, double score) { this.text = text; this.score = score; }
    }
}
```

## 五、注意事项（Bug 风险 / 性能问题 / 使用限制）

### 5.1 安全相关

- **🔴 RandomStringGenerator 不是加密安全随机数生成器**
  - `RandomStringGenerator` 默认使用 `java.util.Random`，不适合生成密码、Token、密钥
  - 生成安全随机字符串时，必须显式传入 `java.security.SecureRandom`：
    ```java
    new RandomStringGenerator.Builder()
        .usingRandom(rng -> rng.nextInt()) // 传入 SecureRandom 的包装
        .build();
    ```
  - 或者使用 `org.apache.commons.lang3.RandomStringUtils` 的 `random(..., SecureRandom)` 重载

- **🔴 转义不是加密**
  - `escapeHtml4`、`escapeJson` 只是让文本在目标格式中安全显示，不能替代加密
  - 敏感数据仍需传输层加密（TLS）和存储加密

- **🟡 防止 XSS 需输出时转义**
  - 仅对用户输入转义不够，输出到 HTML/JS/CSS/URL 时都要按上下文转义
  - Commons Text 只提供基础转义，完整 XSS 防护建议结合 OWASP Java Encoder

### 5.2 API 与行为陷阱

- **🟡 CosineDistance 对短文本不准确**
  - 基于词频向量，短字符串分词后向量稀疏，结果可能反直觉
  - 短字符串建议优先用 Levenshtein / Jaro-Winkler

- **🟡 HammingDistance 要求字符串等长**
  - 长度不等会抛出 `IllegalArgumentException`
  - 使用前务必校验长度，或用 Levenshtein 替代

- **🟡 StringSubstitutor 默认不解析嵌套变量**
  - 如需 `${outer_${inner}}` 这种嵌套，需要开启 `setEnableSubstitutionInVariables(true)`
  - 默认只替换一层 `${key}`

- **🟡 StringSubstitutor 不自动 HTML/XML/JSON 转义**
  - 模板渲染后如需输出到 HTML，需再次调用 `StringEscapeUtils.escapeHtml4`
  - 否则可能产生 XSS 漏洞

- **🟡 CaseUtils.toCamelCase 对连续分隔符处理**
  - `user__name` 会转成 `userName`（连续下划线视为一个分隔）
  - 如需保留连续分隔符含义，需预处理

### 5.3 性能

- **🟢 字符串相似度类实例可复用**
  - `LevenshteinDistance`、`JaroWinklerDistance` 等实例创建有一定开销
  - 高频调用建议作为 Spring Bean 单例注入，避免每次 new

- **🟢 RandomStringGenerator Builder 可复用**
  - Builder 构建完成后可多次调用 `generate(length)`，无需每次重建

- **🟡 长文本 CosineDistance 注意分词开销**
  - 默认按空白字符分词，中文需额外处理
  - 超高频场景建议预分词/缓存向量

### 5.4 版本差异

| 版本 | 关键差异 |
| --- | --- |
| 1.9.x | 引入 `RandomStringGenerator` 新 API |
| 1.10.x | `StringSubstitutor` 增强，修复部分边界行为 |
| 1.11.x | Java 8 兼容的最终稳定版，推荐锁定 |
| 1.12+ | 可能要求 Java 11+，升级前需确认 JDK |

### 5.5 与 Commons Lang3 的关系

- **🟢 互补而非替代**
  - `StringUtils`、`StringEscapeUtils` 在 Lang3 和 Text 中都有同名类
  - Lang3 的 `StringEscapeUtils` 已在 3.6 后标记为 deprecated，推荐使用 Commons Text 版本
  - Lang3 的 `StrSubstitutor` 功能较弱，推荐使用 Commons Text 的 `StringSubstitutor`

## 六、运行方法

### 6.1 构建项目

```bash
cd commons-text-demo
mvn clean package -DskipTests
```

成功后会在 `target/` 下生成：
- `commons-text-demo.jar`（不含依赖）
- `dependency/` 目录（含全部依赖 JAR）

### 6.2 运行 Demo

```bash
# 复制依赖到 target/dependency
mvn dependency:copy-dependencies

# 运行基础 Demo（字符串相似度 / CaseUtils / WordUtils）
java -Dfile.encoding=UTF-8 -cp "target/commons-text-demo.jar;target/dependency/*" \
     com.example.commonstext.CommonsTextBasicDemo

# 运行进阶 Demo（转义 / 模板替换 / 随机字符串 / TextStringBuilder）
java -Dfile.encoding=UTF-8 -cp "target/commons-text-demo.jar;target/dependency/*" \
     com.example.commonstext.CommonsTextAdvancedDemo

# 运行实战 Demo（fuzzy match / 脱敏 / 配置渲染 / 密码生成 / 搜索排序）
java -Dfile.encoding=UTF-8 -cp "target/commons-text-demo.jar;target/dependency/*" \
     com.example.commonstext.CommonsTextPracticalDemo
```

> Windows PowerShell 下 classpath 分号无需转义；Git Bash 下若 `*` 不展开，请改用 `target/dependency/commons-text-1.11.0.jar;target/dependency/...` 显式列出。

## 七、今日总结

| 能力 | 对应类 | 典型场景 |
| --- | --- | --- |
| 字符串相似度 | `LevenshteinDistance` / `JaroWinklerDistance` / `CosineDistance` | 搜索推荐、姓名去重、地址匹配 |
| 文本转义 | `StringEscapeUtils` | HTML/XSS 防护、JSON 日志、CSV 导出 |
| 模板替换 | `StringSubstitutor` | 短信/邮件/推送文案、配置模板 |
| 随机字符串 | `RandomStringGenerator` | 验证码、邀请码、测试数据 |
| 文本格式化 | `CaseUtils` / `WordUtils` | 字段命名转换、报告标题、长文本换行 |
| 增强构建器 | `TextStringBuilder` | 复杂文本拼接、替换、删除 |

**一句话**：Apache Commons Text 是 Java 后端处理文本算法与数据清洗的利器，与 Commons Lang3、Commons Codec 形成完整的字符串/编码/文本处理矩阵，值得每个后端团队掌握。
