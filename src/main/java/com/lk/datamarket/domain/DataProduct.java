package com.lk.datamarket.domain;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DataProduct {
    private Long id;
    private String name;
    private String info;
    private String category;
    private String tags;
    private Integer price;
    private String sizeLabel;
    private String seller;
    private Long authorId;
    private String authorName;
    private String fileName;
    private String summary;
    private Integer likes;
    private Integer stars;
    private Integer downloads;
    private Integer reviewStatus;
    private LocalDate uploadDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
