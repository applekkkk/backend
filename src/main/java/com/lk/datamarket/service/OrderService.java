package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.domain.Order;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.mapper.DataProductMapper;
import com.lk.datamarket.mapper.OrderMapper;
import com.lk.datamarket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DataProductMapper dataProductMapper;

    public Result<List<Order>> getUserOrders(Long buyerId) {
        List<Order> orders = orderMapper.findByBuyerId(buyerId);
        return Result.success(orders);
    }

    public Result<List<Order>> getAllOrders() {
        List<Order> orders = orderMapper.findAll();
        return Result.success(orders);
    }

    public Result<String> createOrder(Order order) {
        return createOrderInternal(order, false);
    }

    public Result<String> createOrderAllowNegative(Order order) {
        return createOrderInternal(order, true);
    }

    private Result<String> createOrderInternal(Order order, boolean allowNegative) {
        if (order == null || order.getBuyerId() == null) {
            return Result.error("参数错误");
        }
        User user = userMapper.findById(order.getBuyerId());
        if (user == null) {
            return Result.error("用户不存在");
        }

        int amount = order.getAmount() == null ? 0 : order.getAmount();
        int current = user.getPoints() == null ? 0 : user.getPoints();
        int next = current + amount;
        if (!allowNegative && next < 0) {
            return Result.error("积分不足");
        }

        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setStatus(1);
        orderMapper.insert(order);

        user.setPoints(next);
        userMapper.update(user);
        return Result.success("交易成功");
    }

    public Result<String> adminSetPurchaseStatus(Long buyerId, Long productId, Boolean purchased) {
        if (buyerId == null || productId == null || purchased == null) {
            return Result.error("参数错误");
        }

        User user = userMapper.findById(buyerId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        DataProduct product = dataProductMapper.findById(productId);
        if (product == null) {
            return Result.error("数据不存在");
        }

        if (Boolean.TRUE.equals(purchased)) {
            int existed = orderMapper.countPurchasedByUserAndProduct(buyerId, productId);
            if (existed > 0 || (product.getAuthorId() != null && product.getAuthorId().equals(buyerId))) {
                return Result.success("状态未变化");
            }
            Order grantOrder = new Order();
            grantOrder.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            grantOrder.setBuyerId(buyerId);
            grantOrder.setProductId(productId);
            grantOrder.setProductName("管理员授权购买:" + (product.getName() == null ? "数据" : product.getName()));
            grantOrder.setAmount(0);
            grantOrder.setStatus(1);
            orderMapper.insert(grantOrder);
            return Result.success("已修改为已购买");
        }

        int affected = orderMapper.deactivatePurchaseByUserAndProduct(buyerId, productId);
        if (affected <= 0) {
            return Result.success("状态未变化");
        }
        return Result.success("已修改为未购买");
    }
}
