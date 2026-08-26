package com.example.commonscollections4;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.PredicatedBag;
import org.apache.commons.collections4.list.GrowthList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Commons Collections4 进阶 Demo — 集合工具类与算法
 *
 * 覆盖内容：
 * 1. CollectionUtils — 集合判空、并交差、筛选、转换、计数
 * 2. ListUtils — 分区、固定大小列表、默认空列表处理
 * 3. MapUtils — 反转 Map、安全取值、空默认值
 * 4. ComparatorUtils — 链式比较器、null 处理比较器
 * 5. SetUtils / CompositeSet — 集合运算与复合集合
 * 6. IterableUtils — 在 Iterable 上执行查找与遍历
 * 7. PredicatedBag — 带约束条件的 Bag（校验元素合法性）
 * 8. GrowthList — 自动扩容的 List（按索引安全写入）
 */
public class CommonsCollections4AdvancedDemo {

    public static void main(String[] args) {
        System.out.println("===== Commons Collections4 进阶Demo =====\n");

        demoCollectionUtilsOperations();
        demoCollectionFilterAndTransform();
        demoListUtilsPartition();
        demoMapUtilsUtilities();
        demoComparatorUtils();
        demoSetUtilsOperations();
        demoIterableUtils();
        demoPredicatedBag();
        demoGrowthList();
    }

    /**
     * 1. CollectionUtils 基础集合运算
     *
     * 应用场景：两个用户集合求共同好友、待办任务差集、权限交集等
     */
    static void demoCollectionUtilsOperations() {
        System.out.println("--- 1. CollectionUtils：集合运算 ---");

        List<String> listA = Arrays.asList("a", "b", "c", "d");
        List<String> listB = Arrays.asList("b", "d", "e", "f");

        // union 并集（重复元素按最大出现次数保留）
        Collection<String> union = CollectionUtils.union(listA, listB);
        System.out.println("并集 union: " + union);

        // intersection 交集
        Collection<String> intersection = CollectionUtils.intersection(listA, listB);
        System.out.println("交集 intersection: " + intersection);

        // subtract 差集（在A中但不在B中）
        Collection<String> subtract = CollectionUtils.subtract(listA, listB);
        System.out.println("差集 subtract(A-B): " + subtract);

        // disjunction 对称差集
        Collection<String> disjunction = CollectionUtils.disjunction(listA, listB);
        System.out.println("对称差集 disjunction: " + disjunction);

        System.out.println("isEmpty 空集合: " + CollectionUtils.isEmpty(Collections.emptyList()));
        System.out.println("isNotEmpty: " + CollectionUtils.isNotEmpty(listA));
        System.out.println();
    }

    /**
     * 2. CollectionUtils 筛选/转换/收集
     *
     * 应用场景：从订单列表中筛选出未支付订单、提取用户姓名列表等
     */
    static void demoCollectionFilterAndTransform() {
        System.out.println("--- 2. CollectionUtils：筛选/转换/收集 ---");

        List<Person> persons = Arrays.asList(
                new Person("Alice", 17),
                new Person("Bob", 22),
                new Person("Charlie", 19),
                new Person("David", 15)
        );

        // select: 返回满足条件的全新集合，原集合不变
        org.apache.commons.collections4.Predicate<Person> adultPredicate = p -> p.age >= 18;
        Collection<Person> adults = CollectionUtils.select(persons, adultPredicate);
        System.out.println("成年人: " + adults);

        // selectRejected: 返回不满足条件的集合
        Collection<Person> minors = CollectionUtils.selectRejected(persons, adultPredicate);
        System.out.println("未成年人: " + minors);

        // collect: 提取属性生成新集合（类似 Stream.map）
        Transformer<Person, String> nameTransformer = Person::getName;
        Collection<String> names = CollectionUtils.collect(persons, nameTransformer);
        System.out.println("姓名列表: " + names);

        // filter: 直接修改原集合，保留满足条件的元素
        List<Person> mutablePersons = new ArrayList<>(persons);
        CollectionUtils.filter(mutablePersons, adultPredicate);
        System.out.println("filter后原集合只保留成年人: " + mutablePersons);

        // countMatches: 统计满足条件的元素个数
        int adultCount = CollectionUtils.countMatches(persons, adultPredicate);
        System.out.println("成年人数: " + adultCount);
        System.out.println();
    }

