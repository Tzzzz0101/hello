package com.lab.emp.controller;

import com.lab.emp.common.Result;
import com.lab.emp.service.DeptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口（拦截器白名单），用于部署后快速确认容器、Spring、数据库链路是否贯通。
 */
@RestController
public class HealthController {

    private final DeptService deptService;

    public HealthController(DeptService deptService) {
        this.deptService = deptService;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("deptCount", deptService.listAll().size());
        data.put("timestamp", System.currentTimeMillis());
        return Result.success(data);
    }
}