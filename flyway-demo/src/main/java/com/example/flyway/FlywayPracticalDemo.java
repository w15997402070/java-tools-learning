package com.example.flyway;

import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Flyway 实战演示：电商订单数据库的完整迁移实战
 *
 * 演示场景：
 * 1. 版本升级路径：V1 建用户表 → V2 建用户资料表 → V3 建订单表 → V4 建订单明细+种子数据
 * 2. 多次调用 migrate() 的增量升级（模拟：数据库先部署到 V2，后新增 V3/V4）
 * 3. 复杂查询验证：多表 JOIN 验证迁移后的数据可用性
 * 4. 说明 Java-based Migration 与 Spring Boot 集成方式
 */
public class FlywayPracticalDemo {

    // 使用文件数据库，便于观察实际数据
    private static final String JDBC_URL = "jdbc:h2:mem:flyway-practical-demo;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void main(String[] args) throws Exception {
        System.out.println("===== Flyway 实战演示：电商订单数据库 =====\n");

        // 第一阶段：初始部署（V1~V4 全部执行）
        System.out.println("--- 阶段1：初始部署 ---");
        Flyway flyway = Flyway.configure()
                .dataSource(JDBC_URL, USER, PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();
        flyway.migrate();
        System.out.println("  初始部署完成：V1-V4 全部应用\n");

        // 验证迁移后的数据结构与数据
        verifySchemaAndData();

        // 演示复杂业务查询（多表 JOIN）
        demonstrateBusinessQueries();

        // 说明 Java-based Migration（实战中复杂数据迁移常用）
        demonstrateJavaMigration();

        // 说明 Spring Boot 集成
        demonstrateSpringBootIntegration();

        System.out.println("===== 演示结束 =====");
    }

    /** 验证表结构与种子数据 */
    private static void verifySchemaAndData() throws SQLException {
        System.out.println("--- 验证表结构与种子数据 ---");
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
            // 列出所有表
            System.out.print("  数据库表: ");
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    System.out.print(rs.getString("TABLE_NAME") + "  ");
                }
            }
            System.out.println();

            // 验证 users 种子数据
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT username, email FROM users")) {
                System.out.println("  users 种子数据:");
                while (rs.next()) {
                    System.out.printf("    - %-12s %s%n", rs.getString("username"), rs.getString("email"));
                }
            }

            // 验证 user_profiles 种子数据
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT u.username, p.full_name, p.phone " +
                         "FROM user_profiles p JOIN users u ON p.user_id = u.id")) {
                System.out.println("  user_profiles 种子数据:");
                while (rs.next()) {
                    System.out.printf("    - %-12s %-15s %s%n",
                            rs.getString("username"), rs.getString("full_name"), rs.getString("phone"));
                }
            }
        }
        System.out.println();
    }

    /** 演示基于迁移结果的多表 JOIN 查询 */
    private static void demonstrateBusinessQueries() throws SQLException {
        System.out.println("--- 业务查询演示（多表 JOIN） ---");
        String sql = "SELECT u.username, COUNT(o.id) AS order_count, COALESCE(SUM(o.total_amount), 0) AS total_spent " +
                "FROM users u " +
                "LEFT JOIN orders o ON o.user_id = u.id " +
                "GROUP BY u.username " +
                "ORDER BY total_spent DESC";

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("  用户消费汇总:");
            System.out.println("  --------------------------------------------------");
            System.out.printf("  %-12s %-12s %-12s%n", "用户名", "订单数", "消费总额");
            while (rs.next()) {
                System.out.printf("  %-12s %-12d ¥%-11.2f%n",
                        rs.getString("username"),
                        rs.getInt("order_count"),
                        rs.getBigDecimal("total_spent").doubleValue());
            }
        }
        System.out.println();

        // 插入一条订单演示 DML（注意：数据变更应放在迁移脚本或业务代码，此处仅为演示查询）
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO orders (user_id, order_no, total_amount, status) VALUES (?, ?, ?, 'PAID')")) {
            ps.setLong(1, 1L);
            ps.setString(2, "SO-20260901-0001");
            ps.setBigDecimal(3, new java.math.BigDecimal("299.50"));
            ps.executeUpdate();
        }

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT order_no, total_amount, status FROM orders")) {
            System.out.println("  订单表数据:");
            while (rs.next()) {
                System.out.printf("    - %s  金额=¥%.2f  状态=%s%n",
                        rs.getString("order_no"),
                        rs.getBigDecimal("total_amount").doubleValue(),
                        rs.getString("status"));
            }
        }
        System.out.println();
    }

    /**
     * Java-based Migration 说明
     * 适用场景：需要执行复杂逻辑的迁移（数据清洗、调用第三方 API、文件处理等）。
     * 命名：V{版本}__{描述}.java，类需实现 BaseJavaMigration 接口。
     * 注意：Java Migration 在 Flyway 9.x 由 flyway-core 提供支持，无需额外依赖。
     */
    private static void demonstrateJavaMigration() {
        System.out.println("--- Java-based Migration 说明 ---");
        System.out.println("  适用场景：复杂数据清洗、文件迁移、API 调用等纯 SQL 无法完成的迁移");
        System.out.println("  示例类（写法）：");
        System.out.println("    public class V5__DataCleanup extends BaseJavaMigration {");
        System.out.println("        public void migrate(Context context) {");
        System.out.println("            // context.getConnection() 获取 JDBC 连接");
        System.out.println("            // 执行复杂的数据清洗逻辑");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println("  文件位置：和 SQL 脚本放同一目录（classpath:db/migration）\n");
    }

    /** Spring Boot 集成说明 */
    private static void demonstrateSpringBootIntegration() {
        System.out.println("--- Spring Boot 集成 ---");
        System.out.println("  方式一（推荐）：加依赖后自动执行");
        System.out.println("    <dependency>");
        System.out.println("      <groupId>org.flywaydb</groupId>");
        System.out.println("      <artifactId>flyway-core</artifactId>");
        System.out.println("    </dependency>");
        System.out.println("    <dependency>");
        System.out.println("      <groupId>org.flywaydb</groupId>");
        System.out.println("      <artifactId>flyway-mysql</artifactId>  <!-- MySQL 8 需要 -->");
        System.out.println("    </dependency>");
        System.out.println("  Spring Boot 启动时会自动调用 flyway.migrate()");
        System.out.println();
        System.out.println("  常用配置（application.yml）：");
        System.out.println("    spring:");
        System.out.println("      flyway:");
        System.out.println("        enabled: true");
        System.out.println("        locations: classpath:db/migration");
        System.out.println("        baseline-on-migrate: true   # 老项目推荐");
        System.out.println("        clean-disabled: true        # 生产必须 true");
        System.out.println("        validate-on-migrate: true");
        System.out.println("        out-of-order: false");
        System.out.println();
        System.out.println("  脚本位置约定：src/main/resources/db/migration/");
        System.out.println("  命名规范：V{版本}__{描述}.sql（两个下划线，描述用单词下划线连接）\n");
    }
}
