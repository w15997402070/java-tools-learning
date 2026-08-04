package com.example.druid;

import com.alibaba.druid.filter.FilterAdapter;
import com.alibaba.druid.filter.config.ConfigFilter;
import com.alibaba.druid.filter.config.ConfigTools;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.proxy.jdbc.StatementProxy;
import com.alibaba.druid.stat.JdbcSqlStat;
import com.alibaba.druid.wall.WallCheckResult;
import com.alibaba.druid.wall.WallFilter;
import com.alibaba.druid.wall.WallProvider;
import com.alibaba.druid.wall.spi.DB2WallProvider;
import com.alibaba.druid.wall.spi.MySqlWallProvider;
import com.alibaba.druid.wall.spi.OracleWallProvider;
import com.alibaba.druid.wall.spi.PGWallProvider;
import com.alibaba.druid.wall.spi.SQLServerWallProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * Druid 进阶演示
 *
 * <p>演示内容：
 * <ul>
 *   <li>1. StatFilter 慢 SQL 监控 + SQL 合并统计 + 日志输出</li>
 *   <li>2. WallFilter SQL 注入防护（MySQL/Oracle/PostgreSQL/SQLServer/DB2 方言）</li>
 *   <li>3. ConfigFilter 数据库密码加密（公私钥 + ConfigTools）</li>
 *   <li>4. 自定义过滤器 SPI 扩展（继承 FilterAdapter）</li>
 *   <li>5. removeAbandoned 连接泄漏检测</li>
 * </ul>
 *
 * <p>这是企业级生产最常用的 Druid 高阶特性。
 *
 * @author example
 * @since 2026-08-04
 */
public class DruidAdvancedDemo {

    private static final String H2_URL =
            "jdbc:h2:mem:druid_adv;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void main(String[] args) throws Exception {
        System.out.println("========== Demo 1: StatFilter 慢 SQL 监控 ==========");
        demo1SlowSqlMonitor();

        System.out.println("\n========== Demo 2: WallFilter SQL 注入防护 ==========");
        demo2WallFilter();

        System.out.println("\n========== Demo 3: ConfigFilter 数据库密码加密 ==========");
        demo3ConfigFilterEncryption();

        System.out.println("\n========== Demo 4: 自定义过滤器（继承 FilterAdapter） ==========");
        demo4CustomFilter();

