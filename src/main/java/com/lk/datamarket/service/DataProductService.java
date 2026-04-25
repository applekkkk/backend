package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.domain.ProductUserAction;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.domain.dto.ProductQueryRequest;
import com.lk.datamarket.mapper.DataProductMapper;
import com.lk.datamarket.mapper.ProductUserActionMapper;
import com.lk.datamarket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private ProductUserActionMapper productUserActionMapper;

    @Autowired
    private UserMapper userMapper;

    @PostConstruct
    public void initProductActionTable() {
        productUserActionMapper.ensureTable();
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

    public Result<String> approveProduct(Long id, Integer status) {
        dataProductMapper.updateReviewStatus(id, status);
        return Result.success(status == 1 ? "成功" : "失败");
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
            return Result.error("成功");
        }
        DataProduct product = dataProductMapper.findById(id);
        if (product == null) {
            return Result.error("该数据商品不存在");
        }

        ProductUserAction current = productUserActionMapper.findByProductAndUser(id, userId);
        int nextLiked = Boolean.TRUE.equals(liked) ? 1 : 0;
        int favorited = current == null ? 0 : safeInt(current.getFavorited());
        productUserActionMapper.upsert(id, userId, nextLiked, favorited);

        return Result.success(recalcAndAttach(product, userId));
    }

    public Result<DataProduct> setFavorite(Long id, Long userId, Boolean favorited) {
        if (id == null || userId == null) {
            return Result.error("无此用户");
        }
        DataProduct product = dataProductMapper.findById(id);
        if (product == null) {
            return Result.error("无此数据商品");
        }

        ProductUserAction current = productUserActionMapper.findByProductAndUser(id, userId);
        int nextFavorited = Boolean.TRUE.equals(favorited) ? 1 : 0;
        int liked = current == null ? 0 : safeInt(current.getLiked());
        productUserActionMapper.upsert(id, userId, liked, nextFavorited);

        return Result.success(recalcAndAttach(product, userId));
    }

    private DataProduct recalcAndAttach(DataProduct product, Long userId) {
        int likes = productUserActionMapper.countLikes(product.getId());
        int stars = productUserActionMapper.countFavorites(product.getId());

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
        if (products == null) return;
        for (DataProduct product : products) {
            enrichAuthorName(product);
        }
    }

    private void enrichAuthorName(DataProduct product) {
        if (product == null || product.getAuthorId() == null) return;
        User user = userMapper.findById(product.getAuthorId());
        if (user == null) return;
        String latestName = user.getName();
        if (latestName == null) return;
        String trimName = latestName.trim();
        if (trimName.isEmpty()) return;
        product.setAuthorName(trimName);
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
        ProductUserAction action = productUserActionMapper.findByProductAndUser(product.getId(), userId);
        if (action == null) {
            return;
        }
        product.setLiked(safeInt(action.getLiked()) == 1);
        product.setFavorited(safeInt(action.getFavorited()) == 1);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
