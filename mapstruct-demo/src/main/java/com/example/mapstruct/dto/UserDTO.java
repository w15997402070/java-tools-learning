package com.example.mapstruct.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户数据传输对象（返回给前端）
 *
 * <p>不包含 password 字段（安全考虑），字段名可能与 Entity 不同。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    /** 用户名（对应 Entity 的 username） */
    private String username;

    /** 姓名（对应 Entity 的 realName） */
    private String name;

    private String email;

    private String phone;

    private Integer age;

    private BigDecimal balance;

    /** 格式化后的创建时间字符串（通过自定义转换） */
    private String createTimeStr;

    /** 状态描述（0-禁用，1-启用）- 通过自定义方法转换 */
    private String statusDesc;
}
