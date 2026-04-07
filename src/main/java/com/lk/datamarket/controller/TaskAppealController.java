package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.TaskAppeal;
import com.lk.datamarket.service.TaskAppealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Result<String> markProcessed(@PathVariable Long appealId) {
        return taskAppealService.markProcessed(appealId);
    }

    @PutMapping("/{appealId}/force-settle")
    public Result<String> forceSettle(@PathVariable Long appealId) {
        log.info("admin force settle appealId={}", appealId);
        return taskAppealService.forceSettle(appealId);
    }

    @PutMapping("/{appealId}/force-release")
    public Result<String> forceRelease(@PathVariable Long appealId) {
        log.info("admin force release appealId={}", appealId);
        return taskAppealService.forceRelease(appealId);
    }
}
