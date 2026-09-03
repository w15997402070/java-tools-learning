package com.example.orika.dto;

import lombok.Data;

import java.util.Date;

/**
 * 用户DTO（传输层模型）
 * 字段名/类型与User不完全一致，用于演示映射配置
 */
@Data
public class UserDTO {
    private Long userId;      // 与 User.id 映射
    private String userName;  // 与 User.username 映射（大小写不同）
    private String email;
    private Integer userAge;  // 与 User.age 映射
    private Date registerTime; // 与 User.createTime 映射
    private String status;    // 与 User.active 映射（Boolean -> String）
}
