package com.example.mockito.repository;

import com.example.mockito.domain.Order;
import com.example.mockito.domain.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * 订单数据访问接口，实际项目中通常对应 MyBatis/JPA Mapper。
 */
public interface OrderRepository {

    /**
     * 根据 ID 查询订单。
     */
    Optional<Order> findById(Long id);

    /**
     * 保存订单并返回持久化后的对象。
     */
    Order save(Order order);

    /**
     * 查询某用户的全部订单。
     */
    List<Order> findByUserId(Long userId);

    /**
     * 更新订单状态。
     */
    int updateStatus(Long id, OrderStatus status);
}
