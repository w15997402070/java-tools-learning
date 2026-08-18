# Day 35 - XXL-Job 分布式任务调度平台

## 工具简介

**XXL-Job** 是大众点评许雪里（xuxueli）开源的分布式任务调度平台，采用 "调度中心 + 执行器" 架构。调度中心负责任务管理、调度策略、日志收集；执行器负责接收调度并执行任务逻辑。相比 Quartz，XXL-Job 提供开箱即用的 Web 管理控制台、可视化任务配置、动态路由、分片广播、失败告警等企业级能力，是国内后端团队最常用的调度方案之一。

- **GitHub**: https://github.com/xuxueli/xxl-job
- **官网文档**: https://www.xuxueli.cn/xxl-job/
- **星标**: 27k+
- **版本**: 2.4.0（Java 8 兼容；3.0+ 需 Java 11+）
- **协议**: GNU GPLv3

### 核心概念

| 概念 | 说明 |
|------|------|
| **调度中心（Admin）** | 统一管理任务配置、调度触发、日志收集，提供 Web 控制台 |
| **执行器（Executor）** | 部署在业务应用中，接收调度中心指令并执行任务 |
| **@XxlJob** | 注解标记任务处理器，方法名即 Handler 名称 |
| **XxlJobHelper** | 任务上下文工具，获取参数、写入日志、标记结果 |
| **路由策略** | 集群环境下选择执行器节点的策略（轮询/故障转移/分片广播等） |
| **分片广播** | 广播到所有执行器并行处理，每个节点处理一部分数据 |
| **GLUE 模式** | 在 Web 控制台在线编写任务代码，无需重新部署 |
| **子任务** | 父任务执行成功后自动触发子任务，实现任务链 |

### Quartz vs XXL-Job 对比

| 特性 | Quartz | XXL-Job |
|------|--------|---------|
| 架构 | 嵌入式，单机为主 | 调度中心+执行器，天生分布式 |
| 管理界面 | 无（需自行开发） | 开箱即用 Web 控制台 |
| 任务配置 | 代码或配置文件 | Web 控制台可视化配置 |
| 动态修改 | 需调用 API | 控制台直接操作，实时生效 |
| 日志收集 | 需自行实现 | 内置，Web 可查看 |
| 失败告警 | 无 | 邮件/钉钉/企微告警 |
| 分片处理 | 无原生支持 | 分片广播，原生支持 |
| 适用场景 | 嵌入式、轻量级 | 分布式、团队协作、运维友好 |

---

## Maven 依赖配置

```xml
<!-- XXL-Job 执行器核心依赖 -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.0</version>
</dependency>
```

> 执行器只需引入 `xxl-job-core`，调度中心需单独部署 `xxl-job-admin`（可从 GitHub Release 下载或源码编译）。

---

## Spring Boot 集成方式

### 1. application.yml 配置

```yaml
xxl:
  job:
    # 调度中心地址（多个用逗号分隔）
    admin:
      addresses: http://127.0.0.1:8080/xxl-job-admin
    # 通信令牌（需与调度中心配置一致）
    accessToken: xxl-job-token-123
    executor:
      # 执行器名称（需在调度中心提前注册）
      appname: my-executor
      # 执行器IP（留空自动获取）
      ip:
      # 执行器端口（留空自动获取）
      port: 9999
      # 日志存储路径
      logpath: /data/xxl-job/logs
      # 日志保留天数
      logretentiondays: 30
```

### 2. 配置类 — 注册执行器

```java
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.ip:}")
    private String ip;

    @Value("${xxl.job.executor.port:0}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays:30}")
    private int logRetentionDays;

    @Bean(initMethod = "start", destroyMethod = "destroy")
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setAppname(appname);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
```

### 3. 编写任务处理器

```java
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class SampleJobHandler {

    /**
     * 简单任务：调度中心配置 JobHandler = "sampleJob"
     */
    @XxlJob("sampleJob")
    public void sampleJob() {
        XxlJobHelper.log("任务执行中...");

        // 获取调度参数
        String param = XxlJobHelper.getJobParam();
        XxlJobHelper.log("参数: {}", param);

        // 业务逻辑...

        XxlJobHelper.handleSuccess("执行完成");
    }

    /**
     * 分片广播任务：路由策略选 "分片广播"
     */
    @XxlJob("shardingJob")
    public void shardingJob() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        // 每个节点只处理部分数据
        for (int i = shardIndex; i < 10000; i += shardTotal) {
            // process(i)
        }
    }
}
```

### 4. 调度中心任务配置

在 Web 控制台（http://127.0.0.1:8080/xxl-job-admin）配置：

| 配置项 | 示例值 | 说明 |
|--------|--------|------|
| 执行器 | my-executor | 下拉选择已注册的执行器 |
| 任务描述 | 订单超时取消 | 便于管理 |
| 路由策略 | ROUND / SHARDING_BROADCAST | 集群环境下选择节点策略 |
| Cron | `0 0/5 * * * ?` | 每 5 分钟执行 |
| 运行模式 | BEAN | Bean 模式使用 @XxlJob |
| JobHandler | orderTimeoutCancelJob | 对应 @XxlJob 注解的值 |
| 任务参数 | timeout=30 | 传给执行器的字符串参数 |
| 子任务 | childJobId | 父任务完成后自动触发 |

---

