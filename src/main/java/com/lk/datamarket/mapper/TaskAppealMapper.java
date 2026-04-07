package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.TaskAppeal;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TaskAppealMapper {
    @Update("CREATE TABLE IF NOT EXISTS task_appeals (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "request_id BIGINT NOT NULL," +
            "request_title VARCHAR(128) NOT NULL DEFAULT ''," +
            "appellant_id BIGINT NOT NULL," +
            "appellant_name VARCHAR(64) NOT NULL DEFAULT ''," +
            "appellant_role VARCHAR(16) NOT NULL DEFAULT ''," +
            "claim_text TEXT NOT NULL," +
            "evidence_text TEXT NULL," +
            "evidence_image VARCHAR(255) NULL," +
            "status TINYINT NOT NULL DEFAULT 0," +
            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP," +
            "INDEX idx_task_appeals_appellant_id (appellant_id)," +
            "INDEX idx_task_appeals_request_id (request_id))")
    void ensureTable();

    @Insert("INSERT INTO task_appeals(request_id, request_title, appellant_id, appellant_name, appellant_role, claim_text, evidence_text, evidence_image, status, created_at) " +
            "VALUES(#{requestId}, #{requestTitle}, #{appellantId}, #{appellantName}, #{appellantRole}, #{claimText}, #{evidenceText}, #{evidenceImage}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskAppeal appeal);

    @Select("SELECT * FROM task_appeals WHERE appellant_id = #{userId} ORDER BY created_at DESC")
    List<TaskAppeal> findByAppellantId(@Param("userId") Long userId);

    @Select("SELECT * FROM task_appeals ORDER BY created_at DESC")
    List<TaskAppeal> findAll();

    @Select("SELECT * FROM task_appeals WHERE id = #{id}")
    TaskAppeal findById(@Param("id") Long id);

    @Update("UPDATE task_appeals SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
