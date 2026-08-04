package com.example.druid;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.wall.WallFilter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Druid 实战演示
 *
 * <p>演示内容：
 * <ul>
 *   <li>1. 多数据源配置（master / slave 读写分离示意）</li>
 *   <li>2. MyBatis + Druid 集成思路（配置示例）</li>
 *   <li>3. Spring Boot Starter 配置（application.yml 完整模板）</li>
 *   <li>4. Web 监控 StatViewServlet 配置（参数说明）</li>
 *   <li>5. 生产推荐配置模板（最大连接数 / 验证 / 过滤 / 泄漏检测）</li>
 * </ul>
 *
 * <p>本 Demo 用纯代码示例展示配置形态；Spring Boot 项目可直接复制到 application.yml。
 *
 * @author example
 * @since 2026-08-04
 */
public class DruidPracticalDemo {

    private static final String H2_URL_MASTER =
            "jdbc:h2:mem:druid_master;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    private static final String H2_URL_SLAVE =
            "jdbc:h2:mem:druid_slave;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void main(String[] args) throws Exception {
        System.out.println("========== Demo 1: 多数据源（master / slave 读写分离） ==========");
        demo1MultiDataSource();

        System.out.println("\n========== Demo 2: MyBatis + Druid 集成思路 ==========");
        demo2MybatisIntegration();

        System.out.println("\n========== Demo 3: Spring Boot Starter 配置示例 ==========");
        demo3SpringBootConfig();

        System.out.println("\n========== Demo 4: Web 监控 StatViewServlet 注册 ==========");
        demo4WebMonitor();

