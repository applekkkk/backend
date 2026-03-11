package com.lk.datamarket.domain;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String name;
    private Integer role;
    private String avatar;
    private String bio;
    private Integer points;
    private Integer status;
    private LocalDate lastCheckInDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
