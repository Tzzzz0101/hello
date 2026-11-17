package com.lab.emp.dto;

import javax.validation.constraints.NotNull;

/**
 * 修改员工请求体：在新增字段的基础上必须携带主键。
 */
public class EmpUpdateDTO extends EmpSaveDTO {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "员工编号不能为空")
    private Integer empId;

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }
}