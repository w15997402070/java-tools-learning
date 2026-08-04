# Day 34: Alibaba Druid — 高性能 JDBC 连接池

> **核心定位**：阿里开源的 Java JDBC 连接池实现，自带 **SQL 统计 / 慢 SQL 告警 / SQL 注入防护（WallFilter）/ 数据库密码加密 / 内嵌 Web 监控**，是 Java 后端最常用的连接池之一（与 HikariCP 齐名）。中文社区和阿里系项目应用极广。

---

## 📚 1. 工具简介

| 项目 | 信息 |
| --- | --- |
| **GitHub** | https://github.com/alibaba/druid |
| **官方文档** | https://github.com/alibaba/druid/wiki |
| **Maven Central** | https://mvnrepository.com/artifact/com.alibaba/druid |
| **最新稳定版** | 1.2.24（Java 8 兼容，1.2.x 系列可放心使用） |
| **License** | Apache 2.0 |
| **Java 版本** | 1.2.x 兼容 Java 8+；2.0+ 需要 Java 11+ |
| **Spring Boot 集成** | `druid-spring-boot-starter`（Boot 2.x）/ `druid-spring-boot-3-starter`（Boot 3.x） |

### 核心特性

- ✅ **高性能连接池**：基于 `ReentrantLock` + `Condition` 实现，性能与 HikariCP 接近
- ✅ **StatFilter 统计**：自动记录每条 SQL 的执行次数 / 耗时 / 最大并发 / 慢 SQL 告警
- ✅ **WallFilter 防 SQL 注入**：内置 SQL 解析器识别危险语句（多语句、危险函数、注释绕过等）
- ✅ **ConfigFilter 密码加密**：RSA 公私钥加密 DB 密码，避免明文泄露
- ✅ **Web 监控界面**：内置 StatViewServlet + WebStatFilter，开箱即用
- ✅ **removeAbandoned**：自动回收长时间未关闭的连接，防止应用层忘记 `close()` 导致连接耗尽
- ✅ **SLF4J 日志**：可与 Logback / Log4j2 集成（推荐生产关闭 DEBUG）

---

## 🔧 2. Maven 依赖配置

### 2.1 核心连接池

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.24</version>
</dependency>
```

### 2.2 Spring Boot 自动装配

**Spring Boot 2.x**（基于 javax.servlet）：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.24</version>
</dependency>
```

**Spring Boot 3.x**（基于 jakarta.servlet）：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-3-starter</artifactId>
    <version>1.2.24</version>
</dependency>
```

---

## 🌱 3. Spring Boot 集成方式

### 3.1 application.yml 完整配置（推荐）

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://${DB_HOST:localhost}:3306/${DB_NAME:app}?useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8
    username: ${DB_USER:root}
    password: ${DB_PWD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    druid:
      # ===== 连接池大小 =====
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      # ===== 性能检测 =====
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      # ===== 过滤器 =====
      filters: stat,wall,config
      filter:
        stat:
          slow-sql-millis: 500        # 慢 SQL 阈值（生产建议 200-500ms）
          log-slow-sql: true
        wall:
          db-type: mysql
          config:
            delete-allow: false        # 禁止 DELETE（视业务调整）
            drop-table-allow: false    # 禁止 DROP TABLE
        config:                       # 密码加密
          enabled: true
      # ===== Web 监控 =====
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: ${DRUID_USER:admin}
        login-password: ${DRUID_PWD:ChangeMe!}
        allow: 127.0.0.1,10.0.0.0/8    # IP 白名单
        reset-enable: false              # 禁止重置统计
      # ===== 泄漏检测 =====
      remove-abandoned: true
      remove-abandoned-timeout: 300      # 5 分钟
      log-abandoned: true                # 打印泄漏堆栈
```

### 3.2 Java Config 方式（不依赖 Spring Boot 自动装配）

