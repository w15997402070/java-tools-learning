package com.example.mapstruct.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建用户请求对象（前端传入）
 *
 * <p>用于演示从请求DTO反向映射到Entity，以及忽略某些字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    private String username;

    private String password;

    private String realName;

    private String email;

    private String phone;

    private Integer age;
}
