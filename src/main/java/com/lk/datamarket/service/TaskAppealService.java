package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.CustomRequest;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.domain.Order;
import com.lk.datamarket.domain.TaskAppeal;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.mapper.CustomRequestMapper;
import com.lk.datamarket.mapper.DataProductMapper;
import com.lk.datamarket.mapper.OrderMapper;
import com.lk.datamarket.mapper.TaskAppealMapper;
import com.lk.datamarket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TaskAppealService {
    @Autowired
    private TaskAppealMapper taskAppealMapper;
    @Autowired
    private CustomRequestMapper customRequestMapper;
    @Autowired
    private DataProductMapper dataProductMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderService orderService;

    @PostConstruct
    public void ensureTable() {
        taskAppealMapper.ensureTable();
    }

    public Result<String> createAppeal(TaskAppeal appeal) {
        if (appeal == null || appeal.getRequestId() == null || appeal.getAppellantId() == null) {
            return Result.error("参数错误");
        }
        String claimText = appeal.getClaimText() == null ? "" : appeal.getClaimText().trim();
        if (claimText.isEmpty()) {
            return Result.error("请填写诉求内容");
        }

        String targetType = appeal.getTargetType() == null ? "TASK" : appeal.getTargetType().trim().toUpperCase();
        if ("DATA".equals(targetType)) {
            return createDataAppeal(appeal, claimText);
        }
        return createTaskAppeal(appeal, claimText);
    }

    private Result<String> createTaskAppeal(TaskAppeal appeal, String claimText) {
        CustomRequest request = customRequestMapper.findById(appeal.getRequestId());
        if (request == null) {
            return Result.error("任务不存在");
        }
        int needStatus = request.getNeedStatus() == null ? 0 : request.getNeedStatus();
        if (needStatus != 1 && needStatus != 2) {
            return Result.error("当前任务状态不可申诉");
        }

        Long userId = appeal.getAppellantId();
        boolean isPublisher = request.getPublisherId() != null && request.getPublisherId().equals(userId);
        boolean isAcceptor = request.getAcceptorId() != null && request.getAcceptorId().equals(userId);
        if (!isPublisher && !isAcceptor) {
            return Result.error("仅任务参与方可申诉");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        appeal.setRequestTitle(request.getTitle() == null ? "" : request.getTitle());
        appeal.setAppellantName(resolveUserName(user));
        appeal.setAppellantRole(isPublisher ? "发布方" : "承接方");
        appeal.setClaimText(claimText);
        appeal.setEvidenceText(appeal.getEvidenceText() == null ? "" : appeal.getEvidenceText().trim());
        appeal.setEvidenceImage(appeal.getEvidenceImage() == null ? "" : appeal.getEvidenceImage().trim());
        appeal.setStatus(0);
        taskAppealMapper.insert(appeal);
        return Result.success("申诉已提交");
    }

    private Result<String> createDataAppeal(TaskAppeal appeal, String claimText) {
        DataProduct product = dataProductMapper.findById(appeal.getRequestId());
        if (product == null) {
            return Result.error("数据不存在");
        }

        Long userId = appeal.getAppellantId();
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        boolean isAuthor = product.getAuthorId() != null && product.getAuthorId().equals(userId);
        boolean isPurchased = orderMapper.countPurchasedByUserAndProduct(userId, product.getId()) > 0;
        if (!isAuthor && !isPurchased) {
            return Result.error("仅已购买用户可申诉");
        }

        appeal.setRequestTitle(product.getName() == null ? "数据" : product.getName());
        appeal.setAppellantName(resolveUserName(user));
        appeal.setAppellantRole("购买方");
        appeal.setClaimText(claimText);
        appeal.setEvidenceText(appeal.getEvidenceText() == null ? "" : appeal.getEvidenceText().trim());
        appeal.setEvidenceImage(appeal.getEvidenceImage() == null ? "" : appeal.getEvidenceImage().trim());
        appeal.setStatus(0);
        taskAppealMapper.insert(appeal);
        return Result.success("申诉已提交");
    }

    public Result<List<TaskAppeal>> getUserAppeals(Long userId) {
        if (userId == null) {
            return Result.error("参数错误");
        }
        return Result.success(taskAppealMapper.findByAppellantId(userId));
    }

    public Result<List<TaskAppeal>> getAllAppeals() {
        return Result.success(taskAppealMapper.findAll());
    }

    public Result<String> markProcessed(Long appealId) {
        TaskAppeal appeal = taskAppealMapper.findById(appealId);
        if (appeal == null) {
            return Result.error("申诉不存在");
        }
        int status = appeal.getStatus() == null ? 0 : appeal.getStatus();
        if (status == 1) {
            return Result.success("已处理");
        }
        taskAppealMapper.updateStatus(appealId, 1);
        return Result.success("处理完成");
    }

    public Result<String> forceSettle(Long appealId) {
        TaskAppeal appeal = taskAppealMapper.findById(appealId);
        if (appeal == null) {
            return Result.error("申诉不存在");
        }
        CustomRequest request = customRequestMapper.findById(appeal.getRequestId());
        if (request == null) {
            return Result.error("任务不存在");
        }
        int needStatus = request.getNeedStatus() == null ? 0 : request.getNeedStatus();
        if (needStatus == 3) {
            taskAppealMapper.updateStatus(appealId, 1);
            return Result.success("任务已是完成状态");
        }
        if (needStatus != 1 && needStatus != 2) {
            return Result.error("当前任务状态不可强制结算");
        }
        if (request.getPublisherId() == null || request.getAcceptorId() == null) {
            return Result.error("任务缺少发布方或承接方，无法强制结算");
        }

        int budget = request.getBudget() == null ? 0 : Math.max(0, request.getBudget());
        String taskTitle = request.getTitle() == null ? "任务" : request.getTitle();

        Order payerOrder = new Order();
        payerOrder.setBuyerId(request.getPublisherId());
        payerOrder.setProductId(request.getId());
        payerOrder.setProductName("任务结算支出:" + taskTitle + "(管理员强制)");
        payerOrder.setAmount(-budget);
        Result<String> payerResult = orderService.createOrderAllowNegative(payerOrder);
        if (payerResult.getCode() != 200) {
            return Result.error(payerResult.getMessage());
        }

        Order workerOrder = new Order();
        workerOrder.setBuyerId(request.getAcceptorId());
        workerOrder.setProductId(request.getId());
        workerOrder.setProductName("任务结算收入:" + taskTitle + "(管理员强制)");
        workerOrder.setAmount(budget);
        Result<String> workerResult = orderService.createOrder(workerOrder);
        if (workerResult.getCode() != 200) {
            return Result.error(workerResult.getMessage());
        }

        int updated = customRequestMapper.forceCompleteById(request.getId());
        if (updated <= 0) {
            return Result.error("任务状态已变化，强制结算失败");
        }
        taskAppealMapper.updateStatus(appealId, 1);
        return Result.success("已强制结算并完成任务");
    }

    public Result<String> forceRelease(Long appealId) {
        TaskAppeal appeal = taskAppealMapper.findById(appealId);
        if (appeal == null) {
            return Result.error("申诉不存在");
        }
        CustomRequest request = customRequestMapper.findById(appeal.getRequestId());
        if (request == null) {
            return Result.error("任务不存在");
        }
        int needStatus = request.getNeedStatus() == null ? 0 : request.getNeedStatus();
        if (needStatus == 0) {
            taskAppealMapper.updateStatus(appealId, 1);
            return Result.success("任务已是未承接状态");
        }
        if (needStatus != 1 && needStatus != 2) {
            return Result.error("当前任务状态不可强制释放");
        }

        int updated = customRequestMapper.forceReleaseById(request.getId());
        if (updated <= 0) {
            return Result.error("任务状态已变化，强制释放失败");
        }
        taskAppealMapper.updateStatus(appealId, 1);
        return Result.success("已强制释放任务，恢复为未承接");
    }

    private String resolveUserName(User user) {
        String name = user.getName();
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return user.getUsername();
    }
}