    /**
     * 3. ListUtils 分区与列表工具
     *
     * 应用场景：批量处理时分页、固定大小窗口消费消息
     */
    static void demoListUtilsPartition() {
        System.out.println("--- 3. ListUtils：分区/空列表/固定列表 ---");

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // partition: 按指定大小分区（常用于批量插入、分页导出）
        List<List<Integer>> partitions = ListUtils.partition(numbers, 3);
        System.out.println("每3个分区: " + partitions);

        // defaultIfNull: null 时返回空列表，避免 NPE
        List<String> nullList = null;
        List<String> safeList = ListUtils.defaultIfNull(nullList, Collections.emptyList());
        System.out.println("null 转空列表大小: " + safeList.size());

        // fixedSizeList: 固定大小，支持修改元素但不支持增删
        List<String> fixed = ListUtils.fixedSizeList(new ArrayList<>(Arrays.asList("A", "B", "C")));
        fixed.set(0, "X");
        System.out.println("fixedSizeList 允许 set: " + fixed);
        // fixed.add("D"); // 会抛 UnsupportedOperationException
        System.out.println();
    }

    /**
     * 4. MapUtils 反转与安全取值
     *
     * 应用场景：编码字典反转、配置 key/value 互换、安全读取外部 Map
     */
    static void demoMapUtilsUtilities() {
        System.out.println("--- 4. MapUtils：反转/安全取值/默认值 ---");

        Map<String, Integer> scoreMap = new HashMap<>();
        scoreMap.put("Math", 95);
        scoreMap.put("English", 88);
        scoreMap.put("Chinese", 92);

        // invertMap: key/value 互换（要求 value 唯一）
        Map<Integer, String> inverted = MapUtils.invertMap(scoreMap);
        System.out.println("反转后 Map: " + inverted);

        // getObject / getInteger: 安全取值，不存在时返回 null（避免自动装箱空指针）
        Integer physics = MapUtils.getInteger(scoreMap, "Physics");
        System.out.println("Physics 分数(不存在): " + physics);

        // getInteger(Map, key, defaultValue): 不存在时返回默认值
        Integer physicsWithDefault = MapUtils.getInteger(scoreMap, "Physics", 60);
        System.out.println("Physics 默认分数: " + physicsWithDefault);

        // emptyIfNull: null Map 转空 Map
        Map<String, String> nullMap = null;
        Map<String, String> safeMap = MapUtils.emptyIfNull(nullMap);
        System.out.println("null Map 转空 Map 大小: " + safeMap.size());
        System.out.println();
    }

    /**
     * 5. ComparatorUtils 比较器组合
     *
     * 应用场景：多字段排序、处理 null 值的稳健排序
     */
    static void demoComparatorUtils() {
        System.out.println("--- 5. ComparatorUtils：链式/空安全比较器 ---");

        List<Person> persons = Arrays.asList(
                new Person("Alice", 22),
                new Person("Bob", 19),
                null,
                new Person("Charlie", 22),
                new Person("David", 19)
        );

        // chainedComparator: 先按年龄升序，再按姓名升序
        Comparator<Person> byAge = Comparator.comparingInt(p -> p.age);
        Comparator<Person> byName = Comparator.comparing(p -> p.name);
        Comparator<Person> chained = ComparatorUtils.chainedComparator(byAge, byName);

        List<Person> sorted = new ArrayList<>(persons);
        sorted.remove(null); // 链式比较器不接受 null 元素，演示前先移除
        sorted.sort(chained);
        System.out.println("链式排序(年龄→姓名): " + sorted);

        // nullLowComparator / nullHighComparator: 将 null 放在最前或最后
        List<Person> withNulls = new ArrayList<>(persons);
        Comparator<Person> nullSafe = ComparatorUtils.nullLowComparator(chained);
        withNulls.sort(nullSafe);
        System.out.println("null 安全排序: " + withNulls);
        System.out.println();
    }

