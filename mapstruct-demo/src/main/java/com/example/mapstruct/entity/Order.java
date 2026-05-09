package com.example.mapstruct.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体类
 *
 * <p>包含嵌套对象（User），用于演示嵌套映射和多对象合并映射。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /** 订单ID */
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 下单用户 */
    private User user;

    /** 商品名称 */
    private String productName;

    /** 商品数量 */
    private Integer quantity;

    /** 单价（分，整数存储） */
    private Long unitPrice;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 订单状态：1-待付款，2-待发货，3-已发货，4-已完成，5-已取消 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;
}
