package com.lab.emp.service;

import com.lab.emp.common.PageResult;
import com.lab.emp.entity.SysLog;

/**
 * 操作日志查询与写入。
 */
public interface LogService {

    /** 切面写入日志 */
    void save(SysLog sysLog);

    /** 分页查询，支持按模块、操作人、关键字过滤 */
    PageResult<SysLog> pageQuery(String module, String username, String keyword, int page, int size);
}