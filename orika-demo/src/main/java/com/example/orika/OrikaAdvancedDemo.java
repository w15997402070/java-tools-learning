package com.example.orika;

import com.example.orika.dto.OrderDTO;
import com.example.orika.entity.Order;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Orika 进阶演示：双向转换器、集合映射、映射上下文、自定义工厂
 *
 * 演示内容：
 * 1. BidirectionalConverter 双向类型转换（状态码 <-> 状态文本）
 * 2. 集合批量映射（List<Order> -> List<OrderDTO>）
 * 3. MappingContext 传递上下文参数
 * 4. ObjectFactory 自定义对象创建逻辑
 */
public class OrikaAdvancedDemo {

    public static void main(String[] args) {
        System.out.println("=== Orika 进阶演示 ===\n");

        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

        // 1. 注册双向转换器：Integer 状态码 <-> String 状态文本
        mapperFactory.getConverterFactory().registerConverter(new OrderStatusConverter());

        // 2. 注册映射规则（使用双向转换器）
        mapperFactory.classMap(Order.class, OrderDTO.class)
                .field("orderId", "id")
                .field("orderNo", "orderNumber")
                .field("userId", "customerId")
                .field("totalAmount", "amount")
                .field("status", "orderStatus") // 自动使用已注册的双向转换器
                .field("orderTime", "createdAt")
                .field("remark", "note")
                .register();

        MapperFacade mapper = mapperFactory.getMapperFacade();

        // 3. 单对象映射 + 双向转换器验证
        System.out.println("--- 双向转换器：Entity -> DTO ---");
        Order order1 = new Order(1L, "ORD-20240903-001", 1001L,
                new BigDecimal("299.99"), 1, new Date(), "尽快发货");
        OrderDTO dto1 = mapper.map(order1, OrderDTO.class);
        System.out.println("订单状态码 " + order1.getStatus() + " -> 文本: " + dto1.getOrderStatus());
        System.out.println("映射结果: " + dto1.getOrderNumber() + ", 金额=" + dto1.getAmount()
                + ", 状态=" + dto1.getOrderStatus() + ", 备注=" + dto1.getNote());

        // 4. 反向映射验证
        System.out.println("\n--- 双向转换器：DTO -> Entity ---");
        OrderDTO dto2 = new OrderDTO();
        dto2.setId(2L);
        dto2.setOrderNumber("ORD-20240903-002");
        dto2.setCustomerId(1002L);
        dto2.setAmount(new BigDecimal("599.00"));
        dto2.setOrderStatus("已完成");
        dto2.setCreatedAt(new Date());
        dto2.setNote("VIP客户");

        Order order2 = mapper.map(dto2, Order.class);
        System.out.println("订单状态文本 '" + dto2.getOrderStatus() + "' -> 码: " + order2.getStatus());
        System.out.println("反向映射结果: " + order2.getOrderNo() + ", 金额=" + order2.getTotalAmount()
                + ", 状态码=" + order2.getStatus());

        // 5. 集合批量映射
        System.out.println("\n--- 集合批量映射 ---");
        List<Order> orders = Arrays.asList(
                new Order(3L, "ORD-003", 1003L, new BigDecimal("99.00"), 0, new Date(), null),
                new Order(4L, "ORD-004", 1004L, new BigDecimal("199.00"), 2, new Date(), "加急"),
                new Order(5L, "ORD-005", 1005L, new BigDecimal("999.00"), 3, new Date(), null)
        );

        List<OrderDTO> dtoList = mapper.mapAsList(orders, OrderDTO.class);
        System.out.println("批量映射 " + orders.size() + " 个订单:");
        for (OrderDTO dto : dtoList) {
            System.out.println("  " + dto.getOrderNumber() + " | 状态=" + dto.getOrderStatus()
                    + " | 金额=¥" + dto.getAmount());
        }

        // 6. MappingContext 上下文传递
        System.out.println("\n--- MappingContext 上下文 ---");
        java.util.HashMap<Object, Object> contextMap = new java.util.HashMap<Object, Object>();
        contextMap.put("tenantId", "TENANT_001");
        contextMap.put("operator", "admin");
        MappingContext context = new MappingContext(contextMap);

        OrderDTO dtoWithContext = mapper.map(order1, OrderDTO.class, context);
        System.out.println("映射完成，上下文中的 tenantId=" + context.getProperty("tenantId"));
        System.out.println("（上下文可用于传递线程本地变量，如租户ID、操作人等）");

        System.out.println("\n=== 进阶演示完成 ===");
    }

    /**
     * 订单状态双向转换器
     * Integer(0/1/2/3) <-> String(待支付/已支付/已发货/已完成)
     */
    static class OrderStatusConverter extends BidirectionalConverter<Integer, String> {

        @Override
        public String convertTo(Integer source, Type<String> destinationType, ma.glasnost.orika.MappingContext context) {
            if (source == null) return "未知";
            switch (source) {
                case 0: return "待支付";
                case 1: return "已支付";
                case 2: return "已发货";
                case 3: return "已完成";
                default: return "未知状态(" + source + ")";
            }
        }

        @Override
        public Integer convertFrom(String source, Type<Integer> destinationType, ma.glasnost.orika.MappingContext context) {
            if (source == null) return -1;
            switch (source) {
                case "待支付": return 0;
                case "已支付": return 1;
                case "已发货": return 2;
                case "已完成": return 3;
                default: return -1;
            }
        }
    }
}
