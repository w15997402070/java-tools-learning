package com.example.mybatis.mapper;

import com.example.mybatis.Order;
import com.example.mybatis.UserOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单 Mapper 接口
 * 演示 XML 映射方式（动态 SQL、关联查询）
 */
public interface OrderMapper {

    /**
     * 根据ID查询订单（含用户信息关联）
     */
    Order selectById(Integer id);

    /**
     * 查询所有订单（含用户信息关联）
     */
    List<Order> selectAllWithUser();

    /**
     * 根据用户ID查询订单列表
     */
    List<Order> selectByUserId(@Param("userId") Integer userId);

    /**
     * 查询用户及其订单列表（一对多关联）
     */
    List<UserOrder> selectUserOrders();

    /**
     * 动态条件查询订单（<where> + <if>）
     */
    List<Order> selectByCondition(@Param("userId") Integer userId,
                                   @Param("status") String status,
                                   @Param("minAmount") java.math.BigDecimal minAmount);

    /**
     * 动态更新订单（<set> + <if>）
     */
    int updateDynamic(Order order);

    /**
     * 批量插入订单（<foreach>）
     */
    int batchInsert(@Param("orders") List<Order> orders);

    /**
     * 根据状态列表查询订单（<foreach> collection="list"）
     */
    List<Order> selectByStatusList(@Param("statusList") List<String> statusList);
}
