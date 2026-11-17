package com.lab.emp.service;

import com.lab.emp.entity.Dept;

import java.util.List;

/**
 * 部门业务接口。
 */
public interface DeptService {

    /** 全部部门（用于前端下拉框） */
    List<Dept> listAll();
}