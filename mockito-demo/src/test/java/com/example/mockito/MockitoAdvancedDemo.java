package com.example.mockito;

import com.example.mockito.domain.Order;
import com.example.mockito.domain.OrderStatus;
import com.example.mockito.domain.User;
import com.example.mockito.repository.OrderRepository;
import com.example.mockito.service.NotificationService;
import com.example.mockito.service.OrderService;
import com.example.mockito.service.PaymentService;
import com.example.mockito.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Mockito 进阶演示：注解初始化、Spy、ArgumentCaptor、BDD 风格。
 */
class MockitoAdvancedDemo {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // 初始化 @Mock / @InjectMocks 注解
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 演示 1：使用 @Mock + @InjectMocks 自动注入依赖。
     */
    @Test
    void demo1_annotationInjection() {
        Order order = new Order(1L, 100L, new BigDecimal("99.00"), OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        User user = new User(100L, "Alice", "alice@example.com", false);
        when(userService.findById(100L)).thenReturn(user);

        BigDecimal finalAmount = orderService.calculateFinalAmount(order);

        assertThat(finalAmount).isEqualByComparingTo(new BigDecimal("99.00"));
    }

    /**
     * 演示 2：Spy —— 部分真实对象，仅 stub 指定方法。
     */
    @Test
    void demo2_spyPartialMock() {
        List<String> realList = Arrays.asList("one", "two", "three");
        List<String> spyList = spy(realList);

        // 仅对第二个元素打桩，其余走真实逻辑
        when(spyList.get(1)).thenReturn("stubbed");

        assertThat(spyList.get(0)).isEqualTo("one");
        assertThat(spyList.get(1)).isEqualTo("stubbed");
        assertThat(spyList.size()).isEqualTo(3);

        // 验证 get(0) 被调用过 1 次
        verify(spyList).get(0);
    }

    /**
     * 演示 3：ArgumentCaptor 捕获方法参数，验证传入对象的具体字段。
     */
    @Test
    void demo3_argumentCaptor() {
        Order order = new Order(2L, 200L, new BigDecimal("299.00"), OrderStatus.PENDING);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(2L);

        // 捕获 save 方法接收的订单参数
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(savedOrder.getId()).isEqualTo(2L);

        // 验证通知服务被调用
        verify(notificationService).notifyCancelled(savedOrder);
    }

    /**
     * 演示 4：BDD 风格 —— given / then 让测试读起来像业务描述。
     */
    @Test
    void demo4_bddStyle() {
        Order order = new Order(3L, 300L, new BigDecimal("199.00"), OrderStatus.PENDING);
        given(orderRepository.findById(3L)).willReturn(Optional.of(order));

        User user = new User(300L, "Bob", "bob@example.com", true);
        given(userService.findById(300L)).willReturn(user);

        BigDecimal finalAmount = orderService.calculateFinalAmount(order);

        // 断言：VIP 用户享受 9 折
        assertThat(finalAmount).isEqualByComparingTo(new BigDecimal("179.10"));

        // BDD 风格验证
        then(userService).should(times(1)).findById(300L);
        then(orderRepository).shouldHaveNoInteractions();
    }

    /**
     * 演示 5：验证调用顺序 —— InOrder。
     */
    @Test
    void demo5_verificationInOrder() {
        Order order = new Order(4L, 400L, new BigDecimal("88.00"), OrderStatus.PENDING);
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(4L);

        // 验证先保存订单，再发送取消通知
        org.mockito.InOrder inOrder = inOrder(orderRepository, notificationService);
        inOrder.verify(orderRepository).save(any(Order.class));
        inOrder.verify(notificationService).notifyCancelled(any(Order.class));
    }
}
