package com.example.lombok;

import lombok.*;

/**
 * Lombok 高级功能演示类
 *
 * 本类演示 Lombok 的进阶用法：
 * 1. @Builder - 建造者模式，自动生成 builder() 方法
 * 2. @With - 生成 withXxx() 方法，用于创建拷贝副本
 * 3. @Singular - 集合类型的单数化，适合Builder模式
 * 4. @Delegate - 委托代理
 * 5. @Value 和 @Immutable - 不可变对象
 * 6. @Log 系列 - 自动生成日志对象
 * 7. @UtilityClass - 工具类注解
 */
public class LombokAdvancedDemo {

    public static void main(String[] args) {
        System.out.println("=== Lombok 高级功能演示 ===\n");

        // 1. @Builder 建造者模式演示
        System.out.println("1. @Builder 建造者模式:");
        UserBuilder user = UserBuilder.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .age(28)
                .build();
        System.out.println("   " + user);
        System.out.println();

        // 2. @Singular 集合单数化演示
        System.out.println("2. @Singular 集合单数化:");
        TeamBuilder team = TeamBuilder.builder()
                .name("技术团队")
                .addMember("张三")
                .addMember("李四")
                .addMember("王五")
                .addSkill("Java")
                .addSkill("Python")
                .build();
        System.out.println("   " + team);
        System.out.println();

        // 3. @With 拷贝副本演示
        System.out.println("3. @With 拷贝副本:");
        Config config = new Config("app", "MyApp", 1);
        Config configV2 = config.withVersion(2);
        Config configName = config.withName("NewApp");
        System.out.println("   原始: " + config);
        System.out.println("   复制+版本更新: " + configV2);
        System.out.println("   复制+名称更新: " + configName);
        System.out.println();

        // 4. @Value 不可变对象演示
        System.out.println("4. @Value 不可变对象:");
        ImmutableUser immutableUser = ImmutableUser.builder()
                .id(1L)
                .name("Bob")
                .email("bob@example.com")
                .build();
        System.out.println("   " + immutableUser);
        // immutableUser.setName("Carol"); // 编译错误！setter 不存在
        System.out.println("   尝试调用 setName() 会编译失败 - setter 不存在");
        System.out.println();

        // 5. @Log 系列日志注解
        System.out.println("5. @Log 日志注解:");
        LogDemo logDemo = new LogDemo();
        logDemo.performAction();
        System.out.println();

        // 6. @Delegate 委托演示
        System.out.println("6. @Delegate 委托代理:");
        Wrapper wrapper = new Wrapper();
        wrapper.doSomething();
        wrapper.doAnother();
        wrapper.legacyMethod();
        System.out.println();

        // 7. @UtilityClass 工具类
        System.out.println("7. @UtilityClass 工具类:");
        String result = StringUtils.trimToUpper("  hello world  ");
        System.out.println("   StringUtils.trimToUpper(\"  hello world  \") = " + result);
        System.out.println();

        // 8. @Builder.Default 默认值
        System.out.println("8. @Builder.Default 默认值:");
        BuilderDefaultDemo demo = BuilderDefaultDemo.builder()
                .name("自定义名称")
                // count 未设置，使用默认值 10
                .build();
        System.out.println("   name=自定义名称, count=未设置: " + demo);
        System.out.println();

        // 9. @Builder 继承问题解决方案
        System.out.println("9. @Builder 继承注意事项:");
        System.out.println("   注意: @Builder 不会继承到子类！");
        System.out.println("   解决方案: 使用 @SuperBuilder 或在子类单独添加 @Builder");
    }
}

// ==================== 演示用类定义 ====================

/**
 * @Builder 演示 - 建造者模式
 */
@Data
@Builder
public class UserBuilder {
    private Long id;
    private String username;
    private String email;
    private Integer age;
}

/**
 * @Singular 演示 - 集合类型的单数化
 * singularName 属性可以自定义单数形式的字段名
 */
@Data
@Builder
public class TeamBuilder {
    private String name;

    @Singular
    private List<String> members;

    @Singular("skill")  // 自定义单数名
    private List<String> skills;
}

/**
 * @With 演示 - 生成 withXxx() 方法
 */
@Data
@With
class Config {
    private String name;
    private String value;
    private Integer version;

    public Config(String name, String value, Integer version) {
        this.name = name;
        this.value = value;
        this.version = version;
    }
}

/**
 * @Value 演示 - 不可变对象
 * 等同于: @Getter @FieldDefaults(makeFinal = true) @AllArgsConstructor @EqualsAndHashCode @ToString
 */
@Value
@Builder
public class ImmutableUser {
    Long id;
    String name;
    String email;
}

/**
 * @Log 系列日志注解演示
 * 支持: @Log, @Slf4j, @Log4j, @Log4j2, @CommonsLog, @JBossLog 等
 */
@Slf4j
class LogDemo {
    public void performAction() {
        log.info("这是一条 info 日志");
        log.warn("这是一条 warn 日志");
        log.debug("这是一条 debug 日志");
        log.error("这是一条 error 日志");
    }
}

/**
 * @Delegate 演示 - 委托代理
 * 自动将接口方法委托给指定字段
 */
public class Wrapper implements InterfaceA, InterfaceB {
    @Delegate(types = InterfaceA.class)
    private final InterfaceA interfaceA = new InterfaceAImpl();

    @Override
    public void doAnother() {
        System.out.println("   Wrapper.doAnother()");
    }

    public void legacyMethod() {
        System.out.println("   Wrapper.legacyMethod()");
    }
}

interface InterfaceA {
    void doSomething();
}

interface InterfaceB {
    void doAnother();
}

class InterfaceAImpl implements InterfaceA {
    @Override
    public void doSomething() {
        System.out.println("   InterfaceAImpl.doSomething()");
    }
}

/**
 * @UtilityClass 工具类注解
 * 自动添加私有构造函数，并标记为 final
 * 注意: @UtilityClass 只能注解在类上，不能注解在 public class 上
 * 这里使用常规方式定义工具类
 */
public final class StringUtils {
    private StringUtils() {
        // 私有构造函数，防止实例化
    }

    public static String trimToUpper(String str) {
        return str == null ? "" : str.trim().toUpperCase();
    }
}

/**
 * @Builder.Default 演示 - Builder 模式下的默认值
 */
@Data
@Builder
public class BuilderDefaultDemo {
    private String name;

    @Builder.Default
    private Integer count = 10;

    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
