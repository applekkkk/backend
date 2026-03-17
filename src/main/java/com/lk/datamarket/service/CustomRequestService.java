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
        List<CustomRequest> list = customRequestMapper.findAll();
        list.forEach(this::fillEmails);
        return Result.success(list);
    }

    public Result<List<CustomRequest>> getMarketRequests(Long userId) {
        List<CustomRequest> list = customRequestMapper.findMarket(userId);
        list.forEach(this::fillEmails);
        return Result.success(list);
    }

    public Result<CustomRequest> getRequestById(Long id) {
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) return Result.error("任务不存在");
        fillEmails(request);
        return Result.success(request);
    }

    public Result<String> createRequest(CustomRequest request) {
        if (request == null || request.getPublisherId() == null) return Result.error("参数错误");
        User publisher = userMapper.findById(request.getPublisherId());
        if (publisher == null) return Result.error("发布者不存在");
        if (!canTakeTask(publisher)) return Result.error("请先完成邮箱验证后再发布任务");

        int budget = request.getBudget() == null ? 0 : request.getBudget();
        if (budget <= 0) return Result.error("积分预算必须大于0");
        int points = publisher.getPoints() == null ? 0 : publisher.getPoints();
        if (budget > points) return Result.error("积分预算不能超过当前积分");

        request.setRequestNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        request.setNeedStatus(0);
        customRequestMapper.insert(request);
        return Result.success("发布成功");
    }

    public Result<String> acceptRequest(Long id, Long acceptorId, String acceptorName) {
        if (id == null || acceptorId == null || isBlank(acceptorName)) return Result.error("参数错误");
        User acceptor = userMapper.findById(acceptorId);
        if (acceptor == null) return Result.error("用户不存在");
        if (!canTakeTask(acceptor)) return Result.error("请先完成邮箱验证后再承接任务");

        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) return Result.error("任务不存在");
        if (request.getPublisherId() != null && request.getPublisherId().equals(acceptorId)) {
            return Result.error("不能承接自己发布的任务");
        }

        int rows = customRequestMapper.updateAccept(id, acceptorId, acceptorName, 1);
        if (rows <= 0) return Result.error("任务已被承接");

        Order acceptRecord = new Order();
        acceptRecord.setBuyerId(acceptorId);
        acceptRecord.setProductId(id);
        acceptRecord.setProductName("承接任务记录：" + (request.getTitle() == null ? "任务" : request.getTitle()));
        acceptRecord.setAmount(0);
        orderService.createOrderAllowNegative(acceptRecord);

        return Result.success("承接成功");
    }

    public Result<String> submitDelivery(Long id, Long acceptorId, String deliveryFileName) {
        if (id == null || acceptorId == null || isBlank(deliveryFileName)) return Result.error("参数错误");
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) return Result.error("任务不存在");
        if (request.getAcceptorId() == null || !request.getAcceptorId().equals(acceptorId)) {
            return Result.error("仅承接方可提交交付文件");
        }
        int rows = customRequestMapper.updateDelivery(id, deliveryFileName, 2);
        if (rows <= 0) return Result.error("交付文件已提交，暂不允许再次覆盖");
        return Result.success("交付成功，等待发布方确认");
    }

    public Result<String> confirmComplete(Long id, Long publisherId) {
        if (id == null || publisherId == null) return Result.error("参数错误");
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) return Result.error("任务不存在");
        if (request.getPublisherId() == null || !request.getPublisherId().equals(publisherId)) {
            return Result.error("仅发布方可确认完成");
        }
        if (request.getAcceptorId() == null) return Result.error("未找到承接方");

        int budget = request.getBudget() == null ? 0 : Math.max(0, request.getBudget());
        String taskTitle = request.getTitle() == null ? "任务" : request.getTitle();

        Order payerOrder = new Order();
        payerOrder.setBuyerId(publisherId);
        payerOrder.setProductId(id);
        payerOrder.setProductName("任务结算支出：" + taskTitle);
        payerOrder.setAmount(-budget);
        Result<String> payerRes = orderService.createOrderAllowNegative(payerOrder);
        if (payerRes.getCode() != 200) return Result.error(payerRes.getMessage());

        Order workerOrder = new Order();
        workerOrder.setBuyerId(request.getAcceptorId());
        workerOrder.setProductId(id);
        workerOrder.setProductName("任务结算收入：" + taskTitle);
        workerOrder.setAmount(budget);
        Result<String> workerRes = orderService.createOrder(workerOrder);
        if (workerRes.getCode() != 200) return Result.error(workerRes.getMessage());

        int rows = customRequestMapper.updateStatus(id, 3);
        if (rows <= 0) return Result.error("任务状态已变化，确认失败");
        return Result.success("任务已完成并结算");
    }

    public Result<String> rejectDelivery(Long id, Long publisherId) {
        if (id == null || publisherId == null) return Result.error("参数错误");
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) return Result.error("任务不存在");
        if (request.getPublisherId() == null || !request.getPublisherId().equals(publisherId)) {
            return Result.error("仅发布方可打回任务");
        }
        if (request.getNeedStatus() == null || request.getNeedStatus() != 2) {
            return Result.error("当前状态不可打回");
        }

        int rows = customRequestMapper.updateStatus(id, 1);
        if (rows <= 0) return Result.error("任务状态已变化，打回失败");
        return Result.success("已打回，任务恢复为进行中");
    }

    public Result<List<CustomRequest>> getUserRequests(Long publisherId) {
        List<CustomRequest> list = customRequestMapper.findByPublisherId(publisherId);
        list.forEach(this::fillEmails);
        return Result.success(list);
    }

    private void fillEmails(CustomRequest request) {
        if (request == null) return;
        if (request.getPublisherId() != null) {
            User publisher = userMapper.findById(request.getPublisherId());
            request.setPublisherEmail(publisher == null ? "" : publisher.getEmail());
        }
        if (request.getAcceptorId() != null) {
            User acceptor = userMapper.findById(request.getAcceptorId());
            request.setAcceptorEmail(acceptor == null ? "" : acceptor.getEmail());
        }
    }

    private boolean canTakeTask(User user) {
        if (user == null) return false;
        int status = user.getStatus() == null ? 0 : user.getStatus();
        int verified = user.getEmailVerified() == null ? 0 : user.getEmailVerified();
        return status == 0 && verified == 1 && user.getEmail() != null && !user.getEmail().trim().isEmpty();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
