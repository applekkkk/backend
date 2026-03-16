package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.AdminMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminMessageMapper {
    @Update("CREATE TABLE IF NOT EXISTS admin_messages (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT NOT NULL," +
            "user_name VARCHAR(64) NOT NULL," +
            "content TEXT NOT NULL," +
            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)")
    void ensureTable();

    @Insert("INSERT INTO admin_messages(user_id, user_name, content, created_at) VALUES(#{userId}, #{userName}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdminMessage message);

    @Select("SELECT * FROM admin_messages ORDER BY created_at DESC")
    List<AdminMessage> findAll();

    @Select("SELECT * FROM admin_messages WHERE user_id=#{userId} ORDER BY created_at DESC")
    List<AdminMessage> findByUserId(@Param("userId") Long userId);
}
