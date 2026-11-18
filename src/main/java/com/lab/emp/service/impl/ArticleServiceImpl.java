package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.common.BusinessException;
import com.lab.emp.common.PageResult;
import com.lab.emp.common.ResultCode;
import com.lab.emp.dto.ArticleQueryDTO;
import com.lab.emp.dto.ArticleSaveDTO;
import com.lab.emp.dto.ArticleVO;
import com.lab.emp.dto.AuditDTO;
import com.lab.emp.entity.CmsArticle;
import com.lab.emp.entity.CmsCategory;
import com.lab.emp.entity.SysUser;
import com.lab.emp.mapper.CmsArticleMapper;
import com.lab.emp.mapper.CmsCategoryMapper;
import com.lab.emp.mapper.SysUserMapper;
import com.lab.emp.service.ArticleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章业务实现。
 * <p>列表页的分类名、作者名用两次批量查询回填，避免“每行再查一次”的 N+1；
 * 排序字段走白名单，杜绝把前端传值直接拼进 ORDER BY。</p>
 */
@Service
public class ArticleServiceImpl implements ArticleService {

    private static final String DRAFT = "DRAFT";
    private static final String PENDING = "PENDING";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String REJECTED = "REJECTED";
    private static final String OFFLINE = "OFFLINE";
    private static final Set<String> STATUSES = new HashSet<>(java.util.Arrays.asList(
            DRAFT, PENDING, PUBLISHED, REJECTED, OFFLINE));

    private final CmsArticleMapper articleMapper;
    private final CmsCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;

