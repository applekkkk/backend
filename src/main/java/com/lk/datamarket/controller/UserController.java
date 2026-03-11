package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/all")
    public Result<List<User>> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    public Result<String> updateUser(@PathVariable Long id, @RequestBody User user) {
        log.info("更新用户信息：{}", id);
        return userService.updateUser(user);
    }

    @PutMapping("/{id}/points")
    public Result<String> updatePoints(@PathVariable Long id, @RequestParam Integer points) {
        return userService.updatePoints(id, points);
    }
}
