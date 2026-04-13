package com.example.validator;

import org.hibernate.validator.constraints.Range;

import javax.validation.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Hibernate Validator 实战演示
 * <p>
 * 展示内容：
 * 1. 电商场景：订单接口参数验证（模拟 Spring MVC Controller 层）
 * 2. 快速失败模式（failFast）—— 遇到第一个错误即停止
 * 3. 编程式验证 vs 注解式验证（对比）
 * 4. 自定义错误消息国际化
 * 5. 实战工具类封装（ValidatorUtils）
 * </p>
 *
 * @author java-tools-learning
 */
public class HibernateValidatorPracticalDemo {

    // ==================== 1. 电商场景：创建订单请求 ====================

    /**
     * 创建订单请求参数
     * 模拟真实业务场景中的接口入参验证
     */
    static class CreateOrderRequest {

        /** 用户 ID，必填 */
        @NotNull(message = "用户ID不能为空")
        @Min(value = 1, message = "用户ID必须大于0")
        private Long userId;

        /** 收货地址 ID */
        @NotNull(message = "收货地址不能为空")
        private Long addressId;

        /** 商品列表，至少1件，最多100件 */
        @NotNull(message = "商品列表不能为空")
        @Size(min = 1, max = 100, message = "商品数量必须在 {min} 到 {max} 之间")
        private List<@Valid OrderItem> items;

        /** 备注，可选，最多200字 */
        @Size(max = 200, message = "备注不能超过 {max} 个字符")
        private String remark;

        /** 优惠券码，可选，8位字母数字 */
        @Pattern(regexp = "^[A-Z0-9]{8}$", message = "优惠券码格式不正确（8位大写字母/数字）")
        private String couponCode;

        /** 期望送达日期：必须是未来日期 */
        @Future(message = "期望送达日期必须是未来日期")
        private LocalDate expectedDate;

        public CreateOrderRequest(Long userId, Long addressId, List<OrderItem> items,
                                  String remark, String couponCode, LocalDate expectedDate) {
            this.userId = userId;
            this.addressId = addressId;
            this.items = items;
            this.remark = remark;
            this.couponCode = couponCode;
            this.expectedDate = expectedDate;
        }

        public Long getUserId() { return userId; }
        public Long getAddressId() { return addressId; }
        public List<OrderItem> getItems() { return items; }
        public String getRemark() { return remark; }
        public String getCouponCode() { return couponCode; }
        public LocalDate getExpectedDate() { return expectedDate; }
    }

    /**
     * 订单商品项
     */
    static class OrderItem {

        @NotNull(message = "商品ID不能为空")
        @Min(value = 1, message = "商品ID无效")
        private Long productId;

        @NotNull(message = "购买数量不能为空")
        @Range(min = 1, max = 999, message = "购买数量必须在 {min} 到 {max} 之间")
        private Integer quantity;

        /** 单价（业务逻辑里通常由服务端查询，这里作为演示） */
        @NotNull(message = "单价不能为空")
        @DecimalMin(value = "0.01", inclusive = true, message = "单价不合法")
        private BigDecimal unitPrice;

        public OrderItem(Long productId, Integer quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public Long getProductId() { return productId; }
        public Integer getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
    }

    // ==================== 2. 验证工具类封装 ====================

    /**
     * 验证结果封装（模拟业务层统一返回）
     */
    static class ValidationResult {
        private final boolean valid;
        private final Map<String, String> errors; // field -> message

        private ValidationResult(boolean valid, Map<String, String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, Collections.emptyMap());
        }

        public static ValidationResult failure(Map<String, String> errors) {
            return new ValidationResult(false, errors);
        }

        public boolean isValid() { return valid; }
        public Map<String, String> getErrors() { return errors; }

        @Override
        public String toString() {
            if (valid) return "✅ 验证通过";
            return "❌ 验证失败，错误信息：" + errors;
        }
    }

    /**
     * 验证工具类 —— 企业项目中常用的封装方式
     */
    static class ValidatorUtils {

        /** 单例 Validator（线程安全，全局共用） */
        private static final Validator VALIDATOR;

        /** 快速失败模式 Validator */
        private static final Validator FAIL_FAST_VALIDATOR;

        static {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            VALIDATOR = factory.getValidator();

            // 快速失败：遇到第一个错误立即停止，适合对性能敏感的场景
            FAIL_FAST_VALIDATOR = Validation.byDefaultProvider()
                    .configure()
                    .addProperty("hibernate.validator.fail_fast", "true")
                    .buildValidatorFactory()
                    .getValidator();
        }

        /**
         * 验证对象，返回所有违规
         *
         * @param obj    被验证对象
         * @param groups 验证分组
         * @return ValidationResult
         */
        public static <T> ValidationResult validate(T obj, Class<?>... groups) {
            Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj, groups);
            if (violations.isEmpty()) {
                return ValidationResult.success();
            }
            Map<String, String> errors = new LinkedHashMap<>();
            for (ConstraintViolation<T> v : violations) {
                errors.put(v.getPropertyPath().toString(), v.getMessage());
            }
            return ValidationResult.failure(errors);
        }

        /**
         * 快速失败验证（发现第一个错误即返回）
         *
         * @param obj    被验证对象
         * @return ValidationResult
         */
        public static <T> ValidationResult validateFailFast(T obj) {
            Set<ConstraintViolation<T>> violations = FAIL_FAST_VALIDATOR.validate(obj);
            if (violations.isEmpty()) {
                return ValidationResult.success();
            }
            ConstraintViolation<T> first = violations.iterator().next();
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put(first.getPropertyPath().toString(), first.getMessage());
            return ValidationResult.failure(errors);
        }

