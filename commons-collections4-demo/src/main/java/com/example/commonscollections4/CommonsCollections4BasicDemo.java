package com.example.commonscollections4;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.PassiveExpiringMap;
import org.apache.commons.collections4.SetUniqueList;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.collections4.multiset.HashMultiSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Commons Collections4 基础Demo — 特殊集合类型
 *
 * 覆盖内容：
 * 1. Bag / MultiSet — 可重复计数集合（统计词频、投票计数）
 * 2. BidiMap — 双向映射（key↔value互查，如ID↔名称）
 * 3. CaseInsensitiveMap — 大小写不敏感Map（HTTP Header、配置键）
 * 4. SetUniqueList — 去重List（保持插入顺序+去重）
 * 5. MultiValuedMap / MultiValueMap — 一键多值Map（分组、一对多关系）
 * 6. OrderedMap / LinkedMap — 有序Map（按插入顺序遍历+按索引访问）
 * 7. PassiveExpiringMap — 自动过期Map（简易缓存场景）
 */
public class CommonsCollections4BasicDemo {

    public static void main(String[] args) {
        System.out.println("===== Commons Collections4 基础Demo =====\n");

        demoBagAndMultiSet();
        demoBidiMap();
        demoCaseInsensitiveMap();
        demoSetUniqueList();
        demoMultiValuedMap();
        demoOrderedMap();
        demoPassiveExpiringMap();
    }

    /**
     * 1. Bag / MultiSet — 可重复计数的集合
     *
     * 应用场景：统计词频、投票计数、购物车商品计数
     * Bag允许同一个元素添加多次，并提供 getCount() 获取某元素出现次数
     * HashBag 基于 HashMap，TreeBag 基于 TreeMap（有序）
     * MultiSet 是 Bag 的新名字（4.x版本开始），接口一致
     */
    static void demoBagAndMultiSet() {
        System.out.println("--- 1. Bag / MultiSet：可重复计数集合 ---");

        // HashBag：基于HashMap的Bag实现
        Bag<String> wordBag = new HashBag<>();
        wordBag.add("Java", 3);    // "Java"出现3次
        wordBag.add("Python", 2);  // "Python"出现2次
        wordBag.add("Go", 1);      // "Go"出现1次
        wordBag.add("Java", 2);    // 再加2次"Java"，总共5次

        System.out.println("Bag内容: " + wordBag);  // [Go:1, Java:5, Python:2]
        System.out.println("Java出现次数: " + wordBag.getCount("Java"));  // 5
        System.out.println("Python出现次数: " + wordBag.getCount("Python"));  // 2
        System.out.println("Bag总元素数(uniqueSet): " + wordBag.uniqueSet());  // [Go, Java, Python]
        System.out.println("Bag总大小(含重复): " + wordBag.size());  // 8

        // 移除元素
        wordBag.remove("Java", 2);  // 移除2次"Java"，剩余3次
        System.out.println("移除2次Java后: Java出现次数 = " + wordBag.getCount("Java"));  // 3

        // MultiSet（Bag的新名称，接口完全兼容）
        MultiSet<String> voteMultiSet = new HashMultiSet<>();
        voteMultiSet.add("Alice");
        voteMultiSet.add("Bob");
        voteMultiSet.add("Alice");
        voteMultiSet.add("Alice");
        voteMultiSet.add("Charlie");
        System.out.println("投票统计: Alice=" + voteMultiSet.getCount("Alice")
                + ", Bob=" + voteMultiSet.getCount("Bob")
                + ", Charlie=" + voteMultiSet.getCount("Charlie"));
        System.out.println();
    }