```java
@Configuration
public class DruidConfig {

    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/app");
        ds.setUsername("root");
        ds.setPassword("password");
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setInitialSize(5);
        ds.setMinIdle(5);
        ds.setMaxActive(20);
        ds.setMaxWait(60000);
        ds.setValidationQuery("SELECT 1");
        ds.setTestWhileIdle(true);
        ds.setPoolPreparedStatements(true);
        // 过滤器
        ds.setFilters("stat,wall,config");
        return ds;
    }
}
```

### 3.3 StatViewServlet 手动注册（Spring MVC）

```java
@Configuration
public class DruidMonitorConfig {

    @Bean
    public ServletRegistrationBean<StatViewServlet> statViewServlet() {
        StatViewServlet servlet = new StatViewServlet();
        ServletRegistrationBean<StatViewServlet> bean =
                new ServletRegistrationBean<>(servlet, "/druid/*");
        Map<String, String> params = new HashMap<>();
        params.put("loginUsername", "admin");
        params.put("loginPassword", "admin");
        params.put("allow", "127.0.0.1");        // 白名单
        params.put("resetEnable", "false");
        bean.setInitParameters(params);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<WebStatFilter> webStatFilter() {
        WebStatFilter filter = new WebStatFilter();
        FilterRegistrationBean<WebStatFilter> bean = new FilterRegistrationBean<>(filter);
        bean.addUrlPatterns("/*");
        Map<String, String> params = new HashMap<>();
        params.put("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        params.put("sessionStatEnable", "true");
        bean.setInitParameters(params);
        return bean;
    }
}
```

### 3.4 MyBatis + Druid 集成

只需在 `application.yml` 写好 Druid 配置，MyBatis 自动使用：

```yaml
spring:
  datasource:                          # 复用上面的 Druid 配置
    type: com.alibaba.druid.pool.DruidDataSource
    ...
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.entity
```

---

## 🛡 4. WallFilter — SQL 注入防护

WallFilter 通过内置 SQL 解析器（Druid SQL Parser）识别危险语句。**生产环境强烈建议开启！**

### 4.1 支持的数据库

| 数据库 | WallProvider 类 |
| --- | --- |
| MySQL | `MySqlWallProvider` |
| Oracle | `OracleWallProvider` |
| PostgreSQL | `PGWallProvider`（不是 PostgreSqlWallProvider） |
| SQL Server | `SQLServerWallProvider` |
| DB2 | `DB2WallProvider` |
| ClickHouse | `CKWallProvider` |
| SQLite | `SQLiteWallProvider` |

### 4.2 默认拦截规则（MySQL）

- ❌ 多语句执行：`SELECT 1; DROP TABLE users`
- ❌ MySQL 注释绕过：`/*! UNION SELECT 2 */`
- ❌ 危险函数：`load_file`、`into outfile`、`SLEEP`、`BENCHMARK`
- ❌ DROP / TRUNCATE 等破坏性语句（可通过配置项放开）
- ❌ `union select` 注入

### 4.3 自定义放行/拦截

```java
WallConfig config = new WallConfig();
config.setDeleteAllow(true);         // 允许 DELETE
config.setDropTableAllow(false);     // 禁止 DROP
config.setReadOnlyTables(Arrays.asList("t_config")); // 禁止写入的表
WallFilter wallFilter = new WallFilter();
wallFilter.setDbType("mysql");
wallFilter.setConfig(config);
```

---

## 🔐 5. ConfigFilter — 数据库密码加密

避免明文密码泄露到 `application.yml` 或配置中心。

### 5.1 生成密钥对（首次部署执行一次）

```bash
java -cp druid-1.2.24.jar com.alibaba.druid.filter.config.ConfigTools
# 输出：
# publicKey=MIIBIjANB...
# privateKey=MIIEvAIBAD...
```

### 5.2 加密密码

```bash
java -cp druid-1.2.24.jar com.alibaba.druid.filter.config.ConfigTools 密码 公钥
# 输出加密后的密文
```

### 5.3 配置文件使用

