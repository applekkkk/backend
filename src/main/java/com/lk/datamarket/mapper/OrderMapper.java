package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper {
    @Select("SELECT * FROM orders WHERE buyer_id = #{buyerId} ORDER BY created_at DESC")
    List<Order> findByBuyerId(Long buyerId);

    @Select("SELECT * FROM orders ORDER BY created_at DESC")
    List<Order> findAll();

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} LIMIT 1")
    Order findByOrderNo(String orderNo);

    @Select("SELECT COUNT(1) FROM orders " +
            "WHERE buyer_id = #{buyerId} AND product_id = #{productId} AND status = 1 " +
            "AND (product_name LIKE '\u8d2d\u4e70\u6570\u636e:%' OR product_name LIKE '\u7ba1\u7406\u5458\u6388\u6743\u8d2d\u4e70:%')")
    int countPurchasedByUserAndProduct(@Param("buyerId") Long buyerId, @Param("productId") Long productId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM orders " +
            "WHERE buyer_id = #{buyerId} AND product_id = #{productId} AND status = 1 " +
            "AND (product_name LIKE '\u8d2d\u4e70\u6570\u636e:%' OR product_name LIKE '\u7ba1\u7406\u5458\u6388\u6743\u8d2d\u4e70:%')")
    Integer sumActivePurchaseAmountByUserAndProduct(@Param("buyerId") Long buyerId, @Param("productId") Long productId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM orders " +
            "WHERE buyer_id = #{sellerId} AND product_id = #{productId} AND status = 1 " +
            "AND product_name LIKE CONCAT('\u6570\u636e\u9500\u552e\u6536\u5165:%[buyerId=', #{purchaseBuyerId}, ']%')")
    Integer sumActiveSaleIncomeBySellerAndProduct(@Param("sellerId") Long sellerId,
                                                   @Param("productId") Long productId,
                                                   @Param("purchaseBuyerId") Long purchaseBuyerId);

    @Insert("INSERT INTO orders(order_no, buyer_id, product_id, product_name, amount, status, created_at) " +
            "VALUES(#{orderNo}, #{buyerId}, #{productId}, #{productName}, #{amount}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("UPDATE orders SET status = 0 " +
            "WHERE buyer_id = #{buyerId} AND product_id = #{productId} AND status = 1 " +
            "AND (product_name LIKE '\u8d2d\u4e70\u6570\u636e:%' OR product_name LIKE '\u7ba1\u7406\u5458\u6388\u6743\u8d2d\u4e70:%')")
    int deactivatePurchaseByUserAndProduct(@Param("buyerId") Long buyerId, @Param("productId") Long productId);

    @Update("UPDATE orders SET status = 0 " +
            "WHERE buyer_id = #{sellerId} AND product_id = #{productId} AND status = 1 " +
            "AND product_name LIKE CONCAT('\u6570\u636e\u9500\u552e\u6536\u5165:%[buyerId=', #{purchaseBuyerId}, ']%')")
    int deactivateSaleIncomeBySellerAndProduct(@Param("sellerId") Long sellerId,
                                                @Param("productId") Long productId,
                                                @Param("purchaseBuyerId") Long purchaseBuyerId);
}
