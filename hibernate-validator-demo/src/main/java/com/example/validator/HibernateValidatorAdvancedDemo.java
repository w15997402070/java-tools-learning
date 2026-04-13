package com.example.validator;

import javax.validation.*;
import javax.validation.constraints.*;
import javax.validation.groups.Default;
import java.lang.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Hibernate Validator 高级演示
 * <p>
 * 展示内容：
 * 1. 自定义约束注解（@PhoneNumber）
 * 2. 验证分组（Groups） —— 不同场景用不同校验规则
 * 3. 跨字段验证（类级别约束）—— 密码确认
 * 4. 集合/数组元素验证
 * </p>
 *
 * @author java-tools-learning
 */
public class HibernateValidatorAdvancedDemo {

    // ==================== 1. 自定义注解 @PhoneNumber ====================

    /**
     * 自定义手机号验证注解
     * 支持中国大陆 11 位手机号（1 开头，第二位 3-9）
     */
    @Documented
    @Constraint(validatedBy = PhoneNumberValidator.class)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PhoneNumber {
        String message() default "手机号格式不正确，需为 11 位中国大陆号码";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        /** 是否允许为空（空值直接通过） */
        boolean nullable() default true;
    }

    /**
     * 手机号验证器实现
     */
    public static class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

        private boolean nullable;

