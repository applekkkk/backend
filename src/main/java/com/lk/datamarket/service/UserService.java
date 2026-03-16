package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.mapper.CustomRequestMapper;
import com.lk.datamarket.mapper.UserMapper;
import com.lk.datamarket.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CustomRequestMapper customRequestMapper;
    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String mailFrom;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<Long, EmailCodeTicket> emailCodeMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void ensureColumns() {
        if (userMapper.existsEmailColumn() == 0) {
            userMapper.addEmailColumn();
        }
        if (userMapper.existsEmailVerifiedColumn() == 0) {
            userMapper.addEmailVerifiedColumn();
        }
    }

    public Result<Map<String, Object>> login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) return Result.error("用户不存在");
        if (!passwordEncoder.matches(password, user.getPassword())) return Result.error("密码错误");

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        String token = JwtUtil.genToken(claims);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("id", user.getId());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("emailVerified", safeInt(user.getEmailVerified()));
        data.put("status", safeInt(user.getStatus()));
        return Result.success(data);
    }

    public Result<String> register(String username, String password) {
        if (userMapper.findByUsername(username) != null) return Result.error("用户名已存在");

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(username);
        user.setRole(0);
        user.setPoints(0);
        user.setStatus(0);
        user.setEmail("");
        user.setEmailVerified(0);
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    public Result<User> getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) return Result.error("用户不存在");
        return Result.success(user);
    }

    public Result<List<User>> getAllUsers() {
        return Result.success(userMapper.findAll());
    }

    public Result<String> updateUser(User user) {
        if (user == null || user.getId() == null) return Result.error("用户ID不能为空");
        User existing = userMapper.findById(user.getId());
        if (existing == null) return Result.error("用户不存在");

        int prevStatus = safeInt(existing.getStatus());
        existing.setName(defaultStr(user.getName(), existing.getName()));
        existing.setAvatar(defaultStr(user.getAvatar(), existing.getAvatar()));
        existing.setBio(defaultStr(user.getBio(), existing.getBio()));
        existing.setPoints(user.getPoints() == null ? existing.getPoints() : user.getPoints());
        existing.setStatus(user.getStatus() == null ? existing.getStatus() : user.getStatus());
        existing.setLastCheckInDate(user.getLastCheckInDate() == null ? existing.getLastCheckInDate() : user.getLastCheckInDate());
        existing.setEmail(user.getEmail() == null ? existing.getEmail() : user.getEmail());
        existing.setEmailVerified(user.getEmailVerified() == null ? existing.getEmailVerified() : user.getEmailVerified());
        userMapper.update(existing);

        int nextStatus = safeInt(existing.getStatus());
        if (prevStatus == 0 && (nextStatus == 1 || nextStatus == 2)) {
            customRequestMapper.releaseByAcceptorId(existing.getId());
        }
        return Result.success("更新成功");
    }

    public Result<String> sendEmailCode(Long userId, String email) {
        if (userId == null) return Result.error("用户ID不能为空");
        if (!isValidEmail(email)) return Result.error("邮箱格式不正确");
        User user = userMapper.findById(userId);
        if (user == null) return Result.error("用户不存在");

        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(10);
        emailCodeMap.put(userId, new EmailCodeTicket(email.trim(), code, expireAt));
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email.trim());
            message.setSubject("DataMarket 邮箱验证码");
            message.setText("你的验证码是：" + code + "，10分钟内有效。");
            mailSender.send(message);
            return Result.success("验证码已发送，请查收邮箱");
        } catch (Exception ex) {
            log.error("send email failed userId={}, email={}", userId, email, ex);
            String reason = ex.getMessage() == null ? "请检查邮箱SMTP配置" : ex.getMessage();
            return Result.error("验证码发送失败：" + reason);
        }
    }

    public Result<String> verifyEmail(Long userId, String email, String code) {
        if (userId == null) return Result.error("用户ID不能为空");
        if (!isValidEmail(email) || isBlank(code)) return Result.error("邮箱或验证码格式不正确");
        User user = userMapper.findById(userId);
        if (user == null) return Result.error("用户不存在");

        EmailCodeTicket ticket = emailCodeMap.get(userId);
        if (ticket == null) return Result.error("请先发送验证码");
        if (LocalDateTime.now().isAfter(ticket.expireAt)) {
            emailCodeMap.remove(userId);
            return Result.error("验证码已过期");
        }
        if (!ticket.email.equalsIgnoreCase(email.trim()) || !ticket.code.equals(code.trim())) {
            return Result.error("验证码错误");
        }

        user.setEmail(email.trim());
        user.setEmailVerified(1);
        userMapper.update(user);
        emailCodeMap.remove(userId);
        return Result.success("邮箱验证成功");
    }

    public Result<String> changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) return Result.error("用户ID不能为空");
        if (isBlank(oldPassword) || isBlank(newPassword)) return Result.error("旧密码和新密码不能为空");
        User user = userMapper.findById(userId);
        if (user == null) return Result.error("用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) return Result.error("旧密码不正确");
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
        return Result.success("密码修改成功");
    }

    public Result<String> updatePoints(Long userId, Integer points) {
        User user = userMapper.findById(userId);
        if (user == null) return Result.error("用户不存在");
        user.setPoints(points);
        userMapper.update(user);
        return Result.success("积分更新成功");
    }

    public Result<String> checkIn(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) return Result.error("用户不存在");
        LocalDate today = LocalDate.now();
        if (today.equals(user.getLastCheckInDate())) return Result.error("今日已签到");
        user.setPoints(safeInt(user.getPoints()) + 10);
        user.setLastCheckInDate(today);
        userMapper.update(user);
        return Result.success("签到成功");
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultStr(String val, String fallback) {
        return val == null ? fallback : val;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String val = email.trim();
        return !val.isEmpty() && EMAIL_PATTERN.matcher(val).matches();
    }

    private static class EmailCodeTicket {
        private final String email;
        private final String code;
        private final LocalDateTime expireAt;

        private EmailCodeTicket(String email, String code, LocalDateTime expireAt) {
            this.email = email;
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
