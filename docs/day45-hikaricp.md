# Day 45: HikariCP — 高性能 JDBC 连接池

## 一、工具简介

**HikariCP** 是由 Brett Wooldridge 开发的高性能 JDBC 连接池，以“zero overhead”为设计目标，是目前 Java 生态中性能最好的连接池之一，也是 **Spring Boot 2.x/3.x 的默认数据源**。

- **GitHub**: https://github.com/brettwooldridge/HikariCP
- **官方文档**: https://github.com/brettwooldridge/HikariCP/blob/dev/README.md
- **Maven Central**: https://central.sonatype.com/artifact/com.zaxxer/HikariCP
- **星标**: 19k+（连接池领域事实标准）
- **当前最新稳定版**: **5.1.0**（需要 Java 11+）
- **Java 8 兼容版本**: **4.0.3**（最后一个支持 Java 8 的版本）
- **License**: Apache License 2.0

### 为什么选择 HikariCP？

| 维度 | HikariCP | Druid | Tomcat JDBC Pool | C3P0 |
| --- | --- | --- | --- | --- |
| 性能 | ⭐⭐⭐ 极致 | ⭐⭐ 优秀 | ⭐⭐ 良好 | ⭐ 一般 |
| 体积 | 小（约 150 KB） | 较大（含监控与防御） | 中等 | 较大 |
| Spring Boot 默认 | ✅ 是 | ❌ 否 | ❌ 否 | ❌ 否 |
| 监控能力 | 基础（JMX/Metrics） | 丰富（Web UI/WallFilter） | 一般 | 一般 |
| 防 SQL 注入 | ❌ 无 | ✅ WallFilter | ❌ 无 | ❌ 无 |
| 配置简洁度 | ✅ 简单 | 中等 | 中等 | 复杂 |

**结论**：
- 追求极简与性能，且已有独立监控体系 → **HikariCP**
- 需要一体化监控、SQL 审计、防注入、密码加密 → **Druid**
- 两者可以互补：主库用 HikariCP，核心资产库用 Druid

### 核心能力

```
连接池管理
├── 快速获取连接（无锁设计 + FastList）
├── 连接生命周期管理（idleTimeout / maxLifetime / connectionTimeout）
├── 连接泄漏检测（leakDetectionThreshold）
└── 连接健康检查（connectionTestQuery / keepalive）

可观测性
├── JMX MBean（HikariPoolMXBean）
├── MetricsTrackerFactory（对接 Micrometer/Prometheus）
└── 详细日志（DEBUG 级别输出连接创建与回收）

集成
├── Spring Boot 自动配置（spring.datasource.hikari.*）
├── MyBatis / JPA / Hibernate 通用
└── 多数据源手动配置
```

## 二、Maven 依赖配置

```xml
<properties>
    <!-- HikariCP 5.x 需要 Java 11+，Java 8 项目请锁定 4.0.3 -->
    <hikaricp.version>4.0.3</hikaricp.version>
    <h2.version>2.2.224</h2.version>
    <slf4j.version>1.7.36</slf4j.version>
    <logback.version>1.2.13</logback.version>
</properties>

<dependencies>
    <!-- HikariCP 核心 -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>${hikaricp.version}</version>
    </dependency>

    <!-- 演示用 H2 内存数据库 -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <version>${h2.version}</version>
    </dependency>

    <!-- 日志 -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>${slf4j.version}</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
    </dependency>
</dependencies>
```

## 三、核心配置参数速览

