package com.example.mapstruct.mapper;

import com.example.mapstruct.dto.UserCreateRequest;
import com.example.mapstruct.dto.UserDTO;
import com.example.mapstruct.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户对象映射器（基础版本）
 *
 * <p>演示字段名不同时的映射、忽略字段、使用表达式转换等基础用法。
 *
 * <p>MapStruct 核心注解说明：
 * <ul>
 *   <li>{@code @Mapper} - 标记接口为 MapStruct 映射器，编译时自动生成实现类</li>
 *   <li>{@code componentModel = "default"} - 使用 Mappers.getMapper() 获取实例</li>
 *   <li>{@code componentModel = "spring"} - 生成 Spring @Component，可 @Autowired 注入</li>
 *   <li>{@code @Mapping} - 指定字段映射关系，source=来源字段，target=目标字段</li>
 *   <li>{@code ignore = true} - 忽略该字段，目标字段保持默认值</li>
 *   <li>{@code expression = "java(...)"} - 使用Java表达式自定义转换逻辑</li>
 * </ul>
 */
@Mapper
public interface UserMapper {

    /**
     * 获取 Mapper 实例（非 Spring 环境下使用）
     *
     * <p>Spring 环境中直接 @Autowired UserMapper 注入即可，无需此常量。
     */
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * User 实体 -> UserDTO
     *
     * <p>关键配置说明：
     * 1. realName -> name：字段名不同时必须显式指定 source 和 target
     * 2. createTime -> createTimeStr：Date 转 String，使用 dateFormat 格式化
     * 3. password 字段：UserDTO 中无此字段，MapStruct 自动忽略（不会报错）
     * 4. status -> statusDesc：使用 expression 调用自定义方法
     */
    @Mapping(source = "realName", target = "name")
    @Mapping(source = "createTime", target = "createTimeStr", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "statusDesc", expression = "java(statusToDesc(user.getStatus()))")
    UserDTO toDTO(User user);

    /**
     * UserDTO -> User（反向映射）
     *
     * <p>演示忽略目标字段：id/password/createTime/updateTime 由后端生成，不从 DTO 映射。
     */
    @Mapping(source = "name", target = "realName")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "balance", ignore = true)
    User toEntity(UserDTO userDTO);

    /**
     * UserCreateRequest -> User（从请求创建实体）
     *
     * <p>常见场景：注册/创建接口，请求对象转实体。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "realName", source = "realName")
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    User fromCreateRequest(UserCreateRequest request);

    /**
     * 更新已有 User 对象（增量更新）
     *
     * <p>@MappingTarget 注解：将源对象的非 null 字段更新到已有目标对象，
     * 而非创建新对象。常用于 PATCH 接口场景。
     */
    @Mapping(source = "name", target = "realName")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "balance", ignore = true)
    void updateFromDTO(UserDTO userDTO, @MappingTarget User user);

    /**
     * 批量映射：List<User> -> List<UserDTO>
     *
     * <p>MapStruct 自动实现集合映射，不需要写循环代码。
     */
    List<UserDTO> toDTOList(List<User> users);

    /**
     * 自定义转换方法：状态码 -> 状态描述
     *
     * <p>在 expression 中通过 "java(statusToDesc(...))" 调用。
     * default 方法让接口可以包含实现，无需单独的抽象类。
     */
    default String statusToDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "启用" : "禁用";
    }
}
