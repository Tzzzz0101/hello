package com.lab.emp.service;

import com.lab.emp.common.PageResult;
import com.lab.emp.dto.ArticleQueryDTO;
import com.lab.emp.dto.ArticleSaveDTO;
import com.lab.emp.dto.ArticleVO;
import com.lab.emp.dto.AuditDTO;
import com.lab.emp.entity.CmsArticle;

import java.util.List;

/**
 * 文章业务：草稿 → 提交审核 → 审核通过 / 驳回 → 发布 / 下架。
 */
public interface ArticleService {

    PageResult<ArticleVO> pageQuery(ArticleQueryDTO query);

    /** 详情，同时累加阅读量 */
    ArticleVO detail(Long articleId);

    CmsArticle save(ArticleSaveDTO dto, Integer authorId);

    CmsArticle update(ArticleSaveDTO dto, Integer operatorId);

    /** 提交审核：草稿或已驳回 → 待审核 */
    void submit(Long articleId);

    /** 审核：通过（PUBLISHED）或驳回（REJECTED，必须填意见） */
    void audit(Long articleId, AuditDTO dto);

    /** 批量发布：草稿 / 待审核直接发布，返回成功条数 */
    int batchPublish(List<Long> articleIds);

    /** 下架：已发布 → 已下架 */
    void offline(Long articleId);

    void delete(Long articleId);
}