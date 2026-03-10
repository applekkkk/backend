package com.lk.datamarket.domain.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
