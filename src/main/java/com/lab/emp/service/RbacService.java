package com.lab.emp.service;

import com.lab.emp.common.PageResult;
import com.lab.emp.dto.UserSaveDTO;
import com.lab.emp.dto.UserVO;
import com.lab.emp.entity.SysPermission;
import com.lab.emp.entity.SysRole;

import java.util.List;

/**
 * 用户与角色权限管理（RBAC）。
 */
public interface RbacService {

    PageResult<UserVO> pageUsers(String keyword, int page, int size);

    UserVO saveUser(UserSaveDTO dto);

    UserVO updateUser(UserSaveDTO dto);

    void assignRoles(Integer userId, List<Integer> roleIds);

    List<SysRole> listRoles();

    /** 权限树（菜单 + 接口权限） */
    List<SysPermission> permissionTree();

    /** 某角色已勾选的权限 id */
    List<Integer> permissionIdsOfRole(Integer roleId);

    /** 重设角色权限，返回变更后的权限条数 */
    int updateRolePermissions(Integer roleId, List<Integer> permIds);
}