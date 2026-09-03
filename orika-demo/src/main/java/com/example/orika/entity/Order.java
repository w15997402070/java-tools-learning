package com.example.orika.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体（数据库模型）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status; // 0-待支付 1-已支付 2-已发货 3-已完成
    private Date orderTime;
    private String remark;
}
