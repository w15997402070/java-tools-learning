# Day 31: MyBatis — Java 持久层框架

## 工具简介

MyBatis 是一款优秀的**半自动 ORM（对象关系映射）框架**，它避免了几乎所有的 JDBC 代码和手动设置参数以及获取结果集的工作。MyBatis 通过 XML 或注解的方式将 SQL 与 Java 对象映射起来，让开发者能够更灵活地控制 SQL 语句，同时享受 ORM 带来的便利。

- **GitHub**: https://github.com/mybatis/mybatis-3
- **官方文档**: https://mybatis.org/mybatis-3/
- **星标**: 19k+
- **版本**: 3.5.15（Java 8 兼容）
- **许可证**: Apache 2.0

### 与 Hibernate/JPA 的区别

| 特性 | MyBatis | Hibernate/JPA |
|------|---------|---------------|
| SQL 控制 | 手动编写，完全可控 | 自动生成，灵活性较低 |
| 学习曲线 | 较低（会 SQL 即可） | 较高（需理解对象状态、缓存等） |
| 性能优化 | 直接优化 SQL 即可 | 需理解框架内部机制 |
| 复杂查询 | 原生 SQL 支持 | 需 JPQL/Criteria API |
| 数据库特性 | 完全支持 | 部分受限 |
| 适用场景 | 遗留系统、复杂 SQL、高性能要求 | 快速开发、标准 CRUD |

---

## Maven 依赖配置

### 核心依赖（无 Spring）

```xml
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.15</version>
</dependency>
```

### Spring Boot 集成

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.1</version>  <!-- Spring Boot 2.x 兼容 -->
</dependency>

<!-- 数据库驱动（示例：MySQL） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- 连接池（推荐 HikariCP，Spring Boot 默认已包含） -->
```

### 可选增强依赖

```xml
<!-- 分页插件 -->
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>1.4.7</version>
</dependency>

<!-- 代码生成器（MyBatis Generator） -->
<dependency>
    <groupId>org.mybatis.generator</groupId>
    <artifactId>mybatis-generator-core</artifactId>
    <version>1.4.2</version>
</dependency>
```

---

## Spring Boot 集成方式

### 1. application.yml 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.entity
  configuration:
    map-underscore-to-camel-case: true  # 自动驼峰映射
    cache-enabled: true               # 全局开启二级缓存
    lazy-loading-enabled: true        # 开启懒加载
    default-executor-type: simple     # simple/reuse/batch
  type-handlers-package: com.example.handler  # 自定义类型处理器
```

### 2. 启动类配置

```java
@SpringBootApplication
@MapperScan("com.example.mapper")  // 扫描 Mapper 接口包
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. Mapper 接口 + Service 使用

```java
@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    List<User> findByCondition(@Param("name") String name, 
                                @Param("age") Integer age);
}
```

```java
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Transactional
    public void createUser(User user) {
        userMapper.insert(user);
        // 其他业务操作...
    }
}
```

### 4. XML 映射文件（resources/mapper/UserMapper.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">
    <select id="findByCondition" resultType="com.example.entity.User">
        SELECT * FROM users
        <where>
            <if test="name != null">AND name LIKE CONCAT('%', #{name}, '%')</if>
            <if test="age != null">AND age = #{age}</if>
        </where>
    </select>
</mapper>
```

---

## 核心概念速查

### SqlSession 生命周期

```
SqlSessionFactoryBuilder（构建一次）
    → SqlSessionFactory（应用生命周期，单例）
        → SqlSession（请求/事务级别，用完关闭）
            → Mapper（从 SqlSession 获取）
```

### 映射方式对比

| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| 注解（@Select/@Insert） | 简洁，无需 XML | 复杂 SQL 难以维护 | 简单 CRUD |
| XML | 复杂 SQL 灵活，可复用 | 需维护 XML 文件 | 复杂查询、动态 SQL |
| 混合 | 简单用注解，复杂用 XML | 分散在两处 | 大多数项目 |

### 动态 SQL 标签

