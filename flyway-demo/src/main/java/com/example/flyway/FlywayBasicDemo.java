package com.example.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.MigrateResult;

import java.sql.*;

/**
 * Flyway 基础演示：初始化、迁移、查看迁移历史
 *
 * 核心概念：
 * - Migration（迁移）：对数据库的每一次变更脚本
 * - Version（版本）：按 V{数字}__{描述}.sql 命名，Flyway 按版本号顺序执行
 * - Schema History Table：flyway_schema_history，记录已执行的迁移
 * - Baseline：对已有数据库建立基线版本
 */
public class FlywayBasicDemo {

    // H2 内存数据库，演示用（实际项目常用 MySQL/PostgreSQL）
    private static final String JDBC_URL = "jdbc:h2:mem:flyway-basic-demo;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void main(String[] args) throws Exception {
        System.out.println("===== Flyway 基础演示 =====\n");

        // 1. 配置并创建 Flyway 实例
        Flyway flyway = Flyway.configure()
                .dataSource(JDBC_URL, USER, PASSWORD)
                .locations("classpath:db/migration")  // 迁移脚本位置
                .baselineOnMigrate(false)              // 不自动基线
                .load();

        // 2. 执行迁移（自动按版本号顺序执行未执行的脚本）
        System.out.println(">>> 执行 migrate()...");
        flyway.migrate();
        System.out.println("迁移完成！\n");

        // 3. 查看迁移历史
        System.out.println(">>> 查询 flyway_schema_history 表：");
        printMigrationHistory();

        // 4. 验证数据库结构
        System.out.println(">>> 验证数据库表结构：");
        printDatabaseTables();

        // 5. 查询种子数据
        System.out.println(">>> 查询 users 表数据：");
        printUsers();

        // 6. 查看 MigrationInfo（API 方式）
        System.out.println(">>> 通过 API 查看迁移信息：");
        MigrationInfoService infoService = flyway.info();
        for (MigrationInfo info : infoService.all()) {
            System.out.printf("  版本: %-5s | 状态: %-10s | 描述: %s%n",
                    info.getVersion(), info.getState(), info.getDescription());
        }

        // 7. 重复执行 migrate() —— 幂等性验证
        System.out.println("\n>>> 再次执行 migrate()（验证幂等性）...");
        MigrateResult result = flyway.migrate();
        System.out.println("迁移数量: " + result.migrationsExecuted + "（应为0，说明已是最新）");

        System.out.println("\n===== 演示结束 =====");
    }

    private static void printMigrationHistory() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT \"version\", \"description\", \"type\", \"script\", \"installed_on\", \"success\" " +
                     "FROM \"flyway_schema_history\" ORDER BY \"installed_rank\"")) {

            System.out.println("  版本 | 描述 | 类型 | 脚本 | 安装时间 | 成功");
            System.out.println("  --------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("  %-5s | %-20s | %-6s | %-25s | %s | %s%n",
                        rs.getString("version"),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getString("script"),
                        rs.getTimestamp("installed_on"),
                        rs.getBoolean("success"));
            }
        }
        System.out.println();
    }

    private static void printDatabaseTables() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            System.out.print("  数据库表: ");
            while (rs.next()) {
                System.out.print(rs.getString("TABLE_NAME") + "  ");
            }
            System.out.println("\n");
        }
    }

    private static void printUsers() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, username, email, created_at FROM users")) {

            while (rs.next()) {
                System.out.printf("  ID=%d | username=%-10s | email=%-20s | created=%s%n",
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getTimestamp("created_at"));
            }
        }
        System.out.println();
    }
}
