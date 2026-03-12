package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.domain.dto.ProductQueryRequest;
import com.lk.datamarket.mapper.DataProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataProductService {
    @Autowired
    private DataProductMapper dataProductMapper;

    public Result<Map<String, Object>> queryProducts(ProductQueryRequest request) {
        if (request == null) {
            request = new ProductQueryRequest();
        }
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(9);
        }
        int offset = (request.getPageNum() - 1) * request.getPageSize();
        int limit = request.getPageSize();

        List<DataProduct> products = dataProductMapper.findByCondition(
            request.getKeyword(),
            request.getCategory(),
            request.getSortBy(),
            offset,
            limit
        );

        int total = dataProductMapper.countByCondition(
            request.getKeyword(),
            request.getCategory()
        );

        int totalPages = (int) Math.ceil((double) total / request.getPageSize());

        Map<String, Object> result = new HashMap<>();
        result.put("list", products);
        result.put("total", total);
        result.put("pageNum", request.getPageNum());
        result.put("pageSize", request.getPageSize());
        result.put("totalPages", totalPages);

        return Result.success(result);
    }

    public Result<String> createProduct(DataProduct product) {
        product.setUploadDate(LocalDate.now());
        product.setReviewStatus(0);
        dataProductMapper.insert(product);
        return Result.success("提交成功，等待审核");
    }

    public Result<String> approveProduct(Long id, Integer status) {
        // status: 1=approve, 2=reject
        dataProductMapper.updateReviewStatus(id, status);
        return Result.success(status == 1 ? "审核通过" : "已驳回");
    }

    public Result<List<DataProduct>> getPendingReviews() {
        List<DataProduct> products = dataProductMapper.findPendingReviews();
        return Result.success(products);
    }

    public Result<DataProduct> getProductById(Long id) {
        DataProduct product = dataProductMapper.findById(id);
        if (product == null) {
            return Result.error("数据不存在");
        }
        return Result.success(product);
    }

    public Result<List<DataProduct>> getUserProducts(Long userId) {
        List<DataProduct> products = dataProductMapper.findApprovedByAuthorId(userId);
        return Result.success(products);
    }
}
