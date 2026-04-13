package com.example.validator;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.*;
import javax.validation.constraints.*;
import java.util.Set;

/**
 * Hibernate Validator 基础演示
 * <p>
 * 展示内容：
 * 1. 常用内置约束注解（@NotNull、@Size、@Pattern 等）
 * 2. 创建 Validator 并执行验证
 * 3. 解析违规信息（ConstraintViolation）
 * 4. 嵌套对象验证（@Valid）
 * </p>
 *
 * @author java-tools-learning
 */
public class HibernateValidatorBasicDemo {

    // ==================== 被验证的实体类 ====================

    /**
     * 用户注册表单 —— 演示常用约束注解
     */
    static class UserRegistration {

        /** 用户名：不能为空，长度 3~20 */
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度必须在 {min} 到 {max} 之间")
        private String username;

        /** 邮箱：格式校验 */
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        /** 密码：最少 8 位 */
        @NotBlank(message = "密码不能为空")
        @Length(min = 8, message = "密码至少需要 {min} 位")
        private String password;

        /** 年龄：18~120 */
        @NotNull(message = "年龄不能为空")
        @Range(min = 18, max = 120, message = "年龄必须在 {min} 到 {max} 之间")
        private Integer age;

        /** 手机号：11 位数字 */
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        /** 网站：可为空，有值则校验 URL 格式 */
        @org.hibernate.validator.constraints.URL(message = "网站地址格式不正确")
        private String website;

        /** 嵌套地址对象：用 @Valid 触发级联验证 */
        @Valid
        @NotNull(message = "地址信息不能为空")
        private Address address;

        // ---------- 构造器 ----------

        public UserRegistration() {}

        public UserRegistration(String username, String email, String password,
                                Integer age, String phone, String website, Address address) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.age = age;
            this.phone = phone;
            this.website = website;
            this.address = address;
        }

        // ---------- Getters（Hibernate Validator 通过 getter 访问属性） ----------

        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getPassword() { return password; }
        public Integer getAge() { return age; }
        public String getPhone() { return phone; }
        public String getWebsite() { return website; }
        public Address getAddress() { return address; }

        @Override
        public String toString() {
            return "UserRegistration{username='" + username + "', email='" + email +
                    "', age=" + age + ", phone='" + phone + "'}";
        }
    }

    /**
     * 地址对象 —— 演示嵌套级联验证
     */
    static class Address {

        @NotBlank(message = "省份不能为空")
        private String province;

        @NotBlank(message = "城市不能为空")
        private String city;

        @NotBlank(message = "街道地址不能为空")
        @Size(max = 100, message = "街道地址不能超过 {max} 个字符")
        private String street;

        public Address() {}

        public Address(String province, String city, String street) {
            this.province = province;
            this.city = city;
            this.street = street;
        }

        public String getProvince() { return province; }
        public String getCity() { return city; }
        public String getStreet() { return street; }
    }

    // ==================== 主程序 ====================

    public static void main(String[] args) {
        System.out.println("========== Hibernate Validator 基础演示 ==========\n");

        // 1. 获取 ValidatorFactory 和 Validator（推荐用单例，线程安全）
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // 2. 演示：验证正常数据（应该无违规）
        demo1_ValidData(validator);

        // 3. 演示：验证错误数据（多种违规）
        demo2_InvalidData(validator);

        // 4. 演示：嵌套对象验证
        demo3_NestedValidation(validator);

        // 5. 演示：只验证指定字段
        demo4_ValidateProperty(validator);

        factory.close();
    }

    /**
     * 演示1：验证合法数据
     */
    private static void demo1_ValidData(Validator validator) {
        System.out.println("--- 演示1：验证合法用户数据 ---");

        Address addr = new Address("广东省", "深圳市", "南山区科技园南区1号楼");
        UserRegistration user = new UserRegistration(
                "zhang_san",
                "zhangsan@example.com",
                "password123",
                25,
                "13812345678",
                "https://www.example.com",
                addr
        );

        Set<ConstraintViolation<UserRegistration>> violations = validator.validate(user);

        if (violations.isEmpty()) {
            System.out.println("✅ 验证通过！用户数据合法：" + user);
        } else {
            printViolations(violations);
        }
        System.out.println();
    }

    /**
     * 演示2：验证非法数据
     */
    private static void demo2_InvalidData(Validator validator) {
        System.out.println("--- 演示2：验证非法用户数据（多项违规）---");

        Address addr = new Address("广东省", "深圳市", "科技园");
        UserRegistration user = new UserRegistration(
                "ab",                   // 太短（<3）
                "not-an-email",         // 邮箱格式错误
                "123",                  // 密码太短（<8）
                15,                     // 年龄不足（<18）
                "12345",                // 手机号格式错误
                "not-a-url",            // URL 格式错误
                addr
        );

        Set<ConstraintViolation<UserRegistration>> violations = validator.validate(user);
        System.out.println("发现 " + violations.size() + " 项违规：");
        printViolations(violations);
        System.out.println();
    }

    /**
     * 演示3：嵌套对象级联验证
     */
    private static void demo3_NestedValidation(Validator validator) {
        System.out.println("--- 演示3：嵌套对象级联验证 ---");

        // 地址对象有空字段
        Address badAddr = new Address("", null, "某街道");
        UserRegistration user = new UserRegistration(
                "lisi",
                "lisi@example.com",
                "securePass1",
                30,
                "13900001234",
                null,       // website 可为空
                badAddr     // 地址字段有问题
        );

        Set<ConstraintViolation<UserRegistration>> violations = validator.validate(user);
        if (violations.isEmpty()) {
            System.out.println("✅ 验证通过");
        } else {
            System.out.println("发现嵌套违规 " + violations.size() + " 项：");
            printViolations(violations);
        }
        System.out.println();
    }

    /**
     * 演示4：只验证某个属性（validateProperty）
     */
    private static void demo4_ValidateProperty(Validator validator) {
        System.out.println("--- 演示4：只验证指定属性（validateProperty）---");

        Address addr = new Address("广东省", "深圳市", "测试路");
        UserRegistration user = new UserRegistration(
                "x",                // 用户名太短
                "good@example.com",
                "password999",
                28,
                "13800001234",
                null,
                addr
        );

        // 只检验 username 字段
        Set<ConstraintViolation<UserRegistration>> violations =
                validator.validateProperty(user, "username");

        System.out.println("仅验证 username 字段，发现 " + violations.size() + " 项违规：");
        printViolations(violations);
        System.out.println();
    }

    /**
     * 打印所有违规信息
     */
    private static <T> void printViolations(Set<ConstraintViolation<T>> violations) {
        for (ConstraintViolation<T> v : violations) {
            System.out.println("  ❌ 字段: " + v.getPropertyPath()
                    + "  |  值: " + v.getInvalidValue()
                    + "  |  原因: " + v.getMessage());
        }
    }
}
