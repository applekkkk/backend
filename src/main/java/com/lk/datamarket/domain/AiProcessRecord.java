package com.lk.datamarket.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiProcessRecord {
    private Long id;
    private String orderNo;
    private Long userId;
    private String sourceFileName;
    private String instruction;
    private String reportMarkdown;
    private String previewJson;
    private String resultFileName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

