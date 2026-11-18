package com.lab.emp.controller;

import com.lab.emp.aspect.OpLog;
import com.lab.emp.aspect.RequirePerm;
import com.lab.emp.common.PageResult;
import com.lab.emp.common.Result;
import com.lab.emp.common.ValidationSupport;
import com.lab.emp.dto.ArticleQueryDTO;
import com.lab.emp.dto.ArticleSaveDTO;
import com.lab.emp.dto.ArticleVO;
import com.lab.emp.dto.AuditDTO;
import com.lab.emp.dto.LoginUser;
import com.lab.emp.entity.CmsArticle;
import com.lab.emp.interceptor.LoginInterceptor;
import com.lab.emp.service.ArticleService;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;

/**
 * 文章管理 REST 接口。
 */
@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    @RequirePerm("article:list")
    public Result<PageResult<ArticleVO>> page(ArticleQueryDTO query) {
        return Result.success(articleService.pageQuery(query));
    }

    @GetMapping("/{articleId}")
    @RequirePerm("article:view")
    public Result<ArticleVO> detail(@PathVariable Long articleId) {
        return Result.success(articleService.detail(articleId));
    }

    @PostMapping
    @RequirePerm("article:add")
    @OpLog(module = "文章管理", operation = "新增文章")
    public Result<CmsArticle> save(@Valid @RequestBody ArticleSaveDTO dto,
                                   BindingResult bindingResult,
                                   HttpSession session) {
        ValidationSupport.check(bindingResult);
        return Result.success("已保存为草稿", articleService.save(dto, currentUserId(session)));
    }

    @PutMapping("/{articleId}")
    @RequirePerm("article:edit")
    @OpLog(module = "文章管理", operation = "编辑文章")
    public Result<CmsArticle> update(@PathVariable Long articleId,
                                     @Valid @RequestBody ArticleSaveDTO dto,
                                     BindingResult bindingResult,
                                     HttpSession session) {
        ValidationSupport.check(bindingResult);
        dto.setArticleId(articleId);
        return Result.success("已保存", articleService.update(dto, currentUserId(session)));
    }

    @PutMapping("/{articleId}/submit")
    @RequirePerm("article:edit")
    @OpLog(module = "文章管理", operation = "提交审核")
    public Result<Void> submit(@PathVariable Long articleId) {
        articleService.submit(articleId);
        return Result.<Void>success("已提交审核", null);
    }

    @PutMapping("/{articleId}/audit")
    @RequirePerm("article:audit")
    @OpLog(module = "文章管理", operation = "审核文章")
    public Result<Void> audit(@PathVariable Long articleId,
                              @Valid @RequestBody AuditDTO dto,
                              BindingResult bindingResult) {
        ValidationSupport.check(bindingResult);
        articleService.audit(articleId, dto);
        return Result.<Void>success("审核完成", null);
    }

    @PutMapping("/{articleId}/offline")
    @RequirePerm("article:publish")
    @OpLog(module = "文章管理", operation = "下架文章")
    public Result<Void> offline(@PathVariable Long articleId) {
        articleService.offline(articleId);
        return Result.<Void>success("已下架", null);
    }

    @PutMapping("/batch-publish")
    @RequirePerm("article:publish")
    @OpLog(module = "文章管理", operation = "批量发布")
    public Result<Integer> batchPublish(@RequestBody List<Long> articleIds) {
        return Result.success("发布成功", articleService.batchPublish(articleIds));
    }

    @DeleteMapping("/{articleId}")
    @RequirePerm("article:delete")
    @OpLog(module = "文章管理", operation = "删除文章")
    public Result<Void> delete(@PathVariable Long articleId) {
        articleService.delete(articleId);
        return Result.<Void>success("已删除", null);
    }

    private Integer currentUserId(HttpSession session) {
        Object value = session.getAttribute(LoginInterceptor.SESSION_LOGIN_USER);
        return value instanceof LoginUser ? ((LoginUser) value).getUserId() : null;
    }
}