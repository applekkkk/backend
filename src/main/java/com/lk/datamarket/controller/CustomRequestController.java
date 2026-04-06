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
@RequestMapping("/custom-requests")
public class CustomRequestController {
    @Autowired
    private CustomRequestService customRequestService;

    @GetMapping
    public Result<List<CustomRequest>> listMarket(@RequestParam(required = false) Long userId) {
        return customRequestService.getMarketRequests(userId);
    }

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
        log.info("Create custom request: {}", request.getTitle());
        return customRequestService.createRequest(request);
    }

    @PutMapping("/{id}/accept")
    public Result<String> acceptRequest(@PathVariable Long id,
                                        @RequestParam Long acceptorId,
                                        @RequestParam String acceptorName) {
        log.info("Accept request id: {}, acceptor: {}", id, acceptorName);
        return customRequestService.acceptRequest(id, acceptorId, acceptorName);
    }

    @PutMapping("/{id}/delivery")
    public Result<String> submitDelivery(@PathVariable Long id,
                                         @RequestParam Long acceptorId,
                                         @RequestParam String deliveryFileName) {
        log.info("Submit delivery, request id: {}, acceptorId: {}, file: {}", id, acceptorId, deliveryFileName);
        return customRequestService.submitDelivery(id, acceptorId, deliveryFileName);
    }

    @PutMapping("/{id}/complete")
    public Result<String> confirmComplete(@PathVariable Long id,
                                          @RequestParam Long publisherId) {
        log.info("Confirm custom request complete, request id: {}, publisherId: {}", id, publisherId);
        return customRequestService.confirmComplete(id, publisherId);
    }

    @PutMapping("/{id}/reject")
    public Result<String> rejectDelivery(@PathVariable Long id,
                                         @RequestParam Long publisherId) {
        log.info("Reject delivery, request id: {}, publisherId: {}", id, publisherId);
        return customRequestService.rejectDelivery(id, publisherId);
    }

    @PutMapping("/{id}/admin-status")
    public Result<String> adminUpdateStatus(@PathVariable Long id,
                                            @RequestParam Integer status) {
        log.info("Admin update custom request status, id: {}, status: {}", id, status);
        return customRequestService.adminUpdateStatus(id, status);
    }

    @GetMapping("/user/{userId}")
    public Result<List<CustomRequest>> getUserRequests(@PathVariable Long userId) {
        return customRequestService.getUserRequests(userId);
    }
}
