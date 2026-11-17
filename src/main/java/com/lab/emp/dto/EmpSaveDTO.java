package com.lab.emp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增员工请求体（JSR-303 校验注解集中在这一层，实体保持"纯数据"）。
 */
public class EmpSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "员工姓名不能为空")
    @Size(max = 50, message = "员工姓名长度不能超过 50 个字符")
    private String empName;

    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "^[男女]$", message = "性别只能填写 男 或 女")
    private String gender;

    @NotNull(message = "所属部门不能为空")
    private Integer deptId;

    @Size(max = 50, message = "岗位名称长度不能超过 50 个字符")
    private String post;

    @NotNull(message = "薪资不能为空")
    @DecimalMin(value = "0", message = "薪资不能为负数")
    @Digits(integer = 8, fraction = 2, message = "薪资最多 8 位整数、2 位小数")
    private BigDecimal salary;

    @PastOrPresent(message = "入职时间不能晚于今天")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    @Min(value = 0, message = "状态只能为 0（离职）或 1（在职）")
    @Max(value = 1, message = "状态只能为 0（离职）或 1（在职）")
    private Integer status;

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
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

    public String getPost() {
        return post;
    }

    public void setPost(String post) {
        this.post = post;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}