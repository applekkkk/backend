package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.CustomRequest;
import com.lk.datamarket.service.CustomRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/custom-requests")
public class CustomRequestController {
    @Autowired
    private CustomRequestService customRequestService;

    @GetMapping("/all")
    public Result<List<CustomRequest>> getAllRequests() {
        return customRequestService.getAllRequests();
    }

    @GetMapping("/{id}")
    public Result<CustomRequest> getRequestById(@PathVariable Long id) {
        return customRequestService.getRequestById(id);
    }

    @PostMapping
    public Result<String> createRequest(@RequestBody CustomRequest request) {
        log.info("发布自定义需求：{}", request.getTitle());
        return customRequestService.createRequest(request);
    }

    @PutMapping("/{id}/accept")
    public Result<String> acceptRequest(@PathVariable Long id, 
                                        @RequestParam Long acceptorId,
                                        @RequestParam String acceptorName) {
        log.info("承接需求 ID: {}, 承接人：{}", id, acceptorName);
        return customRequestService.acceptRequest(id, acceptorId, acceptorName);
    }

    @GetMapping("/user/{userId}")
    public Result<List<CustomRequest>> getUserRequests(@PathVariable Long userId) {
        return customRequestService.getUserRequests(userId);
    }
}
