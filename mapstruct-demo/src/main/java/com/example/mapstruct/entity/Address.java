package com.example.mapstruct.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地址实体类
 *
 * <p>用于演示将多个对象合并映射到一个DTO的场景。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区县 */
    private String district;

    /** 详细地址 */
    private String detail;

    /** 邮编 */
    private String zipCode;
}
