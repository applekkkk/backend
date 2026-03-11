package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.mapper.UserMapper;
import com.lk.datamarket.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public Result<Map<String, Object>> login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (passwordEncoder.matches(password, user.getPassword())) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", user.getId());
            claims.put("username", user.getUsername());
            String token = JwtUtil.genToken(claims);

            // 返回 token 和 role
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("role", user.getRole());  // 0=普通用户 1=管理员
            data.put("id", user.getId());
            data.put("name", user.getName());
            return Result.success(data);
        } else {
            return Result.error("密码错误");
        }
    }

    public Result<String> register(String username, String password) {
        // 检查用户名是否已存在
        User existUser= userMapper.findByUsername(username);
        if (existUser != null) {
            return Result.error("用户名已存在");
        }

        // 密码加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(password);

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setName(username); // 默认昵称和用户名一样
        user.setRole(0);        // 默认普通用户
        user.setPoints(0);
        user.setStatus(0);

        userMapper.insert(user);
        return Result.success("注册成功");
    }

    public Result<User> getUserById(Long id) {
        User user= userMapper.findById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    public Result<List<User>> getAllUsers() {
        List<User> users = userMapper.findAll();
        return Result.success(users);
    }

    public Result<String> updateUser(User user) {
        userMapper.update(user);
        return Result.success("更新成功");
    }

    public Result<String> updatePoints(Long userId, Integer points) {
        User user= userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPoints(points);
        userMapper.update(user);
        return Result.success("积分更新成功");
    }
}
