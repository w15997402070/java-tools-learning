package com.example.mapstruct.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收货地址DTO（用于演示多源合并映射）
 *
 * <p>合并了 User 和 Address 两个对象的字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryInfoDTO {

    /** 来自 User */
    private String recipientName;

    /** 来自 User */
    private String phone;

    /** 来自 Address */
    private String province;

    /** 来自 Address */
    private String city;

    /** 来自 Address */
    private String district;

    /** 来自 Address */
    private String detail;

    /** 来自 Address */
    private String zipCode;

    /** 完整地址（通过表达式拼接） */
    private String fullAddress;
}
