package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.ProductUserAction;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProductUserActionMapper {
    @Update("CREATE TABLE IF NOT EXISTS product_user_actions (" +
            "product_id BIGINT NOT NULL," +
            "user_id BIGINT NOT NULL," +
            "liked TINYINT(1) NOT NULL DEFAULT 0," +
            "favorited TINYINT(1) NOT NULL DEFAULT 0," +
            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "PRIMARY KEY(product_id, user_id)," +
            "INDEX idx_user (user_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
    int ensureTable();

    @Select("SELECT * FROM product_user_actions WHERE product_id=#{productId} AND user_id=#{userId}")
    ProductUserAction findByProductAndUser(@Param("productId") Long productId, @Param("userId") Long userId);

    @Insert("INSERT INTO product_user_actions(product_id, user_id, liked, favorited) " +
            "VALUES(#{productId}, #{userId}, #{liked}, #{favorited}) " +
            "ON DUPLICATE KEY UPDATE liked=VALUES(liked), favorited=VALUES(favorited), updated_at=NOW()")
    int upsert(@Param("productId") Long productId,
               @Param("userId") Long userId,
               @Param("liked") Integer liked,
               @Param("favorited") Integer favorited);

    @Select("SELECT COUNT(*) FROM product_user_actions WHERE product_id=#{productId} AND liked=1")
    int countLikes(@Param("productId") Long productId);

    @Select("SELECT COUNT(*) FROM product_user_actions WHERE product_id=#{productId} AND favorited=1")
    int countFavorites(@Param("productId") Long productId);
}
