package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.domain.dto.ProductQueryRequest;
import com.lk.datamarket.service.DataProductService;
import com.lk.datamarket.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
    public Result<DataProduct> getProduct(@PathVariable Long id,
                                          @RequestParam(required = false) Long userId) {
        return dataProductService.getProductById(id, userId);
    }

    @PutMapping("/{id}/approve")
    public Result<String> approveProduct(@PathVariable Long id,
                                         @RequestParam Integer status,
                                         @RequestParam(required = false) String remark,
                                         HttpServletRequest request) {
        Long adminId = parseUserIdFromToken(request);
        log.info("Review product id: {}, status: {}, adminId: {}, remark: {}", id, status, adminId, remark);
        return dataProductService.approveProduct(id, status, adminId, remark);
    }

    @GetMapping("/user/{userId}")
    public Result<List<DataProduct>> getUserProducts(@PathVariable Long userId) {
        return dataProductService.getUserProducts(userId);
    }

    @GetMapping("/favorites/{userId}")
    public Result<List<DataProduct>> getFavoriteProducts(@PathVariable Long userId) {
        return dataProductService.getFavoriteProducts(userId);
    }

    @PutMapping("/{id}/stats")
    public Result<String> updateStats(@PathVariable Long id, @RequestBody DataProduct payload) {
        return dataProductService.updateStats(id, payload);
    }

    @PutMapping("/{id}/like")
    public Result<DataProduct> setLike(@PathVariable Long id,
                                       @RequestParam Long userId,
                                       @RequestParam Boolean liked) {
        return dataProductService.setLike(id, userId, liked);
    }

    @PutMapping("/{id}/favorite")
    public Result<DataProduct> setFavorite(@PathVariable Long id,
                                           @RequestParam Long userId,
                                           @RequestParam Boolean favorited) {
        return dataProductService.setFavorite(id, userId, favorited);
    }

    private Long parseUserIdFromToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = auth.substring(7).trim();
            Map<String, Object> claims = JwtUtil.parseToken(token);
            Object id = claims.get("id");
            if (id == null) {
                return null;
            }
            return Long.parseLong(String.valueOf(id));
        } catch (Exception ignored) {
            return null;
        }
    }
}
