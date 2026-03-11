package com.lk.datamarket.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long productId;
    private String productName;
    private Integer amount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
