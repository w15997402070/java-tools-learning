package com.example.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Flyway 进阶演示：校验、基线、修复、回调、配置项
 *
 * 核心机制：
 * - validate()：校验已执行脚本的 checksum，防止已提交的迁移被篡改
 * - baseline()：给已有数据库打基线，基线之前的脚本不执行
 * - repair()：修复 history 表中失败的迁移记录（如删除失败记录、重新对齐 checksum）
 * - Callback：在迁移前后插入自定义逻辑（备份/通知等）
 *
 * 说明：每个子演示使用独立的 H2 内存库，避免状态互相干扰。
 */
public class FlywayAdvancedDemo {

    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void main(String[] args) throws Exception {
        System.out.println("===== Flyway 进阶演示 =====\n");

        demoValidate();
        demoRepair();
        demoBaseline();
        demoCallbacks();
        demoConfigOptions();

        System.out.println("===== 演示结束 =====");
    }

    /**
     * 演示 1：validate() —— 校验迁移完整性
     * Flyway 对每个已执行的脚本计算 checksum 并记录在 history 表。
     * 如果有人修改了已经执行过的脚本，validate 会失败——这是团队协作的关键保障。
     */
    private static void demoValidate() {
        System.out.println("--- 1. validate() 校验 ---");
        String url = "jdbc:h2:mem:flyway-adv-validate;DB_CLOSE_DELAY=-1";
        Flyway flyway = configure(url).load();
        org.flywaydb.core.api.output.MigrateResult mr = flyway.migrate();
        System.out.println("  首次 migrate 执行迁移数: " + mr.migrationsExecuted);

        // 模拟应用重启：重新加载一个新的 Flyway 实例再校验
        // （Flyway 的 info/validate 会缓存当前实例的迁移解析结果，
        //   应用重启后才是真实的校验场景）
        Flyway restarted = configure(url).load();
        restarted.validate();
        System.out.println("  重启后校验: 通过 ✓（应用重启时会自动校验，防止已提交迁移被篡改）");

        // 说明：如果修改已执行过的 V1 脚本再校验，会抛出 ValidationFailureException：
        // "Migration checksum mismatch for migration version 1"
        // 解决方案：a) 还原脚本（推荐） b) 执行 repair() 重新对齐 checksum
        System.out.println("  若修改已执行脚本再校验 → checksum mismatch 异常（保护机制）\n");
    }

    /**
     * 演示 2：repair() —— 修复 history 表
     * 典型场景：某次迁移执行到一半失败（DDL 不支持事务回滚，如 MySQL），
     * history 表留下 success=false 的记录，导致后续 migrate 被阻塞。
     * repair() 会删除失败记录，让开发者手工修复数据库后重新执行。
     */
    private static void demoRepair() {
        System.out.println("--- 2. repair() 修复 ---");
        String url = "jdbc:h2:mem:flyway-adv-repair;DB_CLOSE_DELAY=-1";
        Flyway flyway = configure(url).load();
        flyway.migrate();

        // repair 在正常状态下执行：重新对齐 checksum，无副作用
        flyway.repair();
        System.out.println("  repair() 执行完成（重新对齐已迁移脚本的描述与 checksum）");
        System.out.println("  典型用途：清除 failed 迁移记录、对齐被误改脚本的 checksum\n");
    }

