package com.example.druid;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.FilterAdapter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.stat.JdbcDataSourceStat;
import com.alibaba.druid.stat.JdbcSqlStat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

/**
 * Druid 基础演示
 *
 * <p>演示内容：
 * <ul>
 *   <li>1. 通过 {@link DruidDataSourceFactory} 从 Properties 加载数据源（生产推荐方式）</li>
 *   <li>2. 通过 {@link DruidDataSource} 直接 setter 方式构建（开发调试方便）</li>
 *   <li>3. 基础 SQL 执行（DDL/DML/SELECT）</li>
 *   <li>4. 连接池核心参数（initialSize / minIdle / maxActive / poolingCount）</li>
 *   <li>5. 集成 StatFilter 慢 SQL 监控 + 通过 dataSource.getDataSourceStat() 读取统计</li>
 * </ul>
 *
 * <p>运行方式：{@code mvn package} 后 {@code java -jar target/druid-demo-1.0-SNAPSHOT.jar}
 * 或在 IDE 中直接运行 main 方法。
 *
 * @author example
 * @since 2026-08-04
 */
public class DruidBasicDemo {

    /** 内存 H2 数据库 JDBC URL（演示用，无需任何外部依赖） */
    private static final String H2_URL =
            "jdbc:h2:mem:druid_basic;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void main(String[] args) throws Exception {
        System.out.println("========== Demo 1: 工厂方式创建 Druid 数据源 ==========");
        demo1FactoryWay();

        System.out.println("\n========== Demo 2: Setter 方式创建 Druid 数据源 ==========");
        demo2SetterWay();

        System.out.println("\n========== Demo 3: 基础 SQL 操作（DDL + DML + SELECT） ==========");
        demo3BasicSql();

        System.out.println("\n========== Demo 4: 连接池核心参数（initialSize/minIdle/maxActive/poolingCount） ==========");
        demo4PoolParameters();

