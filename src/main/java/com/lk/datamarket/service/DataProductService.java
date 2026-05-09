package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.domain.ReviewLog;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.domain.dto.ProductQueryRequest;
import com.lk.datamarket.mapper.DataProductMapper;
import com.lk.datamarket.mapper.ProductFavoriteMapper;
import com.lk.datamarket.mapper.ProductLikeMapper;
import com.lk.datamarket.mapper.ReviewLogMapper;
import com.lk.datamarket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataProductService {
    @Autowired
    private DataProductMapper dataProductMapper;

    @Autowired
    private ProductLikeMapper productLikeMapper;

    @Autowired
    private ProductFavoriteMapper productFavoriteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReviewLogMapper reviewLogMapper;

    @Autowired
    private AdminAttendanceService adminAttendanceService;

    @PostConstruct
    public void initProductActionTable() {
        productLikeMapper.ensureTable();
        productFavoriteMapper.ensureTable();
        reviewLogMapper.ensureTable();
    }

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

        enrichAuthorNames(products);
        enrichUserActions(products, request.getUserId());

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
        return Result.success("创建成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> approveProduct(Long id, Integer status, Long adminId, String remark) {
        if (id == null || status == null) {
            return Result.error("参数不能为空");
        }
        if (adminId == null) {
            return Result.error("管理员身份无效");
        }
        if (status != 0 && status != 1 && status != 2) {
            return Result.error("审核状态无效");
        }

        DataProduct product = dataProductMapper.findById(id);
        if (product == null) {
            return Result.error("数据不存在");
        }

        Integer oldStatus = product.getReviewStatus() == null ? 0 : product.getReviewStatus();
        int updated = dataProductMapper.updateReviewStatus(id, status);
        if (updated <= 0) {
            return Result.error("审核失败");
        }

        ReviewLog reviewLog = new ReviewLog();
        reviewLog.setProductId(id);
        reviewLog.setAdminId(adminId);
        reviewLog.setOldStatus(oldStatus);
        reviewLog.setNewStatus(status);
        reviewLog.setRemark(StringUtils.hasText(remark) ? remark.trim() : "");
        reviewLogMapper.insert(reviewLog);

        adminAttendanceService.recordReview(adminId);

        return Result.success(status == 1 ? "审核通过" : "审核完成");
    }

    public Result<List<DataProduct>> getPendingReviews() {
        List<DataProduct> products = dataProductMapper.findPendingReviews();
        enrichAuthorNames(products);
        return Result.success(products);
    }

    public Result<DataProduct> getProductById(Long id, Long userId) {
        DataProduct product = dataProductMapper.findById(id);
        if (product == null) {
            return Result.error("无此数据商品");
        }
        enrichAuthorName(product);
        enrichUserAction(product, userId);
        return Result.success(product);
    }

    public Result<List<DataProduct>> getUserProducts(Long userId) {
        List<DataProduct> products = dataProductMapper.findApprovedByAuthorId(userId);
        enrichAuthorNames(products);
        enrichUserActions(products, userId);
        return Result.success(products);
    }

    public Result<List<DataProduct>> getFavoriteProducts(Long userId) {
        List<DataProduct> products = dataProductMapper.findFavoritedByUserId(userId);
        enrichAuthorNames(products);
        enrichUserActions(products, userId);
        return Result.success(products);
    }

    public Result<String> updateStats(Long id, DataProduct payload) {
        DataProduct existing = dataProductMapper.findById(id);
        if (existing == null) {
            return Result.error("该数据商品不存在");
        }
        DataProduct update = new DataProduct();
        update.setId(id);
        update.setLikes(payload.getLikes() == null ? safeInt(existing.getLikes()) : payload.getLikes());
        update.setStars(payload.getStars() == null ? safeInt(existing.getStars()) : payload.getStars());
        update.setDownloads(payload.getDownloads() == null ? safeInt(existing.getDownloads()) : payload.getDownloads());
        dataProductMapper.updateStats(update);
        return Result.success("成功");
    }

    public Result<DataProduct> setLike(Long id, Long userId, Boolean liked) {
        if (id == null || userId == null) {
            return Result.error("参数错误");
        }
        DataProduct product = dataProductMapper.findById(id);
        if (product == null) {
            return Result.error("该数据商品不存在");
        }

        if (Boolean.TRUE.equals(liked)) {
            productLikeMapper.insert(id, userId);
        } else {
            productLikeMapper.delete(id, userId);
        }

        return Result.success(recalcAndAttach(product, userId));
    }

    public Result<DataProduct> setFavorite(Long id, Long userId, Boolean favorited) {
        if (id == null || userId == null) {
            return Result.error("参数错误");
        }
        DataProduct product = dataProductMapper.findById(id);
        if (product == null) {
            return Result.error("无此数据商品");
        }

        if (Boolean.TRUE.equals(favorited)) {
            productFavoriteMapper.insert(id, userId);
        } else {
            productFavoriteMapper.delete(id, userId);
        }

        return Result.success(recalcAndAttach(product, userId));
    }

    private DataProduct recalcAndAttach(DataProduct product, Long userId) {
        int likes = productLikeMapper.countLikes(product.getId());
        int stars = productFavoriteMapper.countFavorites(product.getId());

        DataProduct update = new DataProduct();
        update.setId(product.getId());
        update.setLikes(likes);
        update.setStars(stars);
        update.setDownloads(safeInt(product.getDownloads()));
        dataProductMapper.updateStats(update);

        product.setLikes(likes);
        product.setStars(stars);
        enrichAuthorName(product);
        enrichUserAction(product, userId);
        return product;
    }

    private void enrichAuthorNames(List<DataProduct> products) {
        if (products == null) {
            return;
        }
        for (DataProduct product : products) {
            enrichAuthorName(product);
        }
    }

    private void enrichAuthorName(DataProduct product) {
        if (product == null || product.getAuthorId() == null) {
            return;
        }
        User user = userMapper.findById(product.getAuthorId());
        if (user == null) {
            return;
        }
        String latestName = user.getName();
        if (!StringUtils.hasText(latestName)) {
            return;
        }
        product.setAuthorName(latestName.trim());
    }

    private void enrichUserActions(List<DataProduct> products, Long userId) {
        if (products == null) {
            return;
        }
        for (DataProduct product : products) {
            enrichUserAction(product, userId);
        }
    }

    private void enrichUserAction(DataProduct product, Long userId) {
        if (product == null) {
            return;
        }
        product.setLiked(false);
        product.setFavorited(false);
        if (userId == null) {
            return;
        }
        product.setLiked(productLikeMapper.exists(product.getId(), userId) > 0);
        product.setFavorited(productFavoriteMapper.exists(product.getId(), userId) > 0);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
