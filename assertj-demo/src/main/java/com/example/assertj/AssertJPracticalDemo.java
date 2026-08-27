package com.example.assertj;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AssertJ 实战断言演示。
 *
 * <p>覆盖：业务 DTO 校验 / 接口响应体字段抽取 / 自定义 Condition / 集合分组与排序 /
 * Spring Boot 测试集成代码示例。</p>
 */
public class AssertJPracticalDemo {

    public static void main(String[] args) {
        apiResponseValidation();
        customConditionValidation();
        collectionProjectionsAndSorting();
        businessRuleValidation();
        springBootTestSnippet();
    }

    /** 模拟校验 HTTP 接口返回的统一响应体。 */
    private static void apiResponseValidation() {
        System.out.println("=== API Response Validation ===");
        ApiResponse<List<Order>> response = ApiResponse.success(
                Arrays.asList(
                        new Order(1001L, "PHONE", 2, BigDecimal.valueOf(4999.00)),
                        new Order(1002L, "BOOK", 5, BigDecimal.valueOf(59.00))
                )
        );

        Assertions.assertThat(response)
                .extracting(ApiResponse::getCode, ApiResponse::getMessage)
                .containsExactly(200, "ok");

        Assertions.assertThat(response.getData())
                .hasSize(2)
                .extracting(Order::getCategory)
                .containsExactlyInAnyOrder("PHONE", "BOOK");

        Assertions.assertThat(response.getData())
                .filteredOn(order -> "PHONE".equals(order.getCategory()))
                .hasSize(1)
                .first()
                .extracting(Order::getQuantity)
                .isEqualTo(2);

        System.out.println("API response validation passed.");
    }

    /** 使用自定义 Condition 校验业务规则。 */
    private static void customConditionValidation() {
        System.out.println("=== Custom Condition Validation ===");
        Order order = new Order(2001L, "LAPTOP", 1, BigDecimal.valueOf(8999.00));

        Condition<Order> highValueOrder = new Condition<>(
                o -> o.getTotalAmount().compareTo(BigDecimal.valueOf(5000)) >= 0,
                "high value order (total >= 5000)"
        );

        Assertions.assertThat(order).is(highValueOrder);
        System.out.println("Custom condition validation passed.");
    }

    /** 集合投影、分组、排序与转换。 */
    private static void collectionProjectionsAndSorting() {
        System.out.println("=== Collection Projections & Sorting ===");
        List<Order> orders = Arrays.asList(
                new Order(1L, "A", 1, BigDecimal.valueOf(100)),
                new Order(2L, "B", 2, BigDecimal.valueOf(80)),
                new Order(3L, "A", 3, BigDecimal.valueOf(120))
        );

        // 提取 category 并去重
        List<String> categories = orders.stream()
                .map(Order::getCategory)
                .distinct()
                .collect(Collectors.toList());
        Assertions.assertThat(categories).containsExactlyInAnyOrder("A", "B");

        // 按总金额升序排序后校验
        orders.sort(Comparator.comparing(Order::getTotalAmount));
        Assertions.assertThat(orders)
                .isSortedAccordingTo((o1, o2) -> o1.getTotalAmount().compareTo(o2.getTotalAmount()));

        // 验证所有订单都有正数量
        Assertions.assertThat(orders)
                .map(Order::getQuantity)
                .allMatch(qty -> qty > 0);

        System.out.println("Collection projections passed.");
    }

    /** 业务规则：订单金额与数量一致性校验。 */
    private static void businessRuleValidation() {
        System.out.println("=== Business Rule Validation ===");
        Order order = new Order(3001L, "TABLET", 3, BigDecimal.valueOf(1999.00));

        Assertions.assertThat(order)
                .satisfies(o -> {
                    Assertions.assertThat(o.getQuantity()).isBetween(1, 100);
                    Assertions.assertThat(o.getTotalAmount()).isPositive();
                });

        // 多个字段同时校验：数量 * 单价 不超过 100000
        Assertions.assertThat(order.getTotalAmount().multiply(BigDecimal.valueOf(order.getQuantity())))
                .isLessThanOrEqualTo(BigDecimal.valueOf(100_000));

        System.out.println("Business rule validation passed.");
    }

    /** Spring Boot 测试集成示例（仅代码片段，无需实际引入 Spring 依赖）。 */
    private static void springBootTestSnippet() {
        System.out.println("=== Spring Boot Test Snippet ===");
        System.out.println(
                "@SpringBootTest\n" +
                "@AutoConfigureMockMvc\n" +
                "class UserControllerTest {\n" +
                "    @Autowired\n" +
                "    private MockMvc mockMvc;\n" +
                "\n" +
                "    @Test\n" +
                "    void shouldReturnUser() throws Exception {\n" +
                "        mockMvc.perform(get(\"/users/1\"))\n" +
                "               .andExpect(status().isOk())\n" +
                "               .andExpect(result -> {\n" +
                "                   String body = result.getResponse().getContentAsString();\n" +
                "                   assertThatJson(body)\n" +
                "                       .node(\"username\").isString()\n" +
                "                       .isEqualTo(\"tom\");\n" +
                "                   assertThatJson(body)\n" +
                "                       .node(\"age\").isNumber()\n" +
                "                       .isGreaterThan(0);\n" +
                "               });\n" +
                "    }\n" +
                "}\n" +
                "// 依赖：assertj-core + json-unit-assertj（可选）");
    }

    // ==================== 演示 DTO ====================

    static class ApiResponse<T> {
        private final int code;
        private final String message;
        private final T data;

        private ApiResponse(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(200, "ok", data);
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }
    }

    static class Order {
        private final Long id;
        private final String category;
        private final int quantity;
        private final BigDecimal totalAmount;

        Order(Long id, String category, int quantity, BigDecimal totalAmount) {
            this.id = id;
            this.category = category;
            this.quantity = quantity;
            this.totalAmount = totalAmount;
        }

        public Long getId() {
            return id;
        }

        public String getCategory() {
            return category;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }
}
