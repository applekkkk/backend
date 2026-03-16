package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.AdminMessage;
import com.lk.datamarket.service.AdminMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class AdminMessageController {
    @Autowired
    private AdminMessageService adminMessageService;

    @PostMapping
    public Result<String> createMessage(@RequestParam Long userId, @RequestParam String content) {
        return adminMessageService.createMessage(userId, content);
    }

    @GetMapping("/all")
    public Result<List<AdminMessage>> getAllMessages() {
        return adminMessageService.getAllMessages();
    }

    @GetMapping("/user/{userId}")
    public Result<List<AdminMessage>> getUserMessages(@PathVariable Long userId) {
        return adminMessageService.getUserMessages(userId);
    }
}
