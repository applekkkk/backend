package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.service.DataProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class DataProductController {
    @Autowired
    private DataProductService dataProductService;

    @GetMapping("/approved")
    public Result<List<DataProduct>> getApprovedProducts() {
        return dataProductService.getApprovedProducts();
    }

    @GetMapping("/{id}")
    public Result<DataProduct> getProductById(@PathVariable Long id) {
        return dataProductService.getProductById(id);
    }

    @GetMapping("/pending")
    public Result<List<DataProduct>> getPendingProducts() {
        return dataProductService.getPendingProducts();
    }

    @PostMapping
    public Result<String> createProduct(@RequestBody DataProduct product) {
        log.info("创建数据产品：{}", product.getName());
        return dataProductService.createProduct(product);
    }

    @PutMapping("/{id}/approve")
    public Result<String> approveProduct(@PathVariable Long id, @RequestParam Integer status) {
        log.info("审核数据产品 ID: {}, 状态：{}", id, status);
        return dataProductService.approveProduct(id, status);
    }

    @GetMapping("/user/{userId}")
    public Result<List<DataProduct>> getUserProducts(@PathVariable Long userId) {
        return dataProductService.getUserProducts(userId);
    }
}
