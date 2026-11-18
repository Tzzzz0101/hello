package com.lab.emp.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 文章列表查询条件：标题模糊、分类、状态、作者过滤 + 分页 + 排序字段白名单。
 */
public class ArticleQueryDTO {

    private String title;
    private Integer categoryId;
    private String status;
    private Integer authorId;

    @Min(value = 1, message = "页码必须大于 0")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = 100, message = "每页最多 100 条")
    private Integer size = 10;

    /** 排序字段：publishTime / createTime / viewCount，非白名单值回退到 createTime */
    private String orderBy = "createTime";

    /** asc / desc */
    private String orderDir = "desc";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAuthorId() { return authorId; }
    public void setAuthorId(Integer authorId) { this.authorId = authorId; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getOrderBy() { return orderBy; }
    public void setOrderBy(String orderBy) { this.orderBy = orderBy; }

    public String getOrderDir() { return orderDir; }
    public void setOrderDir(String orderDir) { this.orderDir = orderDir; }
}