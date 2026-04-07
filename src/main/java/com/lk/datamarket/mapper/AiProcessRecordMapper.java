package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.AiProcessRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiProcessRecordMapper {
    @Update({
            "CREATE TABLE IF NOT EXISTS ai_process_records (",
            "id BIGINT PRIMARY KEY AUTO_INCREMENT,",
            "order_no VARCHAR(64) NOT NULL,",
            "user_id BIGINT NOT NULL,",
            "source_file_name VARCHAR(255) NULL,",
            "instruction TEXT NULL,",
            "report_markdown LONGTEXT NULL,",
            "preview_json LONGTEXT NULL,",
            "result_file_name VARCHAR(255) NULL,",
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,",
            "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,",
            "UNIQUE KEY uk_ai_process_order_no(order_no),",
            "KEY idx_ai_process_user_id(user_id)",
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
    })
    void ensureTable();

    @Insert({
            "INSERT INTO ai_process_records(",
            "order_no, user_id, source_file_name, instruction, report_markdown, preview_json, result_file_name, created_at, updated_at",
            ") VALUES (",
            "#{orderNo}, #{userId}, #{sourceFileName}, #{instruction}, #{reportMarkdown}, #{previewJson}, #{resultFileName}, NOW(), NOW()",
            ") ON DUPLICATE KEY UPDATE ",
            "source_file_name = VALUES(source_file_name),",
            "instruction = VALUES(instruction),",
            "report_markdown = VALUES(report_markdown),",
            "preview_json = VALUES(preview_json),",
            "result_file_name = VALUES(result_file_name),",
            "updated_at = NOW()"
    })
    int upsert(AiProcessRecord record);

    @Select("SELECT * FROM ai_process_records WHERE order_no = #{orderNo} LIMIT 1")
    AiProcessRecord findByOrderNo(String orderNo);

    @Select("SELECT * FROM ai_process_records WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<AiProcessRecord> findByUserId(Long userId);
}

