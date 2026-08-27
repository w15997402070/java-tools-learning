package com.example.assertj;

import org.assertj.core.api.Assertions;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AssertJ 基础断言演示。
 *
 * <p>覆盖：字符串 / 数值 / 布尔 / 数组 / 集合 / Map / 对象 / 文件路径等常用断言。</p>
 */
public class AssertJBasicDemo {

    public static void main(String[] args) {
        stringAssertions();
        numberAssertions();
        booleanAndObjectAssertions();
        arrayAssertions();
        collectionAssertions();
        mapAssertions();
        fileAssertions();
    }

    /** 字符串断言：判空、包含、前缀后缀、正则匹配。 */
    private static void stringAssertions() {
        System.out.println("=== String Assertions ===");
        String text = "AssertJ makes testing enjoyable";

        Assertions.assertThat(text)
                .isNotNull()
                .isNotEmpty()
                .startsWith("AssertJ")
                .contains("testing")
                .endsWith("enjoyable")
                .matches(".*enjoyable$")
                .hasSizeGreaterThan(10);

        System.out.println("String assertions passed.");
    }

    /** 数值断言：范围、比较、是否为零/正数。 */
    private static void numberAssertions() {
        System.out.println("=== Number Assertions ===");
        int age = 25;

        Assertions.assertThat(age)
                .isPositive()
                .isGreaterThan(18)
                .isLessThanOrEqualTo(30)
                .isBetween(20, 30)
                .isNotZero();

        System.out.println("Number assertions passed.");
    }

    /** 布尔与对象级断言。 */
    private static void booleanAndObjectAssertions() {
        System.out.println("=== Boolean & Object Assertions ===");
        Boolean flag = true;
        Object nullObj = null;

        Assertions.assertThat(flag).isTrue();
        Assertions.assertThat(nullObj).isNull();
        Assertions.assertThat("fixed").isEqualTo("fixed");

        System.out.println("Boolean & object assertions passed.");
    }

    /** 数组断言：元素、长度、是否包含子序列。 */
    private static void arrayAssertions() {
        System.out.println("=== Array Assertions ===");
        String[] fruits = {"apple", "banana", "cherry"};

        Assertions.assertThat(fruits)
                .hasSize(3)
                .contains("banana")
                .doesNotContain("orange")
                .containsExactly("apple", "banana", "cherry")
                .startsWith("apple");

        System.out.println("Array assertions passed.");
    }

    /** 集合断言：过滤、所有元素满足条件、仅包含唯一元素。 */
    private static void collectionAssertions() {
        System.out.println("=== Collection Assertions ===");
        List<Integer> scores = Arrays.asList(85, 92, 78, 95);

        Assertions.assertThat(scores)
                .hasSize(4)
                .contains(92, 78)
                .allMatch(score -> score > 0 && score <= 100, "score between 0 and 100")
                .doesNotHaveDuplicates();

        System.out.println("Collection assertions passed.");
    }

    /** Map 断言：键值、大小、是否包含指定键/值。 */
    private static void mapAssertions() {
        System.out.println("=== Map Assertions ===");
        Map<String, Integer> ages = new HashMap<>();
        ages.put("Alice", 30);
        ages.put("Bob", 25);

        Assertions.assertThat(ages)
                .hasSize(2)
                .containsKey("Alice")
                .containsEntry("Bob", 25)
                .doesNotContainKey("Charlie");

        System.out.println("Map assertions passed.");
    }

    /** 文件/路径断言。 */
    private static void fileAssertions() {
        System.out.println("=== File Assertions ===");
        File pomFile = Paths.get("pom.xml").toFile();

        Assertions.assertThat(pomFile)
                .exists()
                .isFile()
                .canRead();

        System.out.println("File assertions passed.");
    }
}
