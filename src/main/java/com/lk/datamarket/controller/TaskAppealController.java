package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.TaskAppeal;
import com.lk.datamarket.service.TaskAppealService;
import com.lk.datamarket.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/task-appeals")
public class TaskAppealController {
    @Autowired
    private TaskAppealService taskAppealService;

    @PostMapping
    public Result<String> createAppeal(@RequestBody TaskAppeal appeal) {
        log.info("create appeal, targetId={}, userId={}", appeal == null ? null : appeal.getRequestId(), appeal == null ? null : appeal.getAppellantId());
        return taskAppealService.createAppeal(appeal);
    }

    @GetMapping("/user/{userId}")
    public Result<List<TaskAppeal>> getUserAppeals(@PathVariable Long userId) {
        return taskAppealService.getUserAppeals(userId);
    }

    @GetMapping("/all")
    public Result<List<TaskAppeal>> getAllAppeals() {
        return taskAppealService.getAllAppeals();
    }

    @PutMapping("/{appealId}/process")
    public Result<String> markProcessed(@PathVariable Long appealId, HttpServletRequest request) {
        return taskAppealService.markProcessed(appealId, parseUserIdFromToken(request));
    }

    @PutMapping("/{appealId}/force-settle")
    public Result<String> forceSettle(@PathVariable Long appealId, HttpServletRequest request) {
        log.info("admin force settle appealId={}", appealId);
        return taskAppealService.forceSettle(appealId, parseUserIdFromToken(request));
    }

    @PutMapping("/{appealId}/force-release")
    public Result<String> forceRelease(@PathVariable Long appealId, HttpServletRequest request) {
        log.info("admin force release appealId={}", appealId);
        return taskAppealService.forceRelease(appealId, parseUserIdFromToken(request));
    }

    private Long parseUserIdFromToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = auth.substring(7).trim();
            Map<String, Object> claims = JwtUtil.parseToken(token);
            Object id = claims.get("id");
            if (id == null) {
                return null;
            }
            return Long.parseLong(String.valueOf(id));
        } catch (Exception ignored) {
            return null;
        }
    }
}
