package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.Order;
import com.lk.datamarket.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    @Autowired
    private OrderMapper orderMapper;

    public Result<List<Order>> getUserOrders(Long buyerId) {
        List<Order> orders = orderMapper.findByBuyerId(buyerId);
        return Result.success(orders);
    }

    public Result<List<Order>> getAllOrders() {
        List<Order> orders = orderMapper.findAll();
        return Result.success(orders);
    }

    public Result<String> createOrder(Order order) {
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setStatus(1); // 1=已完成
        orderMapper.insert(order);
        return Result.success("购买成功");
    }
}
