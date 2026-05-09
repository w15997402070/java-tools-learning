package com.example.mapstruct.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户实体类（数据库层对象）
 *
 * <p>实体类通常包含数据库字段，不适合直接暴露给前端或其他服务。
 * 需要通过MapStruct映射到DTO/VO对象进行数据传输。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** 用户ID */
    private Long id;

    /** 登录名 */
    private String username;

    /** 密码（不应该传给前端） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 年龄 */
    private Integer age;

    /** 账户余额 */
    private BigDecimal balance;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 是否启用（0-禁用，1-启用） */
    private Integer status;
}
