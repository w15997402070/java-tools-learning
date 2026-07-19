package com.example.mug;

import com.google.mu.util.Optionals;
import com.google.mu.util.StringFormat;
import com.google.mu.util.Substring;
import com.google.mu.util.stream.BiStream;
import com.google.mu.util.stream.MoreStreams;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Day 30 - Google Mug 进阶演示
 *
 * 覆盖：
 * 1. StringFormat —— 编译期安全的"双向"模板：同一个模板既能 format 又能 parse
 * 2. MoreStreams —— Stream 增强：连续分组、批量、索引等 Java 8 缺失能力
 * 3. Optionals  —— Optional 增强：条件构造、链式 or、Java 9+ API 的 Java 8 平替
 * 4. BiStream 进阶 —— flatMap、分组、拼接、收集到自定义结构
 *
 * 运行：mvn exec:java -Dexec.mainClass="com.example.mug.MugAdvancedDemo"
 */
public class MugAdvancedDemo {

    public static void main(String[] args) {
        System.out.println("===== Day 30: Google Mug 进阶演示 =====\n");

        stringFormatDemo();
        moreStreamsDemo();
        optionalsDemo();
        biStreamAdvancedDemo();
    }

    // ======================== StringFormat ========================

    /**
     * StringFormat：用带命名占位符 {xxx} 的模板，既可 format 也可 parse。
     * 比 String.format 更安全（不靠 %s 位置），比正则更易读。
     */
    static void stringFormatDemo() {
        System.out.println("--- StringFormat 双向模板 ---");

        // 1. parse：从字符串提取字段
        StringFormat pathFmt = new StringFormat("/home/{user}/{date}");
        String input = "/home/alice/2024-03-14";
        Optional<String> parsed = pathFmt.parse(input, (user, date) -> user + " 在 " + date);
        System.out.println("parse 路径: " + parsed.orElse("(不匹配)"));

        // 2. format：用模板生成字符串
        String generated = pathFmt.format("bob", "2024-03-15");
        System.out.println("format 路径: " + generated);

        // 3. matches：仅判断是否匹配，不提取
        boolean match = pathFmt.matches("/home/alice/2024-03-14");
        System.out.println("matches? " + match);

        // 4. 实际场景：解析日志行
        StringFormat logFmt = new StringFormat("[{level}] {ts} {msg}");
        Optional<String> level = logFmt.parse("[ERROR] 2024-03-14 10:00:00 NullPointerException at Foo.java",
                (level1, ts, msg) -> level1);
        System.out.println("日志级别: " + level.orElse("(解析失败)"));

        // 5. 解析带单位的数值
        StringFormat sizeFmt = new StringFormat("{amount}{unit}");
        Optional<Long> bytes = sizeFmt.parse("1.5GB", (amount, unit) -> {
            double n = Double.parseDouble(amount.toString());
            switch (unit.toString()) {
                case "KB": return (long) (n * 1024);
                case "MB": return (long) (n * 1024 * 1024);
                case "GB": return (long) (n * 1024 * 1024 * 1024);
                default: return (long) n;
            }
        });
        System.out.println("1.5GB = " + bytes.orElse(-1L) + " bytes");

        System.out.println();
    }

    // ======================== MoreStreams ========================

    /**
     * MoreStreams：补齐 Java 8 Stream 缺失的实用操作。
     * groupConsecutive 把"连续满足条件的元素"打包成 List。
     */
    static void moreStreamsDemo() {
        System.out.println("--- MoreStreams 增强 ---");

        // 1. groupConsecutive：把连续上涨的股价分组
        List<Double> prices = Arrays.asList(100.0, 101.0, 102.0, 99.0, 100.0, 101.0, 103.0, 95.0);
        // groupConsecutive 返回 Stream<List<T>>，需再 collect 一次
        List<List<Double>> upStreaks = MoreStreams.groupConsecutive(
                prices.stream(),
                (prev, curr) -> curr >= prev,
                Collectors.toList())
                .collect(Collectors.toList());
        System.out.println("连续上涨段: " + upStreaks);

        // 2. groupConsecutive 按字段分组：连续同部门的员工
        List<Emp> emps = Arrays.asList(
                new Emp("A", "RD"), new Emp("B", "RD"), new Emp("C", "RD"),
                new Emp("D", "Sales"), new Emp("E", "Sales"),
                new Emp("F", "RD"));
        List<List<Emp>> byDept = MoreStreams.groupConsecutive(
                emps.stream(),
                (a, b) -> a.dept.equals(b.dept),
                Collectors.toList())
                .collect(Collectors.toList());
        byDept.forEach(group ->
                System.out.println("组(" + group.get(0).dept + "): " +
                        group.stream().map(e -> e.name).collect(Collectors.joining(","))));

        System.out.println();
    }