    /**
     * 2. BidiMap — 双向映射
     *
     * 应用场景：ID↔名称互查、编码↔描述双向映射
     * BidiMap保证key和value都唯一，可以按value查key
     * DualHashBidiMap：双向HashMap（key/value都可查）
     * TreeBidiMap：双向TreeMap（按key自然排序）
     */
    static void demoBidiMap() {
        System.out.println("--- 2. BidiMap：双向映射（key↔value互查） ---");

        // DualHashBidiMap — 双向HashMap
        BidiMap<Integer, String> employeeMap = new DualHashBidiMap<>();
        employeeMap.put(1001, "张三");
        employeeMap.put(1002, "李四");
        employeeMap.put(1003, "王五");

        // 正向查：key → value
        System.out.println("员工ID→姓名: 1001=" + employeeMap.get(1001));  // 张三

        // 反向查：value → key
        System.out.println("姓名→员工ID: 李四=" + employeeMap.getKey("李四"));  // 1002

        // 获取反向Map
        BidiMap<String, Integer> reverseMap = employeeMap.inverseBidiMap();
        System.out.println("反向Map: " + reverseMap);  // {张三=1001, 李四=1002, 王五=1003}

        // TreeBidiMap — 有序双向映射（按key自然排序）
        BidiMap<String, Integer> scoreMap = new TreeBidiMap<>();
        scoreMap.put("English", 90);
        scoreMap.put("Math", 95);
        scoreMap.put("Chinese", 88);
        System.out.println("TreeBidiMap（按key排序）: " + scoreMap);  // {Chinese=88, English=90, Math=95}
        System.out.println("分数→科目: 95=" + scoreMap.getKey(95));  // Math
        System.out.println();
    }

    /**
     * 3. CaseInsensitiveMap — 大小写不敏感的Map
     *
     * 应用场景：HTTP Header处理（Content-Type/content-type视为同一个key）
     *           配置文件读取（区分不了app.name和APP.NAME）
     *           国际化场景
     * 注意：key存储时会转为原始大小写，但查找时大小写不敏感
     */
    static void demoCaseInsensitiveMap() {
        System.out.println("--- 3. CaseInsensitiveMap：大小写不敏感Map ---");

        Map<String, String> headers = new CaseInsensitiveMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token123");

        // 大小写不敏感查找
        System.out.println("content-type: " + headers.get("content-type"));      // application/json
        System.out.println("CONTENT-TYPE: " + headers.get("CONTENT-TYPE"));      // application/json
        System.out.println("authorization: " + headers.get("authorization"));    // Bearer token123

        // containsKey也大小写不敏感
        System.out.println("包含AUTHORIZATION? " + headers.containsKey("AUTHORIZATION"));  // true

        // key存储时保留原始大小写
        System.out.println("实际存储的key: " + headers.keySet());  // [Content-Type, Authorization]
        System.out.println();
    }

    /**
     * 4. SetUniqueList — 去重List
     *
     * 应用场景：保持插入顺序但需要去重的场景
     *           比如用户浏览历史（按时间排列但同一页面只保留一条）
     * 底层是ArrayList + 内部Set辅助去重，add重复元素返回false
     */
    static void demoSetUniqueList() {
        System.out.println("--- 4. SetUniqueList：去重List（保持插入顺序） ---");

        // 从普通List创建去重List
        List<String> rawList = new ArrayList<>(Arrays.asList("Java", "Python", "Java", "Go", "Python", "Rust"));
        List<String> uniqueList = SetUniqueList.setUniqueList(rawList);

        System.out.println("去重后: " + uniqueList);  // [Java, Python, Go, Rust]（保持首次出现的顺序）

        // 添加重复元素会被忽略
        boolean added = uniqueList.add("Java");
        System.out.println("再次添加Java: added=" + added);  // false
        System.out.println("List内容不变: " + uniqueList);    // [Java, Python, Go, Rust]

        // 添加新元素正常
        added = uniqueList.add("C++");
        System.out.println("添加C++: added=" + added);  // true
        System.out.println("List: " + uniqueList);       // [Java, Python, Go, Rust, C++]
        System.out.println();
    }

