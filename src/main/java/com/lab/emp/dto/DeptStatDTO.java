package com.lab.emp.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部门维度统计结果（分組聚合查询）。
 */
public class DeptStatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer deptId;
    private String deptName;
    private Long empCount;
    private BigDecimal avgSalary;
    private BigDecimal maxSalary;

    public Integer getDeptId() {
        return deptId;
    }

    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Long getEmpCount() {
        return empCount;
    }

    public void setEmpCount(Long empCount) {
        this.empCount = empCount;
    }

    public BigDecimal getAvgSalary() {
        return avgSalary;
    }

    public void setAvgSalary(BigDecimal avgSalary) {
        this.avgSalary = avgSalary;
    }

    public BigDecimal getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(BigDecimal maxSalary) {
        this.maxSalary = maxSalary;
    }
}