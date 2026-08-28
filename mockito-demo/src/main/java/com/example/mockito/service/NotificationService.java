package com.example.mockito.service;

import com.example.mockito.domain.Order;

/**
 * 通知服务接口，用于发送订单状态变更通知。
 */
public interface NotificationService {

    /**
     * 发送订单支付成功通知。
     *
     * @param order 已支付订单
     */
    void notifyPaid(Order order);

    /**
     * 发送订单取消通知。
     *
     * @param order 已取消订单
     */
    void notifyCancelled(Order order);
}
