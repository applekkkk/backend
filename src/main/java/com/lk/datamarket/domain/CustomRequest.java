package com.lk.datamarket.domain;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomRequest {
    private Long id;
    private String requestNo;
    private String title;
    private String description;
    private String category;
    private String tags;
    private String amount;
    private Integer budget;
    private LocalDate deadline;
    private Long publisherId;
    private String publisherName;
    private String publisherContact;
    private String attachmentName;
    private Long acceptorId;
    private String acceptorName;
    private String deliveryFileName;
    private Integer needStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
