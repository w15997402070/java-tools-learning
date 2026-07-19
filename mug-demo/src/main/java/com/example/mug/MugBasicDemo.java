package com.example.mug;

import com.google.mu.util.Substring;
import com.google.mu.util.stream.BiStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Day 30 - Google Mug 基础演示
 *
 * 覆盖 Mug 两大核心能力：
 * 1. BiStream —— 双向流，让 Map / 成对数据的流式处理告别 entrySet() 样板代码
 * 2. Substring —— 声明式字符串提取/替换/切割，告别 indexOf + off-by-one
 *
 * 适用人群：写过 map.entrySet().stream().map(e -> Map.entry(...)) 套娃的人
 * 运行：mvn exec:java -Dexec.mainClass="com.example.mug.MugBasicDemo"
 */
public class MugBasicDemo {

    public static void main(String[] args) {
        System.out.println("===== Day 30: Google Mug 基础演示 =====\n");

        biStreamBasics();
        substringBasics();
    }

    // ======================== BiStream 基础 ========================

    /**
     * BiStream：把 Map / 成对集合当成"双值流"操作，告别 entrySet + Map.entry 样板。
     *
     * 对比 JDK 写法：
     *   map.entrySet().stream()
     *       .map(e -> Map.entry(transform(e.getKey()), e.getValue()))
     *       .filter(e -> isGood(e.getKey()))
     *       .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
     *
     * BiStream 写法：
     *   BiStream.from(map).mapKeys(this::transform).filterKeys(this::isGood).toMap();
     */
    static void biStreamBasics() {
        System.out.println("--- BiStream 基础 ---");

        // 1. 从 Map 创建 BiStream
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 90);
        scores.put("Bob", 75);
        scores.put("Charlie", 88);
        scores.put("David", 60);

        // mapKeys / filterKeys / toMap：键值转换 + 过滤，一行搞定
        Map<String, Integer> highScores = BiStream.from(scores)
                .filterValues(v -> v >= 80)
                .toMap();
        System.out.println("成绩>=80: " + highScores);

        // 2. zip：把两个 List 配对成 Map
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        List<Integer> ages = Arrays.asList(25, 30, 28);
        Map<String, Integer> nameToAge = BiStream.zip(names, ages).toMap();
        System.out.println("zip 配对: " + nameToAge);

        // 3. from(Iterable, toKey, toValue)：从对象集合抽取键值对
        List<Person> people = Arrays.asList(
                new Person("P001", "Alice", "Engineering"),
                new Person("P002", "Bob", "Sales"),
                new Person("P003", "Charlie", "Engineering"));
        Map<String, String> idToName = BiStream.from(people, Person::getId, Person::getName).toMap();
        System.out.println("对象→Map: " + idToName);

        // 4. mapValues 同时拿到 key 和 value 做计算
        Map<String, String> gradeLevel = BiStream.from(scores)
                .mapValues((name, score) -> name + ":" + gradeOf(score))
                .toMap();
        System.out.println("带评级: " + gradeLevel);

        // 5. inverse()：键值互换
        Map<Integer, String> scoreToName = BiStream.from(scores).inverse().toMap();
        System.out.println("键值互换: " + scoreToName);

        // 6. anyMatch / allMatch：成对断言
        boolean allPassed = BiStream.from(scores).allMatch((n, s) -> s >= 60);
        System.out.println("全部及格? " + allPassed);

        // 7. forEach：成对消费，无需 Entry 对象
        System.out.print("遍历: ");
        BiStream.from(scores).forEach((name, score) ->
                System.out.printf("[%s=%d] ", name, score));
        System.out.println();

        System.out.println();
    }

    // ======================== Substring 基础 ========================

    /**
     * Substring：用"模式"对象描述要找的子串，再执行提取/删除/替换/切割。
     * 可组合、可复用、自动处理边界，告别 indexOf 的 -1 检查和 off-by-one。
     */
    static void substringBasics() {
        System.out.println("--- Substring 基础 ---");

        // 1. between：提取两个标记之间的内容
        String code = "call(foo, bar)";
        Optional<String> inner = Substring.between("(", ")").from(code);
        System.out.println("between(...) = " + inner.orElse("(无)"));

        // 2. first / last：定位首尾分隔符
        String file = "home/foo/Bar.java";
        Optional<String> dir = Substring.before(Substring.last('/')).from(file);
        Optional<String> ext = Substring.after(Substring.last('.')).from(file);
        System.out.println("目录: " + dir.orElse("(无)") + ", 扩展名: " + ext.orElse("(无)"));

        // 3. prefix / suffix：移除前后缀
        String url = "http://example.com/path";
        String noScheme = Substring.prefix("http://").removeFrom(url);
        String line = "data,";
        String withComma = Substring.suffix(',').addToIfAbsent(line);
        System.out.println("移除http://: " + noScheme);
        System.out.println("确保逗号结尾: " + withComma);

        // 4. split：两路切割（返回 BiOptional，天然适合 BiStream）
        String kv = "name=Alice";
        Optional<Map.Entry<String, String>> entry = Substring.first('=').split(kv)
                .map((k, v) -> new HashMap.SimpleEntry<>(k.toString(), v.toString()));
        System.out.println("split '=': " + entry.map(e -> e.getKey() + "->" + e.getValue()).orElse("(无)"));

        // 5. repeatedly：提取所有匹配（返回 Stream，零拷贝视图）
        String template = "Hello {name}, your id is {id}";
        List<String> placeholders = Substring.between("{", "}").repeatedly()
                .from(template)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("所有占位符: " + placeholders);

        // 6. replaceAllFrom：批量替换
        String rendered = Substring.between("{", "}").repeatedly()
                .replaceAllFrom(template, ph -> {
                    if (ph.equals("name")) return "Alice";
                    if (ph.equals("id")) return "P001";
                    return ph;
                });
        System.out.println("模板渲染: " + rendered);

        // 7. all(',')：按逗号切割成 Stream（shorthand = first(',').repeatedly()）
        // split 返回 Stream<Match>，Match 是 CharSequence 视图，需 toString 再 trim
        List<String> items = Substring.all(',').split("apple, banana, cherry")
                .map(m -> m.toString().trim())
                .collect(java.util.stream.Collectors.toList());
        System.out.println("逗号分割: " + items);

        System.out.println();
    }

    // ======================== 辅助类型 ========================

    static String gradeOf(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        return "D";
    }

    static class Person {
        final String id;
        final String name;
        final String dept;

        Person(String id, String name, String dept) {
            this.id = id;
            this.name = name;
            this.dept = dept;
        }

        String getId() { return id; }
        String getName() { return name; }
        String getDept() { return dept; }

        @Override
        public String toString() {
            return id + "/" + name + "/" + dept;
        }
    }
}
