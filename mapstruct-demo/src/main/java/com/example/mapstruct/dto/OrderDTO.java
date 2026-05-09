package com.example.mapstruct.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单数据传输对象
 *
 * <p>展平了嵌套的 User 对象，字段来自 Order 和 Order.user 两层。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long orderId;

    private String orderNo;

    /** 来自 order.user.username */
    private String username;

    /** 来自 order.user.phone */
    private String userPhone;

    private String productName;

    private Integer quantity;

    /** 单价转换：分 -> 元（Long -> BigDecimal，通过表达式实现） */
    private BigDecimal unitPriceYuan;

    private BigDecimal totalAmount;

    /** 订单状态描述（通过自定义方法转换整型状态码） */
    private String statusDesc;

    private String remark;

    private Date createTime;
}
