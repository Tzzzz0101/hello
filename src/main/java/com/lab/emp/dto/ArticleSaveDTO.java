package com.lab.emp.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 文章新增 / 编辑入参。articleId 为空表示新增；status 由服务端按状态机决定，这里不接收。
 */
public class ArticleSaveDTO {

    private Long articleId;

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 120, message = "文章标题不能超过 120 个字符")
    private String title;

    @Size(max = 255, message = "摘要不能超过 255 个字符")
    private String summary;

    @NotBlank(message = "文章正文不能为空")
    private String content;

    @NotNull(message = "请选择所属分类")
    private Integer categoryId;

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
}