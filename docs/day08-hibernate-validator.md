# Day 08 - Hibernate Validator 数据验证框架

## 📌 工具简介

| 属性 | 信息 |
|------|------|
| **名称** | Hibernate Validator |
| **版本** | 6.2.5.Final（Bean Validation 2.0 参考实现） |
| **GitHub** | https://github.com/hibernate/hibernate-validator |
| **官方文档** | https://hibernate.org/validator/documentation/ |
| **Stars** | 3.5k+ |
| **许可证** | Apache License 2.0 |

Hibernate Validator 是 **JSR-380（Bean Validation 2.0）** 规范的官方参考实现，也是目前 Java 生态中最广泛使用的数据验证框架。

它通过在 Java Bean 字段和方法上标注约束注解（如 `@NotNull`、`@Size`、`@Email`），在运行时自动验证对象的合法性，无需手写 if-else 判断。

**核心价值：**
- 声明式验证，代码简洁
- 与 Spring MVC、Spring Boot 深度集成（`@Valid` / `@Validated`）
- 支持自定义约束、分组验证、跨字段验证
- 错误信息支持国际化

---

## 📦 Maven 依赖配置

```xml
<!-- Hibernate Validator（包含 Bean Validation API） -->
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>6.2.5.Final</version>
</dependency>

<!-- EL 实现（用于消息模板中的表达式，如 {min}、{max}）-->
<dependency>
    <groupId>org.glassfish</groupId>
    <artifactId>jakarta.el</artifactId>
    <version>3.0.4</version>
</dependency>
```

> ⚠️ **注意版本对应关系：**
> - Hibernate Validator **6.x** → Bean Validation **2.0** → Jakarta EE 8（`javax.validation`）
> - Hibernate Validator **7.x / 8.x** → Bean Validation **3.0** → Jakarta EE 10（`jakarta.validation`）
> - Spring Boot 2.x 默认使用 6.x，Spring Boot 3.x 使用 8.x

---

## 🔧 常用约束注解速查

### 基础约束

| 注解 | 适用类型 | 说明 |
|------|---------|------|
| `@NotNull` | 任何类型 | 不能为 null |
| `@NotBlank` | String | 不能为 null 且去除空格后不能为空 |
| `@NotEmpty` | String/集合/数组 | 不能为 null 且不能为空 |
| `@Null` | 任何类型 | 必须为 null |
| `@Size(min, max)` | String/集合/数组/Map | 长度/大小范围 |
| `@Length(min, max)` | String（Hibernate扩展） | 字符串长度范围 |
| `@Min(value)` | 数字类型 | 最小值（含边界） |
| `@Max(value)` | 数字类型 | 最大值（含边界） |
| `@DecimalMin(value)` | 数字/String | 十进制最小值 |
| `@DecimalMax(value)` | 数字/String | 十进制最大值 |
| `@Range(min, max)` | 数字（Hibernate扩展） | 范围（含两端） |
| `@Positive` | 数字 | 必须为正数 |
| `@PositiveOrZero` | 数字 | 必须为正数或零 |
| `@Negative` | 数字 | 必须为负数 |
| `@Email` | String | 邮箱格式 |
| `@Pattern(regexp)` | String | 正则表达式匹配 |
| `@URL` | String（Hibernate扩展） | URL 格式 |
| `@Past` | Date/LocalDate 等 | 必须是过去的日期/时间 |
| `@Future` | Date/LocalDate 等 | 必须是未来的日期/时间 |
| `@PastOrPresent` | Date/LocalDate 等 | 过去或当前 |
| `@FutureOrPresent` | Date/LocalDate 等 | 未来或当前 |

### 特殊注解

| 注解 | 说明 |
|------|------|
| `@Valid` | 触发嵌套对象/集合元素的级联验证 |
| `@Validated` | Spring 专属，支持分组，用于类或方法上 |
| `@Constraint(validatedBy=...)` | 定义自定义约束注解 |

---

## 🚀 Spring Boot 集成

### 1. 依赖（Spring Boot 2.x 自动引入）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 2. Controller 层参数验证

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // 验证请求体（JSON）
    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody @Valid UserRequest req) {
        // 若验证失败，Spring 自动抛出 MethodArgumentNotValidException
        userService.create(req);
        return ResponseEntity.ok("创建成功");
    }

    // 验证路径/查询参数（需在类上加 @Validated）
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(
            @PathVariable @Min(value = 1, message = "ID无效") Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }
}
```

### 3. 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @RequestBody @Valid 触发的验证失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 400);
        response.put("message", "参数验证失败");
        response.put("errors", errors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理 @Validated 方法参数验证失败（路径/查询参数）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                errors.put(v.getPropertyPath().toString(), v.getMessage()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 400);
        response.put("message", "参数验证失败");
        response.put("errors", errors);
        return ResponseEntity.badRequest().body(response);
    }
}
```

### 4. Service 层验证

```java
@Service
@Validated  // 开启方法级别验证
public class OrderService {

    /**
     * 方法参数直接使用约束注解
     */
    public Order createOrder(@Valid @NotNull CreateOrderRequest req) {
        // req 已通过验证
        return orderRepository.save(convert(req));
    }

    /**
     * 返回值验证
     */
    @Valid
    public UserDTO getUserById(@Min(1) Long id) {
        return userRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
```

