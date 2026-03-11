package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM users ORDER BY created_at DESC")
    List<User> findAll();

    @Insert("INSERT INTO users(username, password, name, role, points, status, created_at) " +
            "VALUES(#{username}, #{password}, #{name}, #{role}, #{points}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE users SET name=#{name}, avatar=#{avatar}, bio=#{bio}, " +
            "points=#{points}, last_check_in_date=#{lastCheckInDate}, updated_at=NOW() WHERE id=#{id}")
    int update(User user);
}
