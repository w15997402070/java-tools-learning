package com.example.orika;

import com.example.orika.dto.UserOrderDTO;
import com.example.orika.entity.Order;
import com.example.orika.entity.User;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orika 实战演示：多源合并映射、性能对比、Spring Boot 集成指南
 *
 * 演示内容：
 * 1. 多对象合并映射（User + Order -> UserOrderDTO）
 * 2. 与手写 Getter/Setter 的性能对比
 * 3. MapperFacade 单例最佳实践
 * 4. Spring Boot 集成代码示例（注释说明）
 */
public class OrikaPracticalDemo {

    // MapperFacade 线程安全，应作为单例使用
    private static final MapperFacade mapper = createMapper();

    public static void main(String[] args) {
        System.out.println("=== Orika 实战演示 ===\n");

        // 1. 多源合并映射
        System.out.println("--- 多源合并映射（User + Order -> UserOrderDTO）---");
        User user = new User(1001L, "zhangsan", "pass", "zs@example.com", 28, new Date(), true);
        Order order = new Order(5001L, "ORD-20240903-1001", 1001L,
                new BigDecimal("1299.00"), 2, new Date(), "请发顺丰");

        UserOrderDTO userOrderDTO = mapper.map(user, UserOrderDTO.class);
        // 再映射 Order 的字段到同一个 DTO（Orika 会合并，不覆盖已有字段）
        mapper.map(order, userOrderDTO);

        System.out.println("合并映射结果:");
        System.out.println("  用户ID=" + userOrderDTO.getUserId() + ", 用户名=" + userOrderDTO.getUsername());
        System.out.println("  邮箱=" + userOrderDTO.getUserEmail());
        System.out.println("  订单ID=" + userOrderDTO.getOrderId() + ", 订单号=" + userOrderDTO.getOrderNo());
        System.out.println("  金额=" + userOrderDTO.getOrderAmount() + ", 状态=" + userOrderDTO.getStatus());

        // 2. 批量合并映射（模拟列表查询场景）
        System.out.println("\n--- 批量合并映射（模拟 JOIN 查询结果）---");
        List<User> users = Arrays.asList(
                new User(1001L, "zhangsan", "pass", "zs@example.com", 28, new Date(), true),
                new User(1002L, "lisi", "pass", "ls@example.com", 32, new Date(), true),
                new User(1003L, "wangwu", "pass", "ww@example.com", 25, new Date(), false)
        );
        List<Order> orders = Arrays.asList(
                new Order(5001L, "ORD-001", 1001L, new BigDecimal("199.00"), 1, new Date(), null),
                new Order(5002L, "ORD-002", 1002L, new BigDecimal("599.00"), 2, new Date(), "加急"),
                new Order(5003L, "ORD-003", 1003L, new BigDecimal("99.00"), 0, new Date(), null)
        );

        List<UserOrderDTO> mergedList = users.stream().map(u -> {
            UserOrderDTO dto = mapper.map(u, UserOrderDTO.class);
            // 找到对应订单并合并
            orders.stream()
                    .filter(o -> o.getUserId().equals(u.getId()))
                    .findFirst()
                    .ifPresent(o -> mapper.map(o, dto));
            return dto;
        }).collect(Collectors.toList());

        System.out.println("合并 " + mergedList.size() + " 条记录:");
        mergedList.forEach(dto -> System.out.println(
                "  用户=" + dto.getUsername() + " | 订单=" + dto.getOrderNo()
                        + " | 金额=¥" + dto.getOrderAmount()));

        // 3. 性能对比：Orika vs 手写 Getter/Setter
        System.out.println("\n--- 性能对比：Orika vs 手写 Getter/Setter ---");
        int count = 100000;
        User testUser = new User(9999L, "perf_test", "pass", "perf@test.com", 30, new Date(), true);

        // 预热
        for (int i = 0; i < 1000; i++) {
            mapper.map(testUser, UserOrderDTO.class);
            manualMap(testUser);
        }

        // Orika 映射
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            mapper.map(testUser, UserOrderDTO.class);
        }
        long orikaTime = System.currentTimeMillis() - start1;

