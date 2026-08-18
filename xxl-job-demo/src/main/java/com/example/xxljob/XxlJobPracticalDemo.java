package com.example.xxljob;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * XXL-Job 实战场景演示
 *
 * 涵盖内容：
 *  1. 电商场景：订单超时自动取消（Cron 定时扫描）
 *  2. 运营场景：每日数据报表生成与邮件推送
 *  3. 运维场景：数据库历史数据清理（分片广播）
 *  4. 监控场景：健康检查与告警通知
 *  5. Spring Boot 集成配置说明（XxlJobSpringExecutor）
 *
 * 运行方式：
 *  本类展示真实业务场景下的 @XxlJob 处理器写法。
 *  需配合调度中心使用。独立运行 main 方法可验证业务逻辑。
 */
public class XxlJobPracticalDemo {

    private static final Logger log = LoggerFactory.getLogger(XxlJobPracticalDemo.class);

    // 模拟 Spring 注入的 Service（实际项目中用 @Autowired）
    private final OrderService orderService = new OrderService();
    private final ReportService reportService = new ReportService();
    private final CleanupService cleanupService = new CleanupService();
    private final HealthCheckService healthCheckService = new HealthCheckService();

    // -----------------------------------------------------------------------
    // 场景1：订单超时自动取消（每 5 分钟扫描一次）
    // -----------------------------------------------------------------------

    /**
     * 订单超时取消任务
     * <p>
     * 调度中心 Cron: 0 0/5 * * * ?（每 5 分钟一次）
     * 逻辑：查询超过 30 分钟未支付的订单，自动取消并释放库存
     */
    @XxlJob("orderTimeoutCancelJob")
    public void orderTimeoutCancelJob() {
        XxlJobHelper.log("=== 订单超时取消任务启动 ===");

        // 查询超时订单（模拟）
        List<String> timeoutOrders = orderService.queryTimeoutOrders(30);
        XxlJobHelper.log("查询到 {} 笔超时订单", timeoutOrders.size());

        int successCount = 0;
        int failCount = 0;

        for (String orderId : timeoutOrders) {
            try {
                orderService.cancel(orderId);
                XxlJobHelper.log("订单 {} 取消成功", orderId);
                successCount++;
            } catch (Exception e) {
                XxlJobHelper.log("订单 {} 取消失败: {}", orderId, e.getMessage());
                failCount++;
            }
        }

        XxlJobHelper.log("任务完成: 成功 {} 笔, 失败 {} 笔", successCount, failCount);

        // 如有失败，标记任务为失败（触发调度中心告警）
        if (failCount > 0) {
            XxlJobHelper.handleFail("订单取消有失败: 成功" + successCount + ", 失败" + failCount);
        }
    }

    // -----------------------------------------------------------------------
    // 场景2：每日数据报表生成（每天凌晨 1 点执行）
    // -----------------------------------------------------------------------

