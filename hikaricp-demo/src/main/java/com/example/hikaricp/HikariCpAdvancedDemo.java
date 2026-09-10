package com.example.hikaricp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * HikariCP 进阶演示：连接池高级参数、连接泄漏检测、JMX 指标、连接验证与超时控制。
 *
 * <p>核心知识点：</p>
 * <ul>
 *   <li>leakDetectionThreshold：当连接从池中取出超过该时间未归还时打印泄漏堆栈</li>
 *   <li>connectionTimeout / idleTimeout / maxLifetime：控制连接获取、空闲回收与最大生命周期</li>
 *   <li>registerMbeans：开启 JMX 后可通过 HikariPoolMXBean 读取实时连接池状态</li>
 *   <li>MetricsTrackerFactory：自定义指标采集，适合对接 Prometheus / Micrometer</li>
 * </ul>
 */
public class HikariCpAdvancedDemo {

    public static void main(String[] args) throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:hikaricp_advanced;DB_CLOSE_DELAY=-1;MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
        config.setPoolName("HikariAdvancedPool");

        // 连接池容量参数
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);

        // 连接获取超时：默认 30 秒，演示设为 5 秒以便快速失败
        config.setConnectionTimeout(5000L);
        // 连接空闲超时：空闲连接超过该时间会被回收（minimumIdle 会保证最小连接数）
        config.setIdleTimeout(10000L);
        // 连接最大生命周期：防止数据库端连接被防火墙 / 超时策略断开
        config.setMaxLifetime(30000L);
        // 连接测试查询（H2 支持，MySQL/PostgreSQL 推荐用）
        config.setConnectionTestQuery("SELECT 1");

        // 连接泄漏检测阈值：连接借出超过 2 秒未归还打印堆栈
        config.setLeakDetectionThreshold(2000L);

        // 开启 JMX MBean 注册
        config.setRegisterMbeans(true);

        // 自定义指标采集器（打印关键指标）
        config.setMetricsTrackerFactory(new ConsoleMetricsTrackerFactory());

        HikariDataSource dataSource = new HikariDataSource(config);
        System.out.println("[Advanced] 连接池已启动: " + dataSource.getPoolName());

        initSchema(dataSource);

        // 演示连接获取与 JMX 指标读取
        printPoolStats(dataSource);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM orders")) {
            if (rs.next()) {
                System.out.println("[Advanced] 订单数量: " + rs.getInt("cnt"));
            }
        }
        printPoolStats(dataSource);

        // 模拟连接泄漏：借用连接不关闭，超过 leakDetectionThreshold 会打印泄漏警告
        simulateLeak(dataSource);

        // 等待泄漏检测打印后关闭
        Thread.sleep(2500);
        printPoolStats(dataSource);

        dataSource.close();
        System.out.println("[Advanced] 连接池已关闭");
    }

    /**
     * 初始化订单表并写入示例数据。
     */
    private static void initSchema(HikariDataSource dataSource) throws SQLException {
        String createTable = "CREATE TABLE IF NOT EXISTS orders ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "order_no VARCHAR(50) NOT NULL,"
                + "amount DECIMAL(10,2)"
                + ")";
        String insert = "INSERT INTO orders (order_no, amount) VALUES ('A001', 199.50), ('A002', 299.00)";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            stmt.execute("DELETE FROM orders");
            stmt.execute(insert);
            System.out.println("[Advanced] 表 orders 初始化完成");
        }
    }

    /**
     * 通过 JMX 读取并打印连接池实时状态。
     */
    private static void printPoolStats(HikariDataSource dataSource) {
        HikariPoolMXBean mxBean = dataSource.getHikariPoolMXBean();
        if (mxBean != null) {
            System.out.println("[Advanced] 连接池状态 -> "
                    + "active=" + mxBean.getActiveConnections()
                    + ", idle=" + mxBean.getIdleConnections()
                    + ", total=" + mxBean.getTotalConnections()
                    + ", waiting=" + mxBean.getThreadsAwaitingConnection());
        }
    }

    /**
     * 模拟连接泄漏：获取 Connection 后不关闭。
     * 当借用时间超过 leakDetectionThreshold 时，HikariCP 会记录泄漏堆栈。
     */
    private static void simulateLeak(HikariDataSource dataSource) throws SQLException, InterruptedException {
        Connection leaked = dataSource.getConnection();
        Statement stmt = leaked.createStatement();
        stmt.executeQuery("SELECT 1");
        System.out.println("[Advanced] 模拟连接泄漏：已获取连接但不关闭，等待泄漏检测...");
        // 故意持有连接超过 2 秒，触发 leakDetectionThreshold
        Thread.sleep(2300);
        // 最后归还连接，避免影响 close
        stmt.close();
        leaked.close();
        System.out.println("[Advanced] 连接已归还");
    }

    /**
     * 控制台指标采集器，将连接池关键指标打印到标准输出。
     */
    static class ConsoleMetricsTrackerFactory implements MetricsTrackerFactory {

        @Override
        public IMetricsTracker create(String poolName, PoolStats poolStats) {
            return new IMetricsTracker() {
                @Override
                public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos) {
                    // 连接获取耗时（纳秒）
                }

                @Override
                public void recordConnectionUsageMillis(long elapsedBorrowedMillis) {
                    System.out.println("[Advanced-Metrics] 连接使用时长: " + elapsedBorrowedMillis + " ms, "
                            + "totalConnections=" + poolStats.getTotalConnections()
                            + ", active=" + poolStats.getActiveConnections()
                            + ", idle=" + poolStats.getIdleConnections()
                            + ", pendingThreads=" + poolStats.getPendingThreads());
                }

                @Override
                public void recordConnectionTimeout() {
                    System.err.println("[Advanced-Metrics] 连接获取超时！");
                }

                @Override
                public void recordConnectionCreatedMillis(long connectionCreatedMillis) {
                    System.out.println("[Advanced-Metrics] 新连接创建耗时: " + connectionCreatedMillis + " ms");
                }

                @Override
                public void close() {
                    System.out.println("[Advanced-Metrics] 指标采集器已关闭");
                }
            };
        }
    }
}
