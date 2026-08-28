package com.example.mockito;

import com.example.mockito.domain.Order;
import com.example.mockito.domain.OrderStatus;
import com.example.mockito.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Mockito 基础演示：创建 mock、打桩、验证、参数匹配器。
 */
class MockitoBasicDemo {

    /**
     * 演示 1：使用 mock() 创建代理对象并设置返回值。
     * 当调用 findById(1L) 时返回一个指定订单。
     */
    @Test
    void demo1_basicStubbing() {
        OrderRepository repository = mock(OrderRepository.class);
        Order expected = new Order(1L, 100L, new BigDecimal("99.00"), OrderStatus.PENDING);

        when(repository.findById(1L)).thenReturn(Optional.of(expected));

        Optional<Order> actual = repository.findById(1L);

        assertThat(actual).isPresent();
        assertThat(actual.get().getAmount()).isEqualByComparingTo(new BigDecimal("99.00"));

        // 验证 findById 被调用了 1 次
        verify(repository, times(1)).findById(1L);
    }

    /**
     * 演示 2：使用参数匹配器（any / eq / ArgumentMatchers）。
     * 任何 long 类型的参数都会返回同样的订单。
     */
    @Test
    void demo2_argumentMatchers() {
        OrderRepository repository = mock(OrderRepository.class);
        Order order = new Order(2L, 200L, new BigDecimal("199.00"), OrderStatus.PENDING);

        // any(Long.class) 表示任意 Long 参数；eq(...) 用于固定某个参数
        when(repository.findById(any(Long.class))).thenReturn(Optional.of(order));

        assertThat(repository.findById(999L)).isPresent();
        assertThat(repository.findById(-1L)).isPresent();

        // 验证至少被调用了 2 次
        verify(repository, atLeast(2)).findById(anyLong());
    }

    /**
     * 演示 3：void 方法的验证与调用次数。
     */
    @Test
    void demo3_verifyVoidMethod() {
        OrderRepository repository = mock(OrderRepository.class);
        Order order = new Order(3L, 300L, new BigDecimal("59.00"), OrderStatus.PAID);

        repository.save(order);
        repository.save(order);

        // 验证 save 被调用了 2 次
        verify(repository, times(2)).save(order);
        // 验证 updateStatus 从未被调用
        verify(repository, never()).updateStatus(anyLong(), any(OrderStatus.class));
    }

    /**
     * 演示 4：连续打桩 —— 第一次返回成功，第二次返回失败。
     */
    @Test
    void demo4_consecutiveStubbing() {
        OrderRepository repository = mock(OrderRepository.class);
        Order first = new Order(4L, 400L, new BigDecimal("10.00"), OrderStatus.PENDING);
        Order second = new Order(5L, 500L, new BigDecimal("20.00"), OrderStatus.PAID);

        when(repository.findById(anyLong()))
                .thenReturn(Optional.of(first))
                .thenReturn(Optional.of(second))
                .thenReturn(Optional.empty());

        assertThat(repository.findById(1L).get().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(repository.findById(2L).get().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(repository.findById(3L)).isEmpty();
    }

    /**
     * 演示 5：抛出异常 —— 当调用不存在的订单时抛出 IllegalArgumentException。
     */
    @Test
    void demo5_throwException() {
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findById(ArgumentMatchers.eq(404L)))
                .thenThrow(new IllegalArgumentException("Order not found"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.findById(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }
}
