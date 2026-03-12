package com.lk.datamarket.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class ProductQueryRequest {
    @JsonAlias({"keyword", "search", "searchTerm", "q"})
    private String keyword;      // 搜索关键词（为空则不限制）

    @JsonAlias({"category", "type"})
    private String category;     // 分类（为空则不限制）

    @JsonAlias({"sortBy", "sort", "sortOrder", "order"})
    private String sortBy;       // 排序：recommend(默认), price-asc, price-desc, size-asc, size-desc

    private Integer pageNum = 1; // 页码，默认第 1 页
    private Integer pageSize = 9; // 每页数量，默认 9 条
}
