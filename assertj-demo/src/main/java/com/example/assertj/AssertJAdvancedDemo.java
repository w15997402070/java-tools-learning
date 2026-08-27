package com.example.assertj;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * AssertJ 进阶断言演示。
 *
 * <p>覆盖：异常断言 / Optional / 日期时间 / 组合条件 / 递归字段比较 / 软断言。</p>
 */
public class AssertJAdvancedDemo {

    public static void main(String[] args) {
        exceptionAssertions();
        optionalAssertions();
        dateTimeAssertions();
        predicateAndConditionAssertions();
        recursiveComparisonAssertions();
        softAssertionsDemo();
    }

    /** 异常断言：验证异常类型与消息。 */
    private static void exceptionAssertions() {
        System.out.println("=== Exception Assertions ===");

        Assertions.assertThatThrownBy(() -> divide(1, 0))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("zero");

        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> parsePositiveInt("-5"))
                .withMessageContaining("positive");

        System.out.println("Exception assertions passed.");
    }

    private static int divide(int a, int b) {
        return a / b;
    }

    private static int parsePositiveInt(String value) {
        int parsed = Integer.parseInt(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException("Value must be positive: " + value);
        }
        return parsed;
    }

    /** Optional 断言。 */
    private static void optionalAssertions() {
        System.out.println("=== Optional Assertions ===");
        Optional<String> present = Optional.of("AssertJ");
        Optional<String> empty = Optional.empty();

        Assertions.assertThat(present)
                .isPresent()
                .hasValue("AssertJ");

        Assertions.assertThat(empty).isEmpty();

        System.out.println("Optional assertions passed.");
    }

    /** 日期时间断言。 */
    private static void dateTimeAssertions() {
        System.out.println("=== Date/Time Assertions ===");
        LocalDate today = LocalDate.of(2026, 8, 27);
        LocalDateTime meeting = LocalDateTime.of(2026, 8, 27, 14, 30);

        Assertions.assertThat(today)
                .isAfter(LocalDate.of(2020, 1, 1))
                .isBeforeOrEqualTo(LocalDate.of(2026, 12, 31));

        Assertions.assertThat(meeting)
                .isEqualToIgnoringSeconds(LocalDateTime.of(2026, 8, 27, 14, 30, 0));

        System.out.println("Date/time assertions passed.");
    }

    /** 组合条件与自定义断言。 */
    private static void predicateAndConditionAssertions() {
        System.out.println("=== Predicate & Condition Assertions ===");
        List<String> tags = Arrays.asList("java", "assertj", "testing");

        Assertions.assertThat(tags)
                .allSatisfy(tag -> Assertions.assertThat(tag).matches("^[a-z]+$"))
                .anySatisfy(tag -> Assertions.assertThat(tag).startsWith("assert"));

        System.out.println("Predicate assertions passed.");
    }

    /** 递归字段比较：忽略指定字段，按值比较而非引用。 */
    private static void recursiveComparisonAssertions() {
        System.out.println("=== Recursive Comparison Assertions ===");
        User actual = new User("tom", "Tom", new Address("Beijing", "100000"));
        User expected = new User("tom", "Tom", new Address("Beijing", "100000"));

        // 默认递归比较会检查所有字段，两个不同对象只要字段相等即可通过
        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        // 忽略自动生成的 id 字段
        Product p1 = new Product(1L, "Book", BigDecimal.valueOf(59.00));
        Product p2 = new Product(2L, "Book", BigDecimal.valueOf(59.00));

        RecursiveComparisonConfiguration config = RecursiveComparisonConfiguration.builder()
                .withIgnoredFields("id")
                .build();
        Assertions.assertThat(p1)
                .usingRecursiveComparison(config)
                .isEqualTo(p2);

        System.out.println("Recursive comparison assertions passed.");
    }

    /** 软断言：一次收集所有失败，常用于集成测试一次性校验多个字段。 */
    private static void softAssertionsDemo() {
        System.out.println("=== Soft Assertions ===");
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat("hello").startsWith("h");
        softly.assertThat(42).isGreaterThan(0);
        softly.assertThat(Arrays.asList(1, 2, 3)).contains(2);

        // 所有断言都会执行，最后统一抛出
        softly.assertAll();
        System.out.println("Soft assertions passed.");
    }

    // ==================== 演示实体 ====================

    static class User {
        private final String username;
        private final String displayName;
        private final Address address;

        User(String username, String displayName, Address address) {
            this.username = username;
            this.displayName = displayName;
            this.address = address;
        }
    }

    static class Address {
        private final String city;
        private final String zipCode;

        Address(String city, String zipCode) {
            this.city = city;
            this.zipCode = zipCode;
        }
    }

    static class Product {
        private final Long id;
        private final String name;
        private final BigDecimal price;

        Product(Long id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }
}
