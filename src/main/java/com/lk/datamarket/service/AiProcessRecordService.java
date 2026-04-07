package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.AiProcessRecord;
import com.lk.datamarket.domain.Order;
import com.lk.datamarket.mapper.AiProcessRecordMapper;
import com.lk.datamarket.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Service
public class AiProcessRecordService {
    @Autowired
    private AiProcessRecordMapper aiProcessRecordMapper;

    @Autowired
    private OrderMapper orderMapper;

    @PostConstruct
    public void ensureTable() {
        aiProcessRecordMapper.ensureTable();
    }

    public Result<String> saveRecord(AiProcessRecord record) {
        if (record == null || !StringUtils.hasText(record.getOrderNo()) || record.getUserId() == null) {
            return Result.error("参数错误");
        }

        String orderNo = record.getOrderNo().trim();
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getBuyerId() == null || !order.getBuyerId().equals(record.getUserId())) {
            return Result.error("订单与用户不匹配");
        }

        record.setOrderNo(orderNo);
        if (record.getSourceFileName() == null) record.setSourceFileName("");
        if (record.getInstruction() == null) record.setInstruction("");
        if (record.getReportMarkdown() == null) record.setReportMarkdown("");
        if (record.getPreviewJson() == null) record.setPreviewJson("");
        if (record.getResultFileName() == null) record.setResultFileName("");

        aiProcessRecordMapper.upsert(record);
        return Result.success("ok");
    }

    public Result<AiProcessRecord> getByOrderNo(String orderNo, Long userId) {
        if (!StringUtils.hasText(orderNo)) {
            return Result.error("参数错误");
        }
        AiProcessRecord record = aiProcessRecordMapper.findByOrderNo(orderNo.trim());
        if (record == null) {
            return Result.error("未找到处理记录");
        }
        if (userId != null && record.getUserId() != null && !record.getUserId().equals(userId)) {
            return Result.error("无权限查看");
        }
        return Result.success(record);
    }

    public Result<List<AiProcessRecord>> getUserRecords(Long userId) {
        if (userId == null) {
            return Result.error("参数错误");
        }
        List<AiProcessRecord> list = aiProcessRecordMapper.findByUserId(userId);
        return Result.success(list == null ? Collections.emptyList() : list);
    }
}

