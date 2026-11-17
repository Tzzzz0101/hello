package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.emp.entity.Dept;
import com.lab.emp.mapper.DeptMapper;
import com.lab.emp.service.DeptService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门业务实现。
 */
@Service
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;

    public DeptServiceImpl(DeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @Override
    public List<Dept> listAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getDeptId));
    }
}