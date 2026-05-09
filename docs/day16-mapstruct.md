# Day 16: MapStruct - Java Bean 映射框架

## 工具简介

**MapStruct** 是一个基于注解处理器（Annotation Processor）的 Java Bean 映射框架，在**编译期**自动生成类型安全的映射代码。与 BeanUtils、ModelMapper 等反射型框架不同，MapStruct 生成的是纯 Java `getter/setter` 调用代码，性能与手写代码完全等同。

- **GitHub**: https://github.com/mapstruct/mapstruct
- **官网**: https://mapstruct.org
- **文档**: https://mapstruct.org/documentation/stable/reference/html/
- **Star**: 7.2k+
- **版本**: 1.5.5.Final（当前稳定版，Java 8+）
- **License**: Apache 2.0

### 核心特性

| 特性 | 说明 |
|------|------|
| 零反射 | 编译期生成 setter 代码，运行时无反射开销 |
| 类型安全 | 字段名/类型不匹配在编译期报错 |
| 嵌套映射 | `order.user.username` 一行注解搞定 |
| 多源映射 | 多个对象字段合并到一个 DTO |
| 集合映射 | `List<A>` -> `List<B>` 自动实现 |
| 自定义转换 | expression/qualifiedByName/default方法 |
| Spring 集成 | componentModel="spring"，直接 @Autowired |

---

## Maven 依赖配置

### 依赖声明

```xml
<properties>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <lombok.version>1.18.30</lombok.version>
</properties>

<dependencies>
    <!-- MapStruct 核心（只含注解，运行时依赖） -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>

    <!-- Lombok（可选，但实际项目几乎必用） -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 编译器插件配置（关键！）

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>8</source>
                <target>8</target>
                <!--
                    ⚠️ 关键：Lombok 必须排在 MapStruct 前面！
                    原因：Lombok 先生成 getter/setter，MapStruct 再读取这些方法生成映射代码。
                    如果顺序颠倒，MapStruct 找不到 getter/setter，映射会失败。
                -->
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Gradle 配置

```groovy
dependencies {
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
    // Lombok 同时使用时
    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'
    // 注意：Lombok 的 annotationProcessor 要在 mapstruct-processor 之前声明
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
}
```

---

## 核心用法

### 1. 基础映射（字段名不同时）

```java
// Entity
@Data
public class User {
    private Long id;
    private String username;
    private String realName;   // ← 字段名与 DTO 不同
    private String password;
    private Integer status;
    private Date createTime;
}

// DTO
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String name;           // ← realName 对应
    private String statusDesc;     // ← status 转换后的描述
    private String createTimeStr;  // ← Date 格式化为字符串
    // 注意：没有 password 字段
}

// Mapper
@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(source = "realName", target = "name")
    @Mapping(source = "createTime", target = "createTimeStr",
             dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "statusDesc",
             expression = "java(user.getStatus() == 1 ? \"启用\" : \"禁用\")")
    UserDTO toDTO(User user);
}

// 使用
UserDTO dto = UserMapper.INSTANCE.toDTO(user);
```

### 2. 忽略字段

```java
@Mapper
public interface UserMapper {
    // 创建时，id/password/createTime 由系统生成，不从 DTO 映射
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    User fromCreateRequest(UserCreateRequest request);
}
```

### 3. 嵌套对象展平

```java
// Order 中包含嵌套的 User
public class Order {
    private Long id;
    private User user;  // ← 嵌套对象
    private String productName;
}

// OrderDTO 展平了 user 的字段
public class OrderDTO {
    private Long orderId;
    private String username;    // ← 来自 order.user.username
    private String userPhone;   // ← 来自 order.user.phone
    private String productName;
}

// Mapper
@Mapper
public interface OrderMapper {
    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "user.username", target = "username")  // 嵌套点号语法
    @Mapping(source = "user.phone", target = "userPhone")
    OrderDTO toDTO(Order order);
}
```

### 4. 多源对象合并

```java
// 将 User 和 Address 两个对象合并到 DeliveryInfoDTO
@Mapper
public interface DeliveryInfoMapper {
    @Mapping(source = "user.realName", target = "recipientName")
    @Mapping(source = "user.phone", target = "phone")
    @Mapping(source = "address.province", target = "province")
    @Mapping(source = "address.city", target = "city")
    @Mapping(target = "fullAddress",
             expression = "java(address.getProvince() + address.getCity() + address.getDetail())")
    DeliveryInfoDTO toDTO(User user, Address address);
}

// 使用
DeliveryInfoDTO dto = mapper.toDTO(user, address);
```

### 5. 增量更新（@MappingTarget）

```java
@Mapper
public interface UserMapper {
    // 将 DTO 的字段更新到已有实体，不创建新对象
    @Mapping(source = "name", target = "realName")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateFromDTO(UserDTO dto, @MappingTarget User user);
}

