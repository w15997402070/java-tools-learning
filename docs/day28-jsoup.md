# Day 28: Jsoup - Java HTML 解析器

## 📖 工具简介

**Jsoup** 是一个用于处理真实世界 HTML 的 Java 库。它提供了非常方便的 API 来提取和操作数据，使用 DOM、CSS 和类似 jQuery 的方法。

核心能力：
- 从 URL、文件或字符串解析 HTML
- 使用 CSS 选择器或 DOM 遍历查找和提取数据
- 操作 HTML 元素、属性和文本
- 清理用户提交的内容，防止 XSS 攻击
- 处理不完整的 HTML（容错性极强）

- **GitHub**: https://github.com/jhy/jsoup
- **官网**: https://jsoup.org
- **星标**: 11k+
- **版本**: 1.22.2 (Java 8 兼容)
- **许可证**: MIT License

## 📦 Maven 依赖配置

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.22.2</version>
</dependency>
```

**注意**：Jsoup 完全自包含，没有任何运行时依赖。

## 🚀 核心功能速览

### 1. 解析 HTML

```java
// 从字符串解析
Document doc = Jsoup.parse("<html><head><title>Hello</title></head><body><p>World</p></body></html>");

// 从 URL 加载
Document doc = Jsoup.connect("https://example.com").get();

// 从文件加载
Document doc = Jsoup.parse(new File("input.html"), "UTF-8");
```

### 2. CSS 选择器

```java
// 标签选择器
Elements links = doc.select("a");

// ID 选择器
Element header = doc.select("#header").first();

// Class 选择器
Elements items = doc.select(".item");

// 层级选择器
Elements articles = doc.select("#content .article-list li");

// 属性选择器
Elements extLinks = doc.select("a[href^=http]");  // 以 http 开头的链接
Elements pdfLinks = doc.select("a[href$=.pdf]");   // PDF 链接
Elements imgTags = doc.select("img[src]");          // 有 src 属性的图片

// 组合/伪类选择器
Elements firstItem = doc.select("li:first-child");
Elements inStock = doc.select(".product:has(.in-stock)");
```

### 3. 提取数据

```java
Element link = doc.select("a").first();

String text = link.text();           // 元素文本
String html = link.html();           // 内部 HTML
String href = link.attr("href");     // 属性值
String absUrl = link.absUrl("href"); // 绝对 URL
```

### 4. DOM 修改

```java
// 修改文本
element.text("新文本");

// 修改/添加属性
element.attr("class", "active");
element.addClass("highlighted");

// 添加元素
parent.append("<div class=\"new\">新元素</div>");
parent.prepend("<span>前置</span>");

// 删除元素
element.remove();
```

### 5. HTML 清洗（防 XSS）

```java
// 基本白名单（保留 b, em, i, strong, u, a, blockquote, br, cite, code, dd, dl, dt, h1-h6, img, li, ol, p, pre, q, small, span, strike, sub, sup, ul 等）
String clean = Jsoup.clean(userContent, Safelist.basic());

// 纯文本
String text = Jsoup.clean(userContent, Safelist.simpleText());

// 自定义白名单
Safelist custom = new Safelist()
    .addTags("div", "p", "a", "img")
    .addAttributes("a", "href")
    .addAttributes("img", "src", "alt")
    .addProtocols("a", "href", "http", "https");
String safe = Jsoup.clean(userContent, custom);
```

## 🏗️ Spring Boot 集成

### 1. 添加依赖

在 `pom.xml` 中添加 `jsoup` 依赖（同上）。

### 2. 配置类

```java
@Configuration
public class JsoupConfig {

    @Bean
    public Connection jsoupSession() {
        return Jsoup.newSession()
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .timeout(10000)
            .followRedirects(true);
    }
}
```

### 3. Service 层使用

```java
@Service
public class WebScraperService {

    @Autowired
    private Connection defaultSession;