## 路由策略详解

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| FIRST | 固定选第一个节点 | 测试 |
| LAST | 固定选最后一个节点 | 测试 |
| ROUND | 轮询各节点 | 通用默认 |
| RANDOM | 随机选择 | 简单负载均衡 |
| CONSISTENT_HASH | 一致性哈希，同任务固定到同节点 | 有状态任务 |
| LEAST_FREQUENTLY_USED | 最少使用 | 负载均衡 |
| LEAST_RECENTLY_USED | 最近最久未使用 | 负载均衡 |
| FAILOVER | 故障转移，自动跳过不可用节点 | 高可用要求 |
| BUSYOVER | 忙碌转移，跳过忙碌节点 | 避免过载 |
| SHARDING_BROADCAST | 分片广播，所有节点并行 | 大数据量并行处理 |

---

## 注意事项（Bug 风险 / 性能 / 限制）

### ⚠️ 坑1：调度中心与执行器时钟不一致

调度中心通过时间判断触发时机，执行器通过时间记录执行日志。如果两者时钟偏差较大，会导致：
- 任务不触发或延迟触发
- 日志时间错乱

**解决**：所有服务器配置 NTP 时钟同步。

### ⚠️ 坑2：accessToken 不一致导致执行器注册失败

调度中心和执行器的 `accessToken` 必须完全一致，否则执行器无法注册到调度中心，任务无法触发。

**排查**：查看执行器日志，确认无 "access token error" 日志。

### ⚠️ 坑3：@XxlJob 方法必须 public 且在 Spring Bean 中

- 方法必须为 `public`，否则 XXL-Job 无法通过反射调用
- 方法所在类必须被 Spring 管理（`@Component`），否则执行器启动时扫描不到
- 方法名不可重复（同一执行器内 @XxlJob 的 value 必须唯一）

### ⚠️ 坑4：长时间任务阻塞调度线程

XXL-Job 执行器的调度线程池有限（默认 200+），如果一个任务执行时间过长，会占用调度线程。
- 建议将超长任务拆分为子任务链
- 或使用分片广播并行处理
- 可在控制台配置 "任务超时时间"，超时自动终止

### ⚠️ 坑5：分片广播取模要注意数据分布

分片广播使用 `id % shardTotal == shardIndex` 路由，如果 ID 是连续自增且分片数固定，分布均匀。但如果 ID 是 UUID 或雪花算法生成，取模可能导致数据倾斜。
- 连续自增 ID：分布均匀 ✅
- UUID：取模分布可能不均 ❌ → 改用哈希取模 `hash(id) % shardTotal`

### ⚠️ 坑6：GLUE 模式的代码安全问题

GLUE 模式允许在 Web 控制台直接编写 Java 代码并动态编译执行，存在安全风险：
- 控制台账号泄露 → 可执行任意代码
- 建议仅用于临时需求，稳定后转为 Bean 模式
- 严格控制台访问权限，启用 accessToken

### ⚠️ 坑7：日志文件磁盘空间

执行器日志默认存储在本地磁盘（`logpath` 配置），高频任务会产生大量日志文件。
- 配置 `logretentiondays` 自动清理过期日志
- 建议值为 7~30 天
- 监控磁盘空间，避免日志写满磁盘

### 性能建议

- 执行器线程池：默认 200+，高并发任务可适当调大（通过 `xxl.job.executor.port` 参数间接控制）
- 分片广播：数据量 > 10 万时推荐使用，小数据量用 ROUND 即可
- 日志写入：`XxlJobHelper.log()` 写入的日志会通过网络回传调度中心，避免高频写入
- 任务幂等：务必保证任务幂等性，防止失败重试导致重复处理
- 调度中心高可用：调度中心建议至少 2 节点 + Nginx 负载均衡，数据库为 MySQL

---

## 本 Demo 文件说明

| 文件 | 功能 |
|------|------|
| `XxlJobBasicDemo.java` | 基础：@XxlJob 注解 / 获取参数 / 写入日志 / 标记结果 |
| `XxlJobAdvancedDemo.java` | 进阶：分片广播 / 父子任务链 / GLUE 模式 / 路由策略说明 |
| `XxlJobPracticalDemo.java` | 实战：订单超时取消 / 每日报表 / 日志清理（分片）/ 健康检查 / Spring Boot 集成 |

## 运行方法

```bash
# 进入 xxl-job-demo 目录
cd xxl-job-demo

# 编译
mvn clean package -DskipTests

# 独立运行演示（验证业务逻辑，无需调度中心）
mvn exec:java -Dexec.mainClass="com.example.xxljob.XxlJobBasicDemo"
mvn exec:java -Dexec.mainClass="com.example.xxljob.XxlJobAdvancedDemo"
mvn exec:java -Dexec.mainClass="com.example.xxljob.XxlJobPracticalDemo"
```

### 完整运行流程（配合调度中心）

1. **部署调度中心**：从 [GitHub Release](https://github.com/xuxueli/xxl-job/releases) 下载 `xxl-job-admin`，配置 MySQL 数据库后启动
2. **访问控制台**：http://127.0.0.1:8080/xxl-job-admin（默认账号 admin/123456）
3. **新建执行器**：AppName = `xxl-job-demo`，自动注册
4. **新建任务**：JobHandler = `demoJobHandler`，Cron = `0/5 * * * * ?`
5. **启动任务**：在控制台点击"启动"，观察执行日志

---

## 扩展阅读

- [XXL-Job 官方文档](https://www.xuxueli.cn/xxl-job/)
- [XXL-Job GitHub Wiki](https://github.com/xuxueli/xxl-job/wiki)
- [XXL-Job 架构设计原理](https://www.xuxueli.cn/xxl-job/#5.1%20%E6%9E%B6%E6%9E%84%E8%AE%BE%E8%AE%A1)
- [XXL-Job 与 Quartz/Elastic-Job 对比](https://www.xuxueli.cn/xxl-job/#6.1%20%E7%89%B9%E6%80%A7%E5%AF%B9%E6%AF%94)
