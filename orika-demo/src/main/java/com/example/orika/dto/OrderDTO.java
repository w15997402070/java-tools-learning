package com.example.orika.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单DTO（传输层模型）
 */
@Data
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private BigDecimal amount;
    private String orderStatus; // 枚举字符串：待支付/已支付/已发货/已完成
    private Date createdAt;
    private String note;
}