    public List<Article> fetchArticles(String url) throws IOException {
        Document doc = defaultSession.newRequest()
            .url(url)
            .get();

        return doc.select(".article-item").stream()
            .map(el -> Article.builder()
                .title(el.select(".title").text())
                .summary(el.select(".summary").text())
                .build())
            .collect(Collectors.toList());
    }
}
```

### 4. 异步抓取

```java
@Async
public CompletableFuture<Document> fetchAsync(String url) throws IOException {
    return CompletableFuture.completedFuture(
        defaultSession.newRequest().url(url).get()
    );
}
```

### 5. HTML 清洗工具类

```java
@Component
public class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.basic()
        .addTags("div", "span", "table", "thead", "tbody", "tr", "td", "th")
        .addAttributes("a", "target", "rel");

    public String sanitize(String userHtml) {
        return Jsoup.clean(userHtml, SAFELIST);
    }
}
```

## ⚠️ 注意事项

### 1. 线程安全
- `Document`、`Element`、`Elements` 等对象**不是线程安全的**，不要跨线程共享修改操作
- `Connection` 对象也不是线程安全的，每次请求应创建新实例

### 2. 内存使用
- Jsoup 会将整个 HTML 文档解析为内存中的 DOM 树
- 对于超大 HTML（>50MB），注意内存消耗
- 可以考虑在解析前过滤掉不必要的内容

### 3. 编码处理
- 从 URL 加载时，Jsoup 会：
  1. 检查 Content-Type 头的 charset 参数
  2. 检查 HTML 中的 `<meta charset>` 标签
  3. 如果都不存在，默认使用 UTF-8
- 从字符串解析时，需确保传入的字符串编码正确

### 4. 网络请求限制
- Jsoup 的 HTTP 功能比较简单，不支持连接池、异步请求
- 对于复杂的 HTTP 需求，建议配合 OkHttp 或 Apache HttpClient 使用
- `Jsoup.connect()` 超时默认 30 秒，建议显式设置 `timeout()`

### 5. 反爬虫
- 设置合理的 `User-Agent` 避免被屏蔽
- 控制请求频率，配合 `RateLimiter` 使用
- 尊重 `robots.txt`，注意网站的爬虫协议

### 6. CSS 选择器限制
- Jsoup 使用的是 jsoup 自己的 CSS 选择器引擎，不包含浏览器中所有伪类
- 已支持：`:first-child`, `:last-child`, `:nth-child(an+b)`, `:contains(text)`, `:matches(regex)`, `:has(selector)`, `:not(selector)` 等
- 不支持：`:hover`, `:focus`, `:visited`, `::before`, `::after` 等需要浏览器渲染的伪类

### 7. HTML 清洗注意
- `Jsoup.clean()` 会去除所有不在白名单中的标签和属性
- 默认的白名单可能不够宽松或不够严格，建议根据业务需求自定义
- `Safelist.none()` 可以移除所有 HTML 标签

### 8. 版本兼容
- Jsoup 1.22.x 需要 Java 8+
- 在 Android 使用时需要启用 core library desugaring

## 📂 项目结构

```
jsoup-demo/
├── pom.xml
└── src/main/java/com/example/jsoup/
    ├── JsoupBasicDemo.java      # 基础：HTML解析、CSS选择器、元素提取
    ├── JsoupAdvancedDemo.java   # 进阶：DOM操作、表单解析、HTML清洗(XSS防护)
    └── JsoupPracticalDemo.java  # 实战：网页抓取、表格解析、HTML转文本、Spring Boot集成
```

## 🎯 运行方法

### 编译
```bash
cd jsoup-demo
mvn clean package -DskipTests
```

### 运行基础演示
```bash
mvn exec:java -Dexec.mainClass="com.example.jsoup.JsoupBasicDemo"
```

### 运行高级演示
```bash
mvn exec:java -Dexec.mainClass="com.example.jsoup.JsoupAdvancedDemo"
```

### 运行实战演示
```bash
mvn exec:java -Dexec.mainClass="com.example.jsoup.JsoupPracticalDemo"
```

## 📚 应用场景

| 场景 | 说明 | 关键 API |
|------|------|----------|
| 网页数据抓取 | 从HTML页面提取结构化数据 | `Jsoup.connect().get()`, `select()` |
| HTML清洗 | 过滤用户输入，防XSS攻击 | `Jsoup.clean()`, `Safelist` |
| 邮件模板处理 | 解析和修改HTML邮件模板 | `parse()`, `attr()`, `append()` |
| SEO分析 | 提取页面meta标签、标题、链接 | `select("meta[name]")`, `select("a[href]")` |
| 表格数据提取 | 将HTML表格转换为结构化数据 | `select("tr")`, `select("td/th")` |
| 内容提取器 | 从网页提取正文，去除广告导航 | `select("article")`, `text()` |
| 链接检查 | 验证页面链接有效性 | `select("a[href]")`, `attr("abs:href")` |

## 🔗 参考资源

- [Jsoup 官方文档](https://jsoup.org/cookbook/)
- [Jsoup API 参考](https://jsoup.org/apidocs/)
- [Jsoup CSS 选择器语法](https://jsoup.org/cookbook/extracting-data/selector-syntax)
- [Jsoup GitHub](https://github.com/jhy/jsoup)