    /**
     * 每日销售报表生成
     * <p>
     * 调度中心 Cron: 0 0 1 * * ?（每天 01:00）
     * 参数格式: date=yesterday（默认取昨日数据）
     */
    @XxlJob("dailyReportJob")
    public void dailyReportJob() {
        XxlJobHelper.log("=== 每日报表生成任务启动 ===");

        String param = XxlJobHelper.getJobParam();
        String reportDate = (param != null && !param.isEmpty()) ? param : "yesterday";

        XxlJobHelper.log("报表日期参数: {}", reportDate);

        try {
            // 生成报表
            String reportPath = reportService.generateSalesReport(reportDate);
            XxlJobHelper.log("报表生成完成: {}", reportPath);

            // 发送邮件
            reportService.sendReportByEmail(reportPath, "manager@company.com");
            XxlJobHelper.log("报表邮件已发送");

        } catch (Exception e) {
            XxlJobHelper.log("报表生成失败: {}", e.getMessage());
            XxlJobHelper.handleFail("报表生成异常: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 场景3：历史数据清理（分片广播，每周日凌晨执行）
    // -----------------------------------------------------------------------

    /**
     * 历史日志清理（分片广播模式）
     * <p>
     * 调度中心 Cron: 0 0 2 ? * SUN（每周日 02:00）
     * 路由策略: SHARDING_BROADCAST（分片广播，所有节点并行）
     */
    @XxlJob("logCleanupJob")
    public void logCleanupJob() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("日志清理任务: 分片 {}/{}", shardIndex, shardTotal);

        // 分片处理：每个节点清理一部分日志表
        List<String> allTables = cleanupService.getAllLogTables();
        int myTableCount = 0;

        for (int i = 0; i < allTables.size(); i++) {
            // 分片路由：取模决定哪些表归当前节点清理
            if (i % shardTotal == shardIndex) {
                String table = allTables.get(i);
                try {
                    int deleted = cleanupService.cleanTable(table, 90); // 清理 90 天前的数据
                    XxlJobHelper.log("清理表 {}: 删除 {} 条", table, deleted);
                    myTableCount++;
                } catch (Exception e) {
                    XxlJobHelper.log("清理表 {} 失败: {}", table, e.getMessage());
                }
            }
        }

        XxlJobHelper.log("分片 {} 清理完成, 共处理 {} 张表", shardIndex, myTableCount);
    }

    // -----------------------------------------------------------------------
    // 场景4：健康检查与告警（每分钟执行）
    // -----------------------------------------------------------------------

    /**
     * 系统健康检查任务
     * <p>
     * 调度中心 Cron: 0 * * * * ?（每分钟一次）
     * 检查各项服务健康状态，异常时标记失败触发告警
     */
    @XxlJob("healthCheckJob")
    public void healthCheckJob() {
        XxlJobHelper.log("=== 健康检查任务启动 {} ===",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        List<String> issues = new ArrayList<>();

        // 检查数据库连接
        if (!healthCheckService.checkDatabase()) {
            issues.add("数据库连接异常");
        }

        // 检查 Redis 连接
        if (!healthCheckService.checkRedis()) {
            issues.add("Redis 连接异常");
        }

        // 检查磁盘空间
        double diskUsage = healthCheckService.getDiskUsage();
        if (diskUsage > 90) {
            issues.add(String.format("磁盘空间不足: %.1f%%", diskUsage));
        }

        if (issues.isEmpty()) {
            XxlJobHelper.log("所有检查项正常");
            XxlJobHelper.handleSuccess("健康检查通过");
        } else {
            String msg = "发现异常: " + String.join(", ", issues);
            XxlJobHelper.log(msg);
            // 标记失败 → 调度中心触发告警通知（邮件/钉钉/企微）
            XxlJobHelper.handleFail(msg);
        }
    }

    // -----------------------------------------------------------------------
    // Spring Boot 集成配置说明
    // -----------------------------------------------------------------------

    /**
     * Spring Boot 集成步骤（注释说明，实际在 Spring 配置类中使用）：
     *
     * <pre>{@code
     * // 1. application.yml 配置
     * xxl:
     *   job:
     *     admin:
     *       addresses: http://127.0.0.1:8080/xxl-job-admin  # 调度中心地址
     *     accessToken: xxl-job-token-123                     # 通信令牌（需与调度中心一致）
     *     executor:
     *       appname: my-executor          # 执行器名称
     *       address:                      # 地址（留空自动获取）
     *       ip:                           # IP（留空自动获取）
     *       port: 9999                    # 执行器端口
     *       logpath: /data/xxl-job/logs   # 日志路径
     *       logretentiondays: 30          # 日志保留天数
     *
     * // 2. 配置类
     * @Configuration
     * public class XxlJobConfig {
     *
     *     @Value("${xxl.job.admin.addresses}")
     *     private String adminAddresses;
     *
     *     @Value("${xxl.job.accessToken}")
     *     private String accessToken;
     *
     *     @Value("${xxl.job.executor.appname}")
     *     private String appname;
     *
     *     @Value("${xxl.job.executor.port}")
     *     private int port;
     *
     *     @Value("${xxl.job.executor.logpath}")
     *     private String logPath;
     *
     *     @Bean(initMethod = "start", destroyMethod = "destroy")
     *     public XxlJobSpringExecutor xxlJobExecutor() {
     *         XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
     *         executor.setAdminAddresses(adminAddresses);
     *         executor.setAccessToken(accessToken);
     *         executor.setAppname(appname);
     *         executor.setPort(port);
     *         executor.setLogPath(logPath);
     *         executor.setLogRetentionDays(30);
     *         return executor;
     *     }
     * }
     *
     * // 3. 任务处理器（@Component + @XxlJob）
     * @Component
     * public class MyJobHandler {
     *     @XxlJob("myJobHandler")
     *     public void myJobHandler() {
     *         XxlJobHelper.log("任务执行中...");
     *     }
     * }
     * }</pre>
     */

    // -----------------------------------------------------------------------
    // 模拟业务 Service（实际项目中由 Spring 注入）
    // -----------------------------------------------------------------------

    static class OrderService {
        List<String> queryTimeoutOrders(int minutes) {
            List<String> orders = new ArrayList<>();
            orders.add("ORDER-20240101-001");
            orders.add("ORDER-20240101-002");
            orders.add("ORDER-20240101-003");
            return orders;
        }

        void cancel(String orderId) {
            // 模拟取消订单逻辑
        }
    }

    static class ReportService {
        String generateSalesReport(String date) {
            sleep(1); // 模拟报表生成耗时
            return "/reports/sales_" + date + ".xlsx";
        }

        void sendReportByEmail(String path, String recipient) {
            // 模拟邮件发送
        }
    }

    static class CleanupService {
        List<String> getAllLogTables() {
            List<String> tables = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                tables.add("sys_log_" + i);
            }
            return tables;
        }

        int cleanTable(String table, int retainDays) {
            return (int) (Math.random() * 1000); // 模拟删除行数
        }
    }

    static class HealthCheckService {
        boolean checkDatabase() { return true; }
        boolean checkRedis() { return true; }
        double getDiskUsage() { return 65.5; }
    }

    // -----------------------------------------------------------------------
    // 独立运行入口
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        log.info("=== XXL-Job 实战场景演示 ===");

        XxlJobPracticalDemo demo = new XxlJobPracticalDemo();

        log.info("--- 场景1：订单超时取消 ---");
        demo.orderTimeoutCancelJob();

        log.info("--- 场景2：每日报表生成 ---");
        demo.dailyReportJob();

        log.info("--- 场景3：日志清理（分片广播） ---");
        demo.logCleanupJob();

        log.info("--- 场景4：健康检查 ---");
        demo.healthCheckJob();

        log.info("=== 实战演示结束 ===");
    }

    private static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
