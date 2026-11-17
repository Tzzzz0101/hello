package com.lab.emp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lab.emp.entity.Emp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工视图对象：只向前端暴露需要的字段，隐藏逻辑删除标记等持久层细节。
 */
public class EmpVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer empId;
    private String empName;
    private String gender;
    private Integer deptId;
    private String deptName;
    private String post;
    private BigDecimal salary;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public static EmpVO from(Emp emp) {
        if (emp == null) {
            return null;
        }
        EmpVO vo = new EmpVO();
        vo.setEmpId(emp.getEmpId());
        vo.setEmpName(emp.getEmpName());
        vo.setGender(emp.getGender());
        vo.setDeptId(emp.getDeptId());
        vo.setDeptName(emp.getDeptName());
        vo.setPost(emp.getPost());
        vo.setSalary(emp.getSalary());
        vo.setHireDate(emp.getHireDate());
        vo.setStatus(emp.getStatus());
        vo.setCreateTime(emp.getCreateTime());
        return vo;
    }

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

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

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}