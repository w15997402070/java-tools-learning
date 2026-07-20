package com.example.mybatis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * 对应数据库表 orders
 */
public class Order {
    private Integer id;
    private Integer userId;
    private String orderNo;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createTime;

    // 关联对象：订单所属用户
    private User user;

    public Order() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", orderNo='" + orderNo + '\'' +
               ", amount=" + amount + ", status='" + status + '\'' +
               ", user=" + (user != null ? user.getUsername() : "null") + '}';
    }
}
