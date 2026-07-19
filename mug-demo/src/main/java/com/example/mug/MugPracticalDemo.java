package com.example.mug;

import com.google.mu.util.Substring;
import com.google.mu.util.stream.BiStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Day 30 - Google Mug 实战演示
 *
 * 用 Mug 解决 4 类真实后端开发场景：
 * 1. 配置字符串解析（key=value 串 → Map）
 * 2. CSV / 日志行解析（Substring + BiStream 组合）
 * 3. Map 转换流水线（分组、展平、键值映射）
 * 4. 模板占位符渲染（repeatedly replaceAll）
 *
 * 文末附 Spring Boot 集成建议（见类注释底部）。
 *
 * 运行：mvn exec:java -Dexec.mainClass="com.example.mug.MugPracticalDemo"
 *
 * === Spring Boot 集成建议 ===
 * Mug 是纯工具库（0 依赖），无需特殊集成：
 *   1. pom.xml 引入 com.google.mug:mug:10.6 即可；
 *   2. 没有自动配置 / Starter，直接在 Service / Util 里 new 使用；
 *   3. 可封装成 @Component 工具类，对外暴露 toMap / parse / render 等方法；
 *   4. 若用 Guava，可额外引入 mug-guava，BiStream.collect(ImmutableMap::toImmutableMap) 直达；
 *   5. mug-safesql 提供 SafeSql 模板（编译期防 SQL 注入），适合 MyBatis/JDBC 场景。
 */
public class MugPracticalDemo {

    public static void main(String[] args) {
        System.out.println("===== Day 30: Google Mug 实战演示 =====\n");

        parseConfigString();
        parseCsvLogs();
        mapPipeline();
        templateRender();
    }

    // ======================== 场景1：配置字符串解析 ========================

