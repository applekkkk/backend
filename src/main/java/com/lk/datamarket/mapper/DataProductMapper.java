package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.DataProduct;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DataProductMapper {
    List<DataProduct> findByCondition(@Param("keyword") String keyword,
                                      @Param("category") String category,
                                      @Param("sortBy") String sortBy,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);

    int countByCondition(@Param("keyword") String keyword,
                         @Param("category") String category);

    @Select("SELECT * FROM data_products WHERE review_status = 1 AND author_id = #{authorId} ORDER BY created_at DESC")
    List<DataProduct> findApprovedByAuthorId(@Param("authorId") Long authorId);

    @Select("SELECT p.* FROM data_products p INNER JOIN product_user_actions a ON a.product_id = p.id " +
            "WHERE p.review_status = 1 AND a.user_id = #{userId} AND a.favorited = 1 ORDER BY a.updated_at DESC")
    List<DataProduct> findFavoritedByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM data_products WHERE review_status = 0 ORDER BY created_at DESC")
    List<DataProduct> findPendingReviews();

    @Select("SELECT * FROM data_products WHERE id = #{id}")
    DataProduct findById(@Param("id") Long id);

    @Select("SELECT * FROM data_products WHERE file_name = #{fileName} LIMIT 1")
    DataProduct findByFileName(@Param("fileName") String fileName);

    @Insert("INSERT INTO data_products(name, info, category, tags, price, size_label, seller, " +
            "author_id, author_name, file_name, summary, review_status, upload_date, created_at) " +
            "VALUES(#{name}, #{info}, #{category}, #{tags}, #{price}, #{sizeLabel}, #{seller}, " +
            "#{authorId}, #{authorName}, #{fileName}, #{summary}, 0, #{uploadDate}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataProduct product);

    @Update("UPDATE data_products SET review_status=#{reviewStatus}, updated_at=NOW() WHERE id=#{id}")
    int updateReviewStatus(@Param("id") Long id, @Param("reviewStatus") Integer reviewStatus);

    @Update("UPDATE data_products SET likes=#{likes}, stars=#{stars}, downloads=#{downloads} WHERE id=#{id}")
    int updateStats(DataProduct product);
}
