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
@RequestMapping("/orders")
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

    @GetMapping("/purchased")
    public Result<Boolean> hasPurchased(@RequestParam Long buyerId, @RequestParam Long productId) {
        return orderService.hasPurchased(buyerId, productId);
    }

    @GetMapping("/purchased-products/{buyerId}")
    public Result<List<Long>> getPurchasedProductIds(@PathVariable Long buyerId) {
        return orderService.getPurchasedProductIds(buyerId);
    }

    @PostMapping
    public Result<String> createOrder(@RequestBody Order order) {
        log.info("create order: {}", order.getProductName());
        return orderService.createOrder(order);
    }

    @PutMapping("/admin/purchase-status")
    public Result<String> adminSetPurchaseStatus(@RequestParam Long buyerId,
                                                 @RequestParam Long productId,
                                                 @RequestParam Boolean purchased) {
        log.info("admin set purchase status, buyerId={}, productId={}, purchased={}", buyerId, productId, purchased);
        return orderService.adminSetPurchaseStatus(buyerId, productId, purchased);
    }
}
