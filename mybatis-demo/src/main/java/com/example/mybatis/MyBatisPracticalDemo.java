package com.example.mybatis;

import com.example.mybatis.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * MyBatis 实战演示类
 *
 * 演示内容：
 * 1. 手动事务管理（SqlSession commit / rollback）
 * 2. 批量执行模式（ExecutorType.BATCH）提升插入性能
 * 3. 一级缓存验证（同一 SqlSession 内重复查询）
 * 4. 二级缓存概念说明
 * 5. Spring Boot 集成方式说明（注释代码）
 * 6. 常见生产环境最佳实践
 */
public class MyBatisPracticalDemo {

    private static final Logger logger = LoggerFactory.getLogger(MyBatisPracticalDemo.class);

    public static void main(String[] args) throws IOException {
        logger.info("========== MyBatis 实战演示开始 ==========");

        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        // 1. 手动事务管理：commit / rollback
        logger.info("--- 1. 手动事务管理演示 ---");
        transactionDemo(sqlSessionFactory);

        // 2. 批量执行模式（BATCH Executor）
        logger.info("--- 2. 批量执行模式（BATCH Executor）性能演示 ---");
        batchExecutorDemo(sqlSessionFactory);

        // 3. 一级缓存验证
        logger.info("--- 3. 一级缓存验证（同一 SqlSession） ---");
        firstLevelCacheDemo(sqlSessionFactory);

        // 4. Spring Boot 集成方式说明
        logger.info("--- 4. Spring Boot 集成方式（代码注释说明） ---");
        springBootIntegrationNotes();

        logger.info("========== MyBatis 实战演示结束 ==========");
    }

    /**
     * 手动事务管理演示
     * 使用 openSession(false) 关闭自动提交，手动控制事务
     */
    private static void transactionDemo(SqlSessionFactory factory) {
        // 关闭自动提交，手动控制事务
        try (SqlSession session = factory.openSession(false)) {
            UserMapper mapper = session.getMapper(UserMapper.class);

            try {
                // 插入用户 A
                User userA = new User("事务用户A", "tx_a@example.com", 20, "重庆");
                mapper.insert(userA);
                logger.info("  插入用户A，ID={}", userA.getId());

                // 插入用户 B
                User userB = new User("事务用户B", "tx_b@example.com", 25, "西安");
                mapper.insert(userB);
                logger.info("  插入用户B，ID={}", userB.getId());

                // 模拟业务异常，触发回滚
                // 取消注释下面一行可测试回滚
                // if (true) throw new RuntimeException("模拟业务异常");

                // 提交事务
                session.commit();
                logger.info("  事务提交成功");
            } catch (Exception e) {
                // 回滚事务
                session.rollback();
                logger.info("  事务回滚成功（原因: {}）", e.getMessage());
            }
        }
    }

    /**
     * 批量执行模式演示
     * ExecutorType.BATCH 可减少数据库往返次数，提升批量插入性能
     */
    private static void batchExecutorDemo(SqlSessionFactory factory) {
        // 使用 BATCH 执行器模式
        try (SqlSession session = openBatchSession(factory)) {
            UserMapper mapper = session.getMapper(UserMapper.class);

            long start = System.currentTimeMillis();
            for (int i = 1; i <= 100; i++) {
                User user = new User(
                    "批量用户" + i,
                    "batch" + i + "@example.com",
                    20 + (i % 50),
                    "批量城市"
                );
                mapper.insert(user);
                // 每 20 条刷新一次，避免内存溢出
                if (i % 20 == 0) {
                    session.flushStatements();
                }
            }
            session.commit();
            long end = System.currentTimeMillis();
            logger.info("  批量插入 100 条用户，耗时: {} ms", (end - start));
        }

        // 对比：普通模式（SIMPLE Executor）
        try (SqlSession session = factory.openSession(false)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            long start = System.currentTimeMillis();
            for (int i = 101; i <= 200; i++) {
                User user = new User(
                    "普通用户" + i,
                    "simple" + i + "@example.com",
                    20 + (i % 50),
                    "普通城市"
                );
                mapper.insert(user);
                session.commit(); // 每条都提交
            }
            long end = System.currentTimeMillis();
            logger.info("  普通模式插入 100 条用户（逐条提交），耗时: {} ms", (end - start));
            logger.info("  结论：BATCH 模式通常比逐条提交快 10~50 倍（取决于网络延迟）");
        }
    }

    /**
     * 一级缓存验证
     * 同一 SqlSession 内，相同查询会被缓存，不会重复发送 SQL
     */
    private static void firstLevelCacheDemo(SqlSessionFactory factory) {
        try (SqlSession session = factory.openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);

            // 第一次查询
            logger.info("  第一次查询 ID=1...");
            User user1 = mapper.selectById(1);
            logger.info("  结果: {}", user1.getUsername());

            // 第二次查询（相同 SqlSession，相同参数）
            logger.info("  第二次查询 ID=1（同一 SqlSession）...");
            User user2 = mapper.selectById(1);
            logger.info("  结果: {}", user2.getUsername());
            logger.info("  是否同一对象: {}（true 表示命中一级缓存）", (user1 == user2));
        }

        // 新 SqlSession：缓存不共享
        try (SqlSession session2 = factory.openSession(true)) {
            UserMapper mapper2 = session2.getMapper(UserMapper.class);
            logger.info("  新 SqlSession 查询 ID=1...");
            User user3 = mapper2.selectById(1);
            logger.info("  结果: {}", user3.getUsername());
            logger.info("  注意：一级缓存只在同一 SqlSession 内有效，跨 Session 不共享");
        }
    }

    /**
     * Spring Boot 集成方式说明（代码示例，非运行代码）
     */
    private static void springBootIntegrationNotes() {
        logger.info("  Spring Boot 集成 MyBatis 步骤：");
        logger.info("  1. 添加依赖：mybatis-spring-boot-starter + 数据库驱动");
        logger.info("     <dependency>");
        logger.info("       <groupId>org.mybatis.spring.boot</groupId>");
        logger.info("       <artifactId>mybatis-spring-boot-starter</artifactId>");
        logger.info("       <version>2.3.1</version>");
        logger.info("     </dependency>");
        logger.info("  2. application.yml 配置：");
        logger.info("     mybatis.mapper-locations=classpath:mapper/*.xml");
        logger.info("     mybatis.type-aliases-package=com.example.entity");
        logger.info("     mybatis.configuration.map-underscore-to-camel-case=true");
        logger.info("  3. 在启动类或配置类添加 @MapperScan(\"com.example.mapper\")");
        logger.info("  4. 直接使用 @Autowired 注入 Mapper 接口");
        logger.info("  5. 配合 Spring 的 @Transactional 进行事务管理");
        logger.info("");
        logger.info("  生产环境注意事项：");
        logger.info("  - 使用数据库连接池（Druid / HikariCP）");
        logger.info("  - 开启二级缓存（EhCache / Redis）需评估一致性要求");
        logger.info("  - 分页查询使用 PageHelper 插件");
        logger.info("  - SQL 注入风险：永远使用 #{} 参数绑定，避免 ${} 拼接");
        logger.info("  - 慢查询监控：开启 MyBatis 日志或使用 p6spy");
        logger.info("  - N+1 问题：使用 JOIN + ResultMap 或懒加载（fetchType=lazy）");
    }

    /**
     * 内部引用 ExecutorType，用于 BATCH 模式
     */
    private static SqlSession openBatchSession(SqlSessionFactory factory) {
        return factory.openSession(org.apache.ibatis.session.ExecutorType.BATCH, false);
    }
}