    /**
     * 把 "host=localhost, port=3306, db=mydb, timeout=5000" 解析成 Map。
     *
     * 传统写法：split(",") 再 split("=")，需手动处理空格、空值、边界。
     * Mug 写法：all(',').splitThenTrim 切行 + first('=').split 切键值，链式直达 Map。
     */
    static void parseConfigString() {
        System.out.println("--- 场景1: 配置字符串解析 ---");

        String config = "host = localhost, port = 3306, db = mydb, timeout = 5000";

        // all(',') 按逗号切成片段，再对每段用 first('=').split 拆成 (k,v)
        // 用 BiOptional.map 转成 Map.Entry，再标准 collect 成 Map
        Map<String, String> configMap = Substring.all(',').splitThenTrim(config)
                .map(kv -> Substring.first('=').split(kv)
                        .map((k, v) -> new HashMap.SimpleEntry<String, String>(
                                k.toString().trim(), v.toString().trim()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        configMap.forEach((k, v) -> System.out.println("  " + k + " = " + v));

        // 进一步：把 timeout 转成 int
        int timeout = Integer.parseInt(configMap.get("timeout"));
        System.out.println("  timeout(int) = " + timeout);

        System.out.println();
    }

    // ======================== 场景2：CSV / 日志解析 ========================

    /**
     * 解析 Nginx 访问日志，提取状态码分布。
     * 日志形如：192.168.1.1 - - [10/Oct/2024:13:55:36] "GET /api HTTP/1.1" 200 1234
     */
    static void parseCsvLogs() {
        System.out.println("--- 场景2: 日志解析与统计 ---");

        List<String> logs = Arrays.asList(
                "192.168.1.1 - - [10/Oct/2024:13:55:36] \"GET /api HTTP/1.1\" 200 1234",
                "192.168.1.2 - - [10/Oct/2024:13:55:37] \"POST /login HTTP/1.1\" 401 89",
                "192.168.1.3 - - [10/Oct/2024:13:55:38] \"GET /api HTTP/1.1\" 200 567",
                "192.168.1.4 - - [10/Oct/2024:13:55:39] \"GET /health HTTP/1.1\" 500 0",
                "192.168.1.5 - - [10/Oct/2024:13:55:40] \"GET /api HTTP/1.1\" 200 890");

        // 用 Substring 提取请求方法和状态码：between 两个 \" 之间是请求行，后面跟状态码
        // 请求行格式：METHOD PATH PROTOCOL，取 METHOD 和状态码
        Map<String, Long> statusDist = logs.stream()
                .map(line -> {
                    // 提取 "..." 之间的请求行
                    String request = Substring.between("\"", "\"").from(line).orElse("");
                    // 提取请求行后的状态码（" 后面跟的就是状态码）
                    String status = Substring.after(Substring.last('"')).from(line)
                            .map(String::trim)
                            .map(s -> s.split("\\s+")[0])
                            .orElse("000");
                    String method = request.isEmpty() ? "?" : request.split("\\s+")[0];
                    return method + " " + status;
                })
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        System.out.println("请求方法+状态码 分布:");
        statusDist.forEach((k, v) -> System.out.println("  " + k + " : " + v + " 次"));

        // 统计状态码总数
        Map<String, Long> codeOnly = logs.stream()
                .map(line -> Substring.after(Substring.last('"')).from(line)
                        .map(String::trim)
                        .map(s -> s.split("\\s+")[0])
                        .orElse("000"))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        System.out.println("状态码分布: " + codeOnly);

        System.out.println();
    }

    // ======================== 场景3：Map 转换流水线 ========================

    /**
     * 多级 Map 展平 + 键重命名 + 过滤，一条 BiStream 链搞定。
     * 场景：把按地区分组的销售额展平成 "地区/产品 → 销售额" 的扁平 Map。
     */
    static void mapPipeline() {
        System.out.println("--- 场景3: Map 转换流水线 ---");

        Map<String, Map<String, Long>> salesByRegion = new LinkedHashMap<>();
        salesByRegion.put("华东", regionMap("手机", 1200L, "电脑", 3500L, "耳机", 800L));
        salesByRegion.put("华北", regionMap("手机", 900L, "电脑", 2100L, "平板", 600L));
        salesByRegion.put("华南", regionMap("手机", 1500L, "电脑", 4000L, "耳机", 1200L));

        // 展平 + 只保留销售额>=1000 的产品 + 键格式化为 "地区/产品"
        Map<String, Long> flat = BiStream.from(salesByRegion)
                .flatMap((region, products) ->
                        BiStream.from(products)
                                .filterValues(v -> v >= 1000)
                                .mapKeys(product -> region + "/" + product))
                .toMap();

        System.out.println("高销售额产品(>=1000):");
        flat.forEach((k, v) -> System.out.println("  " + k + " : " + v));

        // 反向：按产品汇总各地区的总销售额
        // 注：BiStream.toMap() 无合并函数重载，用 forEach + Map.merge 处理重复键
        Map<String, Long> byProduct = new LinkedHashMap<>();
        BiStream.from(salesByRegion)
                .flatMap((region, products) -> BiStream.from(products))
                .forEach((k, v) -> byProduct.merge(k, v, Long::sum));

        System.out.println("按产品汇总:");
        byProduct.forEach((k, v) -> System.out.println("  " + k + " : " + v));

        System.out.println();
    }

    // ======================== 场景4：模板渲染 ========================

    /**
     * 简易模板引擎：把 {name} {id} 占位符替换成实际值。
     * Mug 的 repeatedly().replaceAllFrom 是零拷贝视图，性能优于多次字符串拼接。
     */
    static void templateRender() {
        System.out.println("--- 场景4: 模板渲染 ---");

        String template = "亲爱的 {name}，您的订单 {orderId}（金额 {amount} 元）已于 {date} 发货，物流单号 {trackingNo}。";

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("name", "张三");
        vars.put("orderId", "ORD-20240314-001");
        vars.put("amount", "299.00");
        vars.put("date", "2024-03-14");
        vars.put("trackingNo", "SF1234567890");

        // 用 Substring.between("{","}").repeatedly().replaceAllFrom 批量替换
        String rendered = Substring.between("{", "}").repeatedly()
                .replaceAllFrom(template, placeholder -> vars.getOrDefault(placeholder, "{" + placeholder + "}"));

        System.out.println("渲染结果: " + rendered);

        // 提取所有占位符（校验模板变量是否齐全）
        List<String> placeholders = Substring.between("{", "}").repeatedly()
                .from(template)
                .collect(Collectors.toList());
        System.out.println("模板占位符: " + placeholders);

        List<String> missing = placeholders.stream()
                .filter(p -> !vars.containsKey(p))
                .collect(Collectors.toList());
        System.out.println("缺失变量: " + (missing.isEmpty() ? "无" : missing));

        System.out.println();
    }

    // ======================== 辅助 ========================

    @SafeVarargs
    static Map<String, Long> regionMap(Object... kvs) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kvs.length; i += 2) {
            m.put((String) kvs[i], (Long) kvs[i + 1]);
        }
        return m;
    }
}
