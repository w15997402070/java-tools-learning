# Day 41: Orika - Java Bean 映射框架

## 1. 工具简介

**Orika** 是一个高性能的 Java Bean 映射框架，通过运行时动态生成字节码实现对象之间的属性复制。与 MapStruct（编译期生成）不同，Orika 在运行时完成映射逻辑编译，配置更加灵活，适合需要动态映射规则的场景。

| 特性 | 说明 |
|------|------|
| 映射方式 | 运行时字节码生成（基于 Javassist） |
| 性能 | 接近手写 Getter/Setter，远快于反射 |
| 线程安全 | MapperFacade 线程安全，可单例使用 |
| 双向映射 | 自动支持 A->B 和 B->A |
| 类型转换 | 支持自定义 Converter |
| 集合映射 | 支持 List/Set/Map/Array 批量转换 |

- **GitHub**: https://github.com/orika-mapper/orika
- **官方文档**: https://orika-mapper.github.io/orika-docs/
- **版本**: 1.5.4（Java 8 兼容）
- **星标**: 1.2k+

## 2. Maven 依赖

```xml
<dependency>
    <groupId>ma.glasnost.orika</groupId>
    <artifactId>orika-core</artifactId>
    <version>1.5.4</version>
</dependency>
```

## 3. 核心概念

```
┌─────────────────┐     register      ┌─────────────────┐
│  MapperFactory  │ ─────────────────>│  ClassMapBuilder │
│  (映射工厂)      │                   │  (映射规则构建器)  │
└────────┬────────┘                   └─────────────────┘
         │
         │ getMapperFacade()
         ▼
┌─────────────────┐     map()         ┌─────────────────┐
│  MapperFacade   │ ─────────────────>│  目标对象         │
│  (映射执行器)    │                   │  (Destination)   │
│  线程安全单例    │                   └─────────────────┘
└─────────────────┘
```

## 4. 代码示例

### 4.1 基础映射（同名字段自动映射）

```java
// 创建工厂并注册映射
MapperFactory factory = new DefaultMapperFactory.Builder().build();
factory.classMap(Source.class, Target.class)
       .byDefault()  // 同名字段自动映射
       .register();

MapperFacade mapper = factory.getMapperFacade();

// 执行映射
Target target = mapper.map(source, Target.class);
```

### 4.2 字段别名映射

```java
factory.classMap(User.class, UserDTO.class)
       .field("id", "userId")          // 不同字段名映射
       .field("username", "userName")  // 大小写不同
       .field("createTime", "registerTime")
       .exclude("password")            // 排除字段
       .byDefault()
       .register();
```

### 4.3 自定义类型转换器

```java
// 单向转换器
factory.getConverterFactory().registerConverter("booleanToString",
    new CustomConverter<Boolean, String>() {
        @Override
        public String convert(Boolean source, Type<? extends String> destType) {
            return Boolean.TRUE.equals(source) ? "已激活" : "未激活";
        }
    });

// 在映射规则中使用
factory.classMap(User.class, UserDTO.class)
       .fieldMap("active", "status").converter("booleanToString").add()
       .register();

// 双向转换器
factory.getConverterFactory().registerConverter(
    new BidirectionalConverter<Integer, String>() {
        @Override
        public String convertTo(Integer source, Type<String> destType) {
            return source == 1 ? "已支付" : "待支付";
        }
        @Override
        public Integer convertFrom(String source, Type<Integer> destType) {
            return "已支付".equals(source) ? 1 : 0;
        }
    });
```

### 4.4 集合批量映射

```java
List<User> users = userRepository.findAll();
List<UserDTO> dtoList = mapper.mapAsList(users, UserDTO.class);
```

### 4.5 多源合并映射

```java
// 先映射 User 字段
UserOrderDTO dto = mapper.map(user, UserOrderDTO.class);
// 再映射 Order 字段到同一对象（合并，不覆盖）
mapper.map(order, dto);
```

## 5. Spring Boot 集成

### 5.1 配置类

