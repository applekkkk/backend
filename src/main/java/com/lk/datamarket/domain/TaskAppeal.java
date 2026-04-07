package com.lk.datamarket.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskAppeal {
    private Long id;
    private String targetType;
    private Long requestId;
    private String requestTitle;
    private Long appellantId;
    private String appellantName;
    private String appellantRole;
    private String claimText;
    private String evidenceText;
    private String evidenceImage;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