// 使用（PATCH 接口场景）
User existingUser = userRepository.findById(id);
userMapper.updateFromDTO(updateDTO, existingUser);
// existingUser 的字段已更新，但 id/password 保持原值
userRepository.save(existingUser);
```

### 6. 自定义转换方法

```java
@Mapper
public interface OrderMapper {
    // 方式一：在 @Mapping 中用 expression 直接写 Java 代码
    @Mapping(target = "unitPriceYuan",
             expression = "java(new java.math.BigDecimal(order.getUnitPrice()).divide(new java.math.BigDecimal(\"100\")))")
    OrderDTO toDTO(Order order);

    // 方式二：在 Mapper 接口中写 default 方法，然后在 expression 中调用
    default BigDecimal centToYuan(Long cent) {
        return cent == null ? BigDecimal.ZERO
            : new BigDecimal(cent).divide(new BigDecimal("100"));
    }

    // 方式三：使用 @Named 注解命名转换方法，通过 qualifiedByName 引用
    @Named("statusToDesc")
    default String statusToDesc(Integer status) {
        return status == 1 ? "启用" : "禁用";
    }

    @Mapping(target = "statusDesc", source = "status", qualifiedByName = "statusToDesc")
    UserDTO toDTO(User user);
}
```

### 7. 集合映射

```java
@Mapper
public interface UserMapper {
    // 声明了 toDTO(User) 后，集合方法自动可用
    List<UserDTO> toDTOList(List<User> users);
    Set<UserDTO> toDTOSet(Set<User> users);
    Map<String, UserDTO> toDTOMap(Map<String, User> userMap);
}
```

---

## Spring Boot 集成方式

### pom.xml（Spring Boot 场景）

```xml
<!-- Spring Boot 不自带 MapStruct，需手动添加 -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```

### Mapper 接口（Spring 组件模式）

```java
// componentModel = "spring"：MapStruct 生成 @Component 注解的实现类
@Mapper(componentModel = "spring")
public interface UserMapper {
    // 不需要 INSTANCE 常量，通过 @Autowired 注入
    UserDTO toDTO(User user);
    List<UserDTO> toDTOList(List<User> users);
}
```

### Service 层注入

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;  // Spring 自动注入 MapStruct 生成的实现类

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return userMapper.toDTO(user);
    }

    public List<UserDTO> listUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toDTOList(users);  // 批量映射，一行搞定
    }

    public UserDTO createUser(UserCreateRequest request) {
        User user = userMapper.fromCreateRequest(request);
        user.setStatus(1);
        user.setCreateTime(new Date());
        userRepository.save(user);
        return userMapper.toDTO(user);
    }

    public UserDTO updateUser(Long id, UserDTO updateDTO) {
        User user = userRepository.findById(id).orElseThrow();
        userMapper.updateFromDTO(updateDTO, user);  // 增量更新
        user.setUpdateTime(new Date());
        userRepository.save(user);
        return userMapper.toDTO(user);
    }
}
```