    /**
     * 6. SetUtils 集合运算
     *
     * 应用场景：角色并集、权限差集、标签交集
     */
    static void demoSetUtilsOperations() {
        System.out.println("--- 6. SetUtils：集合运算 ---");

        Set<String> adminRoles = new HashSet<>(Arrays.asList("READ", "WRITE", "DELETE"));
        Set<String> editorRoles = new HashSet<>(Arrays.asList("READ", "WRITE", "COMMENT"));

        // union / intersection / difference 返回不可修改视图
        Set<String> union = SetUtils.union(adminRoles, editorRoles);
        Set<String> intersection = SetUtils.intersection(adminRoles, editorRoles);
        Set<String> difference = SetUtils.difference(adminRoles, editorRoles);

        System.out.println("角色并集: " + union);
        System.out.println("角色交集: " + intersection);
        System.out.println("admin 独有角色: " + difference);

        // disjunction: 只属于其中一方的元素
        Set<String> disjunction = SetUtils.disjunction(adminRoles, editorRoles);
        System.out.println("对称差集: " + disjunction);
        System.out.println();
    }

    /**
     * 7. IterableUtils 在 Iterable 上查找
     *
     * 应用场景：对 Iterable（如数据库查询结果）进行搜索，不必先转 List
     */
    static void demoIterableUtils() {
        System.out.println("--- 7. IterableUtils：Iterable 查找与遍历 ---");

        List<Person> persons = Arrays.asList(
                new Person("Alice", 20),
                new Person("Bob", 25),
                new Person("Charlie", 22)
        );

        // find: 返回第一个满足条件的元素
        Person firstAdult = IterableUtils.find(persons, p -> p.age >= 21);
        System.out.println("第一个年龄≥21的人: " + firstAdult);

        // indexOf: 查找满足条件的元素索引
        Person target = new Person("Charlie", 22);
        int index = IterableUtils.indexOf(persons, p -> p.equals(target));
        System.out.println("Charlie 的索引: " + index);

        // matchesAny / matchesAll
        boolean hasAdult = IterableUtils.matchesAny(persons, p -> p.age >= 18);
        boolean allAdult = IterableUtils.matchesAll(persons, p -> p.age >= 18);
        System.out.println("是否有成年人: " + hasAdult + ", 是否全是成年人: " + allAdult);
        System.out.println();
    }

    /**
     * 8. PredicatedBag — 带校验规则的 Bag
     *
     * 应用场景：计数集合中只允许非负整数、只允许特定类型对象等
     */
    static void demoPredicatedBag() {
        System.out.println("--- 8. PredicatedBag：约束型 Bag ---");

        org.apache.commons.collections4.Predicate<Integer> nonNegative = n -> n != null && n >= 0;
        Bag<Integer> scoreBag = PredicatedBag.predicatedBag(new HashBag<>(), nonNegative);

        scoreBag.add(90, 2);
        scoreBag.add(85, 3);
        System.out.println("分数 Bag: " + scoreBag);

        // 尝试添加非法元素会抛 IllegalArgumentException
        try {
            scoreBag.add(-5);
        } catch (IllegalArgumentException e) {
            System.out.println("添加负数时被拒绝: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * 9. GrowthList — 自动扩容 List
     *
     * 应用场景：按索引写入时中间空缺自动填充 null，避免 IndexOutOfBoundsException
     */
    static void demoGrowthList() {
        System.out.println("--- 9. GrowthList：自动扩容 ---");

        List<String> growth = new GrowthList<>();
        growth.set(3, "Index 3"); // 普通 ArrayList.set(3) 会越界，GrowthList 会自动扩容
        System.out.println("GrowthList 大小: " + growth.size());
        System.out.println("GrowthList 内容: " + growth);
        System.out.println();
    }

    /**
     * 简单 POJO，用于演示比较/转换/筛选
     */
    static class Person {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person person = (Person) o;
            return age == person.age && java.util.Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return name + "(" + age + ")";
        }
    }
}