### 5. 自定义约束注解示例

```java
/**
 * 自定义手机号验证注解
 */
@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {
    String message() default "手机号格式不正确";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PhoneValidator implements ConstraintValidator<Phone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isEmpty()) return true; // 空值由 @NotBlank 控制
        return value.matches("^1[3-9]\\d{9}$");
    }
}
```

### 6. 分组验证示例（新增 vs 更新）

```java
public interface OnCreate {}
public interface OnUpdate {}

public class UserDTO {

    @Null(groups = OnCreate.class)
    @NotNull(groups = OnUpdate.class)
    private Long id;

    @NotBlank
    private String username;
}

// Controller 中使用 @Validated 指定分组
@PostMapping
public void create(@RequestBody @Validated(OnCreate.class) UserDTO dto) { ... }

@PutMapping
public void update(@RequestBody @Validated(OnUpdate.class) UserDTO dto) { ... }
```

### 7. 错误消息国际化

在 `resources/ValidationMessages.properties` 中自定义消息：

```properties
# 覆盖默认错误消息
javax.validation.constraints.NotBlank.message=字段不能为空
javax.validation.constraints.Size.message=长度必须在 {min} 到 {max} 之间
com.example.validator.Phone.message=手机号格式不正确
```

---

## ⚠️ 注意事项

### 1. 常见 Bug 风险

**① `@NotBlank` vs `@NotNull` vs `@NotEmpty` 混用**
```java
// 错误：@NotNull 无法阻止空字符串 ""
@NotNull private String name;  // "" 可以通过！

// 正确：字符串字段使用 @NotBlank
@NotBlank private String name;  // null、""、"   " 全部拦截
```

**② 嵌套对象忘加 `@Valid`**
```java
// 错误：Address 内部字段不会被验证
@NotNull
private Address address;

// 正确：加 @Valid 才会触发级联验证
@Valid
@NotNull
private Address address;
```

**③ Spring 方法级别验证忘加 `@Validated`**
```java
// 必须在类上或配置类中开启 @Validated，否则方法参数的约束注解无效
@Service
@Validated  // ← 不能少
public class UserService { ... }
```

**④ `@RequestParam` 验证不触发 MethodArgumentNotValidException**
```java
// 路径/查询参数验证失败抛出 ConstraintViolationException（不是 MethodArgumentNotValidException！）
// 需要在全局异常处理中分别处理两种异常
```

### 2. 性能问题

- **ValidatorFactory 是重量级对象**，应使用单例（Spring 自动管理）。
- **每次请求创建 Validator** 会有显著性能损耗，Spring Boot 已处理，手动使用时注意。
- **快速失败模式（failFast=true）** 适合性能敏感场景，找到第一个错误即返回，减少验证开销。
- **集合元素验证**（如 `List<@Valid Item>`）会遍历每个元素，列表很大时注意性能。

### 3. 使用限制

- **Hibernate Validator 6.x 只支持 Java 8+**，不支持更低版本。
- **循环引用**：若两个对象互相持有引用并都标注了 `@Valid`，会导致无限递归，需用 `@JsonIgnore` 或打断引用。
- **接口方法参数**：在接口上声明的约束注解，只有通过 Spring 代理调用时才生效，直接调用实现类不生效。
- **继承中的约束**：子类不能"放宽"父类的约束，否则会抛出 `ConstraintDeclarationException`。

### 4. 版本升级注意

```
Spring Boot 2.x → 3.x 时：
  import javax.validation.*  →  import jakarta.validation.*
  hibernate-validator 6.x   →  hibernate-validator 8.x
所有 import 语句和依赖版本都需要更新！
```

---

## 📁 代码结构

```
hibernate-validator-demo/
├── pom.xml
└── src/main/java/com/example/validator/
    ├── HibernateValidatorBasicDemo.java    # 基础：内置注解/Validator使用/@Valid嵌套
    ├── HibernateValidatorAdvancedDemo.java # 高级：自定义注解/分组/跨字段验证/集合元素
    └── HibernateValidatorPracticalDemo.java # 实战：电商下单参数验证/快速失败/工具类封装
```

---

## 🏃 运行方法

```bash
# 进入项目目录
cd java-tools-learning/hibernate-validator-demo

# 编译打包
mvn clean package -DskipTests

# 运行基础演示
mvn exec:java -Dexec.mainClass="com.example.validator.HibernateValidatorBasicDemo"

# 运行高级演示
mvn exec:java -Dexec.mainClass="com.example.validator.HibernateValidatorAdvancedDemo"

# 运行实战演示
mvn exec:java -Dexec.mainClass="com.example.validator.HibernateValidatorPracticalDemo"
```

---

## 📚 参考资料

- [Hibernate Validator 官方文档](https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/)
- [Bean Validation 2.0 规范](https://beanvalidation.org/2.0/)
- [Spring Boot Validation 指南](https://spring.io/guides/gs/validating-form-input/)
- [Baeldung - Java Bean Validation Basics](https://www.baeldung.com/javax-validation)
