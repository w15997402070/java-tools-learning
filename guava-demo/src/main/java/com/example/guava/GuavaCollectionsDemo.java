package com.example.guava;

import com.google.common.collect.*;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.*;

/**
 * GuavaCollectionsDemo - Guava 集合工具演示
 *
 * 演示以下功能：
 * 1. ImmutableList/Set/Map - 不可变集合
 * 2. Multimap - 一个 key 对应多个 value
 * 3. BiMap - 双向 Map（key-value 互查）
 * 4. Multiset - 带计数的 Set
 * 5. Table - 二维表格结构
 * 6. Joiner/Splitter - 字符串连接与分割
 * 7. Range - 区间工具
 */
public class GuavaCollectionsDemo {

    public static void main(String[] args) {
        demoImmutableCollections();
        demoMultimap();
        demoBiMap();
        demoMultiset();
        demoTable();
        demoJoinerAndSplitter();
        demoRange();
    }

    /**
     * 演示 Guava 不可变集合
     * 不可变集合线程安全，且拒绝任何修改操作（add/remove 会抛异常）
     */
    private static void demoImmutableCollections() {
        System.out.println("========== 1. 不可变集合 ==========");

        // ImmutableList - 不可变列表
        ImmutableList<String> fruits = ImmutableList.of("苹果", "香蕉", "橙子", "草莓");
        System.out.println("不可变列表: " + fruits);

        // ImmutableSet - 不可变集合（自动去重）
        ImmutableSet<Integer> numbers = ImmutableSet.of(1, 2, 3, 2, 1);
        System.out.println("不可变集合（去重）: " + numbers);

        // ImmutableMap - 不可变 Map
        ImmutableMap<String, Integer> scores = ImmutableMap.of(
                "Alice", 95,
                "Bob",   87,
                "Carol", 92
        );
        System.out.println("不可变 Map: " + scores);

        // ImmutableList.Builder - 构建器模式
        ImmutableList<String> built = ImmutableList.<String>builder()
                .add("Java")
                .add("Python")
                .addAll(Arrays.asList("Go", "Rust"))
                .build();
        System.out.println("Builder 构建的不可变列表: " + built);

        // 尝试修改会抛出 UnsupportedOperationException
        try {
            fruits.add("西瓜");
        } catch (UnsupportedOperationException e) {
            System.out.println("捕获异常：不可变集合不允许修改 - " + e.getClass().getSimpleName());
        }

        System.out.println();
    }

    /**
     * 演示 Multimap - 一个 key 可以对应多个 value
     * 解决了 Map<K, List<V>> 的冗余代码问题
     */
    private static void demoMultimap() {
        System.out.println("========== 2. Multimap ==========");

        // ArrayListMultimap - 每个 key 对应一个 ArrayList
        ArrayListMultimap<String, String> courseStudents = ArrayListMultimap.create();
        courseStudents.put("Java课", "张三");
        courseStudents.put("Java课", "李四");
        courseStudents.put("Java课", "王五");
        courseStudents.put("Python课", "赵六");
        courseStudents.put("Python课", "张三");  // 张三同时学两门课

        System.out.println("Java课学生: " + courseStudents.get("Java课"));
        System.out.println("Python课学生: " + courseStudents.get("Python课"));
        System.out.println("所有课程条目数: " + courseStudents.size());
        System.out.println("课程数（唯一 key）: " + courseStudents.keySet().size());

        // 转换为普通 Map<K, Collection<V>>
        Map<String, Collection<String>> asMap = courseStudents.asMap();
        System.out.println("转为普通 Map: " + asMap);

        // HashMultimap - 值是 HashSet，自动去重
        HashMultimap<String, String> tags = HashMultimap.create();
        tags.put("文章1", "Java");
        tags.put("文章1", "教程");
        tags.put("文章1", "Java");  // 重复，会被去重
        System.out.println("HashMultimap 自动去重: " + tags.get("文章1"));

        System.out.println();
    }

    /**
     * 演示 BiMap - 双向 Map，key 和 value 都唯一
     * 可以通过 value 反查 key
     */
    private static void demoBiMap() {
        System.out.println("========== 3. BiMap（双向 Map）==========");

        HashBiMap<String, Integer> userIds = HashBiMap.create();
        userIds.put("alice", 1001);
        userIds.put("bob",   1002);
        userIds.put("carol", 1003);

        System.out.println("通过 username 查 id: " + userIds.get("alice"));
        System.out.println("通过 id 查 username: " + userIds.inverse().get(1002));

        // value 唯一性：放入已有 value 会抛异常
        try {
            userIds.put("dave", 1001);  // 1001 已被 alice 占用
        } catch (IllegalArgumentException e) {
            System.out.println("BiMap value 重复异常: " + e.getMessage());
        }

        // forcePut 会强制替换
        userIds.forcePut("dave", 1001);
        System.out.println("forcePut 后，id=1001 对应: " + userIds.inverse().get(1001));

        System.out.println();
    }

    /**
     * 演示 Multiset - 带计数功能的 Set
     * 适合统计元素出现次数
     */
    private static void demoMultiset() {
        System.out.println("========== 4. Multiset（计数集合）==========");

        HashMultiset<String> wordCount = HashMultiset.create();

        // 模拟词频统计
        String[] words = {"java", "python", "java", "go", "java", "python", "rust"};
        for (String word : words) {
            wordCount.add(word);
        }

        System.out.println("java 出现次数: " + wordCount.count("java"));
        System.out.println("python 出现次数: " + wordCount.count("python"));
        System.out.println("go 出现次数: " + wordCount.count("go"));
        System.out.println("总词数: " + wordCount.size());
        System.out.println("唯一词数: " + wordCount.elementSet().size());

        // 直接设置计数
        wordCount.setCount("rust", 5);
        System.out.println("设置 rust 计数后: " + wordCount.count("rust"));

        // 按出现次数排序（借助 Multisets 工具）
        System.out.println("所有词及计数: " + wordCount);

        System.out.println();
    }

