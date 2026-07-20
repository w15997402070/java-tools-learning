package com.example.mybatis.mapper;

import org.apache.ibatis.jdbc.SQL;
import com.example.mybatis.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户动态 SQL 构建器
 * 用于 @InsertProvider / @SelectProvider 注解
 */
public class UserSqlProvider {

    /**
     * 构建批量插入 SQL
     * 注意：MyBatis SQL 类不支持多行插入，需手动构建
     */
    public String batchInsert(@Param("users") final List<User> users) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO users (username, email, age, city) VALUES ");
        for (int i = 0; i < users.size(); i++) {
            sql.append("(#{users[").append(i).append("].username}, ");
            sql.append("#{users[").append(i).append("].email}, ");
            sql.append("#{users[").append(i).append("].age}, ");
            sql.append("#{users[").append(i).append("].city})");
            if (i < users.size() - 1) {
                sql.append(", ");
            }
        }
        return sql.toString();
    }

    /**
     * 构建 IN 查询 SQL
     */
    public String selectByIds(@Param("ids") final List<Integer> ids) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, username, email, age, city FROM users WHERE id IN ");
        sql.append("(");
        for (int i = 0; i < ids.size(); i++) {
            sql.append("#{ids[").append(i).append("]}");
            if (i < ids.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        return sql.toString();
    }
}
