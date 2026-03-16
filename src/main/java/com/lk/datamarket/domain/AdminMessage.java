package com.lk.datamarket.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminMessage {
    private Long id;
    private Long userId;
    private String userName;
    private String content;
    private LocalDateTime createdAt;
}