| 参数 | 说明 | 默认值 | 建议 |
| --- | --- | --- | --- |
| `jdbcUrl` | 数据库 JDBC URL | 无 | 必填 |
| `username` / `password` | 数据库账号密码 | 无 | 必填 |
| `driverClassName` | JDBC 驱动类名 | 自动推断 | 一般无需设置 |
| `maximumPoolSize` | 连接池最大连接数 | 10 | CPU 核心数 * 2 + 磁盘数 |
| `minimumIdle` | 最小空闲连接数 | 与 maximumPoolSize 相同 | 固定大小时设为相同 |
| `connectionTimeout` | 获取连接最大等待时间（ms） | 30000 | 通常 5~30 秒 |
| `idleTimeout` | 空闲连接回收时间（ms） | 600000 | 仅当 minimumIdle < maximumPoolSize 生效 |
| `maxLifetime` | 连接最大生命周期（ms） | 1800000 | 小于数据库 wait_timeout |
| `leakDetectionThreshold` | 连接泄漏检测阈值（ms） | 0（关闭） | 开发环境建议 2000~60000 |
| `connectionTestQuery` | 连接测试 SQL | 无 | MySQL/PostgreSQL/H2 用 `SELECT 1` |
| `validationTimeout` | 连接验证超时时间（ms） | 5000 | 小于 connectionTimeout |
| `registerMbeans` | 是否注册 JMX | false | 生产监控建议开启 |
| `poolName` | 连接池名称 | 自动生成 | 建议显式命名 |
| `keepaliveTime` | 连接保活检测间隔（ms） | 0（关闭） | 长连接场景建议开启 |

## 四、Spring Boot 集成方式

### 4.1 单数据源（默认）

在 `application.yml` 中：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_db?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true
    username: order_user
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      pool-name: OrderHikariPool
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
      register-mbeans: true
```

Spring Boot 会自动注入 `DataSource`，直接通过 `@Autowired` 使用：

```java
@Service
public class OrderService {
    @Autowired
    private DataSource dataSource;

