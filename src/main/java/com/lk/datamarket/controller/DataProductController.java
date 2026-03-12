package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.domain.dto.ProductQueryRequest;
import com.lk.datamarket.service.DataProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/products")
public class DataProductController {
    @Autowired
    private DataProductService dataProductService;

    @PostMapping("/query")
    public Result<Map<String, Object>> queryProducts(@RequestBody ProductQueryRequest request) {
        log.info("Query products: {}", request);
        return dataProductService.queryProducts(request);
    }

    @PostMapping
    public Result<String> createProduct(@RequestBody DataProduct product) {
        log.info("Create product: {}", product.getName());
        return dataProductService.createProduct(product);
    }

    @GetMapping("/pending")
    public Result<List<DataProduct>> getPendingReviews() {
        return dataProductService.getPendingReviews();
    }

    @GetMapping("/{id}")
    public Result<DataProduct> getProduct(@PathVariable Long id) {
        return dataProductService.getProductById(id);
    }

    @PutMapping("/{id}/approve")
    public Result<String> approveProduct(@PathVariable Long id, @RequestParam Integer status) {
        log.info("Review product id: {}, status: {}", id, status);
        return dataProductService.approveProduct(id, status);
    }

    @GetMapping("/user/{userId}")
    public Result<List<DataProduct>> getUserProducts(@PathVariable Long userId) {
        return dataProductService.getUserProducts(userId);
    }
}
