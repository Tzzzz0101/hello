package com.lab.emp.controller;

import com.lab.emp.aspect.RequirePerm;
import com.lab.emp.common.Result;
import com.lab.emp.dto.DashboardVO;
import com.lab.emp.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制台统计接口。
 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/stats")
    @RequirePerm("dashboard:view")
    public Result<DashboardVO> stats() {
        return Result.success(dashboardService.stats());
    }
}