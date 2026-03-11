package com.lk.datamarket.service;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.CustomRequest;
import com.lk.datamarket.mapper.CustomRequestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomRequestService {
    @Autowired
    private CustomRequestMapper customRequestMapper;

    public Result<List<CustomRequest>> getAllRequests() {
        List<CustomRequest> requests = customRequestMapper.findAll();
        return Result.success(requests);
    }

    public Result<CustomRequest> getRequestById(Long id) {
        CustomRequest request = customRequestMapper.findById(id);
        if (request == null) {
            return Result.error("需求不存在");
        }
        return Result.success(request);
    }

    public Result<String> createRequest(CustomRequest request) {
        request.setRequestNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        request.setNeedStatus(0); // 0=待承接
        customRequestMapper.insert(request);
        return Result.success("发布成功");
    }

    public Result<String> acceptRequest(Long id, Long acceptorId, String acceptorName) {
        customRequestMapper.insert(new CustomRequest()); // 1=已承接
        return Result.success("承接成功");
    }

    public Result<List<CustomRequest>> getUserRequests(Long publisherId) {
        List<CustomRequest> allRequests = customRequestMapper.findAll();
        return Result.success(allRequests);
    }
}
