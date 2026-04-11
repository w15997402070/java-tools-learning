package com.example.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Quartz 基础演示
 *
 * 涵盖内容：
 *  1. 最简单的 HelloJob — 实现 Job 接口
 *  2. SimpleScheduler — 固定间隔/固定次数触发
 *  3. JobDataMap — Job 与 Trigger 携带参数传递
 *  4. 使用 @DisallowConcurrentExecution 防止并发执行
 *
 * 运行方式：直接执行 main 方法，程序会运行约 15 秒后退出。
 */
public class QuartzBasicDemo {

    private static final Logger log = LoggerFactory.getLogger(QuartzBasicDemo.class);

    // -----------------------------------------------------------------------
    // 1. 最简单的 Job：打印一条日志
    // -----------------------------------------------------------------------

    /**
     * Quartz Job 必须实现 org.quartz.Job 接口，并提供无参公开构造器。
     * execute() 由调度器在触发时调用。
     */
    public static class HelloJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            log.info("[HelloJob] 触发时间: {}", new Date());
        }
    }

    // -----------------------------------------------------------------------
    // 2. 携带参数的 Job（通过 JobDataMap 传参）
    // -----------------------------------------------------------------------

    /**
     * @DisallowConcurrentExecution：当上一次执行还未完成时，调度器不会再启动新实例。
     * 对于耗时任务（如数据库批量操作）非常重要，防止重入导致数据混乱。
     */
    @DisallowConcurrentExecution
    public static class ParamJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap dataMap = context.getMergedJobDataMap();
            // 从 JobDataMap 获取业务参数
            String taskName = dataMap.getString("taskName");
            int    counter  = dataMap.getInt("counter");
            log.info("[ParamJob] 任务名={}, 计数={}, 执行时间={}", taskName, counter, new Date());
        }
    }

    // -----------------------------------------------------------------------
    // 3. 演示主流程
    // -----------------------------------------------------------------------

    public static void main(String[] args) throws Exception {

        // ① 创建调度器（默认使用 RAMJobStore，无需数据库）
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        log.info("调度器已启动");

        // ── 演示1：每 2 秒执行一次，共执行 4 次 ──────────────────────────
        JobDetail helloJob = JobBuilder.newJob(HelloJob.class)
                .withIdentity("helloJob", "group1")
                .build();

        Trigger simpleTrigger = TriggerBuilder.newTrigger()
                .withIdentity("simpleTrigger", "group1")
                .startNow()   // 立刻开始
                .withSchedule(
                    SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(2)   // 每 2 秒一次
                        .withRepeatCount(3)         // 额外重复 3 次（共执行 4 次）
                )
                .build();

        scheduler.scheduleJob(helloJob, simpleTrigger);
        log.info("[演示1] HelloJob 已调度：每 2 秒执行，共 4 次");

        // ── 演示2：带参数的 Job，每 3 秒执行一次，共执行 3 次 ────────────
        JobDetail paramJob = JobBuilder.newJob(ParamJob.class)
                .withIdentity("paramJob", "group1")
                // 向 JobDataMap 写入业务参数
                .usingJobData("taskName", "数据同步任务")
                .usingJobData("counter", 100)
                .build();

        Trigger paramTrigger = TriggerBuilder.newTrigger()
                .withIdentity("paramTrigger", "group1")
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND)) // 1 秒后开始
                .withSchedule(
                    SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(3)
                        .withRepeatCount(2)         // 额外重复 2 次（共 3 次）
                )
                .build();

        scheduler.scheduleJob(paramJob, paramTrigger);
        log.info("[演示2] ParamJob 已调度：1 秒后开始，每 3 秒执行，共 3 次");

        // ── 演示3：只执行一次的延迟任务 ─────────────────────────────────
        JobDetail onceJob = JobBuilder.newJob(HelloJob.class)
                .withIdentity("onceJob", "group2")
                .build();

        // 5 秒后执行一次
        Date runAt = DateBuilder.futureDate(5, DateBuilder.IntervalUnit.SECOND);
        Trigger onceTrigger = TriggerBuilder.newTrigger()
                .withIdentity("onceTrigger", "group2")
                .startAt(runAt)
                .build();   // 没有 withSchedule → 只触发一次

        scheduler.scheduleJob(onceJob, onceTrigger);
        log.info("[演示3] OnceJob 已调度：5 秒后执行一次，时间={}", runAt);

        // 主线程等待 15 秒，让调度器有时间执行任务
        log.info("等待 15 秒，观察任务执行情况...");
        Thread.sleep(15_000);

        // ── 暂停 / 恢复 / 删除 ──────────────────────────────────────────
        log.info("暂停 paramJob...");
        scheduler.pauseJob(JobKey.jobKey("paramJob", "group1"));

        Thread.sleep(2_000);

        log.info("恢复 paramJob...");
        scheduler.resumeJob(JobKey.jobKey("paramJob", "group1"));

        // 关闭调度器（waitForJobsToComplete=true：等待正在运行的 Job 完成）
        scheduler.shutdown(true);
        log.info("调度器已关闭，演示结束");
    }
}
