package com.example.junit5;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assumptions.assumingThat;

/**
 * JUnit 5 基础演示类
 *
 * 本类覆盖 JUnit Jupiter（JUnit 5）的核心基础功能：
 * 1. 生命周期注解（@BeforeAll / @BeforeEach / @AfterEach / @AfterAll）
 * 2. 常用断言（assertEquals / assertTrue / assertThrows / assertAll / assertTimeout）
 * 3. 嵌套测试类（@Nested）
 * 4. 禁用测试（@Disabled）
 * 5. 测试显示名（@DisplayName）
 * 6. 假设跳过（Assumptions）
 *
 * 运行方式：
 *   mvn test                                          # 运行所有测试
 *   mvn test -Dtest=JUnit5BasicDemo                   # 只运行本类
 *   mvn test -Dtest=JUnit5BasicDemo#demoAssertions    # 只运行某方法
 */
@DisplayName("JUnit5 基础功能演示")
public class JUnit5BasicDemo {

    // ============================================================
    // 1. 生命周期注解
    // ============================================================

    /**
     * @BeforeAll：在所有测试方法之前执行一次，必须是 static 方法。
     * 常用于：初始化数据库连接、加载测试配置。
     */
    @BeforeAll
    static void beforeAll() {
        System.out.println(">>> @BeforeAll: 所有测试开始前执行一次（静态方法）");
    }

    /**
     * @AfterAll：在所有测试方法之后执行一次，必须是 static 方法。
     * 常用于：关闭数据库连接、清理全局资源。
     */
    @AfterAll
    static void afterAll() {
        System.out.println(">>> @AfterAll: 所有测试结束后执行一次（静态方法）");
    }