    // ======================== Optionals ========================

    /**
     * Optionals：Java 9+ Optional API 的 Java 8 平替 + 条件构造。
     * optionally(condition, supplier) 替代 if/else 构造 Optional 的样板。
     */
    static void optionalsDemo() {
        System.out.println("--- Optionals 增强 ---");

        // 1. optionally：条件构造 Optional，替代 if/else
        User u = new User("Alice", "alice@example.com");
        Optional<String> email1 = Optionals.optionally(u.hasEmail(), u::getEmail);
        System.out.println("有邮箱: " + email1.orElse("(无)"));

        User u2 = new User("Bob", null);
        Optional<String> email2 = Optionals.optionally(u2.hasEmail(), u2::getEmail);
        System.out.println("Bob 邮箱: " + email2.orElse("(无)"));

        // 2. 条件降级：第一个为空就用第二个（Java 8 无 Optional.or，用三元判断）
        Optional<String> primary = Optional.empty();
        Optional<String> fallback = Optional.of("default@example.com");
        Optional<String> result = primary.isPresent() ? primary : fallback;
        System.out.println("or 降级: " + result.orElse("(无)"));

        System.out.println();
    }

    // ======================== BiStream 进阶 ========================

    /**
     * BiStream 进阶：flatMap 展平嵌套 Map、groupingBy 分组、concat 合并。
     */
    static void biStreamAdvancedDemo() {
        System.out.println("--- BiStream 进阶 ---");

        // 1. flatMap：展平 Map<String, Map<String, Integer>> → Map<String, Integer>
        Map<String, Map<String, Integer>> regions = new java.util.LinkedHashMap<>();
        regions.put("east", mapOf("Alice", 90, "Bob", 80));
        regions.put("west", mapOf("Charlie", 88, "David", 75));

        // 把 "east" + "Alice" 拼成 "east/Alice"
        Map<String, Integer> flat = BiStream.from(regions)
                .flatMap((region, m) -> BiStream.from(m).mapKeys(name -> region + "/" + name))
                .toMap();
        System.out.println("展平嵌套Map: " + flat);

        // 2. concat：合并两个 Map（重复键求和）
        // 注：BiStream.toMap() 无合并函数重载，用 forEach + Map.merge 处理重复键
        Map<String, Integer> base = mapOf("A", 1, "B", 2);
        Map<String, Integer> extra = mapOf("B", 20, "C", 3);
        Map<String, Integer> merged = new LinkedHashMap<>();
        BiStream.concat(base, extra).forEach((k, v) -> merged.merge(k, v, Integer::sum));
        System.out.println("concat合并(求和): " + merged);

        // 3. 分组：按部门分组员工
        List<Emp> emps = Arrays.asList(
                new Emp("Alice", "RD"), new Emp("Bob", "Sales"),
                new Emp("Charlie", "RD"), new Emp("David", "Sales"));
        // groupingBy 作为 Collector，返回 BiStream 再 toMap
        Map<String, List<String>> byDept = emps.stream()
                .collect(BiStream.groupingBy(Emp::getDept, Collectors.mapping(Emp::getName, Collectors.toList())))
                .toMap();
        System.out.println("按部门分组: " + byDept);

        // 4. 收集到自定义结构：用方法引用当 BiCollector
        // collect(Collectors::toConcurrentMap) 等价于 toMap 但线程安全
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("x", 1);
        src.put("y", 2);
        Map<String, Integer> concurrent = BiStream.from(src)
                .collect(Collectors::toConcurrentMap);
        System.out.println("toConcurrentMap: " + concurrent.getClass().getSimpleName() + " " + concurrent);

        System.out.println();
    }

    // ======================== 辅助 ========================

    @SafeVarargs
    static <K, V> Map<K, V> mapOf(Object... kvs) {
        Map<K, V> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < kvs.length; i += 2) {
            m.put((K) kvs[i], (V) kvs[i + 1]);
        }
        return m;
    }

    static class Emp {
        final String name;
        final String dept;

        Emp(String name, String dept) {
            this.name = name;
            this.dept = dept;
        }

        String getDept() { return dept; }
        String getName() { return name; }

        @Override
        public String toString() { return name + "(" + dept + ")"; }
    }

    static class User {
        final String name;
        final String email;

        User(String name, String email) {
            this.name = name;
            this.email = email;
        }

        boolean hasEmail() { return email != null && !email.isEmpty(); }
        String getEmail() { return email; }
    }
}