    /**
     * 5. MultiValuedMap / MultiValueMap — 一键多值Map
     *
     * 应用场景：学生→多门课程、城市→多个邮编、Tag→多篇文章
     * 一个key可以关联多个value，类似Guava Multimap
     * MultiValuedMap是4.1新增接口，MultiValueMap是旧版实现
     */
    static void demoMultiValuedMap() {
        System.out.println("--- 5. MultiValuedMap：一键多值Map ---");

        // MultiValuedMap（4.1+推荐）
        MultiValuedMap<String, String> studentCourses = MultiValueMap.multiValueMap(new HashMap<>(), ArrayList.class);
        studentCourses.put("张三", "数学");
        studentCourses.put("张三", "英语");
        studentCourses.put("张三", "物理");
        studentCourses.put("李四", "化学");
        studentCourses.put("李四", "数学");

        System.out.println("张三的课程: " + studentCourses.get("张三"));  // [数学, 英语, 物理]
        System.out.println("李四的课程: " + studentCourses.get("李四"));  // [化学, 数学]

        // 遍历所有key-value对
        System.out.println("所有学生-课程:");
        studentCourses.entries().forEach(entry ->
                System.out.println("  " + entry.getKey() + " → " + entry.getValue()));

        // keySet和size
        System.out.println("所有学生: " + studentCourses.keys());  // [张三, 李四]
        System.out.println("总选课数: " + studentCourses.size());   // 5
        System.out.println();
    }

    /**
     * 6. OrderedMap / LinkedMap — 有序Map
     *
     * 应用场景：需要按插入顺序遍历+按索引访问的Map
     *           如排行榜、步骤编号映射
     * LinkedMap继承OrderedMap，额外提供 firstKey/lastKey/getValue(index)
     * 比 LinkedHashMap 多了按索引访问的能力
     */
    static void demoOrderedMap() {
        System.out.println("--- 6. OrderedMap / LinkedMap：有序Map ---");

        LinkedMap<String, Integer> stepMap = new LinkedMap<>();
        stepMap.put("步骤1-初始化", 1);
        stepMap.put("步骤2-加载配置", 2);
        stepMap.put("步骤3-连接数据库", 3);
        stepMap.put("步骤4-启动服务", 4);

        // 按插入顺序遍历
        System.out.println("步骤列表: " + stepMap);  // 按插入顺序

        // 按索引访问（LinkedMap独有的功能，LinkedHashMap不支持）
        System.out.println("第2个步骤的key: " + stepMap.get(1));           // 步骤2-加载配置
        System.out.println("最后一个步骤的key: " + stepMap.lastKey());     // 步骤4-启动服务
        System.out.println("第一个步骤的value: " + stepMap.getValue(0));   // 1

        // indexOf — 获取key的索引位置
        System.out.println("步骤3的索引: " + stepMap.indexOf("步骤3-连接数据库"));  // 2
        System.out.println();
    }

    /**
     * 7. PassiveExpiringMap — 自动过期Map
     *
     * 应用场景：简易本地缓存（验证码、临时Token、限流计数器）
     * 元素在指定时间后自动过期被移除，无需手动清理
     * 注意：过期检查发生在访问时（惰性删除），非定时主动删除
     */
    static void demoPassiveExpiringMap() {
        System.out.println("--- 7. PassiveExpiringMap：自动过期Map ---");

        // 创建5秒过期的Map
        PassiveExpiringMap<String, String> cache = new PassiveExpiringMap<>(5000);
        cache.put("验证码:13800001111", "582934");
        cache.put("验证码:13900002222", "123456");

        System.out.println("刚放入: " + cache);  // 两个验证码都在
        System.out.println("138验证码: " + cache.get("验证码:13800001111"));  // 582934

        // 等待6秒让元素过期
        try {
            System.out.println("等待6秒...");
            TimeUnit.SECONDS.sleep(6);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 过期后访问返回null
        System.out.println("6秒后138验证码: " + cache.get("验证码:13800001111"));  // null（已过期）
        System.out.println("6秒后Map大小: " + cache.size());  // 0
        System.out.println();
    }
}
