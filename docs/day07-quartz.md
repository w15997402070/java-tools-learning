# Day 07 - Quartz Scheduler 定时任务调度框架

## 工具简介

**Quartz** 是 Java 生态中历史最悠久、功能最完整的开源定时任务调度框架，由 Terracotta 维护。它支持从简单的固定间隔任务到复杂的 Cron 表达式调度，可与 Spring Boot 无缝集成，也可嵌入任意 Java 应用。

- **GitHub**: https://github.com/quartz-scheduler/quartz
- **Stars**: 5.9k+
- **版本**: 2.3.2（当前稳定版）
- **官方文档**: http://www.quartz-scheduler.org/documentation/
- **协议**: Apache License 2.0

### 核心概念

| 概念 | 说明 |
|------|------|
| **Scheduler** | 调度器，整个框架的入口，负责管理 Job 和 Trigger |
| **Job** | 任务接口，业务逻辑写在 `execute()` 方法中 |
| **JobDetail** | Job 的配置元信息（名称、分组、JobDataMap 等） |
| **Trigger** | 触发器，定义 Job 何时执行（SimpleTrigger / CronTrigger） |
| **JobDataMap** | 在 Scheduler → Job 之间传递参数的 Map |
| **JobStore** | 任务存储方式：RAMJobStore（内存）或 JDBCJobStore（数据库持久化）|
| **JobListener / TriggerListener** | 生命周期回调，用于监控和扩展 |

---

## Maven 依赖配置

```xml
<!-- Quartz 核心 -->
<dependency>
    <groupId>org.quartz-scheduler</groupId>
    <artifactId>quartz</artifactId>
    <version>2.3.2</version>
</dependency>

<!-- 可选：Quartz 内置 Job（FileScanJob、NativeJob 等） -->
<dependency>
    <groupId>org.quartz-scheduler</groupId>
    <artifactId>quartz-jobs</artifactId>
    <version>2.3.2</version>
</dependency>
```

> **Spring Boot 项目**推荐直接使用 Spring 官方 Starter，无需手动引入以上依赖（见下方集成章节）。

---

## Spring Boot 集成方式

### 1. 添加 Spring Boot Starter

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

Spring Boot 2.0+ 已内置对 Quartz 的自动配置，只需引入 Starter 即可。

### 2. 配置文件 `application.yml`

```yaml
spring:
  quartz:
    # 内存模式（快速启动，重启后任务丢失）
    job-store-type: memory

    # 数据库持久化模式（生产推荐）
    # job-store-type: jdbc
    # jdbc:
    #   initialize-schema: always   # 首次自动建表（always/never/embedded）

    properties:
      org:
        quartz:
          scheduler:
            instanceName: MyScheduler
            instanceId: AUTO           # 集群模式下需要 AUTO
          threadPool:
            threadCount: 10
            threadPriority: 5
          # 持久化配置（job-store-type: jdbc 时需要）
          # jobStore:
          #   class: org.quartz.impl.jdbcjobstore.JobStoreTX
          #   driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
          #   useProperties: false
          #   tablePrefix: QRTZ_
          #   isClustered: true        # 开启集群
          #   clusterCheckinInterval: 20000
```

### 3. 定义 Job（推荐方式）

```java
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Spring 环境下 Job 可直接注入 Bean（需继承 QuartzJobBean 或使用 AutowireCapableBeanFactory）。
 * 推荐继承 Spring 提供的 QuartzJobBean，它会自动注入 Spring 上下文。
 */
@Component
public class ReportJob extends org.springframework.scheduling.quartz.QuartzJobBean {

    @Autowired
    private ReportService reportService;   // 直接注入 Spring Bean

    @Override
    protected void executeInternal(JobExecutionContext context) {
        String reportType = context.getMergedJobDataMap().getString("reportType");
        reportService.generate(reportType);
    }
}
```

### 4. 注册 Job 和 Trigger（Java Config 方式）

```java
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

@Configuration
public class QuartzConfig {

    /** 定义 JobDetail */
    @Bean
    public JobDetailFactoryBean reportJobDetail() {
        JobDetailFactoryBean factory = new JobDetailFactoryBean();
        factory.setJobClass(ReportJob.class);
        factory.setGroup("reportGroup");
        factory.setName("reportJob");
        factory.setDurability(true);   // 没有 Trigger 时保留 Job
        // 向 JobDataMap 写入参数
        factory.setJobDataAsMap(Map.of("reportType", "日报"));
        return factory;
    }

    /** 定义 CronTrigger（每天 8:00 执行） */
    @Bean
    public CronTriggerFactoryBean reportCronTrigger(JobDetail reportJobDetail) {
        CronTriggerFactoryBean factory = new CronTriggerFactoryBean();
        factory.setJobDetail(reportJobDetail);
        factory.setGroup("reportGroup");
        factory.setName("reportTrigger");
        factory.setCronExpression("0 0 8 * * ?");   // 每天 08:00
        factory.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_SKIP_IF_LATE);
        return factory;
    }
}
```

### 5. 动态调度（在 Service 中操作 Scheduler）

```java
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SchedulerService {

    @Autowired
    private Scheduler scheduler;

    /** 动态添加一个延迟任务（如订单超时取消） */
    public void scheduleOrderTimeout(String orderId, int delaySeconds) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(OrderTimeoutJob.class)
                .withIdentity("timeout-" + orderId, "order")
                .usingJobData("orderId", orderId)
                .build();

        Date triggerTime = DateBuilder.futureDate(delaySeconds, DateBuilder.IntervalUnit.SECOND);
        Trigger trigger = TriggerBuilder.newTrigger()
                .startAt(triggerTime)
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    /** 暂停任务 */
    public void pauseJob(String jobName, String group) throws SchedulerException {
        scheduler.pauseJob(JobKey.jobKey(jobName, group));
    }

    /** 恢复任务 */
    public void resumeJob(String jobName, String group) throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(jobName, group));
    }

    /** 删除任务 */
    public void deleteJob(String jobName, String group) throws SchedulerException {
        scheduler.deleteJob(JobKey.jobKey(jobName, group));
    }

    /** 立即触发一次任务 */
    public void triggerJobNow(String jobName, String group) throws SchedulerException {
        scheduler.triggerJob(JobKey.jobKey(jobName, group));
    }
}
```

