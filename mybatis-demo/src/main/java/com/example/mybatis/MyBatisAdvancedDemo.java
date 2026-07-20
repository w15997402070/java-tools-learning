package com.example.mybatis;

import com.example.mybatis.mapper.OrderMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * MyBatis 进阶演示类
 *
 * 演示内容：
 * 1. XML 映射方式（ResultMap / association / collection）
 * 2. 一对一关联查询（订单 + 用户信息）
 * 3. 一对多关联查询（用户 + 订单列表）
 * 4. 动态 SQL：<where> + <if> 条件拼接
 * 5. 动态 SQL：<set> + <if> 部分更新
 * 6. 动态 SQL：<foreach> 批量插入
 * 7. 动态 SQL：<foreach> IN 查询
 */
public class MyBatisAdvancedDemo {

    private static final Logger logger = LoggerFactory.getLogger(MyBatisAdvancedDemo.class);

    public static void main(String[] args) throws IOException {
        logger.info("========== MyBatis 进阶演示开始 ==========");

        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            OrderMapper mapper = session.getMapper(OrderMapper.class);

            // 1. 一对一关联查询：订单 + 用户信息
            logger.info("--- 1. 一对一关联查询：根据订单ID查询（含用户信息） ---");
            Order order = mapper.selectById(1);
            if (order != null) {
                logger.info("  订单: {}", order);
                logger.info("  关联用户: username={}, email={}",
                    order.getUser() != null ? order.getUser().getUsername() : "null",
                    order.getUser() != null ? order.getUser().getEmail() : "null");
            }

            // 2. 查询所有订单（含用户信息）
            logger.info("--- 2. 查询所有订单（含用户信息） ---");
            List<Order> allOrders = mapper.selectAllWithUser();
            allOrders.forEach(o -> logger.info("  订单: {}, 用户: {}", o.getOrderNo(),
                o.getUser() != null ? o.getUser().getUsername() : "null"));

            // 3. 根据用户ID查询订单列表
            logger.info("--- 3. 根据用户ID查询订单列表（user_id=1） ---");
            List<Order> userOrders = mapper.selectByUserId(1);
            logger.info("  用户ID=1 的订单数量: {}", userOrders.size());
            userOrders.forEach(o -> logger.info("  {}", o));

            // 4. 一对多关联查询：用户及其订单列表
            logger.info("--- 4. 一对多关联查询：用户及其订单列表 ---");
            List<UserOrder> userOrderList = mapper.selectUserOrders();
            for (UserOrder uo : userOrderList) {
                logger.info("  用户: {} ({}), 订单数: {}",
                    uo.getUsername(), uo.getEmail(),
                    uo.getOrders() != null ? uo.getOrders().size() : 0);
            }

            // 5. 动态 SQL：<where> + <if> 条件查询
            logger.info("--- 5. 动态 SQL 条件查询：user_id=1, status=PAID ---");
            List<Order> conditionOrders = mapper.selectByCondition(1, "PAID", null);
            conditionOrders.forEach(o -> logger.info("  {}", o));

            logger.info("--- 5b. 动态 SQL 条件查询：仅 minAmount=100.00 ---");
            List<Order> conditionOrders2 = mapper.selectByCondition(null, null, new BigDecimal("100.00"));
            conditionOrders2.forEach(o -> logger.info("  {}", o));

            logger.info("--- 5c. 动态 SQL 条件查询：无参数（全表） ---");
            List<Order> conditionOrders3 = mapper.selectByCondition(null, null, null);
            logger.info("  总订单数: {}", conditionOrders3.size());

            // 6. 动态 SQL：<set> + <if> 部分更新
            logger.info("--- 6. 动态 SQL 部分更新：只更新订单状态和金额 ---");
            Order updateOrder = new Order();
            updateOrder.setId(1);
            updateOrder.setStatus("COMPLETED");
            updateOrder.setAmount(new BigDecimal("209.99"));
            // 不设置 orderNo，则 SQL 中不会更新 order_no 字段
            int updateCount = mapper.updateDynamic(updateOrder);
            logger.info("  更新行数: {}", updateCount);

            // 验证更新结果
            Order updated = mapper.selectById(1);
            logger.info("  更新后: status={}, amount={}", updated.getStatus(), updated.getAmount());

            // 7. 动态 SQL：<foreach> 批量插入
            logger.info("--- 7. 批量插入订单（foreach） ---");
            List<Order> newOrders = Arrays.asList(
                createOrder(1, "ORD2024009", new BigDecimal("99.00"), "PAID"),
                createOrder(2, "ORD2024010", new BigDecimal("150.00"), "PAID"),
                createOrder(3, "ORD2024011", new BigDecimal("299.00"), "SHIPPED")
            );
            int batchInsertCount = mapper.batchInsert(newOrders);
            logger.info("  批量插入行数: {}", batchInsertCount);

            // 8. 动态 SQL：<foreach> IN 查询
            logger.info("--- 8. IN 查询：状态在 [PAID, SHIPPED] ---");
            List<String> statusList = Arrays.asList("PAID", "SHIPPED");
            List<Order> inOrders = mapper.selectByStatusList(statusList);
            logger.info("  匹配订单数: {}", inOrders.size());
            inOrders.forEach(o -> logger.info("  {}", o));
        }

        logger.info("========== MyBatis 进阶演示结束 ==========");
    }

    private static Order createOrder(int userId, String orderNo, BigDecimal amount, String status) {
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setAmount(amount);
        order.setStatus(status);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
