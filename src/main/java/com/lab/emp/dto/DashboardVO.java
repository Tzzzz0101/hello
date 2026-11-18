package com.lab.emp.dto;

import java.util.List;
import java.util.Map;

/**
 * 控制台统计：文章状态分布、分类文章数、用户数与总阅读量。
 */
public class DashboardVO {

    private long totalArticles;
    private long published;
    private long pending;
    private long draft;
    private long rejected;
    private long offline;
    private long totalCategories;
    private long totalUsers;
    private long totalViews;
    private List<Map<String, Object>> byCategory;

    public long getTotalArticles() { return totalArticles; }
    public void setTotalArticles(long totalArticles) { this.totalArticles = totalArticles; }

    public long getPublished() { return published; }
    public void setPublished(long published) { this.published = published; }

    public long getPending() { return pending; }
    public void setPending(long pending) { this.pending = pending; }

    public long getDraft() { return draft; }
    public void setDraft(long draft) { this.draft = draft; }

    public long getRejected() { return rejected; }
    public void setRejected(long rejected) { this.rejected = rejected; }

    public long getOffline() { return offline; }
    public void setOffline(long offline) { this.offline = offline; }

    public long getTotalCategories() { return totalCategories; }
    public void setTotalCategories(long totalCategories) { this.totalCategories = totalCategories; }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalViews() { return totalViews; }
    public void setTotalViews(long totalViews) { this.totalViews = totalViews; }

    public List<Map<String, Object>> getByCategory() { return byCategory; }
    public void setByCategory(List<Map<String, Object>> byCategory) { this.byCategory = byCategory; }
}