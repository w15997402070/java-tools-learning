package com.example.hikaricp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * HikariCP 实战演示：Spring Boot 风格配置、多数据源、批量写入、获取连接失败重试。
 *
 * <p>核心知识点：</p>
 * <ul>
 *   <li>Spring Boot 中 spring.datasource.hikari.* 最终都会映射到 HikariConfig</li>
 *   <li>多数据源场景下需要为每个数据源单独配置 HikariDataSource，并手动注入事务管理器</li>
 *   <li>批量插入使用 addBatch() / executeBatch()，配合 rewriteBatchedStatements=true 可显著提升 MySQL 写入性能</li>
 *   <li>高并发下连接获取可能失败，建议对关键操作做有限重试</li>
 * </ul>
 */
public class HikariCpPracticalDemo {

    public static void main(String[] args) throws Exception {
        // 1. 模拟 Spring Boot 默认单数据源配置
        HikariDataSource primaryDs = buildPrimaryDataSource();
        System.out.println("[Practical] 主数据源已启动: " + primaryDs.getPoolName());

        // 2. 模拟 Spring Boot 多数据源中的第二个数据源（例如报表库）
        HikariDataSource reportDs = buildReportDataSource();
        System.out.println("[Practical] 报表数据源已启动: " + reportDs.getPoolName());

        // 3. 初始化表
        initProductTable(primaryDs);
        initReportTable(reportDs);

        // 4. 批量写入商品数据
        List<Product> products = new ArrayList<Product>();
        products.add(new Product("P001", "iPhone", 5999.00));
        products.add(new Product("P002", "MacBook", 12999.00));
        products.add(new Product("P003", "AirPods", 1999.00));
        batchInsertProducts(primaryDs, products);

        // 5. 从主库读取，同步到报表库（跨库同步简化演示）
        List<Product> synced = queryAllProducts(primaryDs);
        batchInsertReportProducts(reportDs, synced);
        System.out.println("[Practical] 已同步 " + synced.size() + " 条商品到报表库");

        // 6. 演示获取连接失败时的重试机制
        executeWithRetry(primaryDs, "SELECT COUNT(*) AS cnt FROM products", 3, 500L);

        // 7. 关闭所有数据源
        primaryDs.close();
        reportDs.close();
        System.out.println("[Practical] 所有数据源已关闭");
    }

    /**
     * 构建 Spring Boot 风格的主数据源。
     */
    private static HikariDataSource buildPrimaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("PrimaryHikariPool");
        config.setJdbcUrl("jdbc:h2:mem:primary_db;DB_CLOSE_DELAY=-1;MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
        // 等价于 spring.datasource.hikari.maximum-pool-size=10
        config.setMaximumPoolSize(10);
        // 等价于 spring.datasource.hikari.minimum-idle=5
        config.setMinimumIdle(5);
        // 等价于 spring.datasource.hikari.connection-timeout=20000
        config.setConnectionTimeout(20000L);
        // 等价于 spring.datasource.hikari.idle-timeout=300000
        config.setIdleTimeout(300000L);
        // 等价于 spring.datasource.hikari.max-lifetime=1200000
        config.setMaxLifetime(1200000L);
        // 等价于 spring.datasource.hikari.pool-name=PrimaryHikariPool
        config.setPoolName("PrimaryHikariPool");
        // 连接测试语句（MySQL/PostgreSQL/H2 通用）
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }

    /**
     * 构建第二个数据源（报表库），通常用于只读查询。
     */
    private static HikariDataSource buildReportDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("ReportHikariPool");
        config.setJdbcUrl("jdbc:h2:mem:report_db;DB_CLOSE_DELAY=-1;MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
        // 报表库查询为主，连接数可相对较小
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000L);
        config.setIdleTimeout(60000L);
        config.setMaxLifetime(300000L);
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }

    /**
     * 初始化商品表。
     */
    private static void initProductTable(HikariDataSource dataSource) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS products ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "code VARCHAR(50) NOT NULL UNIQUE,"
                + "name VARCHAR(100),"
                + "price DECIMAL(10,2)"
                + ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[Practical] 主库 products 表初始化完成");
        }
    }

    /**
     * 初始化报表库商品表。
     */
    private static void initReportTable(HikariDataSource dataSource) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS report_products ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "code VARCHAR(50) NOT NULL,"
                + "name VARCHAR(100),"
                + "price DECIMAL(10,2)"
                + ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[Practical] 报表库 report_products 表初始化完成");
        }
    }

    /**
     * 批量插入商品到主库。
     */
    private static void batchInsertProducts(HikariDataSource dataSource, List<Product> products) throws SQLException {
        String sql = "INSERT INTO products (code, name, price) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Product product : products) {
                ps.setString(1, product.code);
                ps.setString(2, product.name);
                ps.setDouble(3, product.price);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            int total = 0;
            for (int r : results) {
                total += r;
            }
            System.out.println("[Practical] 批量插入商品，影响行数: " + total);
        }
    }

    /**
     * 查询所有商品。
     */
    private static List<Product> queryAllProducts(HikariDataSource dataSource) throws SQLException {
        List<Product> list = new ArrayList<Product>();
        String sql = "SELECT code, name, price FROM products ORDER BY id";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Product(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getDouble("price")));
            }
        }
        return list;
    }

    /**
     * 批量插入商品到报表库。
     */
    private static void batchInsertReportProducts(HikariDataSource dataSource, List<Product> products) throws SQLException {
        String sql = "INSERT INTO report_products (code, name, price) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Product product : products) {
                ps.setString(1, product.code);
                ps.setString(2, product.name);
                ps.setDouble(3, product.price);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * 带重试的 SQL 执行演示。
     *
     * @param dataSource 数据源
     * @param sql        要执行的查询 SQL
     * @param maxRetries 最大重试次数
     * @param retryDelay 重试间隔（毫秒）
     */
    private static void executeWithRetry(HikariDataSource dataSource, String sql,
                                          int maxRetries, long retryDelay) throws Exception {
        SQLException lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    System.out.println("[Practical] 重试执行查询成功（第 " + attempt + " 次），结果: " + rs.getInt(1));
                }
                return;
            } catch (SQLException e) {
                lastException = e;
                System.err.println("[Practical] 第 " + attempt + " 次执行失败: " + e.getMessage());
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelay);
                }
            }
        }
        throw lastException;
    }

    /**
     * 商品领域对象。
     */
    static class Product {
        String code;
        String name;
        double price;

        Product(String code, String name, double price) {
            this.code = code;
            this.name = name;
            this.price = price;
        }
    }
}
