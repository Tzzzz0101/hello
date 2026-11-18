package com.lab.emp.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 分类新增 / 编辑入参。categoryId 为空表示新增。
 */
public class CategorySaveDTO {

    private Integer categoryId;

    @NotNull(message = "上级分类不能为空，一级分类请传 0")
    @Min(value = 0, message = "上级分类编号不合法")
    private Integer parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过 50 个字符")
    private String categoryName;

    @NotBlank(message = "分类编码不能为空")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,49}$", message = "分类编码只能是小写字母、数字与连字符，长度 2-50")
    private String categoryCode;

    @Min(value = 0, message = "排序号不能为负数")
    private Integer sort = 0;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status = 1;

    @Size(max = 200, message = "分类描述不能超过 200 个字符")
    private String description;

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}