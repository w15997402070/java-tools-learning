# Day 40: Flyway — 数据库版本迁移框架

> 数据库结构的版本管理工具，让你像管理代码一样管理数据库变更。

## 📌 简介

Flyway 是一个开源的数据库迁移（Migration）框架，它把每一次数据库结构变更（建表、加字段、加索引、数据修正等）封装成一个**版本化的迁移脚本**，按版本号顺序自动执行，并通过 `flyway_schema_history` 元数据表记录执行历史。

**核心价值**：解决了团队开发中数据库结构"改了就忘、忘了就乱、人人手工同步"的痛点，让数据库结构像 Git 一样可版本化、可追溯、可回滚。

- **GitHub**: https://github.com/flyway/flyway
- **官网**: https://flywaydb.org
- **星标**: 8k+（数据库迁移领域事实标准，Spring Boot 默认集成）
- **版本**: 9.22.3（Java 8 兼容；10.x 需 Java 17+）
- **License**: Apache 2.0（社区版免费；企业版含更多功能）

### 支持数据库
MySQL、PostgreSQL、Oracle、SQL Server、H2、SQLite、MariaDB、DB2、ClickHouse 等 30+ 种。

## 📦 Maven 依赖

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.3</version>
</dependency>

<!-- 仅 MySQL 8+ 需要单独引入（因为 MySQL 驱动拆分） -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
    <version>9.22.3</version>
</dependency>
```

> **注意**：9.x 开始，MySQL 支持被拆分为独立模块 `flyway-mysql`，只用 `flyway-core` 连 MySQL 会报 "Unsupported Database: MySQL"。

## 🧩 核心概念

### 1. 迁移脚本命名规范
```
V{版本号}__{描述}.sql
```
- **V**（版本迁移）：唯一，不可修改已执行的脚本
- **两个下划线** `__` 分隔版本和描述（不是单个下划线）
- 描述用单词+下划线：`V1__Create_user_table.sql`
- 版本号建议纯数字递增：V1、V2、V3...

示例脚本：
```sql
-- V1__Create_user_table.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE
);
```

### 2. Schema History Table
- 默认表名：`flyway_schema_history`
- 记录：版本号、描述、脚本类型（SQL/JAVA）、checksum、执行时间、成功与否
- 每次 `migrate()` 都会对比 history 表与磁盘上的脚本

### 3. 迁移类型
| 类型 | 说明 |
|------|------|
| **Versioned** | 有版本号的迁移（V1、V2...），只执行一次 |
| **Repeatable** | 无版本号（R__xxx.sql），内容变化时重新执行，适合视图/函数/存储过程 |
| **Java-based** | 用 Java 类写迁移逻辑，适合复杂数据清洗 |
| **Undo** | 反向迁移（企业版功能，社区版无） |

## 🔧 常用 API

```java
// 1. 配置并加载
Flyway flyway = Flyway.configure()
        .dataSource(url, user, password)     // 数据源
        .locations("classpath:db/migration") // 脚本位置
        .baselineOnMigrate(true)             // 老项目：自动基线
        .cleanDisabled(true)                 // 生产必须禁用 clean！
        .outOfOrder(false)                   // 禁止乱序
        .load();

// 2. 执行迁移
MigrateResult result = flyway.migrate();
int count = result.migrationsExecuted();     // 本次执行的迁移数

// 3. 校验（每次 migrate 前默认执行）
flyway.validate();                           // checksum 不匹配会抛异常

// 4. 打基线（老库引入 Flyway）
flyway.baseline();                           // baselineVersion 默认为 1

// 5. 修复 history 表
flyway.repair();                             // 清除 failed 记录、对齐 checksum

// 6. 查询迁移信息
MigrationInfoService info = flyway.info();
for (MigrationInfo m : info.all()) {
    m.getVersion();   // 版本
    m.getState();     // 状态
    m.getDescription(); // 描述
}
```

## 🍃 Spring Boot 集成

### 1. 添加依赖
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```
Spring Boot 启动时**自动**执行迁移，无需任何代码。

### 2. 配置 application.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: root
  flyway:
    enabled: true
    locations: classpath:db/migration   # 默认位置
    baseline-on-migrate: true            # 老项目推荐
    baseline-version: 1
    clean-disabled: true                 # 生产必须 true
    validate-on-migrate: true
    out-of-order: false
    encoding: UTF-8
