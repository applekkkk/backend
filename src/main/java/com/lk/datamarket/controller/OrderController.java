package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.Order;
import com.lk.datamarket.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/user/{userId}")
    public Result<List<Order>> getUserOrders(@PathVariable Long userId) {
        return orderService.getUserOrders(userId);
    }

    @GetMapping("/all")
    public Result<List<Order>> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PostMapping
    public Result<String> createOrder(@RequestBody Order order) {
        log.info("创建订单：{}", order.getProductName());
        return orderService.createOrder(order);
    }
}
