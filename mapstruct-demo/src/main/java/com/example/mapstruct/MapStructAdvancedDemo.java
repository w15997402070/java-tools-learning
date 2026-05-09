package com.example.mapstruct;

import com.example.mapstruct.dto.OrderDTO;
import com.example.mapstruct.entity.Order;
import com.example.mapstruct.entity.User;
import com.example.mapstruct.mapper.OrderMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * MapStruct 进阶演示
 *
 * <p>演示内容：
 * 1. 嵌套对象属性展平映射（order.user.username -> orderDTO.username）
 * 2. 字段重命名（id -> orderId）
 * 3. 数值单位转换（分 -> 元，Long -> BigDecimal）
 * 4. 调用自定义方法进行枚举/状态码转换
 * 5. 空值处理（user 为 null 时的行为）
 *
 * <p>运行方法：
 * <pre>
 *   java -cp target/mapstruct-demo-1.0-SNAPSHOT.jar com.example.mapstruct.MapStructAdvancedDemo
 * </pre>
 */
public class MapStructAdvancedDemo {

    public static void main(String[] args) {
        System.out.println("========== MapStruct 进阶演示 ==========\n");

        demo1NestedObjectMapping();
        demo2UnitConversion();
        demo3StatusCodeConversion();
        demo4NullHandling();
        demo5BatchOrderMapping();
    }

    /**
     * Demo 1: 嵌套对象展平映射
     */
    private static void demo1NestedObjectMapping() {
        System.out.println("--- Demo 1: 嵌套对象展平映射 ---");

        User user = new User(100L, "testuser", "pass", "测试用户",
                "test@example.com", "13900001111", 25,
                new BigDecimal("1000"), new Date(), new Date(), 1);

        Order order = new Order(
                5001L,
                "ORDER-2026050901",
                user,                          // 嵌套的 User 对象
                "MacBook Pro 16寸",
                1,
                1599900L,                      // 单价（分）：15999元
                new BigDecimal("15999.00"),
                2,
                "请尽快发货",
                new Date()
        );

        OrderDTO dto = OrderMapper.INSTANCE.toDTO(order);

        System.out.println("Order.id=" + order.getId());
        System.out.println("Order.user.username=" + order.getUser().getUsername());
        System.out.println("Order.user.phone=" + order.getUser().getPhone());
        System.out.println("Order.unitPrice=" + order.getUnitPrice() + " 分");
        System.out.println("Order.status=" + order.getStatus());

        System.out.println("\n映射后 OrderDTO：");
        System.out.println("  orderId=" + dto.getOrderId());         // id -> orderId
        System.out.println("  username=" + dto.getUsername());       // order.user.username
        System.out.println("  userPhone=" + dto.getUserPhone());     // order.user.phone
        System.out.println("  unitPriceYuan=" + dto.getUnitPriceYuan() + " 元"); // 15999.00
        System.out.println("  statusDesc=" + dto.getStatusDesc());   // 2 -> "待发货"
        System.out.println("✅ 嵌套对象字段成功展平到 DTO");
        System.out.println();
    }

    /**
     * Demo 2: 不同金额状态的单位转换演示
     */
    private static void demo2UnitConversion() {
        System.out.println("--- Demo 2: 金额单位转换（分 -> 元）---");

        long[] cents = {0L, 1L, 99L, 100L, 1000L, 99999L, 100000000L};

        System.out.println("分值\t\t-> 元值");
        System.out.println("--------\t\t--------");
        for (long cent : cents) {
            BigDecimal yuan = OrderMapper.INSTANCE.centToYuan(cent);
            System.out.printf("%-12d\t-> %s 元%n", cent, yuan.toPlainString());
        }
        System.out.println("✅ Long(分) -> BigDecimal(元) 转换精确无浮点误差");
        System.out.println();
    }

    /**
     * Demo 3: 状态码转换
     */
    private static void demo3StatusCodeConversion() {
        System.out.println("--- Demo 3: 订单状态码转换 ---");

        int[] statuses = {1, 2, 3, 4, 5, 99};
        for (int s : statuses) {
            System.out.println("  状态码 " + s + " -> " + OrderMapper.INSTANCE.orderStatusToDesc(s));
        }
        System.out.println("✅ 状态码转文字描述，前端无需维护映射表");
        System.out.println();
    }

    /**
     * Demo 4: 空值处理演示（user 为 null 时）
     *
     * <p>MapStruct 默认行为：
     * - 嵌套对象为 null 时，目标字段为 null（不抛 NPE）
     * - 基本类型有默认值时，可能出现 0/false 而非 null
     */
    private static void demo4NullHandling() {
        System.out.println("--- Demo 4: 嵌套对象为 null 时的处理 ---");

        Order orderWithNullUser = new Order(
                9999L, "ORDER-NULL-USER",
                null,  // user 为 null
                "商品A", 2, 5000L, new BigDecimal("100.00"),
                1, null, new Date()
        );

        // MapStruct 编译生成的代码会先判断 null，不会抛 NPE
        OrderDTO dto = OrderMapper.INSTANCE.toDTO(orderWithNullUser);

        System.out.println("Order.user = null");
        System.out.println("OrderDTO.username = " + dto.getUsername());   // null，不报错
        System.out.println("OrderDTO.userPhone = " + dto.getUserPhone()); // null，不报错
        System.out.println("OrderDTO.orderId = " + dto.getOrderId());     // 正常映射
        System.out.println("✅ 嵌套对象为 null 时不抛 NPE，目标字段为 null");
        System.out.println();
    }

    /**
     * Demo 5: 批量订单映射
     */
    private static void demo5BatchOrderMapping() {
        System.out.println("--- Demo 5: 批量订单映射 ---");

        User user = new User(1L, "buyer", "p", "买家", "b@test.com", "13800000001",
                30, BigDecimal.TEN, new Date(), new Date(), 1);

        List<Order> orders = Arrays.asList(
                new Order(1L, "ORD-001", user, "商品1", 1, 10000L,
                        new BigDecimal("100"), 1, null, new Date()),
                new Order(2L, "ORD-002", user, "商品2", 3, 5000L,
                        new BigDecimal("150"), 3, "加急", new Date()),
                new Order(3L, "ORD-003", user, "商品3", 2, 20000L,
                        new BigDecimal("400"), 4, null, new Date())
        );

        List<OrderDTO> dtos = OrderMapper.INSTANCE.toDTOList(orders);

        System.out.println("批量映射 " + orders.size() + " 个订单：");
        for (OrderDTO dto : dtos) {
            System.out.printf("  [%s] %s x%d  单价:%.2f元  状态:%s%n",
                    dto.getOrderNo(), dto.getProductName(),
                    dto.getQuantity(), dto.getUnitPriceYuan(),
                    dto.getStatusDesc());
        }
        System.out.println("✅ 批量映射完成");
        System.out.println();

        System.out.println("========== 演示完成 ==========");
    }
}
