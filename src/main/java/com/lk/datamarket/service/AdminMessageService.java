package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.AdminMessage;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.mapper.AdminMessageMapper;
import com.lk.datamarket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class AdminMessageService {
    @Autowired
    private AdminMessageMapper adminMessageMapper;
    @Autowired
    private UserMapper userMapper;

    @PostConstruct
    public void ensureTable() {
        adminMessageMapper.ensureTable();
    }

    public Result<String> createMessage(Long userId, String content) {
        if (userId == null || content == null || content.trim().isEmpty()) {
            return Result.error("参数错误");
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        AdminMessage message = new AdminMessage();
        message.setUserId(userId);
        message.setUserName(user.getName() == null ? user.getUsername() : user.getName());
        message.setContent(content.trim());
        adminMessageMapper.insert(message);
        return Result.success("留言成功");
    }

    public Result<List<AdminMessage>> getAllMessages() {
        return Result.success(adminMessageMapper.findAll());
    }

    public Result<List<AdminMessage>> getUserMessages(Long userId) {
        return Result.success(adminMessageMapper.findByUserId(userId));
    }
}
