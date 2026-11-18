package com.lab.emp.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 审核入参：通过 PUBLISHED，驳回 REJECTED（驳回必须写原因）。
 */
public class AuditDTO {

    @NotBlank(message = "审核结果不能为空")
    @Pattern(regexp = "^(PUBLISHED|REJECTED)$", message = "审核结果只能是 PUBLISHED 或 REJECTED")
    private String status;

    @Size(max = 255, message = "审核意见不能超过 255 个字符")
    private String remark;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}