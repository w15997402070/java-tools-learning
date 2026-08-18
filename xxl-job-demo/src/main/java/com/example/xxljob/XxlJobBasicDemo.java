package com.example.xxljob;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * XXL-Job 基础演示
 *
 * 涵盖内容：
 *  1. @XxlJob 注解定义任务处理器（最核心的 API）
 *  2. 获取调度参数（XxlJobHelper.getJobParam）
 *  3. 写入执行日志（XxlJobHelper.log — 日志会回传调度中心）
 *  4. 标记执行结果（成功 / 失败）
 *
 * 说明：
 *  XXL-Job 采用 "调度中心 + 执行器" 架构。本类展示的是 **执行器端**
 *  最核心的用法 —— 用 @XxlJob 标注一个方法，它就成为一个可被调度中心
 *  远程触发的任务处理器。方法签名要求：public、返回值 void 或 ReturnT。
 *
 * 运行方式：
 *  本类为注解演示，需要配合调度中心使用。可单独编译验证 API 正确性。
 *  独立运行 main 方法可验证处理器逻辑（模拟调度触发）。
 */
public class XxlJobBasicDemo {

    private static final Logger log = LoggerFactory.getLogger(XxlJobBasicDemo.class);

    // -----------------------------------------------------------------------
    // 1. 最简单的任务：无参数，打印日志
    // -----------------------------------------------------------------------

    /**
     * 简单任务示例（Bean 模式）
     * <p>
     * 在调度中心配置：JobHandler = "demoJobHandler"
     * 触发时，调度中心会通过 HTTP 调用执行器，执行此方法。
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() {
        // XxlJobHelper.log 写入的日志会随执行结果一起回传调度中心，在 Web 控制台可见
        XxlJobHelper.log("XXL-Job 基础任务执行中...");

        log.info("[demoJobHandler] 简单任务执行完毕");
    }

    // -----------------------------------------------------------------------
    // 2. 带参数的任务：通过 jobParam 接收调度中心传入的参数
    // -----------------------------------------------------------------------

    /**
     * 参数传递示例
     * <p>
     * 在调度中心配置任务时，可在 "任务参数" 一栏填入字符串，
     * 执行器通过 XxlJobHelper.getJobParam() 获取。
     */
    @XxlJob("paramJobHandler")
    public void paramJobHandler() {
        // 获取调度中心传来的参数（纯字符串，需自行解析）
        String param = XxlJobHelper.getJobParam();
        XxlJobHelper.log("接收到的任务参数: {}", param);

        if (param == null || param.isEmpty()) {
            XxlJobHelper.log("未收到参数，使用默认值");
            param = "default";
        }

        // 解析参数示例：逗号分隔
        String[] items = param.split(",");
        XxlJobHelper.log("参数拆分结果: {}", Arrays.toString(items));

        // 模拟业务处理
        for (String item : items) {
            XxlJobHelper.log("处理项: {}", item.trim());
        }

        log.info("[paramJobHandler] 参数任务执行完毕，参数={}", param);
    }

    // -----------------------------------------------------------------------
    // 3. 标记执行结果：成功与失败
    // -----------------------------------------------------------------------

    /**
     * 执行结果控制示例
     * <p>
     * XXL-Job 默认认为方法正常返回即成功，抛异常即失败。
     * 也可以主动调用 XxlJobHelper 标记结果：
     *  - handleSuccess()：标记成功
     *  - handleFail()：标记失败（可在 Web 控制台看到失败原因）
     */
    @XxlJob("resultJobHandler")
    public void resultJobHandler() {
        XxlJobHelper.log("结果控制任务执行中...");

        String param = XxlJobHelper.getJobParam();
        try {
            if ("fail".equalsIgnoreCase(param)) {
                // 模拟业务校验失败
                XxlJobHelper.log("业务校验未通过：参数为 fail");
                // 主动标记失败，消息会展示在调度中心
                XxlJobHelper.handleFail("业务校验失败：参数不允许为 fail");
                return;
            }

            // 模拟业务处理成功
            XxlJobHelper.log("业务处理成功");
            // 主动标记成功（可选，默认即成功）
            XxlJobHelper.handleSuccess("业务处理完成，数据已写入");

        } catch (Exception e) {
            // 异常情况下标记失败
            XxlJobHelper.log("任务执行异常: {}", e.getMessage());
            XxlJobHelper.handleFail("执行异常: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 4. 模拟调度触发（独立测试用）
    // -----------------------------------------------------------------------

    /**
     * 独立运行入口：模拟调度中心触发的场景。
     *
     * 正常使用时，调度中心通过 HTTP RPC 调用执行器，XxlJobHelper 的上下文
     * 由 XXL-Job 框架自动初始化。此处手动初始化上下文用于演示逻辑。
     */
    public static void main(String[] args) {
        log.info("=== XXL-Job 基础演示 ===");

        XxlJobBasicDemo demo = new XxlJobBasicDemo();

        // 模拟触发无参数任务
        log.info("--- 演示1：简单任务 ---");
        demo.demoJobHandler();

        // 模拟触发带参数任务
        log.info("--- 演示2：参数任务 ---");
        // 注意：独立运行时 XxlJobHelper.getJobParam() 返回 null
        demo.paramJobHandler();

        // 模拟触发结果控制任务
        log.info("--- 演示3：结果控制（成功） ---");
        demo.resultJobHandler();

        log.info("=== 基础演示结束 ===");
    }
}
