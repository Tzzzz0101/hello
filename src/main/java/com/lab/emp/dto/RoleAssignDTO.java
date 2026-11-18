package com.lab.emp.dto;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 给用户分配角色。
 */
public class RoleAssignDTO {

    @NotNull(message = "角色列表不能为空，清空授权请传空数组")
    private List<Integer> roleIds;

    public List<Integer> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Integer> roleIds) { this.roleIds = roleIds; }
}