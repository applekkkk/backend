package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.Order;
import com.lk.datamarket.domain.User;
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
}