        /**
         * 验证并在失败时直接抛出异常（适合 Service 层直接使用）
         *
         * @throws IllegalArgumentException 包含首个错误信息
         */
        public static <T> void validateAndThrow(T obj) {
            Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj);
            if (!violations.isEmpty()) {
                ConstraintViolation<T> first = violations.iterator().next();
                throw new IllegalArgumentException(
                        first.getPropertyPath() + ": " + first.getMessage());
            }
        }
    }

    // ==================== 主程序 ====================

    public static void main(String[] args) {
        System.out.println("========== Hibernate Validator 实战演示 ==========\n");

        demo1_OrderValidation();
        demo2_FailFastMode();
        demo3_ValidatorUtils();
        demo4_ProgrammaticValidation();
    }

    /**
     * 演示1：电商订单参数验证
     */
    private static void demo1_OrderValidation() {
        System.out.println("--- 演示1：电商订单创建参数验证 ---");

        // 正常订单
        List<OrderItem> items = Arrays.asList(
                new OrderItem(101L, 2, new BigDecimal("299.00")),
                new OrderItem(202L, 1, new BigDecimal("1299.00"))
        );
        CreateOrderRequest goodReq = new CreateOrderRequest(
                1001L,
                5L,
                items,
                "请尽快发货",
                null,
                LocalDate.now().plusDays(3)
        );

        ValidationResult r1 = ValidatorUtils.validate(goodReq);
        System.out.println("正常订单：" + r1);

        // 错误订单
        List<OrderItem> badItems = Arrays.asList(
                new OrderItem(null, 0, null),   // productId/quantity/unitPrice 均有问题
                new OrderItem(303L, 1000, new BigDecimal("0.00"))  // quantity 超范围，单价为0
        );
        CreateOrderRequest badReq = new CreateOrderRequest(
                null,   // userId 为 null
                null,   // addressId 为 null
                badItems,
                null,
                "abc",  // 优惠券格式错误
                LocalDate.now().minusDays(1)    // 过去日期
        );

        ValidationResult r2 = ValidatorUtils.validate(badReq);
        System.out.println("错误订单（多项违规）：");
        r2.getErrors().forEach((field, msg) ->
                System.out.println("  ❌ " + field + ": " + msg));
        System.out.println();
    }

    /**
     * 演示2：快速失败模式
     */
    private static void demo2_FailFastMode() {
        System.out.println("--- 演示2：快速失败模式（failFast）---");

        List<OrderItem> items = Collections.singletonList(
                new OrderItem(1L, 1, new BigDecimal("99.00"))
        );
        CreateOrderRequest req = new CreateOrderRequest(
                null,   // userId 错误
                null,   // addressId 错误
                items,
                "x".repeat(300),    // 备注过长
                "INVALID",          // 优惠券格式错误
                LocalDate.now().minusDays(1)    // 过去日期
        );

        // 普通模式：收集所有错误
        ValidationResult allErrors = ValidatorUtils.validate(req);
        System.out.println("普通模式收集到 " + allErrors.getErrors().size() + " 个错误");

        // 快速失败：只返回第一个错误
        ValidationResult firstError = ValidatorUtils.validateFailFast(req);
        System.out.println("快速失败模式（仅第一个错误）：" + firstError);
        System.out.println();
    }

    /**
     * 演示3：ValidatorUtils 工具类用法
     */
    private static void demo3_ValidatorUtils() {
        System.out.println("--- 演示3：ValidatorUtils 实用封装 ---");

        // 验证通过
        List<OrderItem> items = Collections.singletonList(
                new OrderItem(1L, 2, new BigDecimal("199.00"))
        );
        CreateOrderRequest validReq = new CreateOrderRequest(
                100L, 1L, items, null, null, LocalDate.now().plusDays(7)
        );
        System.out.println("validate()：" + ValidatorUtils.validate(validReq));

        // validateAndThrow 示例
        try {
            CreateOrderRequest empty = new CreateOrderRequest(
                    null, null, Collections.emptyList(), null, null, null
            );
            ValidatorUtils.validateAndThrow(empty);
        } catch (IllegalArgumentException e) {
            System.out.println("validateAndThrow 抛出异常：" + e.getMessage());
        }
        System.out.println();
    }

    /**
     * 演示4：不使用注解，完全编程式设置约束
     * 说明：展示编程式 API 的灵活性（实际项目较少用）
     */
    private static void demo4_ProgrammaticValidation() {
        System.out.println("--- 演示4：编程式验证（不依赖注解）---");

        // 场景：运行时动态决定字段的验证规则
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("username", "");
        formData.put("age", -1);
        formData.put("email", "bad-email");

        // 手动规则：username 非空、age > 0、email 含 @
        List<String> errors = new ArrayList<>();

        String username = (String) formData.get("username");
        if (username == null || username.trim().isEmpty()) {
            errors.add("username: 不能为空");
        }

        Integer age = (Integer) formData.get("age");
        if (age == null || age <= 0) {
            errors.add("age: 必须大于0，当前值=" + age);
        }

        String email = (String) formData.get("email");
        if (email == null || !email.contains("@")) {
            errors.add("email: 格式不正确，当前值=" + email);
        }

        if (errors.isEmpty()) {
            System.out.println("✅ 编程式验证通过");
        } else {
            System.out.println("编程式验证发现 " + errors.size() + " 个错误：");
            errors.forEach(e -> System.out.println("  ❌ " + e));
        }

        System.out.println("\n💡 提示：编程式验证适合动态规则（如表单配置化），");
        System.out.println("   注解式验证适合固定规则（推荐 90% 的业务场景使用）。");
        System.out.println();
    }
}