```properties
# jdbc.properties
jdbc.url=jdbc:mysql://localhost:3306/app
jdbc.username=root
jdbc.password=加密后的密文
jdbc.publicKey=公钥
filters=config
```

**⚠️ 关键**：私钥必须妥善保管，建议放在配置中心或受控目录下，**不能**打进 jar 包！

---

## 🧪 6. 自定义 Filter（SPI 扩展）

继承 `FilterAdapter`，重写 `statement_executeUpdate` / `statement_executeQuery` 等生命周期方法：

```java
FilterAdapter auditFilter = new FilterAdapter() {
    @Override
    public int statement_executeUpdate(FilterChain chain, StatementProxy stmt, String sql) throws SQLException {
        log.info("[审计] DML: {}", sql);
        return super.statement_executeUpdate(chain, stmt, sql);
    }
};
ds.getProxyFilters().add(auditFilter);
```

**应用场景**：SQL 审计 / 性能监控 / 慢 SQL 链路追踪 / 自动 SQL 注释注入（APM 集成）。

---

## 🧯 7. 连接泄漏检测

应用层忘记 `close()` 连接会导致连接池耗尽。**生产环境必须开启**：

```java
ds.setRemoveAbandoned(true);             // 启用
ds.setRemoveAbandonedTimeout(300);       // 5 分钟未关闭视为泄漏
ds.setLogAbandoned(true);                // 打印泄漏堆栈
```

开启后 Druid 会自动回收被泄漏的连接，并在日志中打印泄漏的代码位置（堆栈），便于排查。

---

## ⚠ 8. 注意事项（坑点）

### 8.1 性能调优

| 参数 | 默认值 | 推荐值 | 说明 |
| --- | --- | --- | --- |
| `maxActive` | 8 | 10-50 | 最大活跃连接数，建议 = (QPS × 平均RT) ÷ 0.3 |
| `initialSize` | 0 | = `minIdle` | 启动时建好的连接数 |
| `minIdle` | 0 | = CPU 核数 × 2 | 最小空闲连接，避免冷启动慢 |
| `maxWait` | -1（无限） | 3000-60000 | 拿不到连接时等待 ms，**必须设置上限** |
| `validationQuery` | null | `SELECT 1` | 启用 `testWhileIdle` 时必须设置 |
| `testWhileIdle` | false | true | 空闲时检测，推荐开启 |
| `testOnBorrow` | true | **false** | 借连接时检测，**关闭**（影响性能） |

**maxActive 计算公式（仅供参考）**：
```
maxActive = (核心业务每秒 SQL 数 × 平均 RT) / 0.3
```

### 8.2 版本兼容性

- **Druid 1.2.x**：兼容 Java 8/11/17，**生产首选**
- **Druid 2.0+**：要求 Java 11+，部分新特性
- **Spring Boot 3.x**：必须用 `druid-spring-boot-3-starter`（jakarta 包名）
- **Spring Boot 2.x**：用 `druid-spring-boot-starter`（javax 包名）

### 8.3 常见 Bug / 风险

1. **连接泄漏**：忘记 `close()` 不会立即报错，要等 `removeAbandoned` 触发。**生产必须开启**。
2. **密码泄露**：`filters=config` 没启用时，密码明文传入。生产环境建议 **强制** 启用。
3. **SQL 拦截过严**：`wall` 过滤器的 `selectHaving` / `selectUnion` 等默认配置可能误伤业务 SQL，需要测试验证。
4. **maxWait 无限等待**：默认 `-1` 会在池满时无限阻塞，**必须**设置上限。
5. **Web 监控未鉴权**：`/druid/*` 暴露 SQL 统计，**必须**通过 Nginx / 网关白名单 + 账号密码限制访问。
6. **统计内存增长**：`maxSqlSize` 默认 1000，超过会 LRU 淘汰；超高 QPS 场景适当调大。
7. **多数据源冲突**：使用 Spring Boot 多数据源时，**每个**数据源都需要单独配置 Druid 过滤器，否则 StatFilter 不会生效。
8. **HikariCP 对比**：Druid 提供更多运维特性（监控、SQL 防护、加密），但**纯性能**略逊于 HikariCP。如果不需要这些特性，可考虑 HikariCP。

