package com.lab.emp.controller;

import com.lab.emp.common.Result;
import com.lab.emp.entity.Dept;
import com.lab.emp.service.DeptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门接口：为前端下拉框提供数据。
 */
@RestController
@RequestMapping("/depts")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    /** GET /depts */
    @GetMapping
    public Result<List<Dept>> list() {
        return Result.success(deptService.listAll());
    }
}