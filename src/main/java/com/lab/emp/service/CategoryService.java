package com.lab.emp.service;

import com.lab.emp.dto.CategorySaveDTO;
import com.lab.emp.entity.CmsCategory;

import java.util.List;

/**
 * 内容分类业务。
 */
public interface CategoryService {

    /** 树形结构（含停用节点，供管理端展示） */
    List<CmsCategory> tree();

    /** 扁平列表（仅启用节点，供文章编辑下拉框） */
    List<CmsCategory> listEnabled();

    CmsCategory getById(Integer categoryId);

    CmsCategory save(CategorySaveDTO dto);

    CmsCategory update(CategorySaveDTO dto);

    void delete(Integer categoryId);
}