### 8.4 不适用场景

- ❌ **嵌入式 / 单线程应用**：直接用 `DriverManager.getConnection()` 即可
- ❌ **纯内存计算 / 不需要 DB 的微服务**：根本用不到
- ❌ **超大规模连接池（>1000）**：Druid 在大连接数下 lock 竞争明显，慎用

---

## 🚀 9. 运行方法

### 9.1 IDE 直接运行

打开 `DruidBasicDemo.java` / `DruidAdvancedDemo.java` / `DruidPracticalDemo.java`，右键 `Run`。

### 9.2 命令行运行

```bash
# 编译打包（生成可执行 fat-jar）
mvn clean package -DskipTests

# 运行
java -jar target/druid-demo-1.0-SNAPSHOT.jar
```

### 9.3 集成到 Spring Boot 项目

```bash
# 1. 引入依赖
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>  <!-- 或 druid-spring-boot-3-starter -->
    <version>1.2.24</version>
</dependency>

# 2. 复制 application.yml 配置（见第 3 节）
# 3. 启动项目，访问 http://localhost:8080/druid/
#    登录：admin / ChangeMe!（用配置中的账号密码）
```

---

## 📖 10. 参考资料

- [Druid GitHub Wiki](https://github.com/alibaba/druid/wiki)
- [Druid 常见问题 FAQ](https://github.com/alibaba/druid/wiki/FAQ)
- [Druid 配置参数大全](https://github.com/alibaba/druid/wiki/DruidDataSource%E9%85%8D%E7%BD%AE)
- [WallFilter 配置](https://github.com/alibaba/druid/wiki/%E9%85%8D%E7%BD%AE-wallfilter)
- [Spring Boot 集成 Demo](https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter)

---

## 📝 11. 本 Demo 演示清单

| Demo | 演示内容 | 关键类 / 方法 |
| --- | --- | --- |
| DruidBasicDemo.Demo1 | 工厂方式创建数据源 | `DruidDataSourceFactory.createDataSource()` |
| DruidBasicDemo.Demo2 | Setter 方式创建数据源 | `new DruidDataSource()` + setters |
| DruidBasicDemo.Demo3 | 基础 SQL 执行 | `Connection` / `PreparedStatement` / `ResultSet` |
| DruidBasicDemo.Demo4 | 连接池核心参数 | `getActiveCount()` / `getPoolingCount()` |
| DruidBasicDemo.Demo5 | StatFilter 慢 SQL 监控 | `StatFilter` + `dataSource.getDataSourceStat()` |
| DruidAdvancedDemo.Demo1 | StatFilter 慢 SQL 监控 | `setSlowSqlMillis()` / `setMergeSql()` |
| DruidAdvancedDemo.Demo2 | WallFilter SQL 注入防护 | `MySqlWallProvider` / `WallProvider.check()` |
| DruidAdvancedDemo.Demo3 | ConfigFilter 密码加密 | `ConfigTools.genKeyPair()` / `encrypt()` |
| DruidAdvancedDemo.Demo4 | 自定义 Filter 扩展 | `FilterAdapter` 重写 `statement_executeXxx` |
| DruidAdvancedDemo.Demo5 | 连接泄漏检测 | `setRemoveAbandoned()` / `setLogAbandoned()` |
| DruidPracticalDemo.Demo1 | 多数据源（读写分离） | 多 `DruidDataSource` 实例 |
| DruidPracticalDemo.Demo2-5 | Spring Boot 集成配置 | application.yml 完整模板 |

---

> ✅ **完成时间**：2026-08-04
> ✅ **工具版本**：Alibaba Druid 1.2.24
> ✅ **Java 兼容性**：Java 8+
> ✅ **GitHub**：https://github.com/w15997402070/java-tools-learning