- `<if>`: 条件判断
- `<choose>/<when>/<otherwise>`: 多分支选择
- `<where>`: 自动处理 WHERE 和 AND/OR
- `<set>`: 自动处理 SET 和逗号
- `<foreach>`: 遍历集合（IN查询、批量插入）
- `<trim>`: 通用前缀/后缀处理
- `<bind>`: 创建变量绑定

---

## 注意事项

### ⚠️ Bug 风险与常见问题

1. **SQL 注入风险**
   - ❌ 危险：`${param}`（直接字符串拼接）
   - ✅ 安全：`#{param}`（预编译参数绑定）
   - 唯一使用 `${}` 的场景：动态表名/列名（需白名单校验）

2. **N+1 查询问题**
   - 使用 `collection` 或 `association` 懒加载时，可能触发 N+1 查询
   - 解决：使用 JOIN 一次性查询，或配置 `fetchType="eager"`

3. **一级缓存导致的脏读**
   - 同一 `SqlSession` 内，重复查询会命中缓存
   - 解决：插入/更新/删除后手动调用 `session.clearCache()`

4. **自增主键回写失败**
   - 确保 `@Options(useGeneratedKeys = true, keyProperty = "id")` 配置正确
   - 数据库表必须有自增主键

5. **返回类型匹配错误**
   - `resultType` 用于简单映射，复杂关联用 `resultMap`
   - 字段名不匹配时，开启 `mapUnderscoreToCamelCase` 或配置 `resultMap`

6. **Mapper 接口未扫描到**
   - 检查 `@MapperScan` 包路径是否正确
   - 或检查 XML 的 `namespace` 是否与接口全限定名一致

### ⚠️ 性能问题

1. **BATCH 模式正确使用**
   - 批量插入使用 `ExecutorType.BATCH`，但要定期 `flushStatements()`
   - 大量数据时，每 100~1000 条刷新一次，避免内存溢出

2. **连接池配置**
   - 生产环境必须使用连接池（HikariCP / Druid）
   - POOLED 数据源在并发场景下性能不足

3. **二级缓存慎用**
   - 默认关闭，开启后可能导致数据不一致
   - 分布式环境下推荐用 Redis 替代 MyBatis 二级缓存

4. **分页插件**
   - 大数据量分页避免 `LIMIT offset, size`（深度分页性能差）
   - 使用游标或覆盖索引优化

### ⚠️ 使用限制

1. **Java 版本**：MyBatis 3.5.x 支持 Java 8+，3.6+ 可能需要 Java 11+
2. **Spring Boot 版本兼容性**：
   - Spring Boot 2.x → mybatis-spring-boot-starter 2.x
   - Spring Boot 3.x → mybatis-spring-boot-starter 3.x（需 Java 17+）
3. **XML 文件位置**：Maven 项目需确保 XML 在 `src/main/resources` 下，且不被过滤

---

## 运行方法

### 方式 1：直接运行 Demo 类

```bash
cd mybatis-demo
mvn clean package -DskipTests

# 运行基础演示
mvn exec:java -Dexec.mainClass="com.example.mybatis.MyBatisBasicDemo"

# 运行进阶演示
mvn exec:java -Dexec.mainClass="com.example.mybatis.MyBatisAdvancedDemo"

# 运行实战演示
mvn exec:java -Dexec.mainClass="com.example.mybatis.MyBatisPracticalDemo"
```

### 方式 2：IDE 中运行

直接在 IntelliJ IDEA / Eclipse 中运行对应类的 `main` 方法。

### 方式 3：Maven 构建

```bash
cd mybatis-demo
mvn clean package -DskipTests
java -cp target/mybatis-demo-1.0-SNAPSHOT.jar com.example.mybatis.MyBatisBasicDemo
```

> 注意：本 Demo 使用 H2 内存数据库，无需额外安装数据库，运行后数据自动初始化。

---

## 参考资源

- [MyBatis 官方文档](https://mybatis.org/mybatis-3/)
- [MyBatis-Spring 文档](https://mybatis.org/spring/)
- [MyBatis-Spring-Boot 文档](https://mybatis.org/spring-boot-starter/)
- [MyBatis 3 源码](https://github.com/mybatis/mybatis-3)