---

## Cron 表达式速查

Quartz 的 Cron 格式为 **7 位**（比 Linux Cron 多一个秒字段）：

```
秒 分 时 日 月 周 [年]
```

| 表达式 | 说明 |
|--------|------|
| `0 0 8 * * ?` | 每天 08:00 |
| `0 0/30 9-17 * * ?` | 工作时间内每 30 分钟 |
| `0 0 12 ? * MON-FRI` | 周一至周五 12:00 |
| `0 15 10 15 * ?` | 每月 15 日 10:15 |
| `0 0/5 * * * ?` | 每 5 分钟 |
| `0/3 * * * * ?` | 每 3 秒（测试用） |

> **注意**：日（Day-of-Month）和周（Day-of-Week）不能同时指定，其中一个必须用 `?` 占位。

---

## 注意事项（Bug 风险 / 性能 / 限制）

### ⚠️ 坑1：Misfire（错过触发）

当调度器宕机、线程池满、系统负载高时，触发时间会被错过（Misfire）。
必须为每个 Trigger 指定 **Misfire 处理策略**：

```java
// CronTrigger 常用策略
CronScheduleBuilder.cronSchedule("0 0 8 * * ?")
    // 错过了就跳过（推荐：不补执行）
    .withMisfireHandlingInstructionDoNothing()
    // 或：下次正常时间执行（默认）
    // .withMisfireHandlingInstructionFireAndProceed()

// SimpleTrigger 常用策略
SimpleScheduleBuilder.simpleSchedule()
    .withIntervalInSeconds(60)
    // 错过的次数全部补执行（谨慎！）
    // .withMisfireHandlingInstructionFireNow()
    // 只补执行一次
    .withMisfireHandlingInstructionNowWithRemainingCount()
```

### ⚠️ 坑2：Spring 环境下 Job 无法注入 Bean

直接实现 `Job` 接口时，Quartz 通过反射创建实例，**不走 Spring 容器**，`@Autowired` 失效。

**解决方案**（二选一）：
1. 继承 `QuartzJobBean`（Spring 官方推荐）
2. 自定义 `JobFactory`：

```java
@Component
public class SpringJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        Object job = super.createJobInstance(bundle);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(job);
        return job;
    }
}
```

### ⚠️ 坑3：@PersistJobDataAfterExecution 与并发

`@PersistJobDataAfterExecution` 写回 JobDataMap 时，若有并发执行，会出现数据竞争。  
**必须与 `@DisallowConcurrentExecution` 搭配使用**。

### ⚠️ 坑4：JDBCJobStore 建表 DDL

生产环境需要提前执行 Quartz 官方 DDL（按数据库类型选择）：
- MySQL: `tables_mysql_innodb.sql`
- PostgreSQL: `tables_postgres.sql`
- Oracle: `tables_oracle.sql`

SQL 文件在 `quartz-2.3.2-distribution.tar.gz` 的 `docs/dbTables/` 目录下。

### ⚠️ 坑5：集群模式时钟同步

集群部署时，所有节点的系统时钟必须保持同步（建议误差 < 1 秒），否则会出现重复触发或任务丢失。  
使用 NTP 服务保证时钟同步。

### ⚠️ 坑6：线程池大小评估

默认线程池大小为 **10**。如果同时运行的 Job 超过线程池上限，多余的 Job 会等待，导致任务延迟。  
根据业务并发量合理设置 `threadCount`。

### 性能建议

- **RAMJobStore**：适合任务量小（< 1000）、不需要持久化的场景，速度最快
- **JDBCJobStore**：适合生产环境，支持持久化和集群，但每次触发都要查库，高并发下需要优化数据库索引
- 避免在 `execute()` 中执行长时间阻塞操作，超时会导致线程池耗尽

---

## 本 Demo 文件说明

| 文件 | 功能 |
|------|------|
| `QuartzBasicDemo.java` | 基础：HelloJob / SimpleScheduler / JobDataMap 传参 / @DisallowConcurrentExecution |
| `QuartzAdvancedDemo.java` | 高级：CronTrigger / @PersistJobDataAfterExecution 计数器 / JobListener + TriggerListener / 调度器管理 API |
| `QuartzPracticalDemo.java` | 实战：订单超时取消 / 每日报表调度 / 动态修改间隔（rescheduleJob）/ 任务链（JobA → JobB）|

## 运行方法

```bash
# 进入 quartz-demo 目录
cd quartz-demo

# 编译
mvn clean package -DskipTests

# 运行基础演示
mvn exec:java -Dexec.mainClass="com.example.quartz.QuartzBasicDemo"

# 运行高级演示
mvn exec:java -Dexec.mainClass="com.example.quartz.QuartzAdvancedDemo"

# 运行实战演示
mvn exec:java -Dexec.mainClass="com.example.quartz.QuartzPracticalDemo"
```

---

## 扩展阅读

- [Quartz 官方配置文档](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/)
- [Spring Boot + Quartz 官方指南](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.quartz)
- [Quartz Cron 在线生成器](https://cron.qqe2.com/)
- [Quartz 与 Spring @Scheduled 对比](https://www.baeldung.com/spring-quartz-schedule)