    /**
     * @BeforeEach：在每个测试方法之前执行。
     * 常用于：重置测试数据、创建测试对象实例。
     */
    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        System.out.println("  --- @BeforeEach: 每个测试前执行，当前测试: " + testInfo.getDisplayName());
    }

    /**
     * @AfterEach：在每个测试方法之后执行。
     * 常用于：回滚数据、清理临时文件。
     */
    @AfterEach
    void afterEach() {
        System.out.println("  --- @AfterEach: 每个测试后执行");
    }

    // ============================================================
    // 2. 常用断言演示
    // ============================================================

    @Test
    @DisplayName("assertEquals 基本相等断言")
    void demoAssertEquals() {
        assertEquals(200, 100 + 100, "两数相加应相等");
        assertEquals("Hello", "Hel" + "lo");
        assertEquals(3.14159, Math.PI, 0.00001, "PI 值应精确到小数点后5位");
    }

    @Test
    @DisplayName("assertTrue / assertFalse 布尔断言")
    void demoAssertBoolean() {
        assertTrue("Hello".startsWith("He"), "字符串应以 He 开头");
        assertFalse("Hello".isEmpty(), "字符串不应为空");
    }

    @Test
    @DisplayName("assertNull / assertNotNull 空值断言")
    void demoAssertNull() {
        String nullStr = null;
        String notNullStr = "value";
        assertNull(nullStr, "应为 null");
        assertNotNull(notNullStr, "不应为 null");
    }

    @Test
    @DisplayName("assertSame / assertNotSame 引用相等断言")
    void demoAssertSame() {
        String s1 = new String("test");
        String s2 = s1;
        String s3 = new String("test");
        assertSame(s1, s2, "同一引用");
        assertNotSame(s1, s3, "不同对象（内容相同但引用不同）");
    }

    @Test
    @DisplayName("assertArrayEquals 数组相等断言")
    void demoAssertArrayEquals() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};
        assertArrayEquals(a1, a2, "两数组应相等");
        // assertArrayEquals(a1, a3); // 注释解开会失败
    }

    @Test
    @DisplayName("assertThrows 异常断言")
    void demoAssertThrows() {
        // 断言抛出特定异常
        ArithmeticException ex = assertThrows(
                ArithmeticException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable { int r = 1 / 0; }
                },
                "除以零应抛出 ArithmeticException"
        );
        assertEquals("/ by zero", ex.getMessage());

        // 断言抛出异常并验证异常内容
        assertThrows(NullPointerException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                String s = null;
                s.length();
            }
        });
    }

    @Test
    @DisplayName("assertAll 分组断言（全部执行，不会在首个失败时停止）")
    void demoAssertAll() {
        // assertAll 内的所有断言都会执行，最后统一报告失败
        assertAll("用户对象完整性校验",
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable { assertEquals("张三", "张三"); }
                },
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable { assertEquals(25, 25); }
                },
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable { assertNotNull(new Object()); }
                },
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable { assertTrue(1 < 5); }
                }
        );
        System.out.println("assertAll: 所有分组断言均通过！");
    }

    @Test
    @DisplayName("assertTimeout 超时断言")
    void demoAssertTimeout() {
        // 普通超时断言：方法执行完再检查耗时（assertTimeout + Executable 返回 void）
        assertTimeout(Duration.ofSeconds(2), new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable { Thread.sleep(100); }
        });
        System.out.println("assertTimeout: 在2秒内完成");

        // assertTimeoutPreemptively：超时则立即中断线程（注意：会真正中断）
        assertTimeoutPreemptively(Duration.ofSeconds(1),
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable { /* 快速完成 */ }
                });
        System.out.println("assertTimeoutPreemptively: 在1秒内完成");
    }

    @Test
    @DisplayName("assertLinesMatch 列表匹配断言")
    void demoAssertLinesMatch() {
        // Java 8：不支持 String[]，需要转 List
        List<String> expected = Arrays.asList("Line 1", "Line 2", "Line 3");
        List<String> actual   = Arrays.asList("Line 1", "Line 2", "Line 3");
        assertLinesMatch(expected, actual, "列表内容应匹配");
    }

    // ============================================================
    // 3. @Disabled 禁用测试
    // ============================================================

    @Test
    @DisplayName("已启用的测试")
    void enabledTest() {
        assertTrue(true, "正常执行的测试");
    }

    /**
     * @Disabled("原因说明") 可以禁用某个测试方法。
     * 被禁用的测试不会执行，且报告中会有明显标记。
     * 场景：某个功能还在开发中、已知 Bug 暂不修复。
     */
    @Test
    @Disabled("该功能尚未实现，等待后续支持")
    @DisplayName("已禁用的测试（不可用）")
    void disabledTest() {
        fail("此测试已被禁用，不应执行到这里");
    }

    // ============================================================
    // 4. Assumptions 假设跳过
    // ============================================================

    @Test
    @DisplayName("Assumptions 假设跳过")
    void demoAssumptions() {
        // assumeTrue：条件为 false 时跳过当前测试（标记为 ignored）
        assumeTrue(System.getProperty("os.name").contains("Windows"), "只在 Windows 上运行");
        assertEquals("Hello", "Hello", "Windows 环境断言");
    }

    @Test
    @DisplayName("assumingThat 条件执行断言")
    void demoAssumingThat() {
        // assumingThat：只在条件成立时才执行内部断言块
        assumingThat(2 > 1, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable { assertEquals(4, 2 + 2); }
        });
        assertTrue(true); // 无论条件如何都会执行
    }

    // ============================================================
    // 5. 测试信息（TestInfo / TestReporter）
    // ============================================================

    @Test
    @DisplayName("TestInfo 获取测试元信息")
    void demoTestInfo(TestInfo testInfo) {
        System.out.println("  测试方法: " + testInfo.getDisplayName());
        java.util.Optional<Class<?>> testClass = testInfo.getTestClass();
        System.out.println("  测试类:   " + (testClass.isPresent() ? testClass.get().getName() : "未知"));
        for (String tag : testInfo.getTags()) {
            System.out.println("  Tag: " + tag);
        }
        assertNotNull(testInfo);
    }

    // ============================================================
    // 6. 嵌套测试类演示
    // ============================================================

    @Nested
    @DisplayName("嵌套测试 - 字符串工具")
    class StringUtilsTests {

        @Test
        @DisplayName("字符串非空判断")
        void testIsNotBlank() {
            assertTrue(!"".equals(" ") || true); // 演示框架可用
        }

        @Test
        @DisplayName("字符串截断")
        void testAbbreviate() {
            String longStr = "This is a very long string that needs to be abbreviated";
            assertTrue(longStr.length() > 10);
        }
    }

    @Nested
    @DisplayName("嵌套测试 - 集合操作")
    class CollectionTests {

        @Test
        @DisplayName("列表添加元素")
        void testListAdd() {
            List<String> list = new java.util.ArrayList<String>();
            list.add("item");
            assertEquals(1, list.size());
        }
    }
}
