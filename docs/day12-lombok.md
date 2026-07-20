# Day 12: Lombok - Java 代码生成工具

## 📚 工具简介

**Lombok** 是一个 Java 库，通过注解自动生成样板代码（boilerplate code），如 getter/setter/constructor/toString/equals/hashCode 等。它在编译时处理注解，不依赖运行时反射，极大提升开发效率。

| 项目 | 信息 |
|------|------|
| **GitHub** | https://github.com/projectlombok/lombok |
| **星标** | 24k+ |
| **最新版本** | 1.18.30 |
| **Java 版本** | Java 8+ |
| **官网** | https://projectlombok.org |

## 🎯 为什么使用 Lombok

1. **减少样板代码** - 再也不用写 getXXX()/setXXX()/toString() 等方法
2. **代码更简洁** - 实体类从上百行减少到几十行
3. **统一规范** - 所有生成的代码风格一致
4. **编译时处理** - 不依赖运行时反射，性能无损耗
5. **IDE 支持完善** - 支持 IntelliJ IDEA、Eclipse、VS Code

## 📦 Maven 依赖

```xml
<!-- Lombok 核心依赖 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <scope>provided</scope>
</dependency>
```

**注意**: Lombok 的 scope 是 `provided`，因为它只在编译时使用，不需要打包到最终产物。

## 🔧 Maven 编译配置

**重要**: 必须配置注解处理器路径，否则 Lombok 注解不会生效！

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>1.8</source>
        <target>1.8</target>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>1.18.30</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

## 📝 核心注解一览

### 1. 数据类注解

| 注解 | 说明 | 等价于 |
|------|------|--------|
| `@Data` | 所有基础注解组合 | @Getter @Setter @ToString @EqualsAndHashCode @RequiredArgsConstructor |
| `@Getter` | 生成所有 getter | - |
| `@Setter` | 生成所有 setter | - |
| `@ToString` | 生成 toString() | - |
| `@EqualsAndHashCode` | 生成 equals() 和 hashCode() | - |

### 2. 构造函数注解

| 注解 | 说明 |
|------|------|
| `@NoArgsConstructor` | 生成无参构造 |
| `@AllArgsConstructor` | 生成全参构造 |
| `@RequiredArgsConstructor` | 生成必需参构造（final 和 @NonNull 字段） |

### 3. 高级注解

| 注解 | 说明 |
|------|------|
| `@Builder` | 生成 Builder 模式 |
| `@With` | 生成 withXXX() 方法创建拷贝副本 |
| `@Singular` | 集合单数化，用于 Builder 模式 |
| `@Value` | 生成不可变类（所有字段 final） |
| `@Delegate` | 委托代理 |
| `@Log` | 生成日志对象（支持多种日志框架） |
| `@UtilityClass` | 工具类注解 |
| `@NonNull` | 参数非空校验 |

## 🛠 Spring Boot 集成

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### 2. IDEA 安装插件

1. File → Settings → Plugins
2. 搜索 "Lombok"
3. 安装 "Lombok" 插件
4. 重启 IDEA

### 3. 配置属性类示例

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String name;
    private Integer port;
    private List<String> allowedOrigins;
}
```

```yaml
# application.yml
app:
  name: my-application
  port: 8080
  allowed-origins:
    - https://example.com
```

### 4. 实体类示例

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 100)
    private String email;

    // 排除字段不参与 equals/hashCode
    @EqualsAndHashCode.Exclude
    private String password;

    @ToString.Exclude
    private String salt;
}
```

### 5. 统一响应封装

```java
@Data
@Builder
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
```

## ⚠️ 注意事项

### 1. IDE 插件必须安装

- **IntelliJ IDEA**: 安装 Lombok 插件
- **Eclipse**: 需要安装 lombok.jar 或使用 lombok-eclipse 插件
- **VS Code**: 安装 Red Hat Java Extensions Pack

### 2. @EqualsAndHashCode 的继承问题

```java
@Data
class Parent {
    private Long id;
}

@Data
class Child extends Parent {
    private String name;
}
```

**问题**: 子类的 equals/hashCode 默认不包含父类字段。

**解决方案**:
```java
@Data(callSuper = true)
class Child extends Parent {
    private String name;
}
```

### 3. @Builder 不继承

@Builder 注解不会自动继承到子类！

**解决方案**:
```java
// 使用 @SuperBuilder（ Lombok 1.18.6+）
@SuperBuilder
class Parent {
    private Long id;
}

@SuperBuilder
class Child extends Parent {
    private String name;
}
```

### 4. 序列化兼容性

使用 `@Data` 时，生成的 `equals()` 和 `hashCode()` 基于所有非 static 字段。如果类实现 `Serializable`，确保添加 `serialVersionUID`：

```java
@Data
class MyClass implements Serializable {
    private static final long serialVersionUID = 1L;
    private String field1;
    private transient String field2; // transient 字段被排除
}
```

### 5. @Value vs @Data

| 特性 | @Value | @Data |
|------|--------|-------|
| 所有字段 | final | 非 final |
| Setter | ❌ | ✅ |
| CanEqual | ❌ | ✅ |
| 适用场景 | DTO/VO/不可变对象 | 实体类/Mutable 对象 |

### 6. @Builder.Default 的坑

```java
@Data
@Builder
class MyClass {
    private String name;

    @Builder.Default
    private List<String> items = new ArrayList<>(); // 默认值
}

// 正确用法 - 使用 builder 时指定默认值
MyClass obj = MyClass.builder().name("test").build();
System.out.println(obj.getItems().size()); // 0

// 问题：如果不通过 builder 创建
MyClass obj2 = new MyClass();
obj2.setName("test");
System.out.println(obj2.getItems().size()); // 0 - 默认值生效
```

### 7. MapStruct 配合问题

如果同时使用 Lombok 和 MapStruct，需要注意处理器顺序：

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>1.5.5.Final</version>
    </path>
</annotationProcessorPaths>
```

**注意**: Lombok 必须放在 MapStruct 之前！

## 🏃 运行方法

### 编译项目

```bash
cd lombok-demo
mvn clean compile
```

### 运行基础演示

```bash
mvn exec:java -Dexec.mainClass="com.example.lombok.LombokBasicDemo"
```

### 运行高级演示

```bash
mvn exec:java -Dexec.mainClass="com.example.lombok.LombokAdvancedDemo"
```

### 运行实战演示

```bash
mvn exec:java -Dexec.mainClass="com.example.lombok.LombokPracticalDemo"
```

### 打包

```bash
mvn clean package -DskipTests
```

## 📚 扩展阅读

- [Lombok 官方文档](https://projectlombok.org/features/)
- [Lombok 使用指南](https://www.baeldung.com/intro-to-lombok)
- [@Builder 继承解决方案](https://www.baeldung.com/lombok-builder-inheritance)
- [Lombok vs Record (Java 16+)](https://www.baeldung.com/java-record-vs-lombok)

## 🎯 最佳实践总结

1. **@Data 慎用** - 只在确定需要所有基础方法时使用
2. **实体类用 @Data + callSuper** - 处理继承关系
3. **DTO/VO 用 @Value + @Builder** - 不可变对象更安全
4. **统一响应用 @Builder** - 链式构建清晰
5. **记得安装 IDE 插件** - 开发体验的关键
6. **处理 @Builder 继承** - 使用 @SuperBuilder
