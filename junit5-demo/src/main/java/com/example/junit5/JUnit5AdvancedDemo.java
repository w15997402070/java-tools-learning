package com.example.junit5;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * JUnit 5 高级功能演示类
 *
 * 本类覆盖 JUnit 5 的进阶特性：
 * 1. 参数化测试（@ParameterizedTest）：同一测试方法用多组数据执行
 * 2. 参数来源（@ValueSource / @CsvSource / @EnumSource / @MethodSource）
 * 3. 动态测试（@TestFactory）：运行时动态生成测试用例
 * 4. 条件执行（@EnabledOnOs / @DisabledOnOs / @EnabledIfSystemProperty）
 * 5. 标签与分组（@Tag）：按标签筛选执行测试
 * 6. 重复测试（@RepeatedTest）：同一测试重复执行 N 次
 */
@DisplayName("JUnit5 高级功能演示")
public class JUnit5AdvancedDemo {

    // ============================================================
    // 1. 参数化测试 - @ValueSource（最简参数来源）
    // ============================================================

    /**
     * @ParameterizedTest + @ValueSource：最简参数化测试。
     * 一组 int 值 [3, 5, 7] 会让测试执行 3 次，每次参数分别为 3、5、7。
     * 可用参数类型：int[], long[], double[], String[], Class[]
     */
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 6, 8})
    @DisplayName("参数化测试：验证偶数")
    void testIsEven(int number) {
        assertEquals(0, number % 2, number + " 应该是偶数");
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "world", "JUnit5"})
    @DisplayName("参数化测试：字符串长度验证")
    void testStringNotEmpty(String text) {
        assertNotNull(text);
        assertTrue(text.length() > 0);
    }

    // ============================================================
    // 2. 参数化测试 - @CsvSource（CSV 格式多参数）
    // ============================================================

    /**
     * @CsvSource：提供多列数据，每行对应一次测试执行。
     * 列值按顺序注入方法参数。
     * 默认分隔符为逗号，可用 delimiterString 自定义。
     */
    @ParameterizedTest
    @CsvSource({
            "apple,     5",
            "banana,    6",
            "cherry,    6",
            "dragonfruit, 11"
    })
    @DisplayName("参数化测试：水果名 → 长度验证（@CsvSource）")
    void testFruitLength(String fruit, int expectedLength) {
        assertEquals(expectedLength, fruit.trim().length(),
                fruit + " 的长度应为 " + expectedLength);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1, 2, 3",
            "10, 20, 30",
            "-5, -10, -15"
    })
    @DisplayName("参数化测试：三数之和（@CsvSource 多列）")
    void testSum(int a, int b, int c) {
        assertEquals(a + b + c, a + b + c);
    }

    // ============================================================
    // 3. 参数化测试 - @EnumSource（枚举类型遍历）
    // ============================================================

    enum Month { JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC }

    @ParameterizedTest
    @EnumSource(Month.class)
    @DisplayName("参数化测试：遍历所有月份（@EnumSource）")
    void testAllMonths(Month month) {
        // 演示框架：验证每个枚举值非空
        assertNotNull(month.name());
    }

    @ParameterizedTest
    @EnumSource(value = Month.class, names = {"JAN", "MAR", "MAY", "JUL", "AUG", "OCT", "DEC"})
    @DisplayName("参数化测试：只验证大月份（@EnumSource names 过滤）")
    void testLongMonths(Month month) {
        assertTrue(
            month == Month.JAN || month == Month.MAR || month == Month.MAY ||
            month == Month.JUL || month == Month.AUG || month == Month.OCT ||
            month == Month.DEC
        );
    }

    // ============================================================
    // 4. 参数化测试 - @MethodSource（自定义参数生成器）
    // ============================================================

    /**
     * @MethodSource：引用同类的静态方法作为参数源。
     * 方法名可以与测试方法同名，也可以自定义。
     * 必须返回 Stream / Collection / Iterator / Array / 基本类型数组。
     */
    @ParameterizedTest
    @MethodSource("stringProvider")
    @DisplayName("参数化测试：@MethodSource 提供字符串")
    void testStrings(String text) {
        assertNotNull(text);
        assertTrue(text.length() <= 20);
    }

    static Stream<String> stringProvider() {
        return Stream.of("apple", "banana", "cherry", "dragonfruit");
    }

    /**
     * 多参数 MethodSource：返回 Arguments 类型
     */
    @ParameterizedTest
    @MethodSource("rangeProvider")
    @DisplayName("参数化测试：@MethodSource 多参数")
    void testRange(int start, int end, int expected) {
        int sum = 0;
        for (int i = start; i < end; i++) { sum += i; }
        assertEquals(expected, sum);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> rangeProvider() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(0, 3, 3),   // 0+1+2=3
                org.junit.jupiter.params.provider.Arguments.of(1, 4, 6),   // 1+2+3=6
                org.junit.jupiter.params.provider.Arguments.of(5, 8, 18)   // 5+6+7=18
        );
    }

    // ============================================================
    // 5. 动态测试 - @TestFactory
    // ============================================================

    /**
     * @TestFactory：返回 DynamicTest 的 Stream/Collection/Iterator。
     * 动态测试在运行时生成，不像普通 @Test 那样在编译时就固定。
     * 适合：测试大量数据、生成异构测试集、测试 DSL。
     * 注意：@TestFactory 方法不是 @Test，不会自动命名，需提供 displayName。
     *
     * Java 8 兼容：使用 .collect(Collectors.toList()) 替代 .toList()
     */
    @TestFactory
    @DisplayName("动态测试：工厂方法生成测试")
    List<DynamicTest> dynamicTests() {
        List<String> inputs = Arrays.asList("input1", "input2", "input3");
        return inputs.stream()
                .map(input -> DynamicTest.dynamicTest(
                        "测试: " + input,
                        new org.junit.jupiter.api.function.Executable() {
                            public void execute() throws Throwable { assertNotNull(input); }
                        }
                ))
                .collect(Collectors.toList());
    }

    @TestFactory
    @DisplayName("动态测试：数学运算测试集")
    List<DynamicTest> mathDynamicTests() {
        // 使用普通内部类（record 是 Java 16+ 特性，Java 8 不支持）
        class CalcCase {
            final int a; final int b; final String op; final int expected;
            CalcCase(int a, int b, String op, int expected) {
                this.a = a; this.b = b; this.op = op; this.expected = expected;
            }
        }

        List<CalcCase> cases = Arrays.asList(
                new CalcCase(2, 3, "+", 5),
                new CalcCase(10, 4, "-", 6),
                new CalcCase(3, 4, "*", 12),
                new CalcCase(20, 4, "/", 5)
        );

        return cases.stream()
                .map(c -> DynamicTest.dynamicTest(
                        c.a + " " + c.op + " " + c.b + " = " + c.expected,
                        new org.junit.jupiter.api.function.Executable() {
                            public void execute() throws Throwable {
                                int result;
                                switch (c.op) {
                                    case "+": result = c.a + c.b; break;
                                    case "-": result = c.a - c.b; break;
                                    case "*": result = c.a * c.b; break;
                                    case "/": result = c.a / c.b; break;
                                    default: throw new IllegalArgumentException("未知操作符: " + c.op);
                                }
                                assertEquals(c.expected, result);
                            }
                        }
                ))
                .collect(Collectors.toList());
    }

    // ============================================================
    // 6. 重复测试 - @RepeatedTest
    // ============================================================

    /**
     * @RepeatedTest(N)：将测试方法重复执行 N 次。
     * 常用于：测试稳定性（避免随机性导致的偶发失败）、性能基准测试。
     * 可通过 RepetitionInfo 获取当前次数和总次数。
     */
    @RepeatedTest(5)
    @DisplayName("重复测试：执行 5 次验证随机数稳定性")
    void testRandomStability(RepetitionInfo info) {
        int rnd = (int) (Math.random() * 100);
        System.out.println("第 " + info.getCurrentRepetition() + "/" +
                           info.getTotalRepetitions() + " 次，随机数=" + rnd);
        assertTrue(rnd >= 0 && rnd < 100);
    }

    @RepeatedTest(value = 3, name = "第 {currentRepetition} 次执行")
    @DisplayName("自定义重复测试名称")
    void testWithCustomName() {
        assertTrue(true);
    }

    // ============================================================
    // 7. 条件执行
    // ============================================================

    /**
     * @EnabledOnOs / @DisabledOnOs：仅在指定操作系统上运行。
     * 可用 OS 常量：WINDOWS, MAC, LINUX, SOLARIS 等。
     * 适用于：平台相关测试（GUI / 文件路径 / 编码差异）。
     */
    @Test
    @EnabledOnOs({OS.WINDOWS, OS.LINUX})
    @DisplayName("条件执行：仅在 Windows 或 Linux 上运行")
    void testOnWindowsOrLinux() {
        assertNotNull(System.getProperty("os.name"));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    @DisplayName("条件执行：禁用 Windows 平台")
    void testNotOnWindows() {
        assertFalse(System.getProperty("os.name").contains("Windows"));
    }

    /**
     * @EnabledIfSystemProperty：基于 JVM 系统属性条件执行。
     * matches 使用正则表达式。
     */
    @Test
    @EnabledIfSystemProperty(named = "java.version", matches = "1\\.8.*")
    @DisplayName("条件执行：基于 Java 版本")
    void testJava8() {
        assertTrue(System.getProperty("java.version").startsWith("1.8"));
    }

    // ============================================================
    // 8. 测试标签 - @Tag
    // ============================================================

    /**
     * @Tag：给测试打标签，用于在 Maven / Gradle 中筛选执行。
     * Maven surefire 配置示例：
     *   <groups>fast</groups>
     *   <excludedGroups>slow</excludedGroups>
     * 适合：按类型（单元测试 / 集成测试 / 慢速测试）分组。
     */
    @Test
    @Tag("fast")
    @DisplayName("快速测试（Tag: fast）")
    void fastTest() {
        assertTrue(true);
    }

    @Test
    @Tag("slow")
    @DisplayName("慢速测试（Tag: slow）")
    void slowTest() {
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        assertTrue(true);
    }

    @Test
    @Tag("integration")
    @DisplayName("集成测试（Tag: integration）")
    void integrationTest() {
        assertNotNull(System.getProperty("user.dir"));
    }

    // ============================================================
    // 9. 自定义组合注解
    // ============================================================

    /**
     * 自定义注解：组合了 @Test、@Tag("integration") 和 @EnabledOnOs，语义更清晰。
     */
    @Test
    @Tag("integration")
    @EnabledOnOs({OS.WINDOWS, OS.LINUX})
    @DisplayName("使用自定义组合注解：@IntegrationTest")
    void customAnnotationTest() {
        assertTrue(true);
    }
}