### Controller 层示例

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody @Valid UserCreateRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UserDTO updateDTO) {
        return ResponseEntity.ok(userService.updateUser(id, updateDTO));
    }
}
```

### 全局 MapStruct 配置（可选）

```java
// 配置所有 Mapper 的默认行为
@MapperConfig(
    componentModel = "spring",
    // NullValueMappingStrategy：如何处理 null 源对象
    // RETURN_NULL（默认）：源为 null 时返回 null
    // RETURN_DEFAULT：返回空对象（new TargetType()）
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL,
    // NullValuePropertyMappingStrategy：@MappingTarget 时如何处理 null 字段
    // SET_TO_NULL（默认）：源字段为 null，目标字段也设为 null
    // IGNORE：源字段为 null 时，不更新目标字段（保留原值）
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BaseMapperConfig {}

// 各 Mapper 引用全局配置
@Mapper(config = BaseMapperConfig.class)
public interface UserMapper {
    // ...
}
```

---

## 注意事项

### ⚠️ Bug 风险

**1. Lombok + MapStruct 注解处理器顺序错误**
```
问题：User 使用 @Data，UserMapper 编译时找不到 getter/setter，生成空映射
症状：所有字段映射后都是 null
解决：annotationProcessorPaths 中 lombok 必须排在 mapstruct-processor 前面
验证：查看 target/generated-sources/annotations/ 中的 *MapperImpl.java
```

**2. 同名字段不同类型导致静默失败**
```
问题：User.status 是 int，UserDTO.status 是 String
症状：不报错，但映射结果不正确（调用了 Integer.toString()）
解决：用 @Mapping(target="status", ignore=true) + expression 显式转换
```

**3. @MappingTarget 与 null 字段**
```
问题：使用 updateFromDTO 时，DTO 中 null 字段也会覆盖目标对象原有值
场景：PATCH 接口只更新部分字段，null 表示"不更新"
解决：在 @Mapper 或 @MapperConfig 中配置
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
```

**4. 多模块 Maven 项目**
```
问题：子模块继承父 pom 时，annotationProcessorPaths 不被继承
症状：子模块中 Mapper 生成的实现类为空
解决：每个需要 MapStruct 的子模块都要单独配置 maven-compiler-plugin
```

### ⚠️ 性能问题

**5. expression 中创建大量临时对象**
```java
// 不推荐：在 expression 中频繁 new 对象
@Mapping(target = "price",
    expression = "java(new java.math.BigDecimal(entity.getPrice()).divide(new java.math.BigDecimal(\"100\")))")

// 推荐：提取为 default 方法，可复用且更清晰
default BigDecimal centToYuan(Long cent) {
    return new BigDecimal(cent).divide(BigDecimal.valueOf(100));
}
```

### ⚠️ 使用限制

**6. 不支持运行时动态映射**
```
MapStruct 是编译期框架，无法在运行时动态改变映射规则。
如果需要运行时动态映射（如根据配置决定字段映射），需用 ModelMapper 或手写代码。
```

**7. 循环引用处理**
```
场景：User 包含 List<Order>，Order 又包含 User（双向关联）
问题：直接映射会栈溢出
解决：
  方案1：在其中一端用 @Mapping(target="user", ignore=true) 打断循环
  方案2：DTO 设计时不包含反向关联（推荐）
  方案3：使用 @Context 传入已映射对象集合，手动处理循环引用
```

**8. 接口与抽象类的选择**
```
优先用接口（interface）：更简洁，可定义 default 方法
需要用抽象类（abstract class）的场景：
  - 需要注入 Spring Bean（如 Spring @Autowired 其他 Service）
  - 需要在映射前后执行复杂的初始化逻辑
```

---

## 运行方法

### 1. 直接运行 Demo

```bash
cd java-tools-learning/mapstruct-demo

# 编译并打包（MapStruct 在此时生成映射代码）
mvn clean package -DskipTests

# 运行基础演示（User 映射，字段名转换，批量映射）
java -cp target/mapstruct-demo-1.0-SNAPSHOT.jar \
     com.example.mapstruct.MapStructBasicDemo

# 运行进阶演示（嵌套对象展平，单位转换，状态码转换）
java -cp target/mapstruct-demo-1.0-SNAPSHOT.jar \
     com.example.mapstruct.MapStructAdvancedDemo

# 运行实战演示（多源合并映射，批量处理，Spring Boot 集成指南）
java -cp target/mapstruct-demo-1.0-SNAPSHOT.jar \
     com.example.mapstruct.MapStructPracticalDemo
```

### 2. 查看生成的映射代码

```bash
# 编译后，查看 MapStruct 生成的实现类（理解其工作原理）
cat target/generated-sources/annotations/com/example/mapstruct/mapper/UserMapperImpl.java
```

生成的代码示例（MapStruct 自动生成，等同于手写）：
```java
@Generated(...)
public class UserMapperImpl implements UserMapper {
    @Override
    public UserDTO toDTO(User user) {
        if (user == null) { return null; }
        UserDTO userDTO = new UserDTO();
        userDTO.setName(user.getRealName());          // realName -> name
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        // ... 其他字段
        // Date -> String 格式化
        if (user.getCreateTime() != null) {
            userDTO.setCreateTimeStr(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.getCreateTime()));
        }
        // 调用自定义方法
        userDTO.setStatusDesc(statusToDesc(user.getStatus()));
        return userDTO;
    }
}
```

### 3. IntelliJ IDEA 插件

安装 **MapStruct Support** 插件（JetBrains Marketplace），获得：
- `@Mapping` 注解中字段名自动补全
- 字段名错误时高亮提示
- 从 Entity 字段跳转到 Mapper 映射定义
- 从 Mapper 跳转到生成的实现类

---

## 快速对比：MapStruct vs BeanUtils

```java
// ❌ BeanUtils.copyProperties（反射，字段名必须完全一致，运行时才发现错误）
BeanUtils.copyProperties(user, userDTO);
// 问题：realName 不会映射到 name；password 也会被复制（安全漏洞！）

// ✅ MapStruct（编译期，字段名可不同，编译期报错，零安全风险）
UserDTO dto = userMapper.toDTO(user);
// realName -> name 正确映射；password 不在 DTO 中，编译期保证安全
```
