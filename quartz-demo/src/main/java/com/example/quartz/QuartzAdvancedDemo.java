package com.example.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

/**
 * Quartz 高级特性演示
 *
 * 涵盖内容：
 *  1. CronTrigger — 基于 Cron 表达式的精确时间调度
 *  2. JobListener / TriggerListener — 监听 Job/Trigger 生命周期
 *  3. @PersistJobDataAfterExecution — 在多次执行间持久化 JobDataMap（计数器场景）
 *  4. 调度器管理 API：列举 Job、暂停 Group、获取 Trigger 状态
 *
 * 运行方式：直接执行 main 方法，程序运行约 20 秒后退出。
 */
public class QuartzAdvancedDemo {

    private static final Logger log = LoggerFactory.getLogger(QuartzAdvancedDemo.class);

    // -----------------------------------------------------------------------
    // 1. 自增计数 Job（搭配 @PersistJobDataAfterExecution）
    // -----------------------------------------------------------------------

    /**
     * @PersistJobDataAfterExecution：每次执行完毕后，将修改过的 JobDataMap 持久化回 JobStore。
     * 与 @DisallowConcurrentExecution 通常成对使用，防止并发时的计数错乱。
     */
    @PersistJobDataAfterExecution
    @DisallowConcurrentExecution
    public static class CounterJob implements Job {

