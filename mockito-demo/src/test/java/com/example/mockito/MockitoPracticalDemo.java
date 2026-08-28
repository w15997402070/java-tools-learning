package com.example.mockito;

import com.example.mockito.domain.Order;
import com.example.mockito.domain.OrderStatus;
import com.example.mockito.domain.PaymentResult;
import com.example.mockito.domain.User;
import com.example.mockito.repository.OrderRepository;
import com.example.mockito.service.NotificationService;
import com.example.mockito.service.OrderService;
import com.example.mockito.service.PaymentService;
import com.example.mockito.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Mockito 实战演示：完整订单支付/取消流程的单元测试。
 */
class MockitoPracticalDemo {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, paymentService, notificationService, userService);
    }

    /**
     * 实战 1：支付成功流程 —— 验证状态变更、保存、通知都被正确调用。
     */
    @Test
    void practical1_payOrderSuccess() {
        Order order = new Order(1L, 100L, new BigDecimal("199.00"), OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentService.charge(order))
                .thenReturn(new PaymentResult(true, "TXN-20260828-001", "Payment succeeded"));

        boolean result = orderService.payOrder(1L);

        assertThat(result).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
        verify(notificationService).notifyPaid(order);
        verify(paymentService).charge(order);
    }

    /**
     * 实战 2：支付失败流程 —— 验证不保存、不通知、返回 false。
     */
    @Test
    void practical2_payOrderFailed() {
        Order order = new Order(2L, 200L, new BigDecimal("99.00"), OrderStatus.PENDING);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(paymentService.charge(order))
                .thenReturn(new PaymentResult(false, null, "Insufficient balance"));

        boolean result = orderService.payOrder(2L);

        assertThat(result).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationService, never()).notifyPaid(any(Order.class));
    }

    /**
     * 实战 3：重复支付保护 —— 非 PENDING 状态的订单不能支付。
     */
    @Test
    void practical3_payNonPendingOrderShouldFail() {
        Order order = new Order(3L, 300L, new BigDecimal("50.00"), OrderStatus.PAID);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.payOrder(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order is not payable");

        verify(paymentService, never()).charge(any(Order.class));
    }

    /**
     * 实战 4：VIP 折扣计算 —— 依赖 UserService 的返回值。
     */
    @Test
    void practical4_vipDiscountCalculation() {
        Order order = new Order(4L, 400L, new BigDecimal("1000.00"), OrderStatus.PENDING);
        User vipUser = new User(400L, "Charlie", "charlie@example.com", true);
        when(userService.findById(400L)).thenReturn(vipUser);

        BigDecimal finalAmount = orderService.calculateFinalAmount(order);

        assertThat(finalAmount).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    /**
     * 实战 5：取消已发货订单应抛出异常，且不能调用通知服务。
     */
    @Test
    void practical5_cancelShippedOrderShouldFail() {
        Order order = new Order(5L, 500L, new BigDecimal("150.00"), OrderStatus.SHIPPED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel order");

        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationService, never()).notifyCancelled(any(Order.class));
    }
}
