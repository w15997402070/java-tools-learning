package com.example.mockito.service;

import com.example.mockito.domain.User;

/**
 * 用户服务接口。
 */
public interface UserService {

    /**
     * 根据用户 ID 查询用户。
     *
     * @param userId 用户 ID
     * @return 用户对象，不存在时返回 null
     */
    User findById(Long userId);
}