        @Override
        public void initialize(PhoneNumber annotation) {
            this.nullable = annotation.nullable();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // 空值处理
            if (value == null || value.trim().isEmpty()) {
                return nullable; // nullable=true 时空值通过
            }
            // 验证格式：1[3-9]\d{9}
            return value.matches("^1[3-9]\\d{9}$");
        }
    }

    // ==================== 2. 验证分组 ====================

    /**
     * 分组接口：新增操作（POST）
     */
    public interface OnCreate extends Default {}

    /**
     * 分组接口：更新操作（PUT）
     */
    public interface OnUpdate extends Default {}

    /**
     * 产品 DTO，不同接口用不同的验证规则
     */
    static class ProductDTO {

        /** id 仅在更新时需要 */
        @NotNull(groups = OnUpdate.class, message = "更新时 ID 不能为空")
        @Null(groups = OnCreate.class, message = "新增时不应传入 ID")
        private Long id;

        /** 名称始终必填 */
        @NotBlank(message = "产品名称不能为空")
        @Size(max = 50, message = "产品名称不能超过 {max} 个字符")
        private String name;

        /** 价格：新增时必填，更新时可选 */
        @NotNull(groups = OnCreate.class, message = "新增时价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于 {value}")
        @DecimalMax(value = "9999999.99", message = "价格不能超过 {value}")
        private Double price;

        /** 库存 */
        @Min(value = 0, message = "库存不能为负数")
        private Integer stock;

        public ProductDTO(Long id, String name, Double price, Integer stock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.stock = stock;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public Double getPrice() { return price; }
        public Integer getStock() { return stock; }
    }

    // ==================== 3. 跨字段验证（类级别约束）====================

    /**
     * 密码确认一致性注解（类级别）
     */
    @Documented
    @Constraint(validatedBy = PasswordMatchValidator.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PasswordMatch {
        String message() default "两次输入的密码不一致";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    /**
     * 密码匹配验证器（类级别）
     */
    public static class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, ChangePasswordForm> {

        @Override
        public boolean isValid(ChangePasswordForm form, ConstraintValidatorContext context) {
            if (form.getNewPassword() == null || form.getConfirmPassword() == null) {
                return true; // null 由 @NotBlank 处理
            }
            boolean match = form.getNewPassword().equals(form.getConfirmPassword());
            if (!match) {
                // 将违规绑定到 confirmPassword 字段，而非整个类
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                        .addPropertyNode("confirmPassword")
                        .addConstraintViolation();
            }
            return match;
        }
    }

    /**
     * 修改密码表单 —— 使用类级别约束
     */
    @PasswordMatch
    static class ChangePasswordForm {

        @NotBlank(message = "原密码不能为空")
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, message = "新密码至少 {min} 位")
        private String newPassword;

        @NotBlank(message = "确认密码不能为空")
        private String confirmPassword;

        public ChangePasswordForm(String oldPassword, String newPassword, String confirmPassword) {
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
            this.confirmPassword = confirmPassword;
        }

        public String getOldPassword() { return oldPassword; }
        public String getNewPassword() { return newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
    }

    // ==================== 4. 集合元素验证 ====================

    /**
     * 订单对象 —— 演示集合/列表元素验证
     */
    static class Order {

        @NotBlank(message = "订单号不能为空")
        private String orderNo;

        /** @Size 作用于集合本身（至少1个商品）*/
        @NotNull(message = "商品列表不能为空")
        @Size(min = 1, message = "订单至少包含 {min} 件商品")
        private List<@NotBlank(message = "商品名称不能为空") String> items;

        /** 标签列表中不能有 null 元素 */
        private List<@NotNull(message = "标签不能为 null") String> tags;

        public Order(String orderNo, List<String> items, List<String> tags) {
            this.orderNo = orderNo;
            this.items = items;
            this.tags = tags;
        }

        public String getOrderNo() { return orderNo; }
        public List<String> getItems() { return items; }
        public List<String> getTags() { return tags; }
    }

    // ==================== 主程序 ====================

    public static void main(String[] args) {
        System.out.println("========== Hibernate Validator 高级演示 ==========\n");

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        demo1_CustomAnnotation(validator);
        demo2_Groups(validator);
        demo3_CrossFieldValidation(validator);
        demo4_CollectionValidation(validator);

        factory.close();
    }

    /**
     * 演示1：自定义 @PhoneNumber 约束注解
     */
    private static void demo1_CustomAnnotation(Validator validator) {
        System.out.println("--- 演示1：自定义 @PhoneNumber 注解 ---");

        // 创建一个含 @PhoneNumber 字段的简单 Bean
        PhoneBean valid = new PhoneBean("13812345678");
        PhoneBean invalid = new PhoneBean("12345");
        PhoneBean empty = new PhoneBean(null);

        System.out.println("手机号 13812345678：" + (validator.validate(valid).isEmpty() ? "✅ 通过" : "❌ 失败"));
        Set<ConstraintViolation<PhoneBean>> v2 = validator.validate(invalid);
        System.out.println("手机号 12345：" + (v2.isEmpty() ? "✅ 通过" : "❌ " + v2.iterator().next().getMessage()));
        System.out.println("手机号 null（nullable=true）：" + (validator.validate(empty).isEmpty() ? "✅ 通过" : "❌ 失败"));
        System.out.println();
    }

    /**
     * 用于自定义注解演示的简单 Bean
     */
    static class PhoneBean {
        @PhoneNumber(nullable = true)
        private String phone;
        public PhoneBean(String phone) { this.phone = phone; }
        public String getPhone() { return phone; }
    }

    /**
     * 演示2：分组验证
     */
    private static void demo2_Groups(Validator validator) {
        System.out.println("--- 演示2：分组验证（新增 vs 更新）---");

        // 新增：id 应为 null，price 必填
        ProductDTO createDTO = new ProductDTO(null, "Apple MacBook Pro", 12999.0, 50);
        Set<ConstraintViolation<ProductDTO>> createViolations =
                validator.validate(createDTO, OnCreate.class);
        System.out.println("新增时验证（id=null，price有值）：" +
                (createViolations.isEmpty() ? "✅ 通过" : "❌ " + createViolations.size() + " 项违规"));

        // 新增：id 不该有值
        ProductDTO createWithId = new ProductDTO(1L, "Test", 99.0, 10);
        Set<ConstraintViolation<ProductDTO>> badCreate =
                validator.validate(createWithId, OnCreate.class);
        System.out.println("新增时传入 id（应报错）：" +
                (badCreate.isEmpty() ? "✅ 通过" : "❌ " + badCreate.iterator().next().getMessage()));

        // 更新：id 必填
        ProductDTO updateDTO = new ProductDTO(null, "Test Product", null, 5);
        Set<ConstraintViolation<ProductDTO>> updateViolations =
                validator.validate(updateDTO, OnUpdate.class);
        System.out.println("更新时 id=null（应报错）：" +
                (updateViolations.isEmpty() ? "✅ 通过" : "❌ " + updateViolations.iterator().next().getMessage()));
        System.out.println();
    }

    /**
     * 演示3：跨字段验证（密码确认）
     */
    private static void demo3_CrossFieldValidation(Validator validator) {
        System.out.println("--- 演示3：跨字段验证（密码确认一致性）---");

        ChangePasswordForm ok = new ChangePasswordForm("oldPass1", "newPass123", "newPass123");
        Set<ConstraintViolation<ChangePasswordForm>> v1 = validator.validate(ok);
        System.out.println("新密码 == 确认密码：" + (v1.isEmpty() ? "✅ 通过" : "❌ 失败"));

        ChangePasswordForm mismatch = new ChangePasswordForm("oldPass1", "newPass123", "differentPass");
        Set<ConstraintViolation<ChangePasswordForm>> v2 = validator.validate(mismatch);
        System.out.println("新密码 != 确认密码（应报错）：" +
                (v2.isEmpty() ? "✅ 通过" : "❌ " + v2.iterator().next().getMessage()));
        System.out.println();
    }

    /**
     * 演示4：集合元素验证
     */
    private static void demo4_CollectionValidation(Validator validator) {
        System.out.println("--- 演示4：集合元素验证 ---");

        // 正常订单
        Order goodOrder = new Order("ORD-001",
                Arrays.asList("MacBook Pro", "AirPods"),
                Arrays.asList("电子产品", "数码"));
        Set<ConstraintViolation<Order>> v1 = validator.validate(goodOrder);
        System.out.println("正常订单：" + (v1.isEmpty() ? "✅ 通过" : "❌ " + v1.size() + " 项违规"));

        // 商品名有空字符串
        Order badOrder = new Order("ORD-002",
                Arrays.asList("MacBook", "", null),
                Arrays.asList("数码", null));
        Set<ConstraintViolation<Order>> v2 = validator.validate(badOrder);
        System.out.println("商品列表含空/null 元素（应报错）：");
        for (ConstraintViolation<Order> v : v2) {
            System.out.println("  ❌ " + v.getPropertyPath() + " -> " + v.getMessage());
        }
        System.out.println();
    }
}
