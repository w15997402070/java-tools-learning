package com.example.mybatis.mapper;

import com.example.mybatis.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户 Mapper 接口
 * 演示 MyBatis 注解式映射（无需 XML）
 */
public interface UserMapper {

    /**
     * 根据ID查询用户（注解方式）
     */
    @Select("SELECT id, username, email, age, city FROM users WHERE id = #{id}")
    @Results(id = "userMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "email", column = "email"),
        @Result(property = "age", column = "age"),
        @Result(property = "city", column = "city")
    })
    User selectById(Integer id);

    /**
     * 查询所有用户
     */
    @Select("SELECT id, username, email, age, city FROM users")
    @ResultMap("userMap")
    List<User> selectAll();

    /**
     * 插入用户（返回自增ID）
     */
    @Insert("INSERT INTO users(username, email, age, city) VALUES(#{username}, #{email}, #{age}, #{city})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 批量插入用户（使用动态SQL foreach）
     * 注意：注解方式不支持 <foreach>，需用 Provider 或 XML
     */
    @InsertProvider(type = UserSqlProvider.class, method = "batchInsert")
    int batchInsert(@Param("users") List<User> users);

    /**
     * 更新用户
     */
    @Update("UPDATE users SET username=#{username}, email=#{email}, age=#{age}, city=#{city} WHERE id=#{id}")
    int update(User user);

    /**
     * 删除用户
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Integer id);

    /**
     * 根据城市查询用户
     */
    @Select("SELECT id, username, email, age, city FROM users WHERE city = #{city}")
    @ResultMap("userMap")
    List<User> selectByCity(@Param("city") String city);

    /**
     * 根据ID列表查询用户（IN查询）
     */
    @SelectProvider(type = UserSqlProvider.class, method = "selectByIds")
    @ResultMap("userMap")
    List<User> selectByIds(@Param("ids") List<Integer> ids);
}
