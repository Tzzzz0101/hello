package com.lab.emp.controller;

import com.lab.emp.aspect.OpLog;
import com.lab.emp.aspect.RequirePerm;
import com.lab.emp.common.PageResult;
import com.lab.emp.common.Result;
import com.lab.emp.common.ValidationSupport;
import com.lab.emp.dto.RoleAssignDTO;
import com.lab.emp.dto.UserSaveDTO;
import com.lab.emp.dto.UserVO;
import com.lab.emp.entity.SysPermission;
import com.lab.emp.entity.SysRole;
import com.lab.emp.service.RbacService;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户、角色、权限管理接口（RBAC）。
 */
@RestController
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/users")
    @RequirePerm("user:list")
    public Result<PageResult<UserVO>> pageUsers(@RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return Result.success(rbacService.pageUsers(keyword, page, size));
    }

    @PostMapping("/users")
    @RequirePerm("user:save")
    @OpLog(module = "用户管理", operation = "新增用户")
    public Result<UserVO> saveUser(@Valid @RequestBody UserSaveDTO dto, BindingResult bindingResult) {
        ValidationSupport.check(bindingResult);
        return Result.success("新增成功", rbacService.saveUser(dto));
    }

    @PutMapping("/users/{userId}")
    @RequirePerm("user:save")
    @OpLog(module = "用户管理", operation = "编辑用户")
    public Result<UserVO> updateUser(@PathVariable Integer userId,
                                     @Valid @RequestBody UserSaveDTO dto,
                                     BindingResult bindingResult) {
        ValidationSupport.check(bindingResult);
        dto.setUserId(userId);
        return Result.success("保存成功", rbacService.updateUser(dto));
    }

    @PutMapping("/users/{userId}/roles")
    @RequirePerm("role:assign")
    @OpLog(module = "用户管理", operation = "分配角色")
    public Result<Void> assignRoles(@PathVariable Integer userId,
                                    @Valid @RequestBody RoleAssignDTO dto,
                                    BindingResult bindingResult) {
        ValidationSupport.check(bindingResult);
        rbacService.assignRoles(userId, dto.getRoleIds());
        return Result.<Void>success("角色已更新", null);
    }

    @GetMapping("/roles")
    @RequirePerm("role:assign")
    public Result<List<SysRole>> roles() {
        return Result.success(rbacService.listRoles());
    }

    @GetMapping("/permissions/tree")
    @RequirePerm("role:assign")
    public Result<List<SysPermission>> permissionTree() {
        return Result.success(rbacService.permissionTree());
    }

    @GetMapping("/roles/{roleId}/permissions")
    @RequirePerm("role:assign")
    public Result<List<Integer>> rolePermissions(@PathVariable Integer roleId) {
        return Result.success(rbacService.permissionIdsOfRole(roleId));
    }

    @PutMapping("/roles/{roleId}/permissions")
    @RequirePerm("role:assign")
    @OpLog(module = "角色管理", operation = "重设角色权限")
    public Result<Integer> updateRolePermissions(@PathVariable Integer roleId,
                                                 @RequestBody List<Integer> permIds) {
        return Result.success("权限已更新", rbacService.updateRolePermissions(roleId, permIds));
    }
}