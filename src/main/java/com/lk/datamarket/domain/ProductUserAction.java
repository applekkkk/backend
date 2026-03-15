package com.lk.datamarket.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductUserAction {
    private Long productId;
    private Long userId;
    private Integer liked;
    private Integer favorited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
