package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.Order;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderMapper {
    @Select("SELECT * FROM `order` WHERE buyer_id = #{buyerId} ORDER BY created_at DESC")
    List<Order> findByBuyerId(Long buyerId);

    @Select("SELECT * FROM `order` ORDER BY created_at DESC")
    List<Order> findAll();

    @Insert("INSERT INTO `order`(order_no, buyer_id, product_id, product_name, amount, status, created_at) " +
            "VALUES(#{orderNo}, #{buyerId}, #{productId}, #{productName}, #{amount}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);
}