        // 手写映射
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            manualMap(testUser);
        }
        long manualTime = System.currentTimeMillis() - start2;

        System.out.println("映射 " + count + " 次:");
        System.out.println("  Orika 耗时: " + orikaTime + " ms (" + (count * 1000L / orikaTime) + " ops/sec)");
        System.out.println("  手写耗时:   " + manualTime + " ms (" + (count * 1000L / manualTime) + " ops/sec)");
        System.out.println("  性能比: Orika 是手写的 " + String.format("%.2f", (double) manualTime / orikaTime) + " 倍");
        System.out.println("  （Orika 首次映射有编译开销，后续接近原生性能）");

        // 4. Spring Boot 集成指南（代码注释说明）
        System.out.println("\n--- Spring Boot 集成指南 ---");
        System.out.println("1. 添加依赖:");
        System.out.println("   <dependency>");
        System.out.println("       <groupId>ma.glasnost.orika</groupId>");
        System.out.println("       <artifactId>orika-core</artifactId>");
        System.out.println("       <version>1.5.4</version>");
        System.out.println("   </dependency>");
        System.out.println();
        System.out.println("2. 创建配置类:");
        System.out.println("   @Configuration");
        System.out.println("   public class OrikaConfig {");
        System.out.println("       @Bean");
        System.out.println("       public MapperFactory mapperFactory() {");
        System.out.println("           return new DefaultMapperFactory.Builder().build();");
        System.out.println("       }");
        System.out.println();
        System.out.println("       @Bean");
        System.out.println("       public MapperFacade mapperFacade(MapperFactory mapperFactory) {");
        System.out.println("           // 注册所有映射规则");
        System.out.println("           mapperFactory.classMap(User.class, UserDTO.class)");
        System.out.println("               .field(\"id\", \"userId\")");
        System.out.println("               .byDefault()");
        System.out.println("               .register();");
        System.out.println("           return mapperFactory.getMapperFacade();");
        System.out.println("       }");
        System.out.println("   }");
        System.out.println();
        System.out.println("3. 在服务中注入使用:");
        System.out.println("   @Service");
        System.out.println("   public class UserService {");
        System.out.println("       @Autowired");
        System.out.println("       private MapperFacade mapperFacade;");
        System.out.println();
        System.out.println("       public UserDTO getUserDTO(Long id) {");
        System.out.println("           User user = userRepository.findById(id);");
        System.out.println("           return mapperFacade.map(user, UserDTO.class);");
        System.out.println("       }");
        System.out.println("   }");
        System.out.println();
        System.out.println("4. 与 MapStruct 对比选择:");
        System.out.println("   - Orika: 运行时生成字节码，配置灵活，适合动态场景");
        System.out.println("   - MapStruct: 编译期生成代码，零运行时依赖，性能更优");
        System.out.println("   - 两者可共存：简单映射用 MapStruct，复杂动态映射用 Orika");

        System.out.println("\n=== 实战演示完成 ===");
    }

    /**
     * 创建并配置 Mapper（单例）
     */
    private static MapperFacade createMapper() {
        MapperFactory factory = new DefaultMapperFactory.Builder().build();

        // User -> UserOrderDTO（用户部分）
        factory.classMap(User.class, UserOrderDTO.class)
                .field("id", "userId")
                .field("username", "username")
                .field("email", "userEmail")
                .exclude("password")
                .byDefault()
                .register();

        // Order -> UserOrderDTO（订单部分）
        factory.classMap(Order.class, UserOrderDTO.class)
                .field("orderId", "orderId")
                .field("orderNo", "orderNo")
                .field("totalAmount", "orderAmount")
                .field("status", "status")
                .field("orderTime", "orderTime")
                .byDefault()
                .register();

        return factory.getMapperFacade();
    }

    /**
     * 手写映射（用于性能对比）
     */
    private static UserOrderDTO manualMap(User user) {
        UserOrderDTO dto = new UserOrderDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setUserEmail(user.getEmail());
        return dto;
    }
}