    /**
     * 演示 3：baseline() —— 已有数据库引入 Flyway
     * 老项目已有表结构但没有迁移脚本时的处理方式：
     * 打一个 baseline（如版本 2），只有版本 > baseline 的脚本才会执行。
     */
    private static void demoBaseline() throws SQLException {
        System.out.println("--- 3. baseline() 基线 ---");
        String url = "jdbc:h2:mem:flyway-adv-baseline;DB_CLOSE_DELAY=-1";

        // 模拟"老数据库"：V1/V2 对应的表早已存在（当年手工建的），无任何 Flyway 痕迹
        try (Connection conn = DriverManager.getConnection(url, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "email VARCHAR(100) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE user_profiles (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL UNIQUE, " +
                    "full_name VARCHAR(100), " +
                    "phone VARCHAR(20), " +
                    "address VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        System.out.println("  模拟老库：users / user_profiles 已存在（无 flyway_schema_history）");

        // 对老库打基线：baselineVersion=2 表示 V1、V2 视为已应用（老库已有这些结构）
        Flyway baselineFlyway = configure(url)
                .baselineVersion("2")
                .baselineDescription("legacy schema import")
                .load();
        baselineFlyway.baseline();
        System.out.println("  baseline() 完成：基线版本 = 2");

        // 重新加载并迁移：只会执行 V3、V4（版本 > 2）
        Flyway migrateFlyway = configure(url).load();
        migrateFlyway.migrate();

        for (MigrationInfo info : migrateFlyway.info().applied()) {
            System.out.printf("  已应用: V%-3s %-30s [%s]%n",
                    info.getVersion(), info.getDescription(), info.getState());
        }

        // 验证：V1/V2 不会重复执行（BELOW_BASELINE 状态）
        boolean v1Skipped = false;
        for (MigrationInfo info : migrateFlyway.info().all()) {
            if (info.getVersion() != null
                    && "1".equals(info.getVersion().getVersion())
                    && info.getState() == MigrationState.BELOW_BASELINE) {
                v1Skipped = true;
            }
        }
        System.out.println("  V1 状态为 BELOW_BASELINE（跳过不执行）: " + (v1Skipped ? "是 ✓" : "否"));

        // 验证 V4 种子数据正常写入
        try (Connection conn = DriverManager.getConnection(url, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            System.out.println("  users 表行数: " + rs.getInt(1) + "（含 V4 插入的 3 条种子数据）");
        }
        System.out.println();
    }

    /**
     * 演示 4：Callback —— 迁移生命周期钩子
     * 可用于：迁移前备份、迁移后刷新缓存、发送通知等。
     * 注意：Flyway 9.x 中 Callback 接口含 canHandleInTransaction 与 getCallbackName 方法。
     */
    private static void demoCallbacks() {
        System.out.println("--- 4. Callback 生命周期钩子 ---");
        String url = "jdbc:h2:mem:flyway-adv-callback;DB_CLOSE_DELAY=-1";

        org.flywaydb.core.api.callback.Callback beforeMigrate =
                new org.flywaydb.core.api.callback.Callback() {
                    @Override
                    public boolean supports(org.flywaydb.core.api.callback.Event event,
                                            org.flywaydb.core.api.callback.Context context) {
                        return event == org.flywaydb.core.api.callback.Event.BEFORE_MIGRATE;
                    }

                    @Override
                    public void handle(org.flywaydb.core.api.callback.Event event,
                                       org.flywaydb.core.api.callback.Context context) {
                        System.out.println("  [Callback] BEFORE_MIGRATE：迁移前（此处可做全库备份）");
                    }

                    @Override
                    public boolean canHandleInTransaction(org.flywaydb.core.api.callback.Event event,
                                                          org.flywaydb.core.api.callback.Context context) {
                        return true;
                    }

                    @Override
                    public String getCallbackName() {
                        return "BeforeMigrateBackup";
                    }
                };

        org.flywaydb.core.api.callback.Callback afterMigrate =
                new org.flywaydb.core.api.callback.Callback() {
                    @Override
                    public boolean supports(org.flywaydb.core.api.callback.Event event,
                                            org.flywaydb.core.api.callback.Context context) {
                        return event == org.flywaydb.core.api.callback.Event.AFTER_MIGRATE;
                    }

                    @Override
                    public void handle(org.flywaydb.core.api.callback.Event event,
                                       org.flywaydb.core.api.callback.Context context) {
                        System.out.println("  [Callback] AFTER_MIGRATE：迁移后（此处可刷新缓存/发通知）");
                    }

                    @Override
                    public boolean canHandleInTransaction(org.flywaydb.core.api.callback.Event event,
                                                          org.flywaydb.core.api.callback.Context context) {
                        return true;
                    }

                    @Override
                    public String getCallbackName() {
                        return "AfterMigrateNotify";
                    }
                };

        Flyway flyway = configure(url)
                .callbacks(beforeMigrate, afterMigrate)
                .load();

        flyway.migrate();
        System.out.println("  带回调的迁移完成 ✓\n");
    }

    /**
     * 演示 5：常用配置项
     */
    private static void demoConfigOptions() {
        System.out.println("--- 5. 常用配置项 ---");
        String url = "jdbc:h2:mem:flyway-adv-config;DB_CLOSE_DELAY=-1";

        Flyway flyway = configure(url)
                // outOfOrder=true：允许"补插"低版本脚本（多人并行开发时慎用）
                .outOfOrder(false)
                // table：自定义 history 表名（默认 flyway_schema_history）
                .table("flyway_schema_history")
                // group=true：同一迁移批次失败时是否整体处理（依赖数据库事务支持）
                .group(false)
                // mixed=true：是否允许同一批次混合 pending/missing 状态
                .mixed(false)
                .load();

        flyway.migrate();

        System.out.println("  outOfOrder=false       禁止乱序执行（推荐，保证顺序确定性）");
        System.out.println("  table                  自定义 history 表名");
        System.out.println("  baselineOnMigrate=true 空库以外的非空库自动打基线（老项目推荐）");
        System.out.println("  cleanDisabled=true     生产环境必须禁用 clean！（防误删全库）");
        System.out.println("  validateOnMigrate=true 每次迁移前自动校验（默认，勿关闭）\n");
    }

    /** 统一的 Flyway 配置（每个演示使用独立的内存库 URL） */
    private static FluentConfiguration configure(String url) {
        return Flyway.configure()
                .dataSource(url, USER, PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(true); // 安全起见，演示代码也禁用 clean
    }
}