        System.out.println("\n========== Demo 5: 生产推荐配置模板 ==========");
        demo5ProductionConfig();
    }

    /**
     * Demo 1: 多数据源（master 写，slave 读）
     */
    private static void demo1MultiDataSource() throws Exception {
        // Master 数据源（写）
        DruidDataSource master = buildFromProperties("master-ds", H2_URL_MASTER);

        // Slave 数据源（读）
        DruidDataSource slave = buildFromProperties("slave-ds", H2_URL_SLAVE);

        // 模拟初始化
        master.init();
        slave.init();

        // Master 写
        try (Connection conn = master.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS t_user (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(50))");
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO t_user(name) VALUES (?)")) {
                ps.setString(1, "master-inserted");
                ps.executeUpdate();
            }
        }

        // Slave 读
        try (Connection conn = slave.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 AS result");
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.println("[多数据源] slave 查询: result=" + rs.getInt("result"));
            }
        }

        // 实际路由：业务层使用 AbstractRoutingDataSource + 注解/@DS 切换
        // 或借助 dynamic-datasource-spring-boot-starter
        System.out.println("[多数据源] 实际生产可用 dynamic-datasource-spring-boot-starter 一行注解切换");

        master.close();
        slave.close();
    }

    /**
     * Demo 2: MyBatis + Druid 集成思路（YAML 配置示例）
     */
    private static void demo2MybatisIntegration() {
        System.out.println("[MyBatis集成] 三种方式任选其一：\n");

        System.out.println("方式 A：Spring Boot application.yml");
        System.out.println("  spring:");
        System.out.println("    datasource:");
        System.out.println("      type: com.alibaba.druid.pool.DruidDataSource");
        System.out.println("      url: jdbc:mysql://host:3306/db");
        System.out.println("      driver-class-name: com.mysql.cj.jdbc.Driver");
        System.out.println("      username: root");
        System.out.println("      password: password");
        System.out.println("      druid:");
        System.out.println("        initial-size: 5");
        System.out.println("        min-idle: 5");
        System.out.println("        max-active: 20");
        System.out.println("        max-wait: 60000");
        System.out.println("        filters: stat,wall,config");
        System.out.println("        stat-view-servlet:");
        System.out.println("          enabled: true");
        System.out.println("          login-username: admin");
        System.out.println("          login-password: admin");
        System.out.println("          url-pattern: /druid/*");
        System.out.println();

        System.out.println("方式 B：Java Config（不依赖 Spring Boot 自动装配）");
        System.out.println("  @Configuration");
        System.out.println("  public class DataSourceConfig {");
        System.out.println("    @Bean");
        System.out.println("    public DataSource dataSource() {");
        System.out.println("        return new DruidDataSource(); // 通过 setter 或 properties 注入");
        System.out.println("    }");
        System.out.println("  }");
        System.out.println();

        System.out.println("方式 C：MyBatis-Plus 已内置 Druid 解析器，");
        System.out.println("  引入 mybatis-plus-boot-starter 后直接写 spring.datasource.* 即可");
    }

    /**
     * Demo 3: Spring Boot Starter 完整配置
     */
    private static void demo3SpringBootConfig() {
        System.out.println("[SpringBoot配置] 部署时需引入依赖：");
        System.out.println("    <dependency>");
        System.out.println("        <groupId>com.alibaba</groupId>");
        System.out.println("        <artifactId>druid-spring-boot-3-starter</artifactId>");
        System.out.println("        <version>1.2.24</version>");
        System.out.println("    </dependency>");
        System.out.println();
        System.out.println("（注意：druid-spring-boot-3-starter 用于 Spring Boot 3.x；");
        System.out.println(" Spring Boot 2.x 用 druid-spring-boot-starter）");
    }

    /**
     * Demo 4: Web 监控参数说明
     *
     * <p>生产环境务必：
     * <ul>
     *   <li>开启账号密码（不要空）</li>
     *   <li>配置 IP 白名单（allow）</li>
     *   <li>生产环境禁用 resetEnable</li>
     *   <li>将 /druid/* 加入网关或 Nginx 白名单</li>
     * </ul>
     */
    private static void demo4WebMonitor() {
        // StatViewServlet 参数
        Map<String, String> initParams = new HashMap<>();
        initParams.put("loginUsername", "admin");
        initParams.put("loginPassword", "admin");
        initParams.put("allow", "127.0.0.1,192.168.1.0/24");   // 白名单
        // initParams.put("deny", "192.168.1.100");             // 黑名单
        initParams.put("resetEnable", "false");                 // 禁止重置统计
        System.out.println("[Web监控] StatViewServlet 推荐参数: " + initParams);

        // WebStatFilter 参数
        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        filterParams.put("sessionStatEnable", "true");
        filterParams.put("sessionStatMaxCount", "1000");
        filterParams.put("principalSessionName", "USER_SESSION");
        System.out.println("[WebStatFilter] 排除静态资源 + 启用 Session 统计: " + filterParams);

        // Spring Boot 中可改为 application.yml：
        System.out.println();
        System.out.println("Spring Boot application.yml 配置：");
        System.out.println("  spring.datasource.druid.stat-view-servlet.enabled: true");
        System.out.println("  spring.datasource.druid.stat-view-servlet.url-pattern: /druid/*");
        System.out.println("  spring.datasource.druid.stat-view-servlet.login-username: admin");
        System.out.println("  spring.datasource.druid.stat-view-servlet.login-password: admin");
        System.out.println("  spring.datasource.druid.stat-view-servlet.allow: 127.0.0.1,10.0.0.0/8");
        System.out.println("  spring.datasource.druid.stat-view-servlet.reset-enable: false");
        System.out.println("  spring.datasource.druid.web-stat-filter.exclusions: " +
                "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
    }

    /**
     * Demo 5: 生产推荐配置模板
     */
    private static void demo5ProductionConfig() {
        System.out.println("[生产配置] application.yml 推荐写法：\n");
        System.out.println("# ===== Druid 数据源 =====");
        System.out.println("spring.datasource.type: com.alibaba.druid.pool.DruidDataSource");
        System.out.println("spring.datasource.url: jdbc:mysql://${DB_HOST:localhost}:3306/${DB_NAME:app}?useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8");
        System.out.println("spring.datasource.username: ${DB_USER:root}");
        System.out.println("spring.datasource.password: ${DB_PWD}");
        System.out.println("spring.datasource.driver-class-name: com.mysql.cj.jdbc.Driver");
        System.out.println();
        System.out.println("# ===== 连接池大小 =====");
        System.out.println("spring.datasource.druid.initial-size: 5");
        System.out.println("spring.datasource.druid.min-idle: 5");
        System.out.println("spring.datasource.druid.max-active: 20");
        System.out.println("spring.datasource.druid.max-wait: 60000");
        System.out.println();
        System.out.println("# ===== 性能检测 =====");
        System.out.println("spring.datasource.druid.validation-query: SELECT 1");
        System.out.println("spring.datasource.druid.test-while-idle: true");
        System.out.println("spring.datasource.druid.test-on-borrow: false");
        System.out.println("spring.datasource.druid.test-on-return: false");
        System.out.println("spring.datasource.druid.pool-prepared-statements: true");
        System.out.println("spring.datasource.druid.max-pool-prepared-statement-per-connection-size: 20");
        System.out.println();
        System.out.println("# ===== 过滤器 =====");
        System.out.println("spring.datasource.druid.filters: stat,wall,config");
        System.out.println("spring.datasource.druid.filter.stat.slow-sql-millis: 500");
        System.out.println("spring.datasource.druid.filter.stat.log-slow-sql: true");
        System.out.println("spring.datasource.druid.filter.wall.db-type: mysql");
        System.out.println("spring.datasource.druid.filter.wall.config.delete-allow: false");
        System.out.println("spring.datasource.druid.filter.wall.config.drop-table-allow: false");
        System.out.println("spring.datasource.druid.filter.config.enabled: true");
        System.out.println();
        System.out.println("# ===== Web 监控 =====");
        System.out.println("spring.datasource.druid.stat-view-servlet.enabled: true");
        System.out.println("spring.datasource.druid.stat-view-servlet.url-pattern: /druid/*");
        System.out.println("spring.datasource.druid.stat-view-servlet.login-username: ${DRUID_USER:admin}");
        System.out.println("spring.datasource.druid.stat-view-servlet.login-password: ${DRUID_PWD:ChangeMe!}");
        System.out.println("spring.datasource.druid.stat-view-servlet.allow: 127.0.0.1,10.0.0.0/8");
        System.out.println("spring.datasource.druid.stat-view-servlet.reset-enable: false");
        System.out.println();
        System.out.println("# ===== 泄漏检测 =====");
        System.out.println("spring.datasource.druid.remove-abandoned: true");
        System.out.println("spring.datasource.druid.remove-abandoned-timeout: 300");
        System.out.println("spring.datasource.druid.log-abandoned: true");
    }

    /**
     * 通用：通过 properties 构建 Druid 数据源
     */
    private static DruidDataSource buildFromProperties(String name, String url) throws Exception {
        Properties props = new Properties();
        props.setProperty("driverClassName", "org.h2.Driver");
        props.setProperty("url", url);
        props.setProperty("username", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("name", name);
        props.setProperty("initialSize", "1");
        props.setProperty("minIdle", "1");
        props.setProperty("maxActive", "3");
        props.setProperty("maxWait", "1000");
        props.setProperty("validationQuery", "SELECT 1");
        return (DruidDataSource) DruidDataSourceFactory.createDataSource(props);
    }
}