        System.out.println("\n========== Demo 5: 连接泄漏检测 removeAbandoned ==========");
        demo5RemoveAbandoned();
    }

    /**
     * Demo 1: StatFilter 慢 SQL 监控
     *
     * <p>StatFilter 提供：
     * <ul>
     *   <li>SQL 执行总次数 / 总耗时 / 最大并发 / TPS</li>
     *   <li>慢 SQL 阈值告警（默认 3000ms，生产建议 200-500ms）</li>
     *   <li>SQL 合并统计（normalize 后归并）</li>
     * </ul>
     */
    private static void demo1SlowSqlMonitor() throws Exception {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(H2_URL);
        ds.setUsername(USER);
        ds.setPassword(PASSWORD);
        ds.setName("slow-sql-ds");
        ds.setInitialSize(1);
        ds.setMinIdle(1);
        ds.setMaxActive(3);
        ds.setMaxWait(1000);
        ds.setValidationQuery("SELECT 1");

        // 自定义 StatFilter：阈值 1ms、合并统计、详细日志
        StatFilter statFilter = new StatFilter();
        statFilter.setSlowSqlMillis(1);
        statFilter.setLogSlowSql(true);
        statFilter.setMergeSql(true);     // 相似 SQL 合并统计
        ds.getProxyFilters().add(statFilter);

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS t_order (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, amount DECIMAL(10,2))");

            // 批量执行同一类 SQL（参数不同但模板相同，会被合并）
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO t_order(amount) VALUES (?)")) {
                for (int i = 0; i < 5; i++) {
                    ps.setBigDecimal(1, new java.math.BigDecimal(i * 100));
                    ps.executeUpdate();
                }
            }

            // 模拟一次"慢"查询
            try {
                stmt.execute("CALL SLEEP(20)");
            } catch (Exception ignore) {
                // H2 不支持 SLEEP 时忽略
            }
        }

        // 通过 dataSource.getDataSourceStat() 读取统计信息
        long totalExecute = 0, totalMillis = 0;
        for (JdbcSqlStat sqlStat : ds.getDataSourceStat().getSqlStatMap().values()) {
            totalExecute += sqlStat.getExecuteCount();
            totalMillis += sqlStat.getExecuteMillisTotal();
        }
        System.out.println("[StatFilter] 数据源总执行: " + totalExecute);
        System.out.println("[StatFilter] 数据源总耗时(ms): " + totalMillis);

        System.out.println("[StatFilter] 各类 SQL 统计：");
        ds.getDataSourceStat().getSqlStatMap().forEach((sql, stat) -> {
            System.out.printf("  SQL=%s | 执行 %d 次, 总耗时 %d ms, 最大 %d ms%n",
                    sql.substring(0, Math.min(60, sql.length())),
                    stat.getExecuteCount(), stat.getExecuteMillisTotal(),
                    stat.getExecuteMillisMax());
        });

        ds.close();
    }

    /**
     * Demo 2: WallFilter SQL 注入防护
     *
     * <p>WallFilter 通过 SQL 解析拦截危险语句：
     * <ul>
     *   <li>禁止多语句执行（防止 ;DROP TABLE）</li>
     *   <li>禁止危险的 SQL 函数（SLEEP、BENCHMARK、load_file 等）</li>
     *   <li>禁止 MySQL 注释符 # --</li>
     *   <li>禁止 union select 等注入关键字</li>
     * </ul>
     *
     * <p>不同数据库选择不同 WallProvider：MySQL/Oracle/PostgreSQL/SQLServer/DB2。
     */
    private static void demo2WallFilter() {
        // 不同数据库对应不同的 WallProvider（Druid 1.2.24 实际类名）
        List<WallProvider> providers = Arrays.asList(
                new MySqlWallProvider(),
                new OracleWallProvider(),
                new PGWallProvider(),
                new SQLServerWallProvider(),
                new DB2WallProvider()
        );
        System.out.println("[WallFilter] 支持的 WallProvider：");
        providers.forEach(p -> System.out.println("  - " + p.getClass().getSimpleName()));

        // 演示 MySQL WallProvider 拦截危险 SQL
        WallProvider provider = new MySqlWallProvider();

        String[] dangerous = new String[]{
                "SELECT * FROM users WHERE id = 1 OR 1=1",          // 经典 OR 注入
                "SELECT * FROM users; DROP TABLE users",            // 多语句
                "SELECT SLEEP(10) FROM dual",                      // 慢函数
                "SELECT * FROM users INTO OUTFILE '/tmp/x.txt'",   // 文件读写
                "SELECT load_file('/etc/passwd')",                 // 文件读取
                "SELECT * FROM users -- 注释绕过",                 // 注释
                "SELECT 1 /*! UNION SELECT 2 */"                  // MySQL 特有注释绕过
        };

        for (String sql : dangerous) {
            WallCheckResult result = provider.check(sql);
            if (result.getViolations().isEmpty()) {
                System.out.println("[WallFilter] ✓ 允许: " + sql);
            } else {
                String reason = result.getViolations().get(0).getMessage();
                System.out.println("[WallFilter] ✗ 拦截: " + sql);
                System.out.println("           原因: " + (reason == null ? "violation" :
                        reason.split("\n")[0]));
            }
        }

        // 构造一个 WallFilter 集成到 Druid
        WallFilter wallFilter = new WallFilter();
        wallFilter.setDbType("mysql");
        wallFilter.setThrowException(true);  // 拦截时抛异常
        System.out.println("[WallFilter] WallFilter 已就绪: dbType="
                + wallFilter.getDbType() + ", throwException=" + wallFilter.isThrowException());
    }

    /**
     * Demo 3: ConfigFilter 数据库密码加密
     *
     * <p>生产环境数据库密码不能明文写在配置文件中。ConfigFilter 配合 ConfigTools
     * 实现 RSA 加密：配置文件写密文，运行期解密。
     *
     * <p>注意：密钥库（publicKey / privateKey）必须妥善保管，建议放在
     * 配置中心或受控目录下，不要打进 jar 包。
     */
    private static void demo3ConfigFilterEncryption() throws Exception {
        // 1. 生成 RSA 密钥对（实际项目只在首次部署时执行一次）
        String plainPassword = "my-secret-db-password";
        String[] keyPair = ConfigTools.genKeyPair(512);
        String publicKey = keyPair[0];
        String privateKey = keyPair[1];

        // 2. 用公钥加密密码
        String encryptedPassword = ConfigTools.encrypt(publicKey, plainPassword);
        System.out.println("[ConfigFilter] 原文密码: " + plainPassword);
        System.out.println("[ConfigFilter] 公钥(写入配置): " + publicKey);
        System.out.println("[ConfigFilter] 加密密码(写入配置): " + encryptedPassword);

        // 3. 验证：私钥能解出原文
        String decrypted = ConfigTools.decrypt(privateKey, encryptedPassword);
        System.out.println("[ConfigFilter] 私钥解密: " + decrypted + "  ← 应等于原文");

        // 4. 模拟从配置加载（生产：写入 jdbc.properties）
        // jdbc.url=jdbc:mysql://...
        // jdbc.username=root
        // jdbc.password=加密后的密文
        // jdbc.publicKey=公钥
        // filters=config

        // 5. ConfigFilter 集成
        ConfigFilter configFilter = new ConfigFilter();
        System.out.println("[ConfigFilter] 过滤器已就绪: " + configFilter.getClass().getSimpleName());
    }

    /**
     * Demo 4: 自定义过滤器（继承 FilterAdapter）
     *
     * <p>实际扩展点：继承 {@link FilterAdapter}，重写 statement_executeUpdate /
     * statement_executeQuery 等生命周期方法，可注入业务逻辑（审计、性能监控、链路追踪等）。
     */
    private static void demo4CustomFilter() {
        // 自定义过滤器：打印每条执行的 SQL（演示用）
        FilterAdapter customFilter = new FilterAdapter() {
            @Override
            public int statement_executeUpdate(
                    com.alibaba.druid.filter.FilterChain chain,
                    StatementProxy statement, String sql) throws SQLException {
                System.out.println("  [审计] DML: " + sql);
                return super.statement_executeUpdate(chain, statement, sql);
            }

            @Override
            public com.alibaba.druid.proxy.jdbc.ResultSetProxy statement_executeQuery(
                    com.alibaba.druid.filter.FilterChain chain,
                    StatementProxy statement, String sql) throws SQLException {
                System.out.println("  [审计] SELECT: " + sql);
                return super.statement_executeQuery(chain, statement, sql);
            }
        };
        System.out.println("[自定义Filter] FilterAdapter 匿名子类: "
                + customFilter.getClass().getName());

        // 集成到 Druid
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(H2_URL);
        ds.setUsername(USER);
        ds.setPassword(PASSWORD);
        ds.setInitialSize(1);
        ds.setMinIdle(1);
        ds.setMaxActive(3);
        ds.setMaxWait(1000);
        ds.setValidationQuery("SELECT 1");
        ds.getProxyFilters().add(customFilter);

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS t_demo (id INT)");
            stmt.executeUpdate("INSERT INTO t_demo VALUES (1)");
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM t_demo")) {
                rs.next();
            }
        } catch (Exception e) {
            // 忽略表已存在等异常
        }
        ds.close();
    }

    /**
     * Demo 5: 连接泄漏检测
     *
     * <p>应用层忘记 close 连接会导致连接池耗尽。Druid 提供：
     * <ul>
     *   <li>removeAbandoned=true 启用泄漏检测</li>
     *   <li>removeAbandonedTimeout 超时阈值（秒）</li>
     *   <li>logAbandoned=true 打印泄漏堆栈（生产建议开启）</li>
     * </ul>
     */
    private static void demo5RemoveAbandoned() throws Exception {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(H2_URL);
        ds.setUsername(USER);
        ds.setPassword(PASSWORD);
        ds.setName("abandoned-ds");
        ds.setInitialSize(1);
        ds.setMinIdle(1);
        ds.setMaxActive(2);
        ds.setMaxWait(1000);
        ds.setValidationQuery("SELECT 1");

        // 关键配置：泄漏检测
        ds.setRemoveAbandoned(true);
        ds.setRemoveAbandonedTimeout(2);  // 2 秒未关闭视为泄漏
        ds.setLogAbandoned(true);

        // 正常获取并立即释放
        try (Connection c1 = ds.getConnection()) {
            System.out.println("[泄漏检测] 正常连接 1 已获取并 close");
        }

        // 拿到第二条不释放
        Connection c2 = ds.getConnection();
        System.out.println("[泄漏检测] 拿到连接 2 但故意不 close，等 2.5 秒...");
        Thread.sleep(2500);

        // 拿第 3 条，会触发 Druid 回收泄漏的 c2
        try (Connection c3 = ds.getConnection()) {
            System.out.println("[泄漏检测] 拿到连接 3 (此时 Druid 应已回收泄漏的 c2)");
        } catch (SQLException e) {
            System.out.println("[泄漏检测] 获取连接失败: " + e.getMessage());
        }

        // 主动 close c2
        try { c2.close(); } catch (Exception ignore) {}
        ds.close();
    }
}
