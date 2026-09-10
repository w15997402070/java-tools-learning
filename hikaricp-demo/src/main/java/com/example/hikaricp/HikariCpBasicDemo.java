package com.example.hikaricp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * HikariCP 基础演示：配置连接池、获取连接、执行 CRUD。
 *
 * <p>核心知识点：</p>
 * <ul>
 *   <li>DataSource 是 JDBC 连接的标准抽象，HikariDataSource 是其高性能实现</li>
 *   <li>使用 try-with-resources 自动关闭 Connection / Statement / ResultSet</li>
 *   <li>连接池的核心参数：jdbcUrl、username、password、maximumPoolSize、minimumIdle</li>
 * </ul>
 */
public class HikariCpBasicDemo {

    public static void main(String[] args) throws Exception {
        // 1. 通过 HikariConfig 构建连接池配置
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:hikaricp_basic;DB_CLOSE_DELAY=-1;MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
        // 连接池大小：演示场景保持较小，生产环境一般按 CPU 核心数 * 2 + 有效磁盘数估算
        config.setMaximumPoolSize(5);
        // 最小空闲连接数，与 maximumPoolSize 相等时即为固定连接数池
        config.setMinimumIdle(2);
        // 连接池名称，方便日志与监控识别
        config.setPoolName("HikariBasicPool");

        // 2. 创建数据源（启动连接池）
        HikariDataSource dataSource = new HikariDataSource(config);
        System.out.println("[Basic] 连接池已启动: " + dataSource.getPoolName());

        // 3. 初始化表结构
        initSchema(dataSource);

        // 4. 插入数据
        insertUser(dataSource, "alice", "alice@example.com");
        insertUser(dataSource, "bob", "bob@example.com");

        // 5. 查询数据
        queryAllUsers(dataSource);

        // 6. 更新数据
        updateUserEmail(dataSource, "alice", "alice.new@example.com");

        // 7. 删除数据
        deleteUser(dataSource, "bob");

        // 8. 关闭连接池（重要：应用关闭时务必调用 close()）
        dataSource.close();
        System.out.println("[Basic] 连接池已关闭");
    }

    /**
     * 初始化用户表。
     */
    private static void initSchema(HikariDataSource dataSource) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "username VARCHAR(50) NOT NULL,"
                + "email VARCHAR(100)"
                + ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[Basic] 表 users 初始化完成");
        }
    }

    /**
     * 插入一条用户记录。
     */
    private static void insertUser(HikariDataSource dataSource, String username, String email) throws SQLException {
        String sql = "INSERT INTO users (username, email) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            int affected = ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("[Basic] 插入用户: id=" + id + ", username=" + username + ", affected=" + affected);
                }
            }
        }
    }

    /**
     * 查询所有用户。
     */
    private static void queryAllUsers(HikariDataSource dataSource) throws SQLException {
        String sql = "SELECT id, username, email FROM users ORDER BY id";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("[Basic] 当前用户列表:");
            while (rs.next()) {
                System.out.println("  id=" + rs.getInt("id")
                        + ", username=" + rs.getString("username")
                        + ", email=" + rs.getString("email"));
            }
        }
    }

    /**
     * 更新用户邮箱。
     */
    private static void updateUserEmail(HikariDataSource dataSource, String username, String newEmail) throws SQLException {
        String sql = "UPDATE users SET email = ? WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newEmail);
            ps.setString(2, username);
            int affected = ps.executeUpdate();
            System.out.println("[Basic] 更新用户 " + username + " 邮箱，影响行数: " + affected);
        }
    }

    /**
     * 删除用户。
     */
    private static void deleteUser(HikariDataSource dataSource, String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            int affected = ps.executeUpdate();
            System.out.println("[Basic] 删除用户 " + username + "，影响行数: " + affected);
        }
    }
}
