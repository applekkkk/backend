package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.mapper.UserMapper;
import com.lk.datamarket.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Result<Map<String, Object>> login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        String token = JwtUtil.genToken(claims);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("id", user.getId());
        data.put("name", user.getName());
        return Result.success(data);
    }

    public Result<String> register(String username, String password) {
        User existUser = userMapper.findByUsername(username);
        if (existUser != null) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(username);
        user.setRole(0);
        user.setPoints(0);
        user.setStatus(0);
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    public Result<User> getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    public Result<List<User>> getAllUsers() {
        return Result.success(userMapper.findAll());
    }

    public Result<String> updateUser(User user) {
        if (user == null || user.getId() == null) {
            return Result.error("用户ID不能为空");
        }
        User existing = userMapper.findById(user.getId());
        if (existing == null) {
            return Result.error("用户不存在");
        }
        existing.setName(user.getName());
        existing.setAvatar(user.getAvatar());
        existing.setBio(user.getBio());
        userMapper.update(existing);
        return Result.success("更新成功");
    }

    public Result<String> changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        if (oldPassword == null || oldPassword.trim().isEmpty()
                || newPassword == null || newPassword.trim().isEmpty()) {
            return Result.error("旧密码和新密码不能为空");
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码不正确");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
        return Result.success("密码修改成功");
    }

    public Result<String> updatePoints(Long userId, Integer points) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPoints(points);
        userMapper.update(user);
        return Result.success("积分更新成功");
    }

    public Result<String> checkIn(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        LocalDate today = LocalDate.now();
        if (today.equals(user.getLastCheckInDate())) {
            return Result.error("今日已签到");
        }
        int current = user.getPoints() == null ? 0 : user.getPoints();
        user.setPoints(current + 10);
        user.setLastCheckInDate(today);
        userMapper.update(user);
        return Result.success("签到成功");
    }
}
