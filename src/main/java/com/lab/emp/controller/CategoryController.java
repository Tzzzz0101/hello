package com.lab.emp.controller;

import com.lab.emp.aspect.OpLog;
import com.lab.emp.aspect.RequirePerm;
import com.lab.emp.common.Result;
import com.lab.emp.common.ValidationSupport;
import com.lab.emp.dto.CategorySaveDTO;
import com.lab.emp.entity.CmsCategory;
import com.lab.emp.service.CategoryService;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 内容分类 REST 接口。
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    @RequirePerm("category:list")
    public Result<List<CmsCategory>> tree() {
        return Result.success(categoryService.tree());
    }

    /** 文章表单的分类下拉：任何已登录用户都可读，不额外要求分类管理权限 */
    @GetMapping("/options")
    public Result<List<CmsCategory>> options() {
        return Result.success(categoryService.listEnabled());
    }

    @PostMapping
    @RequirePerm("category:add")
    @OpLog(module = "分类管理", operation = "新增分类")
    public Result<CmsCategory> save(@Valid @RequestBody CategorySaveDTO dto, BindingResult bindingResult) {
        ValidationSupport.check(bindingResult);
        return Result.success("新增成功", categoryService.save(dto));
    }

    @PutMapping("/{categoryId}")
    @RequirePerm("category:edit")
    @OpLog(module = "分类管理", operation = "编辑分类")
    public Result<CmsCategory> update(@PathVariable Integer categoryId,
                                      @Valid @RequestBody CategorySaveDTO dto,
                                      BindingResult bindingResult) {
        ValidationSupport.check(bindingResult);
        dto.setCategoryId(categoryId);
        return Result.success("保存成功", categoryService.update(dto));
    }

    @DeleteMapping("/{categoryId}")
    @RequirePerm("category:delete")
    @OpLog(module = "分类管理", operation = "删除分类")
    public Result<Void> delete(@PathVariable Integer categoryId) {
        categoryService.delete(categoryId);
        return Result.<Void>success("已删除", null);
    }
}