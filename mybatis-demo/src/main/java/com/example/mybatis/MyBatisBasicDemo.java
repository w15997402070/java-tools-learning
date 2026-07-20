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
 * MyBatis 基础演示类
 *
 * 演示内容：
 * 1. 构建 SqlSessionFactory（从 XML 配置）
 * 2. 使用 SqlSession 执行基本 CRUD 操作
 * 3. 注解式 Mapper（@Select / @Insert / @Update / @Delete）
 * 4. 自增主键回写（@Options useGeneratedKeys）
 * 5. 批量操作（@InsertProvider 动态 SQL）
 * 6. IN 查询（@SelectProvider 动态 SQL）
 */
public class MyBatisBasicDemo {

    private static final Logger logger = LoggerFactory.getLogger(MyBatisBasicDemo.class);

    public static void main(String[] args) throws IOException {
        logger.info("========== MyBatis 基础演示开始 ==========");

        // 1. 从 mybatis-config.xml 构建 SqlSessionFactory
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        // 2. 打开 SqlSession（自动提交模式，演示用）
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            UserMapper mapper = session.getMapper(UserMapper.class);

            // 2.1 查询所有用户
            logger.info("--- 1. 查询所有用户 ---");
            List<User> allUsers = mapper.selectAll();
            allUsers.forEach(user -> logger.info("  {}", user));

            // 2.2 根据ID查询用户
            logger.info("--- 2. 根据ID查询用户（ID=1） ---");
            User user = mapper.selectById(1);
            logger.info("  结果: {}", user);

            // 2.3 插入新用户（自增主键回写）
            logger.info("--- 3. 插入新用户（自增ID回写） ---");
            User newUser = new User("周八", "zhouba@example.com", 24, "杭州");
            int insertCount = mapper.insert(newUser);
            logger.info("  插入行数: {}, 回写ID: {}", insertCount, newUser.getId());

            // 2.4 更新用户
            logger.info("--- 4. 更新用户（ID=1 的年龄改为 29） ---");
            User updateUser = new User("张三", "zhangsan_new@example.com", 29, "北京");
            updateUser.setId(1);
            int updateCount = mapper.update(updateUser);
            logger.info("  更新行数: {}", updateCount);

            // 2.5 条件查询：根据城市
            logger.info("--- 5. 条件查询：城市 = 北京 ---");
            List<User> beijingUsers = mapper.selectByCity("北京");
            beijingUsers.forEach(u -> logger.info("  {}", u));

            // 2.6 IN 查询：根据ID列表（动态 SQL Provider）
            logger.info("--- 6. IN 查询：ID 在 [1, 3, 5] ---");
            List<User> inUsers = mapper.selectByIds(Arrays.asList(1, 3, 5));
            inUsers.forEach(u -> logger.info("  {}", u));

            // 2.7 批量插入（动态 SQL Provider）
            logger.info("--- 7. 批量插入 3 个用户 ---");
            List<User> batchUsers = Arrays.asList(
                new User("吴九", "wujiu@example.com", 31, "成都"),
                new User("郑十", "zhengshi@example.com", 27, "武汉"),
                new User("钱十一", "qianshiyi@example.com", 33, "南京")
            );
            int batchCount = mapper.batchInsert(batchUsers);
            logger.info("  批量插入行数: {}", batchCount);

            // 2.8 验证批量插入结果
            logger.info("--- 8. 验证批量插入后所有用户 ---");
            List<User> finalUsers = mapper.selectAll();
            logger.info("  总用户数: {}", finalUsers.size());
            finalUsers.forEach(u -> logger.info("  {}", u));

            // 2.9 删除演示（删除刚插入的ID=6的用户）
            logger.info("--- 9. 删除用户（ID=6） ---");
            int deleteCount = mapper.deleteById(6);
            logger.info("  删除行数: {}", deleteCount);
        }

        logger.info("========== MyBatis 基础演示结束 ==========");
    }
}