    public int countOrders() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
```

### 4.2 多数据源

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public HikariConfig primaryConfig() {
        return new HikariConfig();
    }

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return new HikariDataSource(primaryConfig());
    }

    @Bean
    @ConfigurationProperties("spring.datasource.report")
    public HikariConfig reportConfig() {
        return new HikariConfig();
    }

    @Bean
    public DataSource reportDataSource() {
        return new HikariDataSource(reportConfig());
    }
}
```

对应 `application.yml`：

```yaml
spring:
  datasource:
    primary:
      jdbc-url: jdbc:mysql://localhost:3306/order_db
      username: order_user
      password: ${DB_PASSWORD}
      maximum-pool-size: 20
    report:
      jdbc-url: jdbc:mysql://localhost:3306/report_db
      username: report_user
      password: ${DB_PASSWORD}
      maximum-pool-size: 5
```

**注意**：多数据源时必须使用 `jdbc-url` 而不是 `url`，因为 `HikariConfig` 的属性名是 `jdbcUrl`。

## 五、演示代码说明

### 5.1 HikariCpBasicDemo

- 演示连接池创建、CRUD、连接释放
- 重点：始终使用 try-with-resources 关闭 Connection / Statement / ResultSet

### 5.2 HikariCpAdvancedDemo

- 演示 `leakDetectionThreshold`、`connectionTimeout`、`idleTimeout`、`maxLifetime`
- 演示 JMX `HikariPoolMXBean` 读取连接池实时状态
- 演示自定义 `MetricsTrackerFactory` 对接指标系统

### 5.3 HikariCpPracticalDemo

- 演示 Spring Boot 风格的连接池配置
- 演示多数据源（主库 + 报表库）
- 演示批量插入 `addBatch()` / `executeBatch()`
- 演示连接获取失败时的重试机制

## 六、注意事项

### 6.1 版本与 Java 兼容性

- **HikariCP 4.0.3** 是最后一个支持 **Java 8** 的版本
- **HikariCP 5.x** 要求 **Java 11+**
- Spring Boot 3.x 默认使用 HikariCP 5.x，若项目仍使用 Java 8，需要手动降级到 4.0.3

### 6.2 连接池大小设置

- 经验公式：`connections = ((core_count * 2) + effective_spindle_count)`
- 现代 SSD 环境下 effective_spindle_count 通常取 1
- 不要盲目开大连接池，过多连接会拖垮数据库（连接数 ≠ 吞吐量）

### 6.3 连接泄漏

- 最常见的生产故障：获取 Connection 后未 close
- 开启 `leakDetectionThreshold` 后，借用时间超过阈值会打印堆栈，帮助定位
- 推荐搭配 `try-with-resources` 或 Spring 的 `@Transactional`

### 6.4 maxLifetime 必须小于数据库超时

- `maxLifetime` 应小于数据库的 `wait_timeout`（MySQL）或 `idle_in_transaction_session_timeout`（PostgreSQL）
- 否则数据库端先关闭连接，连接池可能拿到一个已断开的连接

### 6.5 connectionTestQuery 与 keepaliveTime

- HikariCP 默认使用 `Connection.isValid()` 验证连接，通常无需设置 `connectionTestQuery`
- 某些旧驱动不支持 `isValid()` 时，才需要显式设置 `connectionTestQuery`
- `keepaliveTime` 用于定期探活，适合数据库防火墙会断开长连接的场景

### 6.6 Spring Boot 多数据源配置陷阱

- 多数据源时必须使用 `jdbc-url` 而不是 `url`
- `spring.datasource.hikari.*` 只作用于默认单数据源；多数据源需拆分到 `spring.datasource.xxx.hikari.*`
- 多数据源下事务管理器需要手动配置，否则 `@Transactional` 会找不到事务

### 6.7 不要在 try-with-resources 外持有 Connection

```java
// 错误：Connection 作用域超出 try 块，容易泄漏
Connection conn = dataSource.getConnection();
try { ... } finally { conn.close(); }

// 正确
 try (Connection conn = dataSource.getConnection()) { ... }
```

### 6.8 与 Druid 的对比

- HikariCP 只解决“高性能连接池”问题，不提供 SQL 监控、防火墙、密码加密
- 若项目需要强审计和监控，可主库用 HikariCP、核心库用 Druid，或额外引入 p6spy

## 七、运行方法

### 7.1 编译打包

```bash
cd hikaricp-demo
mvn clean package -DskipTests
```

### 7.2 运行指定 Demo

```bash
# 基础演示
java -jar target/hikaricp-demo-1.0-SNAPSHOT.jar

# 进阶演示
java -cp target/hikaricp-demo-1.0-SNAPSHOT.jar com.example.hikaricp.HikariCpAdvancedDemo

# 实战演示
java -cp target/hikaricp-demo-1.0-SNAPSHOT.jar com.example.hikaricp.HikariCpPracticalDemo
```

### 7.3 Windows 环境下直接运行

若 `mvn` 命令在 Git Bash 中报 classworlds 错误，可使用 JDK 直接调用 Maven Launcher：

```bash
"D:\jdk\jdk1.8.0_212\bin\java.exe" \
  -Dmaven.multiModuleProjectDirectory=D:\ai\workbuddy\java-tools-learning\hikaricp-demo \
  -classpath "D:\apache-maven-3.6.3\boot\plexus-classworlds-2.6.0.jar" \
  -Dclassworlds.conf=D:\apache-maven-3.6.3\bin\m2.conf \
  -Dmaven.home=D:\apache-maven-3.6.3 \
  org.codehaus.plexus.classworlds.launcher.Launcher clean package -DskipTests
```

## 八、总结

HikariCP 凭借其极简设计、出色性能和 Spring Boot 默认集成，成为现代 Java 后端连接池的首选。掌握其核心参数（`maximumPoolSize`、`minimumIdle`、`maxLifetime`、`leakDetectionThreshold`）和正确关闭资源的习惯，能有效避免连接泄漏、数据库挂起等生产故障。对于需要更强监控和 SQL 防御能力的场景，可与 Druid 或 p6spy 配合使用。
