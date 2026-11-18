package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.emp.common.BusinessException;
import com.lab.emp.common.ResultCode;
import com.lab.emp.dto.CategorySaveDTO;
import com.lab.emp.entity.CmsCategory;
import com.lab.emp.mapper.CmsArticleMapper;
import com.lab.emp.mapper.CmsCategoryMapper;
import com.lab.emp.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分类业务实现：树形装配、父子关系校验、删除前的占用校验。
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CmsCategoryMapper categoryMapper;
    private final CmsArticleMapper articleMapper;

    public CategoryServiceImpl(CmsCategoryMapper categoryMapper, CmsArticleMapper articleMapper) {
        this.categoryMapper = categoryMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    public List<CmsCategory> tree() {
        List<CmsCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<CmsCategory>()
                .orderByAsc(CmsCategory::getSort)
                .orderByAsc(CmsCategory::getCategoryId));

        Map<Integer, CmsCategory> index = new HashMap<>();
        for (CmsCategory c : all) {
            c.setChildren(new ArrayList<>());
            index.put(c.getCategoryId(), c);
        }
        List<CmsCategory> roots = new ArrayList<>();
        for (CmsCategory c : all) {
            CmsCategory parent = c.getParentId() == null ? null : index.get(c.getParentId());
            if (c.getParentId() == null || c.getParentId() == 0 || parent == null) {
                roots.add(c);
            } else {
                parent.getChildren().add(c);
                c.setParentName(parent.getCategoryName());
            }
        }
        return roots;
    }

    @Override
    public List<CmsCategory> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CmsCategory>()
                .eq(CmsCategory::getStatus, 1)
                .orderByAsc(CmsCategory::getSort)
                .orderByAsc(CmsCategory::getCategoryId));
    }

    @Override
    public CmsCategory getById(Integer categoryId) {
        CmsCategory category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分类不存在，编号=" + categoryId);
        }
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CmsCategory save(CategorySaveDTO dto) {
        checkCodeUnique(dto.getCategoryCode(), null);
        checkParent(dto.getParentId(), null);

        CmsCategory category = new CmsCategory();
        category.setParentId(normalizeParent(dto.getParentId()));
        category.setCategoryName(dto.getCategoryName().trim());
        category.setCategoryCode(dto.getCategoryCode().trim());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setDescription(dto.getDescription());
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CmsCategory update(CategorySaveDTO dto) {
        CmsCategory exists = getById(dto.getCategoryId());
        checkCodeUnique(dto.getCategoryCode(), exists.getCategoryId());
        checkParent(dto.getParentId(), exists.getCategoryId());

        exists.setParentId(normalizeParent(dto.getParentId()));
        exists.setCategoryName(dto.getCategoryName().trim());
        exists.setCategoryCode(dto.getCategoryCode().trim());
        exists.setSort(dto.getSort() == null ? 0 : dto.getSort());
        exists.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        exists.setDescription(dto.getDescription());
        categoryMapper.updateById(exists);
        return exists;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer categoryId) {
        CmsCategory exists = getById(categoryId);

        Long children = categoryMapper.selectCount(new LambdaQueryWrapper<CmsCategory>()
                .eq(CmsCategory::getParentId, categoryId));
        if (children != null && children > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该分类下还有 " + children + " 个子分类，请先删除子分类");
        }
        int articles = articleMapper.countArticleByCategory(categoryId);
        if (articles > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该分类下还有 " + articles + " 篇文章，不能删除");
        }
        categoryMapper.deleteById(exists.getCategoryId());
    }

    /** 前端用 0 表示“一级分类”，数据库里用 NULL 表示，才能满足自引用外键 */
    private Integer normalizeParent(Integer parentId) {
        return parentId == null || parentId == 0 ? null : parentId;
    }

    /** 分类编码全局唯一 */
    private void checkCodeUnique(String code, Integer excludeId) {
        if (code == null) {
            return;
        }
        CmsCategory other = categoryMapper.selectOne(new LambdaQueryWrapper<CmsCategory>()
                .eq(CmsCategory::getCategoryCode, code.trim())
                .last("LIMIT 1"));
        if (other != null && !other.getCategoryId().equals(excludeId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "分类编码已存在：" + code);
        }
    }

    /** 上级分类必须存在，且不能把自己或自己的后代设为上级（成环校验） */
    private void checkParent(Integer parentId, Integer selfId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        CmsCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上级分类不存在，编号=" + parentId);
        }
        if (selfId == null) {
            return;
        }
        if (selfId.equals(parentId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上级分类不能是自己");
        }
        Set<Integer> visited = new HashSet<>();
        Integer cursor = parent.getParentId();
        while (cursor != null && cursor != 0 && visited.add(cursor)) {
            if (selfId.equals(cursor)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不能把子分类设为上级分类");
            }
            CmsCategory up = categoryMapper.selectById(cursor);
            cursor = up == null ? 0 : up.getParentId();
        }
    }
}