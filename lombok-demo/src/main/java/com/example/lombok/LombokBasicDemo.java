package com.example.lombok;

import lombok.*;

/**
 * Lombok 基础演示类
 *
 * 本类演示 Lombok 最常用的注解：
 * 1. @Data - 自动生成 getter/setter/toString/equals/hashCode
 * 2. @Getter/@Setter - 单独控制字段的 getter/setter 生成
 * 3. @ToString - 自定义 toString 输出格式
 * 4. @EqualsAndHashCode - 自定义 equals 和 hashCode
 * 5. @NoArgsConstructor/@AllArgsConstructor/@RequiredArgsConstructor - 构造函数生成
 */
public class LombokBasicDemo {

    public static void main(String[] args) {
        System.out.println("=== Lombok 基础功能演示 ===\n");

        // 1. @Data 注解演示 - 一站式解决所有样板代码
        System.out.println("1. @Data 注解演示:");
        User user = new User();
        user.setId(1L);
        user.setUsername("zhangsan");
        user.setEmail("zhangsan@example.com");
        user.setAge(25);

        System.out.println("   创建用户对象: " + user);
        System.out.println("   getUsername(): " + user.getUsername());
        System.out.println("   setAge(30): " + (user.setAge(30) != null ? "成功" : "返回void"));
        System.out.println();

        // 2. @Getter/@Setter 演示 - 细粒度控制
        System.out.println("2. @Getter/@Setter 注解演示:");
        Product product = new Product();
        product.setProductId("P001");
        product.setName("笔记本电脑");
        product.setPrice(5999.99);
        product.setStock(100);

        System.out.println("   商品信息: " + product);
        System.out.println("   库存 getter: " + product.getStock());
        System.out.println();

        // 3. @ToString 演示 - 自定义输出格式
        System.out.println("3. @ToString 自定义演示:");
        Order order = new Order();
        order.setOrderId("ORD20240101");
        order.setAmount(999.99);
        order.setStatus("PENDING");
        System.out.println("   订单信息: " + order.toString());
        System.out.println();

        // 4. 构造函数演示
        System.out.println("4. 构造函数演示:");
        // 4.1 无参构造函数 (@NoArgsConstructor)
        Person person1 = new Person();
        System.out.println("   无参构造: " + person1);

        // 4.2 全参构造函数 (@AllArgsConstructor)
        Person person2 = new Person(1L, "lisi", 30);
        System.out.println("   全参构造: " + person2);

        // 4.3 必需参构造函数 (@RequiredArgsConstructor) - 用于 final 和 @NonNull 字段
        Person person3 = new Person("wangwu");
        System.out.println("   必需参构造: " + person3);
        System.out.println();

        // 5. @EqualsAndHashCode 自定义演示
        System.out.println("5. @EqualsAndHashCode 演示:");
        Config config1 = new Config("app.name", "MyApp");
        Config config2 = new Config("app.name", "MyApp");
        Config config3 = new Config("app.name", "OtherApp");

        System.out.println("   config1: " + config1);
        System.out.println("   config2: " + config2);
        System.out.println("   config3: " + config3);
        System.out.println("   config1.equals(config2): " + config1.equals(config2));
        System.out.println("   config1.equals(config3): " + config1.equals(config3));
        System.out.println("   config1.hashCode() == config2.hashCode(): " + (config1.hashCode() == config2.hashCode()));
    }
}

// ==================== 演示用类定义 ====================

/**
 * @Data 演示 - 最常用的注解组合
 * 等同于同时添加: @Getter @Setter @ToString @EqualsAndHashCode
 */
@Data
class User {
    private Long id;
    private String username;
    private String email;
    private Integer age;
}

/**
 * @Getter/@Setter 单独使用演示
 * 可以针对特定字段或整个类
 */
class Product {
    @Getter @Setter
    private String productId;

    @Getter @Setter
    private String name;

    // 只生成 getter，不生成 setter - 不可变价格
    @Getter
    private Double price;

    @Setter
    private Integer stock;

    // 排除某些字段不生成 toString
    @Getter @Setter @ToString.Exclude
    private String internalCode;
}

/**
 * @ToString 自定义演示
 * - exclude: 排除某些字段
 * - callSuper: 是否调用父类的 toString
 */
@ToString(callSuper = true, exclude = {"secretKey"})
class Order extends BaseEntity {
    private String orderId;
    private Double amount;
    private String status;
    private String secretKey;
}

class BaseEntity {
    @ToString.Include(name = "ID")
    private Long id = 1001L;
}

/**
 * 构造函数注解演示
 */
@NoArgsConstructor          // 无参构造
@AllArgsConstructor         // 全参构造
@RequiredArgsConstructor    // 必需参构造（final 和 @NonNull 字段）
@ToString
class Person {
    private Long id;
    private String name;
    private Integer age;

    // @NonNull 字段会生成必需参数构造
    @NonNull
    private String nickname;

    // final 字段也会包含在必需参构造中
    private final String type = "PERSON";
}

/**
 * @EqualsAndHashCode 自定义演示
 * - exclude: 排除某些字段不参与比较
 * - of: 只包含指定字段
 */
@EqualsAndHashCode(exclude = {"description", "version"})
@ToString
class Config {
    private String key;
    private String value;
    private String description;
    private Integer version;

    public Config(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
