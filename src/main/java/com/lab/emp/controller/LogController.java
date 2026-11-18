package com.lab.emp.controller;

import com.lab.emp.aspect.RequirePerm;
import com.lab.emp.common.PageResult;
import com.lab.emp.common.Result;
import com.lab.emp.entity.SysLog;
import com.lab.emp.service.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询接口。
 */
@RestController
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/logs")
    @RequirePerm("log:list")
    public Result<PageResult<SysLog>> page(@RequestParam(required = false) String module,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.success(logService.pageQuery(module, username, keyword, page, size));
    }
}