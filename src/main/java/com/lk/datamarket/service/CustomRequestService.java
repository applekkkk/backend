package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.CustomRequest;
import com.lk.datamarket.domain.Order;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.mapper.CustomRequestMapper;
import com.lk.datamarket.mapper.UserMapper;
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

    @Autowired
    private UserMapper userMapper;

    public Result<List<CustomRequest>> getAllRequests() {
        return Result.success(customRequestMapper.findAll());
    }

    public Result<List<CustomRequest>> getMarketRequests(Long userId) {
        return Result.success(customRequestMapper.findMarket(userId));
    }

    public Result<CustomRequest> getRequestById(Long id) {
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) {
            return Result.error("需求不存在");
        }
        return Result.success(request);
    }

    public Result<String> createRequest(CustomRequest request) {
        if (request == null || request.getPublisherId() == null) {
            return Result.error("参数错误");
        }

        User publisher = userMapper.findById(request.getPublisherId());
        if (publisher == null) {
            return Result.error("发布者不存在");
        }

        int budget = request.getBudget() == null ? 0 : request.getBudget();
        if (budget <= 0) {
            return Result.error("积分预算必须大于 0");
        }
        int points = publisher.getPoints() == null ? 0 : publisher.getPoints();
        if (budget > points) {
            return Result.error("积分预算不能超过当前积分");
        }

        request.setRequestNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        request.setNeedStatus(0);
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
        if (request.getPublisherId() != null && request.getPublisherId().equals(acceptorId)) {
            return Result.error("不能承接自己发布的任务");
        }

        int rows = customRequestMapper.updateAccept(id, acceptorId, acceptorName, 1);
        if (rows <= 0) {
            return Result.error("任务已被承接");
        }
        return Result.success("承接成功");
    }

    public Result<String> submitDelivery(Long id, Long acceptorId, String deliveryFileName) {
        if (id == null || acceptorId == null || deliveryFileName == null || deliveryFileName.trim().isEmpty()) {
            return Result.error("参数错误");
        }
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) {
            return Result.error("需求不存在");
        }
        if (request.getAcceptorId() == null || !request.getAcceptorId().equals(acceptorId)) {
            return Result.error("仅承接方可提交交付文件");
        }

        int rows = customRequestMapper.updateDelivery(id, deliveryFileName, 2);
        if (rows <= 0) {
            return Result.error("交付文件已提交，暂不允许再次覆盖");
        }
        return Result.success("交付成功，等待发布方确认");
    }

    public Result<String> confirmComplete(Long id, Long publisherId) {
        if (id == null || publisherId == null) {
            return Result.error("参数错误");
        }
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) {
            return Result.error("需求不存在");
        }
        if (request.getPublisherId() == null || !request.getPublisherId().equals(publisherId)) {
            return Result.error("仅发布方可确认完成");
        }
        if (request.getAcceptorId() == null) {
            return Result.error("未找到承接方");
        }

        int budget = request.getBudget() == null ? 0 : Math.max(0, request.getBudget());
        String taskTitle = request.getTitle() == null ? "任务" : request.getTitle();

        Order payerOrder = new Order();
        payerOrder.setBuyerId(publisherId);
        payerOrder.setProductId(id);
        payerOrder.setProductName("任务结算支出：" + taskTitle);
        payerOrder.setAmount(-budget);
        Result<String> payerRes = orderService.createOrderAllowNegative(payerOrder);
        if (payerRes.getCode() != 200) {
            return Result.error(payerRes.getMessage());
        }

        Order workerOrder = new Order();
        workerOrder.setBuyerId(request.getAcceptorId());
        workerOrder.setProductId(id);
        workerOrder.setProductName("任务结算收入：" + taskTitle);
        workerOrder.setAmount(budget);
        Result<String> workerRes = orderService.createOrder(workerOrder);
        if (workerRes.getCode() != 200) {
            return Result.error(workerRes.getMessage());
        }

        int rows = customRequestMapper.updateStatus(id, 3);
        if (rows <= 0) {
            return Result.error("任务状态已变化，确认失败");
        }
        return Result.success("任务已完成并结算");
    }

    public Result<List<CustomRequest>> getUserRequests(Long publisherId) {
        return Result.success(customRequestMapper.findByPublisherId(publisherId));
    }
}
