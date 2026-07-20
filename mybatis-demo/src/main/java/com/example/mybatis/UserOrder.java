package com.example.mybatis;

import java.util.List;

/**
 * 用户与订单关联视图对象
 * 用于演示 ResultMap 一对多关联查询
 */
public class UserOrder {
    private Integer userId;
    private String username;
    private String email;
    private List<Order> orders;

    // Getters and Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }

    @Override
    public String toString() {
        return "UserOrder{userId=" + userId + ", username='" + username + '\'' +
               ", ordersCount=" + (orders != null ? orders.size() : 0) + '}';
    }
}
