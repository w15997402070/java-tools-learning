package com.example.orika.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户订单聚合DTO（多源合并映射目标）
 */
@Data
public class UserOrderDTO {
    // 来自 User
    private Long userId;
    private String username;
    private String userEmail;

    // 来自 Order
    private Long orderId;
    private String orderNo;
    private BigDecimal orderAmount;
    private String status;
    private Date orderTime;
}
