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

    public Result<String> adminUpdateStatus(Long id, Integer status) {
        if (id == null || status == null) return Result.error("参数错误");
        if (status < 0 || status > 3) return Result.error("状态值不合法");

        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) return Result.error("任务不存在");
        int current = request.getNeedStatus() == null ? 0 : request.getNeedStatus();

        if (current == 3 && status != 3) {
            return Result.error("已完成任务不允许改回其他状态");
        }
        if (current == status) {
            return Result.success("状态未变化");
        }

        if (status == 0) {
            if (current != 1 && current != 2) {
                return Result.error("当前状态不可释放为未承接");
            }
            int released = customRequestMapper.forceReleaseById(id);
            if (released <= 0) return Result.error("状态更新失败，请刷新后重试");
            return Result.success("已修改为未承接");
        }

        if (status == 3) {
            if (current != 1 && current != 2) {
                return Result.error("当前状态不可修改为已完成");
            }
            return settleAndCompleteAsAdmin(request);
        }

        if (request.getAcceptorId() == null) {
            return Result.error("当前任务无承接方，不能修改为进行中或待发布者确认");
        }
        int rows = customRequestMapper.adminUpdateStatus(id, status);
        if (rows <= 0) return Result.error("状态更新失败");
        return Result.success("状态更新成功");
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
            if (publisher != null) {
                // 始终回填最新用户名，避免历史快照名不更新
                request.setPublisherName(publisher.getName());
                request.setPublisherEmail(publisher.getEmail());
            } else {
                request.setPublisherEmail("");
            }
        }
        if (request.getAcceptorId() != null) {
            User acceptor = userMapper.findById(request.getAcceptorId());
            if (acceptor != null) {
                // 始终回填最新用户名，避免历史快照名不更新
                request.setAcceptorName(acceptor.getName());
                request.setAcceptorEmail(acceptor.getEmail());
            } else {
                request.setAcceptorEmail("");
            }
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

    private Result<String> settleAndCompleteAsAdmin(CustomRequest request) {
        if (request.getPublisherId() == null || request.getAcceptorId() == null) {
            return Result.error("任务缺少发布方或承接方，无法结算");
        }

        int budget = request.getBudget() == null ? 0 : Math.max(0, request.getBudget());
        String taskTitle = request.getTitle() == null ? "任务" : request.getTitle();

        Order payerOrder = new Order();
        payerOrder.setBuyerId(request.getPublisherId());
        payerOrder.setProductId(request.getId());
        payerOrder.setProductName("任务结算支出：" + taskTitle + "（管理员修改状态）");
        payerOrder.setAmount(-budget);
        Result<String> payerRes = orderService.createOrderAllowNegative(payerOrder);
        if (payerRes.getCode() != 200) return Result.error(payerRes.getMessage());

        Order workerOrder = new Order();
        workerOrder.setBuyerId(request.getAcceptorId());
        workerOrder.setProductId(request.getId());
        workerOrder.setProductName("任务结算收入：" + taskTitle + "（管理员修改状态）");
        workerOrder.setAmount(budget);
        Result<String> workerRes = orderService.createOrder(workerOrder);
        if (workerRes.getCode() != 200) return Result.error(workerRes.getMessage());

        int rows = customRequestMapper.forceCompleteById(request.getId());
        if (rows <= 0) return Result.error("任务状态已变化，修改失败");
        return Result.success("已修改为完成并完成结算");
    }
}
