package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.common.PageResult;
import com.lab.emp.entity.SysLog;
import com.lab.emp.mapper.SysLogMapper;
import com.lab.emp.service.LogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志实现。
 */
@Service
public class LogServiceImpl implements LogService {

    private final SysLogMapper sysLogMapper;

    public LogServiceImpl(SysLogMapper sysLogMapper) {
        this.sysLogMapper = sysLogMapper;
    }

    @Override
    public void save(SysLog sysLog) {
        sysLogMapper.insert(sysLog);
    }

    @Override
    public PageResult<SysLog> pageQuery(String module, String username, String keyword, int page, int size) {
        int current = Math.max(page, 1);
        int pageSize = size < 1 ? 10 : Math.min(size, 100);

        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(module != null && !module.isEmpty(), SysLog::getModule, module)
                .like(username != null && !username.isEmpty(), SysLog::getUsername, username)
                .and(keyword != null && !keyword.isEmpty(),
                        w -> w.like(SysLog::getOperation, keyword)
                                .or().like(SysLog::getParams, keyword))
                .orderByDesc(SysLog::getLogId);

        return PageResult.of(sysLogMapper.selectPage(new Page<>(current, pageSize), wrapper));
    }
}