    public ArticleServiceImpl(CmsArticleMapper articleMapper,
                              CmsCategoryMapper categoryMapper,
                              SysUserMapper userMapper) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<ArticleVO> pageQuery(ArticleQueryDTO query) {
        int current = query.getPage() == null ? 1 : Math.max(1, query.getPage());
        int size = query.getSize() == null ? 10 : Math.min(Math.max(1, query.getSize()), 100);

        LambdaQueryWrapper<CmsArticle> wrapper = new LambdaQueryWrapper<>();
        String title = trimToNull(query.getTitle());
        String status = trimToNull(query.getStatus());
        if (status != null && !STATUSES.contains(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "状态取值非法：" + status + "，可选 DRAFT/PENDING/PUBLISHED/REJECTED/OFFLINE");
        }
        wrapper.like(title != null, CmsArticle::getTitle, title)
                .eq(query.getCategoryId() != null, CmsArticle::getCategoryId, query.getCategoryId())
                .eq(status != null, CmsArticle::getStatus, status)
                .eq(query.getAuthorId() != null, CmsArticle::getAuthorId, query.getAuthorId());

        SFunction<CmsArticle, ?> orderColumn = orderColumn(query.getOrderBy());
        boolean asc = "asc".equalsIgnoreCase(query.getOrderDir());
        wrapper.orderBy(true, asc, orderColumn).orderByDesc(CmsArticle::getArticleId);

        Page<CmsArticle> page = articleMapper.selectPage(new Page<>(current, size), wrapper);
        List<ArticleVO> records = toVoList(page.getRecords());

        Page<ArticleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public ArticleVO detail(Long articleId) {
        CmsArticle article = requireArticle(articleId);
        articleMapper.update(null, new LambdaUpdateWrapper<CmsArticle>()
                .eq(CmsArticle::getArticleId, articleId)
                .setSql("view_count = view_count + 1"));

        ArticleVO vo = ArticleVO.from(article);
        vo.setContent(article.getContent());
        vo.setCategoryName(categoryName(article.getCategoryId()));
        vo.setAuthorName(realName(article.getAuthorId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CmsArticle save(ArticleSaveDTO dto, Integer authorId) {
        checkCategory(dto.getCategoryId());

        CmsArticle article = new CmsArticle();
        article.setTitle(dto.getTitle().trim());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setCategoryId(dto.getCategoryId());
        article.setAuthorId(authorId);
        article.setStatus(DRAFT);
        article.setViewCount(0);
        articleMapper.insert(article);

        CmsArticle saved = new CmsArticle();
        saved.setArticleId(article.getArticleId());
        saved.setStatus(DRAFT);
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CmsArticle update(ArticleSaveDTO dto, Integer operatorId) {
        CmsArticle article = requireArticle(dto.getArticleId());
        if (!DRAFT.equals(article.getStatus()) && !REJECTED.equals(article.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "只有草稿或已驳回的文章可以编辑，当前状态：" + ArticleVO.statusText(article.getStatus()));
        }
        checkCategory(dto.getCategoryId());

        CmsArticle update = new CmsArticle();
        update.setArticleId(article.getArticleId());
        update.setTitle(dto.getTitle().trim());
        update.setSummary(dto.getSummary());
        update.setContent(dto.getContent());
        update.setCategoryId(dto.getCategoryId());
        articleMapper.updateById(update);

        CmsArticle result = new CmsArticle();
        result.setArticleId(article.getArticleId());
        result.setStatus(article.getStatus());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long articleId) {
        CmsArticle article = requireArticle(articleId);
        if (!DRAFT.equals(article.getStatus()) && !REJECTED.equals(article.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "只有草稿或已驳回的文章可以提交审核，当前状态：" + ArticleVO.statusText(article.getStatus()));
        }
        changeStatus(articleId, PENDING, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long articleId, AuditDTO dto) {
        CmsArticle article = requireArticle(articleId);
        if (!PENDING.equals(article.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "只有待审核的文章可以审核，当前状态：" + ArticleVO.statusText(article.getStatus()));
        }
        if (REJECTED.equals(dto.getStatus()) && (dto.getRemark() == null || dto.getRemark().trim().isEmpty())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "驳回时必须填写审核意见");
        }
        changeStatus(articleId, dto.getStatus(), dto.getRemark(),
                PUBLISHED.equals(dto.getStatus()) ? LocalDateTime.now() : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchPublish(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择要发布的文章");
        }
        Set<Long> ids = new HashSet<>(articleIds);
        List<CmsArticle> articles = articleMapper.selectBatchIds(ids);
        if (articles.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "所选文章都不存在");
        }
        List<Long> publishable = new ArrayList<>();
        for (CmsArticle a : articles) {
            if (DRAFT.equals(a.getStatus()) || PENDING.equals(a.getStatus())) {
                publishable.add(a.getArticleId());
            }
        }
        if (publishable.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "所选文章中没有可发布的（仅草稿或待审核可发布）");
        }
        articleMapper.update(null, new LambdaUpdateWrapper<CmsArticle>()
                .in(CmsArticle::getArticleId, publishable)
                .set(CmsArticle::getStatus, PUBLISHED)
                .set(CmsArticle::getPublishTime, LocalDateTime.now()));
        return publishable.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long articleId) {
        CmsArticle article = requireArticle(articleId);
        if (!PUBLISHED.equals(article.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "只有已发布的文章可以下架，当前状态：" + ArticleVO.statusText(article.getStatus()));
        }
        changeStatus(articleId, OFFLINE, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long articleId) {
        requireArticle(articleId);
        articleMapper.deleteById(articleId);
    }

    // ------------------------------------------------------------------ 私有方法

    private void changeStatus(Long articleId, String status, String remark, LocalDateTime publishTime) {
        LambdaUpdateWrapper<CmsArticle> wrapper = new LambdaUpdateWrapper<CmsArticle>()
                .eq(CmsArticle::getArticleId, articleId)
                .set(CmsArticle::getStatus, status);
        if (remark != null) {
            wrapper.set(CmsArticle::getAuditRemark, remark.trim());
        }
        if (publishTime != null) {
            wrapper.set(CmsArticle::getPublishTime, publishTime);
        }
        articleMapper.update(null, wrapper);
    }

    private CmsArticle requireArticle(Long articleId) {
        CmsArticle article = articleId == null ? null : articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在，编号=" + articleId);
        }
        return article;
    }

    private void checkCategory(Integer categoryId) {
        CmsCategory category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "所选分类不存在，编号=" + categoryId);
        }
        if (category.getStatus() != null && category.getStatus() == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "分类已停用，不能在其下发布文章：" + category.getCategoryName());
        }
    }

    /** 批量回填分类名与作者名，避免 N+1 查询 */
    private List<ArticleVO> toVoList(List<CmsArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Integer> categoryIds = articles.stream()
                .map(CmsArticle::getCategoryId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Integer> authorIds = articles.stream()
                .map(CmsArticle::getAuthorId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        Map<Integer, String> categoryNames = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            for (CmsCategory c : categoryMapper.selectBatchIds(categoryIds)) {
                categoryNames.put(c.getCategoryId(), c.getCategoryName());
            }
        }
        Map<Integer, String> authorNames = new HashMap<>();
        if (!authorIds.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(authorIds)) {
                authorNames.put(u.getUserId(), u.getRealName());
            }
        }

        List<ArticleVO> list = new ArrayList<>(articles.size());
        for (CmsArticle a : articles) {
            ArticleVO vo = ArticleVO.from(a);
            vo.setCategoryName(categoryNames.get(a.getCategoryId()));
            vo.setAuthorName(authorNames.get(a.getAuthorId()));
            list.add(vo);
        }
        return list;
    }

    private String categoryName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        CmsCategory c = categoryMapper.selectById(categoryId);
        return c == null ? null : c.getCategoryName();
    }

    private String realName(Integer userId) {
        if (userId == null) {
            return null;
        }
        SysUser u = userMapper.selectById(userId);
        return u == null ? null : u.getRealName();
    }

    /** 排序字段白名单：只接受三个已知字段，其余一律回退 createTime */
    private SFunction<CmsArticle, ?> orderColumn(String orderBy) {
        String key = orderBy == null ? "" : orderBy.trim();
        switch (key) {
            case "publishTime":
                return CmsArticle::getPublishTime;
            case "viewCount":
                return CmsArticle::getViewCount;
            case "updateTime":
                return CmsArticle::getUpdateTime;
            default:
                return CmsArticle::getCreateTime;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}