```java
@Configuration
public class OrikaConfig {

    @Bean
    public MapperFactory mapperFactory() {
        return new DefaultMapperFactory.Builder().build();
    }

    @Bean
    public MapperFacade mapperFacade(MapperFactory mapperFactory) {
        // 注册所有映射规则
        mapperFactory.classMap(User.class, UserDTO.class)
                .field("id", "userId")
                .field("username", "userName")
                .exclude("password")
                .byDefault()
                .register();

        mapperFactory.classMap(Order.class, OrderDTO.class)
                .field("orderId", "id")
                .field("totalAmount", "amount")
                .register();

        return mapperFactory.getMapperFacade();
    }
}
```

### 5.2 Service 中使用

```java
@Service
public class UserService {

    @Autowired
    private MapperFacade mapperFacade;
    @Autowired
    private UserRepository userRepository;

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return mapperFacade.map(user, UserDTO.class);
    }

    public List<UserDTO> listUsers() {
        List<User> users = userRepository.findAll();
        return mapperFacade.mapAsList(users, UserDTO.class);
    }
}
```

## 6. 注意事项

### 6.1 性能优化

| 优化点 | 说明 |
|--------|------|
| 单例使用 | MapperFacade 线程安全，应作为单例复用 |
| 预注册规则 | 启动时注册所有映射规则，避免运行时动态注册 |
| 避免频繁创建 Factory | 每个 Factory 有编译开销，应用生命周期内只创建一次 |
| 编译模式 | 默认使用 Javassist，可通过 `useBuiltinConverters(false)` 减少内置转换器扫描 |

### 6.2 常见 Bug 与限制

1. **循环引用问题**
   ```java
   // 如果 User 包含 List<Order>，Order 又包含 User，会导致 StackOverflow
   // 解决方案：使用 .exclude() 排除循环字段，或自定义映射逻辑
   factory.classMap(User.class, UserDTO.class)
          .exclude("orders")  // 排除循环引用字段
          .byDefault()
          .register();
   ```

2. **泛型擦除**
   ```java
   // 以下代码会编译失败，因为泛型擦除
   // mapper.map(source, List.class); // 错误！
   // 正确做法：
   mapper.mapAsList(sourceList, Target.class);
   ```

3. **null 值处理**
   ```java
   // 默认 null 字段也会映射为 null
   // 如需跳过 null：
   factory.classMap(Source.class, Target.class)
          .mapNulls(false)  // 不映射 null 值
          .byDefault()
          .register();
   ```

4. **版本兼容性**
   - Orika 1.5.x 是最后一个支持 Java 8 的版本
   - 1.6.x 开始需要 Java 11+
   - 项目维护活跃度较低（2020年后更新减少），生产环境建议评估 MapStruct 作为替代

5. **与 Lombok 配合**
   ```java
   // 确保 Lombok 在编译期生成 getter/setter
   // Orika 运行时通过反射读取属性，需要完整的访问器
   @Data
   public class User {
       private String name;
   }
   ```

### 6.3 与 MapStruct 对比

| 维度 | Orika | MapStruct |
|------|-------|-----------|
| 映射时机 | 运行时 | 编译期 |
| 性能 | 接近原生（有首次编译开销） | 原生（无运行时开销） |
| 配置灵活性 | 高（可运行时动态配置） | 中（编译期确定） |
| 调试难度 | 较难（字节码生成） | 简单（生成可见代码） |
| 依赖 | 运行时依赖 orika-core | 仅编译期依赖 |
| 维护状态 | 维护缓慢 | 活跃维护 |
| 适用场景 | 动态映射、规则多变 | 静态映射、追求极致性能 |

## 7. 运行方法

```bash
# 进入项目目录
cd orika-demo

# 编译打包
mvn clean package -DskipTests

# 运行基础演示
java -cp target/orika-demo-1.0-SNAPSHOT.jar:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) com.example.orika.OrikaBasicDemo

# 或直接用 Maven Exec 插件运行
mvn exec:java -Dexec.mainClass="com.example.orika.OrikaBasicDemo"
mvn exec:java -Dexec.mainClass="com.example.orika.OrikaAdvancedDemo"
mvn exec:java -Dexec.mainClass="com.example.orika.OrikaPracticalDemo"
```

## 8. 参考资源

- [Orika GitHub](https://github.com/orika-mapper/orika)
- [Orika 官方文档](https://orika-mapper.github.io/orika-docs/)
- [Orika vs MapStack 性能对比](https://github.com/arey/java-object-mapper-benchmark)
