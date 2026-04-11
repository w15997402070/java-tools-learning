package com.example.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Quartz 实战场景演示
 *
 * 涵盖内容：
 *  1. 电商场景：订单超时自动取消（延迟单次任务）
 *  2. 报表场景：每日定时生成报表（Cron + JobDataMap 传参）
 *  3. 动态调度：运行时动态新增/修改/删除 Job（rescheduleJob + deleteJob）
 *  4. 任务链（Chain）：JobA 完成后，通过 JobListener 触发 JobB
 *
 * 运行方式：直接执行 main 方法，程序运行约 25 秒后退出。
 */
public class QuartzPracticalDemo {

    private static final Logger log = LoggerFactory.getLogger(QuartzPracticalDemo.class);

    // -----------------------------------------------------------------------
    // 场景1：订单超时取消 Job（模拟延迟任务）
    // -----------------------------------------------------------------------

    public static class OrderTimeoutJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            String orderId = context.getMergedJobDataMap().getString("orderId");
            log.info("[OrderTimeoutJob] 订单超时，执行取消逻辑，orderId={}, 时间={}", orderId, new Date());
            // 实际场景：调用 orderService.cancel(orderId)
        }
    }

    // -----------------------------------------------------------------------
    // 场景2：每日报表 Job（模拟定时任务）
    // -----------------------------------------------------------------------

    public static class DailyReportJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            String reportType = context.getMergedJobDataMap().getString("reportType");
            String recipients = context.getMergedJobDataMap().getString("recipients");
            log.info("[DailyReportJob] 生成{}报表，发送给：{}，时间={}", reportType, recipients, new Date());
            // 实际场景：生成 Excel，通过邮件发送
        }
    }

    // -----------------------------------------------------------------------
    // 场景3：动态调度演示 Job（可被动态修改的任务）
    // -----------------------------------------------------------------------

    public static class DynamicJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            int interval = context.getMergedJobDataMap().getInt("interval");
            log.info("[DynamicJob] 执行中，当前间隔={}s，时间={}", interval, new Date());
        }
    }

    // -----------------------------------------------------------------------
    // 场景4：任务链 —— JobA → JobB（JobA 完成后自动触发 JobB）
    // -----------------------------------------------------------------------

    public static class ChainJobA implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            log.info("[ChainJobA] 数据采集完成，时间={}", new Date());
            // 执行完毕后，由 ChainJobListener 自动调度 ChainJobB
        }
    }

    public static class ChainJobB implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            log.info("[ChainJobB] 数据处理完成（由 JobA 触发），时间={}", new Date());
        }
    }

    /**
     * 任务链监听器：JobA 执行完毕后，立即触发 JobB 执行一次。
     */
    public static class ChainJobListener implements JobListener {

        private final Scheduler scheduler;

        public ChainJobListener(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public String getName() {
            return "ChainJobListener";
        }

        @Override
        public void jobToBeExecuted(JobExecutionContext context) {
            // 不做处理
        }

        @Override
        public void jobExecutionVetoed(JobExecutionContext context) {
            // 不做处理
        }

        @Override
        public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
            // 只当 ChainJobA 执行完且没有异常时，才触发 ChainJobB
            if (context.getJobDetail().getJobClass().equals(ChainJobA.class) && jobException == null) {
                log.info("[ChainJobListener] 检测到 JobA 完成，触发 JobB...");
                try {
                    JobDetail jobB = JobBuilder.newJob(ChainJobB.class)
                            .withIdentity("chainJobB-" + System.currentTimeMillis(), "chain")
                            .build();

                    Trigger triggerB = TriggerBuilder.newTrigger()
                            .startNow()
                            .build();

                    scheduler.scheduleJob(jobB, triggerB);
                } catch (SchedulerException e) {
                    log.error("[ChainJobListener] 触发 JobB 失败", e);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // 主流程
    // -----------------------------------------------------------------------

    public static void main(String[] args) throws Exception {

        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

        // 注册任务链监听器（只监听 "chain" 分组）
        scheduler.getListenerManager().addJobListener(
                new ChainJobListener(scheduler),
                org.quartz.impl.matchers.GroupMatcher.jobGroupEquals("chain")
        );

        scheduler.start();
        log.info("调度器已启动，开始实战场景演示");

        // ── 场景1：模拟3个订单超时（5/8/12秒后各触发一次） ────────────
        scheduleOrderTimeout(scheduler, "ORDER-001", 5);
        scheduleOrderTimeout(scheduler, "ORDER-002", 8);
        scheduleOrderTimeout(scheduler, "ORDER-003", 12);
        log.info("[场景1] 3 个订单超时任务已调度（5s/8s/12s 后触发）");

        // ── 场景2：每日报表（用短 Cron 模拟，每 10 秒触发）─────────────
        JobDetail reportJob = JobBuilder.newJob(DailyReportJob.class)
                .withIdentity("dailyReport", "report")
                .usingJobData("reportType", "销售日报")
                .usingJobData("recipients", "manager@company.com,ceo@company.com")
                .build();

        Trigger reportTrigger = TriggerBuilder.newTrigger()
                .withIdentity("reportTrigger", "report")
                .withSchedule(CronScheduleBuilder.cronSchedule("0/10 * * * * ?"))
                .build();

        scheduler.scheduleJob(reportJob, reportTrigger);
        log.info("[场景2] 每日报表任务已调度（每 10 秒模拟一次）");

        // ── 场景3：动态调度 —— 先调度，然后动态修改间隔 ─────────────────
        JobDetail dynamicJob = JobBuilder.newJob(DynamicJob.class)
                .withIdentity("dynamicJob", "dynamic")
                .usingJobData("interval", 3)
                .build();

        Trigger dynamicTrigger = TriggerBuilder.newTrigger()
                .withIdentity("dynamicTrigger", "dynamic")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(3)
                        .repeatForever())
                .build();

        scheduler.scheduleJob(dynamicJob, dynamicTrigger);
        log.info("[场景3] 动态 Job 已调度：初始间隔 3 秒");

        // 7 秒后，动态将间隔改为 6 秒
        Thread.sleep(7_000);
        log.info("[场景3] 动态修改间隔：3s → 6s");
        Trigger newTrigger = TriggerBuilder.newTrigger()
                .withIdentity("dynamicTrigger", "dynamic")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(6)
                        .repeatForever())
                .usingJobData("interval", 6)   // 更新 DataMap 以便 Job 读取
                .build();
        // rescheduleJob：替换旧 Trigger（Job 本身不变）
        scheduler.rescheduleJob(TriggerKey.triggerKey("dynamicTrigger", "dynamic"), newTrigger);

        // ── 场景4：任务链（JobA 完成后自动触发 JobB） ─────────────────────
        Thread.sleep(3_000);

        JobDetail chainJobA = JobBuilder.newJob(ChainJobA.class)
                .withIdentity("chainJobA", "chain")
                .build();

        Trigger chainTriggerA = TriggerBuilder.newTrigger()
                .startNow()
                .build();

        scheduler.scheduleJob(chainJobA, chainTriggerA);
        log.info("[场景4] 任务链启动：ChainJobA 触发，完成后自动触发 ChainJobB");

        // 等待所有任务执行完毕
        Thread.sleep(15_000);

        // 演示删除 Job
        log.info("[场景3] 动态删除 dynamicJob");
        scheduler.deleteJob(JobKey.jobKey("dynamicJob", "dynamic"));

        Thread.sleep(2_000);

        scheduler.shutdown(true);
        log.info("调度器已关闭，实战演示结束");
    }

    /**
     * 工具方法：为指定订单创建超时取消任务。
     *
     * @param scheduler  调度器
     * @param orderId    订单ID
     * @param delaySeconds 延迟秒数（模拟超时时间）
     */
    private static void scheduleOrderTimeout(Scheduler scheduler, String orderId, int delaySeconds)
            throws SchedulerException {

        JobDetail job = JobBuilder.newJob(OrderTimeoutJob.class)
                .withIdentity("timeout-" + orderId, "order")
                .usingJobData("orderId", orderId)
                .build();

        Date triggerTime = DateBuilder.futureDate(delaySeconds, DateBuilder.IntervalUnit.SECOND);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + orderId, "order")
                .startAt(triggerTime)   // 到点只触发一次
                .build();

        scheduler.scheduleJob(job, trigger);
        log.info("  订单 {} 超时取消已调度，将于 {} 秒后执行", orderId, delaySeconds);
    }
}
