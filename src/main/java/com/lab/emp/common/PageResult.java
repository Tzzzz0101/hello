package com.lab.emp.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果包装，屏蔽 MyBatis-Plus 的 IPage 细节，避免持久层对象直接暴露给前端。
 *
 * @param <T> 记录类型
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码（从 1 开始） */
    private long page;
    /** 每页条数 */
    private long size;
    /** 总记录数 */
    private long total;
    /** 总页数 */
    private long pages;
    /** 当前页数据 */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long page, long size, long total, long pages, List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.pages = pages;
        this.records = records;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getPages(), page.getRecords() == null ? Collections.emptyList() : page.getRecords());
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}