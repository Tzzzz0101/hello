package com.lab.emp.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录用户上下文：登录成功时把用户、角色、权限一次性算好放进 session，
 * 后续鉴权只需查内存集合，避免每个请求都联表查权限。
 */
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String username;
    private String realName;
    private List<String> roleCodes;
    private List<String> roleNames;
    private Set<String> permissions;

    public LoginUser() {
        this.roleCodes = Collections.emptyList();
        this.roleNames = Collections.emptyList();
        this.permissions = Collections.emptySet();
    }

    /** 是否拥有某个权限标识，ADMIN 视为通配 */
    public boolean has(String permCode) {
        if (permCode == null || permCode.isEmpty()) {
            return true;
        }
        return roleCodes.contains("ADMIN") || permissions.contains(permCode);
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes == null ? Collections.emptyList() : roleCodes;
    }

    public List<String> getRoleNames() { return roleNames; }
    public void setRoleNames(List<String> roleNames) {
        this.roleNames = roleNames == null ? Collections.emptyList() : roleNames;
    }

    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions == null ? Collections.emptySet() : new LinkedHashSet<>(permissions);
    }
}