        System.out.println("\n========== Demo 5: 集成 StatFilter 慢 SQL 监控 ==========");
        demo5StatFilter();
    }

    /**
     * Demo 1: 通过 DruidDataSourceFactory 从 properties 文件加载数据源
     *
     * <p>这是生产环境最推荐的方式，便于从外部配置文件解耦。
     */
    private static void demo1FactoryWay() throws Exception {
        Properties props = new Properties();
        props.setProperty("driverClassName", "org.h2.Driver");
        props.setProperty("url", H2_URL);
        props.setProperty("username", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("name", "factory-ds");
        props.setProperty("initialSize", "2");
        props.setProperty("minIdle", "2");
        props.setProperty("maxActive", "5");
        props.setProperty("maxWait", "1000");
        props.setProperty("validationQuery", "SELECT 1");
        // 开启 StatFilter 统计
        props.setProperty("filters", "stat");

        DruidDataSource ds = (DruidDataSource) DruidDataSourceFactory.createDataSource(props);

        System.out.println("[工厂创建] 数据源名称: " + ds.getName());
        System.out.println("[工厂创建] 初始连接数: " + ds.getInitialSize());
        System.out.println("[工厂创建] 最小空闲: " + ds.getMinIdle());
        System.out.println("[工厂创建] 最大活跃: " + ds.getMaxActive());

        // 获取并立即释放连接，触发初始化
        try (DruidPooledConnection conn = ds.getConnection()) {
            System.out.println("[工厂创建] 成功获取连接: " + conn);
        }
        ds.close();
    }

    /**
     * Demo 2: 直接使用 DruidDataSource 的 setter 构建数据源
     *
     * <p>适合开发调试或纯代码配置场景。生产环境仍推荐配置文件方式。
     */
    private static void demo2SetterWay() throws Exception {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(H2_URL);
        ds.setUsername(USER);
        ds.setPassword(PASSWORD);
        ds.setName("setter-demo-ds");
        ds.setInitialSize(1);
        ds.setMinIdle(1);
        ds.setMaxActive(3);

        System.out.println("[Setter创建] 数据源名称: " + ds.getName());

        // 模拟两次获取连接
        try (DruidPooledConnection c1 = ds.getConnection();
             DruidPooledConnection c2 = ds.getConnection()) {
            System.out.println("[Setter创建] 持有连接数: 2");
            System.out.println("[Setter创建] 活跃连接数: " + ds.getActiveCount());
            System.out.println("[Setter创建] 池中空闲连接数: " + ds.getPoolingCount());
        }
        System.out.println("[Setter创建] 关闭后池中空闲连接数: " + ds.getPoolingCount());
        ds.close();
    }

    /**
     * Demo 3: 在 Druid 管理的连接上执行 SQL（DDL + DML + SELECT）
     */
    private static void demo3BasicSql() throws Exception {
        DruidDataSource ds = buildSimpleDataSource("basic-sql-ds");

        try (Connection conn = ds.getConnection()) {
            // DDL
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS t_user");
                stmt.execute("CREATE TABLE t_user (id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "name VARCHAR(50), age INT)");
            }

            // DML - 插入
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO t_user(name, age) VALUES (?, ?)")) {
                ps.setString(1, "张三");
                ps.setInt(2, 28);
                ps.executeUpdate();
                ps.setString(1, "李四");
                ps.setInt(2, 32);
                ps.executeUpdate();
            }

            // SELECT
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, age FROM t_user WHERE age > ? ORDER BY age")) {
                ps.setInt(1, 25);
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("[SQL执行] age > 25 的用户：");
                    while (rs.next()) {
                        System.out.printf("  id=%d, name=%s, age=%d%n",
                                rs.getInt("id"), rs.getString("name"), rs.getInt("age"));
                    }
                }
            }
        } finally {
            ds.close();
        }
    }

    /**
     * Demo 4: 演示连接池参数对连接数的影响
     */
    private static void demo4PoolParameters() throws Exception {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(H2_URL);
        ds.setUsername(USER);
        ds.setPassword(PASSWORD);
        ds.setName("pool-params-ds");
        ds.setInitialSize(2);   // 启动时创建 2 条
        ds.setMinIdle(2);       // 最小保持 2 条空闲
        ds.setMaxActive(5);     // 最多 5 条活跃
        ds.setMaxWait(1000);    // 等不到连接时最多等 1 秒
        ds.setValidationQuery("SELECT 1");
        ds.setTestWhileIdle(true);

        // 触发初始化
        ds.init();
        System.out.println("[连接池参数] 启动后活跃=" + ds.getActiveCount()
                + ", 池中空闲=" + ds.getPoolingCount());

        // 并发获取 5 条
        Connection[] conns = new Connection[5];
        for (int i = 0; i < 5; i++) {
            conns[i] = ds.getConnection();
        }
        System.out.println("[连接池参数] 获取5条后: 活跃=" + ds.getActiveCount()
                + ", 等待线程=" + ds.getWaitThreadCount());

        // 尝试获取第 6 条（被 maxActive 限制，1 秒后抛 SQLException）
        try {
            Connection c6 = ds.getConnection();
            System.out.println("[连接池参数] 异常分支：拿到第6条 " + c6);
            c6.close();
        } catch (Exception e) {
            System.out.println("[连接池参数] 拿到第6条失败: " + e.getMessage());
        }

        // 释放 3 条，连接回到池中
        for (int i = 0; i < 3; i++) {
            conns[i].close();
        }
        System.out.println("[连接池参数] 释放3条后: 活跃=" + ds.getActiveCount()
                + ", 池中空闲=" + ds.getPoolingCount());

        for (int i = 3; i < 5; i++) {
            conns[i].close();
        }
        ds.close();
    }

    /**
     * Demo 5: 集成 StatFilter 慢 SQL 监控 + 通过 dataSource.getDataSourceStat() 读取统计
     *
     * <p>注意：getDataSourceStat() 需要 dataSource 已被 StatFilter 过滤后才有数据。
     */
    private static void demo5StatFilter() throws Exception {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(H2_URL);
        ds.setUsername(USER);
        ds.setPassword(PASSWORD);
        ds.setName("stat-demo-ds");
        ds.setInitialSize(1);
        ds.setMinIdle(1);
        ds.setMaxActive(3);
        ds.setMaxWait(1000);
        ds.setValidationQuery("SELECT 1");

        // 自定义 StatFilter：慢 SQL 阈值 1ms，启用慢 SQL 日志，启用 SQL 合并统计
        StatFilter statFilter = new StatFilter();
        statFilter.setSlowSqlMillis(1);
        statFilter.setLogSlowSql(true);
        statFilter.setMergeSql(true);
        ds.getProxyFilters().add(statFilter);

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS t_order (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, amount DECIMAL(10,2))");
            stmt.execute("INSERT INTO t_order(amount) VALUES (100)");
            stmt.execute("INSERT INTO t_order(amount) VALUES (200)");

            // 触发慢 SQL 日志
            try {
                stmt.execute("CALL SLEEP(20)");
            } catch (Exception ignore) {
                // H2 不支持 SLEEP 时忽略
            }
        }

        // 通过 dataSource.getDataSourceStat() 读取统计信息
        JdbcDataSourceStat dsStat = ds.getDataSourceStat();
        // 聚合所有 SQL 统计
        long totalExecute = 0, totalMillis = 0;
        for (JdbcSqlStat sqlStat : dsStat.getSqlStatMap().values()) {
            totalExecute += sqlStat.getExecuteCount();
            totalMillis += sqlStat.getExecuteMillisTotal();
        }
        System.out.println("[StatFilter] 累计执行次数: " + totalExecute);
        System.out.println("[StatFilter] 累计耗时(ms): " + totalMillis);
        System.out.println("[StatFilter] 连接数: 活跃=" + dsStat.getConnectionActiveCount()
                + ", 连接累计=" + dsStat.getConnectionStat().getConnectCount()
                + ", 关闭累计=" + dsStat.getConnectionStat().getCloseCount());
        System.out.println("[StatFilter] 各类 SQL 执行数：");
        Map<String, JdbcSqlStat> sqlMap = dsStat.getSqlStatMap();
        for (Map.Entry<String, JdbcSqlStat> e : sqlMap.entrySet()) {
            JdbcSqlStat sqlStat = e.getValue();
            System.out.printf("  - SQL: %s | 执行 %d 次, 总耗时 %d ms, 最大 %d ms%n",
                    e.getKey().substring(0, Math.min(60, e.getKey().length())),
                    sqlStat.getExecuteCount(),
                    sqlStat.getExecuteMillisTotal(),
                    sqlStat.getExecuteMillisMax());
        }

        ds.close();
    }

    /**
     * 构造一个最小可用的 DruidDataSource（公共方法）
     */
    private static DruidDataSource buildSimpleDataSource(String name) {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(H2_URL);
        ds.setUsername(USER);
        ds.setPassword(PASSWORD);
        ds.setName(name);
        ds.setInitialSize(1);
        ds.setMinIdle(1);
        ds.setMaxActive(3);
        ds.setMaxWait(1000);
        ds.setValidationQuery("SELECT 1");
        return ds;
    }
}