    /**
     * 演示 Table - 二维表格（row + column -> value）
     * 相当于 Map<R, Map<C, V>>，但使用更简洁
     */
    private static void demoTable() {
        System.out.println("========== 5. Table（二维表格）==========");

        // 创建成绩表：学生 x 科目 -> 分数
        Table<String, String, Integer> gradeTable = HashBasedTable.create();
        gradeTable.put("张三", "语文", 85);
        gradeTable.put("张三", "数学", 92);
        gradeTable.put("张三", "英语", 78);
        gradeTable.put("李四", "语文", 90);
        gradeTable.put("李四", "数学", 88);
        gradeTable.put("李四", "英语", 95);

        // 按 row 查
        System.out.println("张三的所有成绩: " + gradeTable.row("张三"));
        // 按 column 查
        System.out.println("数学所有人成绩: " + gradeTable.column("数学"));
        // 精确查询
        System.out.println("李四英语成绩: " + gradeTable.get("李四", "英语"));

        // 遍历所有 Cell
        System.out.println("所有成绩:");
        for (Table.Cell<String, String, Integer> cell : gradeTable.cellSet()) {
            System.out.printf("  %s - %s: %d%n", cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }

        System.out.println();
    }

    /**
     * 演示 Joiner 和 Splitter
     * 比 String.join / split 更灵活，支持 null 处理、Map 连接等
     */
    private static void demoJoinerAndSplitter() {
        System.out.println("========== 6. Joiner & Splitter ==========");

        // Joiner 基本用法
        String joined = Joiner.on(", ").join("Java", "Python", "Go");
        System.out.println("基本 join: " + joined);

        // 跳过 null
        String withNull = Joiner.on(", ").skipNulls().join("Java", null, "Go", null, "Rust");
        System.out.println("跳过 null: " + withNull);

        // 替换 null
        String replaceNull = Joiner.on(", ").useForNull("N/A").join("Java", null, "Go");
        System.out.println("替换 null: " + replaceNull);

        // Map Joiner
        Map<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3);
        String mapStr = Joiner.on("; ").withKeyValueSeparator("=").join(map);
        System.out.println("Map join: " + mapStr);

        System.out.println();

        // Splitter 基本用法
        List<String> parts = Splitter.on(",").splitToList("a,b,c,d");
        System.out.println("基本 split: " + parts);

        // 去除前后空格
        List<String> trimmed = Splitter.on(",").trimResults().splitToList("  a , b , c ");
        System.out.println("trimResults: " + trimmed);

        // 忽略空字符串
        List<String> noEmpty = Splitter.on(",").omitEmptyStrings().splitToList("a,,b,,c");
        System.out.println("omitEmptyStrings: " + noEmpty);

        // 按固定长度分割
        List<String> fixedLen = Splitter.fixedLength(3).splitToList("abcdefghi");
        System.out.println("fixedLength(3): " + fixedLen);

        // 分割为 Map
        Map<String, String> splitMap = Splitter.on(";").withKeyValueSeparator("=").split("name=Alice;age=25;city=Beijing");
        System.out.println("split 为 Map: " + splitMap);

        System.out.println();
    }

    /**
     * 演示 Range - 区间/范围工具
     * 支持开区间、闭区间、无穷区间
     */
    private static void demoRange() {
        System.out.println("========== 7. Range（区间工具）==========");

        // 闭区间 [1, 10]
        Range<Integer> closed = Range.closed(1, 10);
        System.out.println("闭区间 [1,10]: " + closed);
        System.out.println("5 在闭区间内: " + closed.contains(5));
        System.out.println("10 在闭区间内: " + closed.contains(10));
        System.out.println("11 在闭区间内: " + closed.contains(11));

        // 开区间 (1, 10)
        Range<Integer> open = Range.open(1, 10);
        System.out.println("开区间 (1,10): " + open);
        System.out.println("1 在开区间内: " + open.contains(1));

        // 左闭右开 [1, 10)
        Range<Integer> closedOpen = Range.closedOpen(1, 10);
        System.out.println("半开区间 [1,10): " + closedOpen);

        // 无穷区间
        Range<Integer> atLeast = Range.atLeast(5);
        System.out.println(">=5: " + atLeast + ", 100 在内: " + atLeast.contains(100));

        Range<Integer> lessThan = Range.lessThan(5);
        System.out.println("<5: " + lessThan + ", 4 在内: " + lessThan.contains(4));

        // 区间运算
        Range<Integer> r1 = Range.closed(1, 5);
        Range<Integer> r2 = Range.closed(3, 8);
        System.out.println("r1 与 r2 是否连通: " + r1.isConnected(r2));
        System.out.println("r1 与 r2 的交集: " + r1.intersection(r2));
        System.out.println("r1 与 r2 的跨度: " + r1.span(r2));

        // 过滤集合中符合区间的元素
        List<Integer> nums = Arrays.asList(1, 3, 5, 7, 9, 11, 13);
        Range<Integer> filter = Range.closed(4, 10);
        List<Integer> filtered = new ArrayList<>();
        for (Integer n : nums) {
            if (filter.contains(n)) {
                filtered.add(n);
            }
        }
        System.out.println("4~10 区间内的元素: " + filtered);

        System.out.println();
    }
}
