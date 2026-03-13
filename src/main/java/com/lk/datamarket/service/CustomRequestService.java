package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.CustomRequest;
import com.lk.datamarket.domain.Order;
import com.lk.datamarket.mapper.CustomRequestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomRequestService {
    @Autowired
    private CustomRequestMapper customRequestMapper;

    @Autowired
    private OrderService orderService;

    public Result<List<CustomRequest>> getAllRequests() {
        List<CustomRequest> requests = customRequestMapper.findAll();
        return Result.success(requests);
    }

    public Result<List<CustomRequest>> getMarketRequests(Long userId) {
        List<CustomRequest> requests = customRequestMapper.findMarket(userId);
        return Result.success(requests);
    }

    public Result<CustomRequest> getRequestById(Long id) {
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) {
            return Result.error("需求不存在");
        }
        return Result.success(request);
    }

    public Result<String> createRequest(CustomRequest request) {
        if (request == null) {
            return Result.error("参数错误");
        }
        request.setRequestNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        request.setNeedStatus(0); // 0=待承接
        customRequestMapper.insert(request);
        return Result.success("发布成功");
    }

    public Result<String> acceptRequest(Long id, Long acceptorId, String acceptorName) {
        if (id == null || acceptorId == null || acceptorName == null || acceptorName.trim().isEmpty()) {
            return Result.error("参数错误");
        }
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) {
            return Result.error("需求不存在");
        }
        Integer status = request.getNeedStatus();
        if (status != null && status != 0) {
            return Result.error("该任务已被承接");
        }
        request.setAcceptorId(acceptorId);
        request.setAcceptorName(acceptorName);
        request.setNeedStatus(1); // 1=已承接
        customRequestMapper.update(request);

        Order order = new Order();
        order.setBuyerId(acceptorId);
        order.setProductId(0L);
        order.setProductName("承接任务: " + (request.getTitle() == null ? "" : request.getTitle()));
        order.setAmount(request.getBudget() == null ? 0 : request.getBudget());
        Result<String> orderRes = orderService.createOrder(order);
        if (orderRes.getCode() != 200) {
            return Result.error(orderRes.getMessage());
        }
        return Result.success("承接成功");
    }

    public Result<List<CustomRequest>> getUserRequests(Long publisherId) {
        List<CustomRequest> requests = customRequestMapper.findByPublisherId(publisherId);
        return Result.success(requests);
    }
}