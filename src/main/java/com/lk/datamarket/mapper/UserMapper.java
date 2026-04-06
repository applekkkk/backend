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

    @Select("SELECT COUNT(1) FROM users WHERE name = #{name} AND id <> #{id}")
    int countByNameExcludeId(@Param("name") String name, @Param("id") Long id);

    @Select("SELECT * FROM users where role = 0 ORDER BY created_at DESC")
    List<User> findAll();

    @Insert("INSERT INTO users(username, password, name, role, points, status, email, email_verified, created_at) " +
            "VALUES(#{username}, #{password}, #{name}, #{role}, #{points}, #{status}, #{email}, #{emailVerified}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE users SET name=#{name}, avatar=#{avatar}, bio=#{bio}, " +
            "email=#{email}, email_verified=#{emailVerified}, points=#{points}, status=#{status}, " +
            "last_check_in_date=#{lastCheckInDate}, updated_at=NOW() WHERE id=#{id}")
    int update(User user);

    @Update("UPDATE users SET password=#{password}, updated_at=NOW() WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Select("SELECT COUNT(1) FROM information_schema.columns " +
            "WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'email'")
    int existsEmailColumn();

    @Select("SELECT COUNT(1) FROM information_schema.columns " +
            "WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'email_verified'")
    int existsEmailVerifiedColumn();

    @Update("ALTER TABLE users ADD COLUMN email VARCHAR(128) NULL")
    void addEmailColumn();

    @Update("ALTER TABLE users ADD COLUMN email_verified TINYINT NOT NULL DEFAULT 0")
    void addEmailVerifiedColumn();
}
