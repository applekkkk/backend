package com.lk.datamarket.mapper;

import com.lk.datamarket.domain.CustomRequest;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CustomRequestMapper {
    @Select("SELECT * FROM custom_requests ORDER BY created_at DESC")
    List<CustomRequest> findAll();

    @Select("SELECT * FROM custom_requests WHERE need_status = 0 AND (publisher_id <> #{publisherId} OR #{publisherId} IS NULL) ORDER BY created_at DESC")
    List<CustomRequest> findMarket(@Param("publisherId") Long publisherId);

    @Select("SELECT * FROM custom_requests WHERE publisher_id = #{publisherId} ORDER BY created_at DESC")
    List<CustomRequest> findByPublisherId(@Param("publisherId") Long publisherId);

    @Select("SELECT * FROM custom_requests WHERE id = #{id}")
    CustomRequest findById(@Param("id") Long id);

    @Insert("INSERT INTO custom_requests(request_no, title, description, category, tags, amount, budget, " +
            "deadline, publisher_id, publisher_name, publisher_contact, attachment_name, need_status, created_at) " +
            "VALUES(#{requestNo}, #{title}, #{description}, #{category}, #{tags}, #{amount}, #{budget}, " +
            "#{deadline}, #{publisherId}, #{publisherName}, #{publisherContact}, #{attachmentName}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CustomRequest request);

    @Update("UPDATE custom_requests SET acceptor_id=#{acceptorId}, acceptor_name=#{acceptorName}, " +
            "need_status=#{needStatus}, updated_at=NOW() WHERE id=#{id}")
    int update(CustomRequest request);
}