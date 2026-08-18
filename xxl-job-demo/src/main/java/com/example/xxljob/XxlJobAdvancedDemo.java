package com.example.xxljob;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * XXL-Job 进阶演示
 *
 * 涵盖内容：
 *  1. 分片广播（ShardingBroadcast）—— 大数据量并行处理的标准模式
 *  2. 子任务链（ChildJob）—— 任务执行完成后触发子任务
 *  3. GLUE 模式（Java 代码在线编辑）—— 无需重新部署的灵活任务
 *  4. 路由策略说明 —— 调度中心如何选择执行器节点
 *
 * 运行方式：
 *  本类为注解演示，需配合调度中心使用。独立运行 main 方法可验证分片逻辑。
 */
public class XxlJobAdvancedDemo {

    private static final Logger log = LoggerFactory.getLogger(XxlJobAdvancedDemo.class);

    // -----------------------------------------------------------------------
    // 1. 分片广播（ShardingBroadcast）—— 大数据量并行处理
    // -----------------------------------------------------------------------

    /**
     * 分片广播任务
     * <p>
     * 调度中心配置路由策略为 "分片广播" 时，会同时向所有执行器节点发送请求，
     * 每个节点通过分片号（shardIndex）和总数（shardTotal）只处理一部分数据。
     * <p>
     * 典型场景：1000 万用户数据需要处理，3 台执行器各处理 1/3。
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() {
        // 获取分片信息（由调度中心注入）
        int shardIndex = XxlJobHelper.getShardIndex();   // 当前分片号（从 0 开始）
        int shardTotal = XxlJobHelper.getShardTotal();    // 总分片数

        XxlJobHelper.log("分片广播任务: 当前分片={}/{}, 节点处理中...", shardIndex, shardTotal);

        // 模拟分片处理 —— 每个节点只处理 ID % shardTotal == shardIndex 的数据
        int totalUsers = 1000; // 假设有 1000 条数据
        int myCount = 0;
        for (int i = 0; i < totalUsers; i++) {
            // 分片路由：取模决定哪些数据归当前节点处理
            if (i % shardTotal == shardIndex) {
                myCount++;
                // 实际场景：processUser(userId=i)
            }
        }

        XxlJobHelper.log("分片 {} 处理完成，共处理 {} 条数据", shardIndex, myCount);
        log.info("[shardingJobHandler] 分片 {}/{} 处理了 {} 条数据", shardIndex, shardTotal, myCount);
    }

    // -----------------------------------------------------------------------
    // 2. 子任务链（ChildJob）—— 任务完成后自动触发子任务
    // -----------------------------------------------------------------------

    /**
     * 父任务：数据采集
     * <p>
     * 在调度中心配置 "子任务"（ChildJobId），父任务执行成功后会自动触发子任务。
     * 也可以在代码中动态触发：XxlJobHelper.handleSuccess("子任务Key")
     */
    @XxlJob("dataCollectParentJob")
    public void dataCollectParentJob() {
        XxlJobHelper.log("父任务：数据采集开始...");

        // 模拟采集
        sleep(1);

        XxlJobHelper.log("父任务：数据采集完成，共采集 5000 条");

        // 父任务返回成功后，调度中心会根据配置自动触发子任务（dataProcessChildJob）
        XxlJobHelper.handleSuccess("数据采集完成，触发子任务");
    }

    /**
     * 子任务：数据处理（由父任务完成后自动触发）
     */
    @XxlJob("dataProcessChildJob")
    public void dataProcessChildJob() {
        XxlJobHelper.log("子任务：数据处理开始（由父任务触发）...");

        // 模拟处理
        sleep(2);

        XxlJobHelper.log("子任务：数据处理完成");
        log.info("[dataProcessChildJob] 子任务执行完毕");
    }

    // -----------------------------------------------------------------------
    // 3. GLUE 模式说明（Java 代码在线编辑）
    // -----------------------------------------------------------------------

    /**
     * GLUE（Java）模式说明
     * <p>
     * 与 Bean 模式不同，GLUE 模式下任务代码直接写在调度中心的 Web 控制台上，
     * 调度中心将代码片段发送给执行器动态编译执行，**无需重新部署应用**。
     * <p>
     * 以下是一个 GLUE 代码片段示例（直接粘贴到调度中心 GLUE IDE 中）：
     * <pre>{@code
     * public ReturnT<String> execute(String param) {
     *     // 可直接使用 Spring Bean（需通过 XxlJobExecutor.getApplicationContext()）
     *     XxlJobHelper.log("GLUE 任务执行，参数={}", param);
     *
     *     // 业务逻辑...
     *
     *     return ReturnT.SUCCESS;
     * }
     * }</pre>
     * <p>
     * 适用场景：临时需求、紧急修复、不便于重新发版的任务。
     * 注意：GLUE 代码缺乏版本管理，不建议长期使用，稳定后应转为 Bean 模式。
     */

    // -----------------------------------------------------------------------
    // 4. 路由策略对照表
    // -----------------------------------------------------------------------

    /**
     * XXL-Job 路由策略说明
     * <p>
     * 当执行器集群部署时，调度中心需要选择一个节点执行任务。
     * 通过 "路由策略" 决定如何选择：
     * <p>
     * | 策略 | 说明 |
     * |------|------|
     * | FIRST（第一个）    | 固定选择第一个节点 |
     * | LAST（最后一个）   | 固定选择最后一个节点 |
     * | ROUND（轮询）      | 轮流选择每个节点（默认常用） |
     * | RANDOM（随机）     | 随机选择一个节点 |
     * | CONSISTENT_HASH（一致性哈希） | 同一任务固定路由到同一节点 |
     * | LEAST_FREQUENTLY_USED（最不经常使用）| 选择使用频率最低的节点 |
     * | LEAST_RECENTLY_USED（最近最久未使用）| 选择最久未使用的节点 |
     * | FAILOVER（故障转移）| 依次探测，跳过不可用节点 |
     * | BUSYOVER（忙碌转移）| 依次探测，跳过忙碌节点 |
     * | SHARDING_BROADCAST（分片广播）| 广播到所有节点并行处理 |
     */

    // -----------------------------------------------------------------------
    // 5. 模拟独立运行
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        log.info("=== XXL-Job 进阶演示 ===");

        XxlJobAdvancedDemo demo = new XxlJobAdvancedDemo();

        // 模拟分片广播（独立运行时 shardIndex=0, shardTotal=1）
        log.info("--- 演示1：分片广播 ---");
        demo.shardingJobHandler();

        // 模拟父子任务链
        log.info("--- 演示2：父任务 ---");
        demo.dataCollectParentJob();
        log.info("--- 演示2：子任务（自动触发） ---");
        demo.dataProcessChildJob();

        log.info("=== 进阶演示结束 ===");
    }

    /** 模拟耗时操作 */
    private static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
