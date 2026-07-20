package com.example.junit5;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 实战演示类
 *
 * 本类模拟真实业务场景的测试用例：
 * 1. 业务模型测试：User / TestOrder 实体类的核心逻辑
 * 2. Service 层测试：UserService（含参数校验、异常情况）
 * 3. 工具类测试：TestOrder 订单状态机
 * 4. 测试执行顺序（@TestMethodOrder）
 * 5. 测试生命周期范围（@TestInstance）
 * 6. 自定义 Assertions 工具类封装
 * 7. @Nested 真实业务场景嵌套分组
 *
 * 注意：内部类名为 TestOrder（而非 Order），避免与 JUnit Jupiter 的 @Order 注解冲突。
 */
@DisplayName("JUnit5 实战场景演示")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JUnit5PracticalDemo {

    // ============================================================
    // 1. 实战模型：User 实体
    // ============================================================

    static class User {
        private final Long id;
        private final String username;
        private final String email;
        private final int age;
        private final UserStatus status;

        enum UserStatus { ACTIVE, INACTIVE, BANNED }

        User(Long id, String username, String email, int age, UserStatus status) {
            this.id = id; this.username = username; this.email = email;
            this.age = age; this.status = status;
        }
        Long        getId()       { return id; }
        String      getUsername() { return username; }
        String      getEmail()    { return email; }
        int         getAge()      { return age; }
        UserStatus  getStatus()   { return status; }

        /** 业务规则：用户名非空，年龄 18~100，邮箱含 @ */
        boolean isValid() {
            return username != null && !username.isEmpty()
                    && age >= 18 && age <= 100
                    && email != null && email.contains("@");
        }
    }

    // ============================================================
    // 2. 实战模型：TestOrder 订单（注意：类名避开 @Order 注解）
    // ============================================================

    // 使用 TestOrder 而非 Order，避免与 org.junit.jupiter.api.Order 注解冲突
    static class TestOrder {
        private Long id;
        private TestOrderStatus status;
        private double amount;
        private LocalDateTime createTime;
        private LocalDateTime payTime;

        enum TestOrderStatus {
            PENDING, PAID, SHIPPED, COMPLETED, CANCELLED, REFUNDED
        }

        TestOrder(Long id, TestOrderStatus status, double amount) {
            this.id = id; this.status = status; this.amount = amount;
            this.createTime = LocalDateTime.now();
        }

        void pay() {
            if (this.status != TestOrderStatus.PENDING)
                throw new IllegalStateException("只有待支付订单可以支付");
            this.status = TestOrderStatus.PAID;
            this.payTime = LocalDateTime.now();
        }

        void ship() {
            if (this.status != TestOrderStatus.PAID)
                throw new IllegalStateException("只有已支付订单可以发货");
            this.status = TestOrderStatus.SHIPPED;
        }

        void complete() {
            if (this.status != TestOrderStatus.SHIPPED)
                throw new IllegalStateException("只有已发货订单可以完成");
            this.status = TestOrderStatus.COMPLETED;
        }

        void cancel() {
            if (this.status == TestOrderStatus.SHIPPED || this.status == TestOrderStatus.COMPLETED)
                throw new IllegalStateException("已发货或已完成的订单不可取消");
            this.status = TestOrderStatus.CANCELLED;
        }

        Long            getId()          { return id; }
        TestOrderStatus getStatus()      { return status; }
        double          getAmount()       { return amount; }
        LocalDateTime   getPayTime()     { return payTime; }
    }

    // ============================================================
    // 3. 实战 Service：UserService
    // ============================================================

    static class UserService {
        private final Map<Long, User> store = new HashMap<Long, User>();
        private long nextId = 1L;

        /** 创建用户，校验规则：用户名 2~20 字符，邮箱唯一，年龄 18~100 */
        User createUser(String username, String email, int age) {
            if (username == null || username.length() < 2 || username.length() > 20) {
                throw new AssertionError("用户名必须 2~20 字符");
            }
            for (User u : store.values()) {
                if (u.getEmail().equals(email)) {
                    throw new AssertionError("邮箱已被注册");
                }
            }
            if (age < 18 || age > 100) {
                throw new AssertionError("年龄必须在 18~100 岁之间");
            }
            User user = new User(nextId++, username, email, age, User.UserStatus.ACTIVE);
            store.put(user.getId(), user);
            return user;
        }

        User findById(Long id) { return store.get(id); }

        List<User> findAll() { return new ArrayList<User>(store.values()); }
    }

    // ============================================================
    // 4. 测试 User 模型
    // ============================================================

    @Nested
    @DisplayName("User 模型测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UserModelTests {

        @Test
        @Order(1)
        @DisplayName("正常用户应通过校验")
        void validUserShouldPass() {
            User user = new User(1L, "张三", "zhangsan@example.com", 25, User.UserStatus.ACTIVE);
            assertTrue(user.isValid());
        }

        @Test
        @Order(2)
        @DisplayName("空用户名应校验失败")
        void emptyUsernameShouldFail() {
            User user = new User(2L, "", "test@example.com", 20, User.UserStatus.ACTIVE);
            assertFalse(user.isValid());
        }

        @Test
        @Order(3)
        @DisplayName("年龄超出范围应校验失败")
        void invalidAgeShouldFail() {
            User user1 = new User(3L, "李四", "lisi@example.com", -1, User.UserStatus.ACTIVE);
            User user2 = new User(4L, "王五", "wangwu@example.com", 200, User.UserStatus.ACTIVE);
            assertFalse(user1.isValid());
            assertFalse(user2.isValid());
        }

        @ParameterizedTest
        @Order(4)
        @MethodSource("com.example.junit5.JUnit5PracticalDemo#userProvider")
        @DisplayName("参数化测试：多用户场景")
        void testMultipleUsers(User user, boolean expectedValid) {
            assertEquals(expectedValid, user.isValid());
        }
    }

    static Stream<Arguments> userProvider() {
        return Stream.of(
                Arguments.of(new User(10L, "Alice", "alice@example.com", 22, User.UserStatus.ACTIVE), true),
                Arguments.of(new User(11L, "Bob", "bob@example.com", 17, User.UserStatus.INACTIVE), false),
                Arguments.of(new User(12L, "Charlie", "charlie", 30, User.UserStatus.ACTIVE), false),
                Arguments.of(new User(13L, "", "david@example.com", 25, User.UserStatus.ACTIVE), false)
        );
    }

    // ============================================================
    // 5. 测试 TestOrder 状态机
    // ============================================================

    @Nested
    @DisplayName("TestOrder 订单状态机测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class OrderStateMachineTests {

        @Test
        @DisplayName("订单正常流转：待支付 → 已支付 → 已发货 → 已完成")
        void normalOrderFlow() {
            TestOrder order = new TestOrder(100L, TestOrder.TestOrderStatus.PENDING, 99.9);

            order.pay();
            assertEquals(TestOrder.TestOrderStatus.PAID, order.getStatus());
            assertNotNull(order.getPayTime());

            order.ship();
            assertEquals(TestOrder.TestOrderStatus.SHIPPED, order.getStatus());

            order.complete();
            assertEquals(TestOrder.TestOrderStatus.COMPLETED, order.getStatus());
        }

        @Test
        @DisplayName("待支付订单取消")
        void cancelPendingOrder() {
            TestOrder order = new TestOrder(101L, TestOrder.TestOrderStatus.PENDING, 50.0);
            order.cancel();
            assertEquals(TestOrder.TestOrderStatus.CANCELLED, order.getStatus());
        }

        @Test
        @DisplayName("已完成订单不可取消 → 抛出异常")
        void completedOrderCannotCancel() {
            TestOrder order = new TestOrder(102L, TestOrder.TestOrderStatus.COMPLETED, 50.0);
            IllegalStateException ex = assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
                public void execute() throws Throwable { order.cancel(); }
            });
            assertTrue(ex.getMessage().contains("已发货或已完成"));
        }

        @Test
        @DisplayName("未支付订单不可发货 → 抛出异常")
        void unpaidOrderCannotShip() {
            TestOrder order = new TestOrder(103L, TestOrder.TestOrderStatus.PENDING, 50.0);
            assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
                public void execute() throws Throwable { order.ship(); }
            });
        }

        @ParameterizedTest
        @CsvSource({
            "PENDING,    true",
            "PAID,       true",
            "CANCELLED,  true",
            "SHIPPED,    false",
            "COMPLETED,  false",
            "REFUNDED,   true"
        })
        @DisplayName("参数化测试：各状态下取消订单行为")
        void cancelAtDifferentStatus(TestOrder.TestOrderStatus initialStatus, boolean shouldSucceed) {
            TestOrder order = new TestOrder(200L, initialStatus, 100.0);
            if (shouldSucceed) {
                order.cancel();
                assertEquals(TestOrder.TestOrderStatus.CANCELLED, order.getStatus());
            } else {
                // SHIPPED/COMPLETED 状态取消会抛出 IllegalStateException
                assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable { order.cancel(); }
                });
            }
        }
    }

    // ============================================================
    // 6. 测试 UserService
    // ============================================================

    @Nested
    @DisplayName("UserService 业务测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class UserServiceTests {

        private UserService userService;

        @BeforeEach
        void setUp() {
            userService = new UserService();
        }

        @Test
        @Order(1)
        @DisplayName("正常创建用户")
        void createUserSuccess() {
            User user = userService.createUser("testuser", "test@example.com", 25);
            assertNotNull(user.getId());
            assertEquals("testuser", user.getUsername());
            assertEquals("test@example.com", user.getEmail());
            assertEquals(25, user.getAge());
            assertEquals(User.UserStatus.ACTIVE, user.getStatus());
        }

        @Test
        @Order(2)
        @DisplayName("用户名过短应抛出异常")
        void usernameTooShort() {
            AssertionError ex = assertThrows(AssertionError.class,
                    new org.junit.jupiter.api.function.Executable() {
                        public void execute() throws Throwable {
                            userService.createUser("A", "a@example.com", 25);
                        }
                    });
            assertTrue(ex.getMessage().contains("2~20"));
        }

        @Test
        @Order(3)
        @DisplayName("邮箱重复应抛出异常")
        void duplicateEmail() {
            userService.createUser("user1", "dup@example.com", 20);
            assertThrows(AssertionError.class,
                    new org.junit.jupiter.api.function.Executable() {
                        public void execute() throws Throwable {
                            userService.createUser("user2", "dup@example.com", 30);
                        }
                    });
        }

        @Test
        @Order(4)
        @DisplayName("findById 查询")
        void findById() {
            User created = userService.createUser("findme", "find@example.com", 22);
            User found = userService.findById(created.getId());
            assertEquals(created.getUsername(), found.getUsername());
        }

        @Test
        @Order(5)
        @DisplayName("findAll 返回所有用户")
        void findAll() {
            userService.createUser("u1", "u1@example.com", 20);
            userService.createUser("u2", "u2@example.com", 21);
            assertEquals(2, userService.findAll().size());
        }
    }

    // ============================================================
    // 7. 测试执行顺序（@TestMethodOrder）
    // ============================================================

    static int counter = 0;

    @Test
    @Order(10)
    @DisplayName("执行顺序测试 - 第1个")
    @Tag("order-test")
    void testOrder1() {
        assertEquals(0, counter);
        counter++;
    }

    @Test
    @Order(20)
    @DisplayName("执行顺序测试 - 第2个")
    @Tag("order-test")
    void testOrder2() {
        assertEquals(1, counter);
        counter++;
    }

    @Test
    @Order(30)
    @DisplayName("执行顺序测试 - 第3个")
    @Tag("order-test")
    void testOrder3() {
        assertEquals(2, counter);
        counter = 0; // 重置，为其他测试类留清净环境
    }

    // ============================================================
    // 8. 自定义断言工具类演示
    // ============================================================

    @Nested
    @DisplayName("自定义断言封装演示")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CustomAssertionsDemo {

        @Test
        @Order(1)
        @DisplayName("使用 assertAll 批量校验 User 对象")
        void assertAllUserFields() {
            User user = new User(1L, "Alice", "alice@example.com", 22, User.UserStatus.ACTIVE);

            // 批量断言：所有字段同时校验，一次性报告所有失败
            assertAll("User 字段完整性",
                    new org.junit.jupiter.api.function.Executable() {
                        public void execute() throws Throwable { assertEquals(1L, user.getId()); }
                    },
                    new org.junit.jupiter.api.function.Executable() {
                        public void execute() throws Throwable { assertEquals("Alice", user.getUsername()); }
                    },
                    new org.junit.jupiter.api.function.Executable() {
                        public void execute() throws Throwable { assertEquals("alice@example.com", user.getEmail()); }
                    },
                    new org.junit.jupiter.api.function.Executable() {
                        public void execute() throws Throwable { assertEquals(22, user.getAge()); }
                    },
                    new org.junit.jupiter.api.function.Executable() {
                        public void execute() throws Throwable { assertEquals(User.UserStatus.ACTIVE, user.getStatus()); }
                    },
                    new org.junit.jupiter.api.function.Executable() {
                        public void execute() throws Throwable { assertTrue(user.isValid()); }
                    }
            );
        }

        @Test
        @Order(2)
        @DisplayName("使用 assertThrows + assertDoesNotThrow 组合")
        void assertThrowsCombination() {
            TestOrder pending = new TestOrder(1L, TestOrder.TestOrderStatus.PENDING, 50.0);
            TestOrder completed = new TestOrder(2L, TestOrder.TestOrderStatus.COMPLETED, 50.0);

            assertDoesNotThrow(new org.junit.jupiter.api.function.Executable() {
                public void execute() throws Throwable { pending.pay(); }
            }, "待支付可以支付");

            assertThrows(IllegalStateException.class,
                    new org.junit.jupiter.api.function.Executable() {
                        public void execute() throws Throwable { completed.pay(); }
                    },
                    "已完成不能支付");
        }
    }
}
