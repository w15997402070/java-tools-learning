package com.example.mockito.service;

import com.example.mockito.domain.Order;
import com.example.mockito.domain.PaymentResult;

/**
 * 支付服务接口，负责与第三方支付网关交互。
 */
public interface PaymentService {

    /**
     * 对指定订单发起扣款。
     *
     * @param order 待支付订单
     * @return 支付结果
     */
    PaymentResult charge(Order order);
}