```

### 3. 代码示例（配置 Bean）
```java
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        // 自定义迁移逻辑（例如先清空再迁移——仅测试环境）
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
```

### 4. 脚本位置
```
src/main/resources/db/migration/
├── V1__Create_user_table.sql
├── V2__Add_user_profile_table.sql
└── V3__Add_order_table.sql
```

## ⚠️ 注意事项（坑）

### 1. checksum 不匹配（最常见坑）
- **现象**：`Migration checksum mismatch for migration version 1`
- **原因**：有人修改了**已执行过**的迁移脚本
- **解决**：a) 还原脚本（推荐） b) `repair()` 重新对齐（危险，可能掩盖问题）
- **原则**：**已提交的脚本永不修改**，新变更用新版本 V{n+1}

### 2. 命名规范错误
- `V1_create_table.sql`（单下划线）→ Flyway 无法识别版本
- 正确：`V1__create_table.sql`（双下划线）
- Repeatable 脚本：`R__create_view.sql`

### 3. MySQL DDL 隐式提交
- MySQL 的 DDL 不支持事务回滚
- 迁移中途失败会留下"半执行"状态
- 此时 `migrate()` 会被阻塞（history 表有 failed 记录）
- 解决：手工修复数据库后 `repair()`

### 4. 版本号重复/冲突
- 多人并行开发同时加 V3 → 提交时冲突
- 解决：a) 团队约定版本号分配规则 b) 使用 outOfOrder 但风险高
- **推荐**：用分支合并时解决冲突，保持版本号全局唯一

### 5. clean() 是危险操作
- `clean()` 会删除**所有表**（包括非 Flyway 管理的）
- 生产环境**必须** `clean-disabled: true`
- 测试环境也建议禁用，用 drop/create 代替

### 6. 大表 ALTER 性能问题
- MySQL 加字段/索引在大表上会锁表、耗时
- 大型迁移建议：a) 业务低峰执行 b) 使用在线 DDL（pt-online-schema-change）c) 分阶段迁移

### 7. 迁移与代码顺序
- 新代码依赖新字段，但迁移未执行 → 报错
- 部署顺序：**先跑迁移，再发布代码**（或同批次内迁移在前）

## 🚀 运行方法

```bash
# 进入项目目录
cd flyway-demo

# 编译并打包
mvn clean package -DskipTests

# 运行基础演示（迁移 + 历史 + 验证）
java -cp target/flyway-demo-1.0-SNAPSHOT.jar com.example.flyway.FlywayBasicDemo

# 运行进阶演示（校验/基线/修复/回调/配置）
java -cp target/flyway-demo-1.0-SNAPSHOT.jar com.example.flyway.FlywayAdvancedDemo

# 运行实战演示（电商数据库完整迁移）
java -cp target/flyway-demo-1.0-SNAPSHOT.jar com.example.flyway.FlywayPracticalDemo
```

或使用 Maven 插件（需先启动对应数据库）：
```bash
# 执行迁移
mvn flyway:migrate

# 查看信息
mvn flyway:info

# 校验
mvn flyway:validate
```

## 🎯 演示类说明

| 类 | 说明 |
|----|------|
| `FlywayBasicDemo` | 基础：配置、迁移、history 表、幂等性验证 |
| `FlywayAdvancedDemo` | 进阶：validate/repair/baseline/Callback/常用配置 |
| `FlywayPracticalDemo` | 实战：电商订单数据库版本升级、多表 JOIN、Spring Boot 集成指南 |

### 迁移脚本
| 脚本 | 内容 |
|------|------|
| `V1__Create_user_table.sql` | 建用户表 + 索引 |
| `V2__Create_user_profile_table.sql` | 建用户资料表（一对一） |
| `V3__Add_order_table.sql` | 建订单表 + 索引 |
| `V4__Add_order_items_and_seed_data.sql` | 建订单明细表 + 种子数据 |

## 💡 最佳实践

1. **版本号只增不减**，已提交的迁移永不改
2. **一个迁移做一件事**，便于 review 和回滚
3. 每个迁移脚本都**过 code review**，像对待代码一样
4. CI 环境跑 `mvn flyway:validate` + `mvn flyway:migrate` 自动化验证
5. 生产发布：迁移先行，代码跟进
6. 老项目引入：用 `baseline()` 打基线，不要重做历史
7. 需要视图/函数：用 Repeatable 迁移（R__）
8. 敏感数据变更：用 Java-based migration 加日志和审计
