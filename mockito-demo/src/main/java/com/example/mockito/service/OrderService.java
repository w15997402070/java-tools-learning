package com.example.mockito.service;

import com.example.mockito.domain.Order;
import com.example.mockito.domain.OrderStatus;
import com.example.mockito.domain.PaymentResult;
import com.example.mockito.domain.User;
import com.example.mockito.repository.OrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单业务服务，演示 Mockito 中常见的依赖注入与协作对象验证。
 */
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final UserService userService;

    public OrderService(OrderRepository orderRepository,
                        PaymentService paymentService,
                        NotificationService notificationService,
                        UserService userService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /**
     * 支付订单：扣款、更新状态、发送通知。
     *
     * @param orderId 订单 ID
     * @return 是否支付成功
     */
    public boolean payOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is not payable: " + order.getStatus());
        }

        PaymentResult result = paymentService.charge(order);
        if (result.isSuccess()) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            notificationService.notifyPaid(order);
            return true;
        }
        return false;
    }

    /**
     * 计算订单最终金额：VIP 用户享受 9 折优惠。
     *
     * @param order 订单
     * @return 折后金额
     */
    public BigDecimal calculateFinalAmount(Order order) {
        if (order == null || order.getAmount() == null) {
            throw new IllegalArgumentException("Order amount is required");
        }
        User user = userService.findById(order.getUserId());
        BigDecimal discount = (user != null && user.isVip()) ? new BigDecimal("0.9") : BigDecimal.ONE;
        return order.getAmount().multiply(discount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 取消订单：仅允许取消 PENDING 状态的订单。
     *
     * @param orderId 订单 ID
     */
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        notificationService.notifyCancelled(order);
    }
}
