package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.Order;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderMapper {
    @Select("SELECT * FROM orders WHERE buyer_id = #{buyerId} ORDER BY created_at DESC")
    List<Order> findByBuyerId(Long buyerId);

    @Select("SELECT * FROM orders ORDER BY created_at DESC")
    List<Order> findAll();

    @Select("SELECT COUNT(1) FROM orders WHERE buyer_id = #{buyerId} AND product_id = #{productId} AND amount < 0")
    int countPurchasedByUserAndProduct(@Param("buyerId") Long buyerId, @Param("productId") Long productId);

    @Insert("INSERT INTO orders(order_no, buyer_id, product_id, product_name, amount, status, created_at) " +
            "VALUES(#{orderNo}, #{buyerId}, #{productId}, #{productName}, #{amount}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);
}