        public static final String COUNT_KEY = "count";

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            int count = dataMap.getInt(COUNT_KEY);
            count++;
            dataMap.put(COUNT_KEY, count);   // 写回 DataMap，下次执行时可读到更新值
            log.info("[CounterJob] 第 {} 次执行，时间={}", count, new Date());
        }
    }

    // -----------------------------------------------------------------------
    // 2. Cron 测试 Job
    // -----------------------------------------------------------------------

    public static class CronJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            String expression = context.getTrigger().getJobDataMap().getString("expr");
            log.info("[CronJob] 触发（cron={}），时间={}", expression, new Date());
        }
    }

    // -----------------------------------------------------------------------
    // 3. JobListener — 监听 Job 执行前/后/被拒绝
    // -----------------------------------------------------------------------

    public static class AuditJobListener implements JobListener {

        @Override
        public String getName() {
            return "AuditJobListener";
        }

        /** Job 即将被执行 */
        @Override
        public void jobToBeExecuted(JobExecutionContext context) {
            log.info("[AuditListener] Job 即将执行: {}", context.getJobDetail().getKey());
        }

        /** Job 执行被 TriggerListener 否决 */
        @Override
        public void jobExecutionVetoed(JobExecutionContext context) {
            log.warn("[AuditListener] Job 执行被拒绝: {}", context.getJobDetail().getKey());
        }

        /** Job 执行完毕（jobException != null 表示执行抛出异常） */
        @Override
        public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
            if (jobException != null) {
                log.error("[AuditListener] Job 执行异常: {}, 错误={}", context.getJobDetail().getKey(), jobException.getMessage());
            } else {
                log.info("[AuditListener] Job 执行完毕: {}, 耗时={}ms", context.getJobDetail().getKey(), context.getJobRunTime());
            }
        }
    }

    // -----------------------------------------------------------------------
    // 4. TriggerListener — 监听 Trigger 点火/完成/错过
    // -----------------------------------------------------------------------

    public static class LogTriggerListener implements TriggerListener {

        @Override
        public String getName() {
            return "LogTriggerListener";
        }

        @Override
        public void triggerFired(Trigger trigger, JobExecutionContext context) {
            log.debug("[TriggerListener] Trigger 点火: {}", trigger.getKey());
        }

        /**
         * 返回 true 则取消本次 Job 执行（用于实现"业务开关"）。
         * 此处始终返回 false，表示不拦截。
         */
        @Override
        public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
            return false;
        }

        @Override
        public void triggerMisfired(Trigger trigger) {
            log.warn("[TriggerListener] Trigger 错过触发: {}，错过时间={}", trigger.getKey(), new Date());
        }

        @Override
        public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
            log.debug("[TriggerListener] Trigger 完成: {}", trigger.getKey());
        }
    }

    // -----------------------------------------------------------------------
    // 5. 演示主流程
    // -----------------------------------------------------------------------

    public static void main(String[] args) throws Exception {

        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

        // 注册全局 JobListener（监听所有 Job）
        scheduler.getListenerManager().addJobListener(
                new AuditJobListener(), GroupMatcher.anyJobGroup());

        // 注册全局 TriggerListener
        scheduler.getListenerManager().addTriggerListener(
                new LogTriggerListener(), GroupMatcher.anyTriggerGroup());

        scheduler.start();
        log.info("调度器已启动（含 JobListener + TriggerListener）");

        // ── 演示1：CronTrigger — 每 3 秒执行（用秒级 Cron 表达式） ──────
        // Quartz Cron 格式：秒 分 时 日 月 周 [年]
        // "0/3 * * * * ?" 表示每 3 秒触发一次
        JobDetail cronJob = JobBuilder.newJob(CronJob.class)
                .withIdentity("cronJob", "cronGroup")
                .usingJobData("expr", "0/3 * * * * ?")
                .build();

        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity("cronTrigger", "cronGroup")
                .withSchedule(
                    CronScheduleBuilder
                        .cronSchedule("0/3 * * * * ?")
                        .inTimeZone(TimeZone.getTimeZone("Asia/Shanghai"))  // 明确时区，避免夏令时坑
                )
                .build();

        scheduler.scheduleJob(cronJob, cronTrigger);
        log.info("[演示1] CronJob 已调度：每 3 秒触发 (cron=0/3 * * * * ?)");

        // ── 演示2：@PersistJobDataAfterExecution 计数器 Job ─────────────
        JobDetail counterJob = JobBuilder.newJob(CounterJob.class)
                .withIdentity("counterJob", "counterGroup")
                .usingJobData(CounterJob.COUNT_KEY, 0)   // 初始计数 = 0
                .storeDurably()
                .build();

        Trigger counterTrigger = TriggerBuilder.newTrigger()
                .withIdentity("counterTrigger", "counterGroup")
                .startNow()
                .withSchedule(
                    SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(4)
                        .repeatForever()   // 持续重复，直到手动停止
                )
                .build();

        scheduler.scheduleJob(counterJob, counterTrigger);
        log.info("[演示2] CounterJob 已调度：每 4 秒触发，自动累加计数");

        // 运行 18 秒后进行管理操作演示
        Thread.sleep(18_000);

        // ── 演示3：调度器管理 API ─────────────────────────────────────────
        log.info("=== 调度器管理 API 演示 ===");

        // 列举所有 JobKey
        log.info("当前所有 Job：");
        for (String group : scheduler.getJobGroupNames()) {
            Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group));
            for (JobKey key : keys) {
                JobDetail detail = scheduler.getJobDetail(key);
                log.info("  Group={}, Key={}, Class={}", group, key, detail.getJobClass().getSimpleName());
            }
        }

        // 获取 Trigger 当前状态
        Trigger.TriggerState state = scheduler.getTriggerState(TriggerKey.triggerKey("cronTrigger", "cronGroup"));
        log.info("cronTrigger 状态: {}", state);

        // 暂停整个 cronGroup
        scheduler.pauseTriggers(GroupMatcher.triggerGroupEquals("cronGroup"));
        log.info("已暂停 cronGroup 下的所有 Trigger");

        Thread.sleep(3_000);

        // 恢复
        scheduler.resumeTriggers(GroupMatcher.triggerGroupEquals("cronGroup"));
        log.info("已恢复 cronGroup 下的所有 Trigger");

        Thread.sleep(5_000);

        scheduler.shutdown(true);
        log.info("调度器已关闭，高级演示结束");
    }
}
