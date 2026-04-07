package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.AiProcessRecord;
import com.lk.datamarket.service.AiProcessRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai-process-records")
public class AiProcessRecordController {
    @Autowired
    private AiProcessRecordService aiProcessRecordService;

    @PostMapping
    public Result<String> saveRecord(@RequestBody AiProcessRecord record) {
        log.info("save ai process record, orderNo={}, userId={}", record == null ? null : record.getOrderNo(), record == null ? null : record.getUserId());
        return aiProcessRecordService.saveRecord(record);
    }

    @GetMapping("/{orderNo}")
    public Result<AiProcessRecord> getByOrderNo(@PathVariable String orderNo, @RequestParam(required = false) Long userId) {
        return aiProcessRecordService.getByOrderNo(orderNo, userId);
    }

    @GetMapping("/user/{userId}")
    public Result<List<AiProcessRecord>> getUserRecords(@PathVariable Long userId) {
        return aiProcessRecordService.getUserRecords(userId);
    }
}

