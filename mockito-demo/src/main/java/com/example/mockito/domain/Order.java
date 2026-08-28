package com.example.mockito.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 订单领域模型，用于 Mockito 实战演示。
 */
public class Order {

    private final Long id;
    private final Long userId;
    private final BigDecimal amount;
    private OrderStatus status;
    private LocalDateTime createTime;

    public Order(Long id, Long userId, BigDecimal amount, OrderStatus status) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id)
                && Objects.equals(userId, order.userId)
                && Objects.equals(amount, order.amount)
                && status == order.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, amount, status);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", userId=" + userId +
                ", amount=" + amount +
                ", status=" + status +
                '}';
    }
}
