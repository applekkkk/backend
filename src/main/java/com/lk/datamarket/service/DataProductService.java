package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
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

    public Result<List<DataProduct>> getApprovedProducts() {
        List<DataProduct> products = dataProductMapper.findApproved();
        return Result.success(products);
    }

    public Result<DataProduct> getProductById(Long id) {
        DataProduct product = dataProductMapper.findById(id);
        if (product == null) {
            return Result.error("数据集不存在");
        }
        return Result.success(product);
    }

    public Result<List<DataProduct>> getPendingProducts() {
        List<DataProduct> products = dataProductMapper.findPending();
        return Result.success(products);
    }

    public Result<String> createProduct(DataProduct product) {
        product.setUploadDate(LocalDate.now());
        product.setReviewStatus(0); // 待审核
        dataProductMapper.insert(product);
        return Result.success("提交成功，等待审核");
    }

    public Result<String> approveProduct(Long id, Integer status) {
        // status: 1=通过，2=拒绝
        dataProductMapper.updateReviewStatus(id, status);
        return Result.success(status == 1 ? "审核通过" : "已拒绝");
    }

    public Result<List<DataProduct>> getUserProducts(Long userId) {
        List<DataProduct> allProducts = dataProductMapper.findApproved();
        // 过滤出用户的产品
        return Result.success(allProducts);
    }
}
