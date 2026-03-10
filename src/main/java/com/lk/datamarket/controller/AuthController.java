package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.dto.LoginRequest;
import com.lk.datamarket.domain.dto.RegisterRequest;
import com.lk.datamarket.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest loginRequest){
        log.info("login: username "+loginRequest.getUsername()+" password "+loginRequest.getPassword());
        return userService.login(loginRequest.getUsername(), loginRequest.getPassword());
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest req) {
        log.info("register: username={},password={},password2={}", req.getUsername(),req.getPassword(),req.getConfirmPassword() );
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            return Result.error("两次密码输入不一致");
        }
        return userService.register(req.getUsername(), req.getPassword());
    }
}
