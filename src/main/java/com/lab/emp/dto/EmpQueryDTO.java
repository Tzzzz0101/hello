package com.lab.emp.dto;

import java.io.Serializable;

/**
 * 员工列表查询条件。
 * <p>page / size 不设 JSR-303 下限，由 Service 做防御性归一化（page&lt;1 视为 1，size 超出 [1,100] 时纠正），
 * 保证边界参数也能返回有效数据而不是 500。</p>
 */
public class EmpQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 姓名模糊查询关键字 */
    private String name;

    /** 性别精确匹配：男 / 女 */
    private String gender;

    /** 部门编号精确匹配 */
    private Integer deptId;

    /** 在职状态精确匹配：1 在职，0 离职 */
    private Integer status;

    private Integer page = 1;

    private Integer size = 5;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getDeptId() {
        return deptId;
    }

    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "EmpQueryDTO{name='" + name + "', gender='" + gender + "', deptId=" + deptId
                + ", status=" + status + ", page=" + page + ", size=" + size + '}';
    }
}