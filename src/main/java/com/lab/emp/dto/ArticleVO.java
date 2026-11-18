package com.lab.emp.dto;

import com.lab.emp.entity.CmsArticle;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 文章展示对象：把分类名、作者名拼好返回，前端不需要再发请求补数据。
 */
public class ArticleVO {

    private static final Map<String, String> STATUS_TEXT = new HashMap<>();

    static {
        STATUS_TEXT.put("DRAFT", "草稿");
        STATUS_TEXT.put("PENDING", "待审核");
        STATUS_TEXT.put("PUBLISHED", "已发布");
        STATUS_TEXT.put("REJECTED", "已驳回");
        STATUS_TEXT.put("OFFLINE", "已下架");
    }

    private Long articleId;
    private String title;
    private String summary;

    /** 列表接口不返回正文，详情接口才填充，避免一次拉回大量 MEDIUMTEXT */
    private String content;

    private Integer categoryId;
    private String categoryName;
    private Integer authorId;
    private String authorName;
    private String status;
    private String statusText;
    private String auditRemark;
    private Integer viewCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public static ArticleVO from(CmsArticle a) {
        ArticleVO vo = new ArticleVO();
        vo.articleId = a.getArticleId();
        vo.title = a.getTitle();
        vo.summary = a.getSummary();
        vo.categoryId = a.getCategoryId();
        vo.authorId = a.getAuthorId();
        vo.status = a.getStatus();
        vo.statusText = STATUS_TEXT.getOrDefault(a.getStatus(), a.getStatus());
        vo.auditRemark = a.getAuditRemark();
        vo.viewCount = a.getViewCount();
        vo.publishTime = a.getPublishTime();
        vo.createTime = a.getCreateTime();
        vo.updateTime = a.getUpdateTime();
        return vo;
    }

    public static String statusText(String status) {
        return STATUS_TEXT.getOrDefault(status, status);
    }

    public Long getArticleId() { return articleId; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public String getAuditRemark() { return auditRemark; }
    public Integer getViewCount() { return viewCount; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}