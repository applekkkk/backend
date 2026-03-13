package com.lk.datamarket.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class ProductQueryRequest {
    @JsonAlias({"keyword", "search", "searchTerm", "q"})
    private String keyword;

    @JsonAlias({"category", "type"})
    private String category;

    @JsonAlias({"sortBy", "sort", "sortOrder", "order"})
    private String sortBy;

    private Long userId;

    private Integer pageNum = 1;
    private Integer pageSize = 9;
}
