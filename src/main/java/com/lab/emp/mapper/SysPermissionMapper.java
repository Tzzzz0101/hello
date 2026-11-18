package com.lab.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.emp.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /** 用户 → 角色 → 权限，一次查询拿到去重后的权限标识 */
    @Select("SELECT DISTINCT p.* FROM sys_permission p "
            + "JOIN sys_role_permission rp ON rp.perm_id = p.perm_id "
            + "JOIN sys_user_role ur ON ur.role_id = rp.role_id "
            + "JOIN sys_role r ON r.role_id = rp.role_id "
            + "WHERE ur.user_id = #{userId} AND p.status = 1 AND r.status = 1 AND r.deleted = 0 "
            + "ORDER BY p.sort")
    List<SysPermission> selectByUserId(Integer userId);

    /** 某个角色已勾选的权限 id，用于角色授权回显 */
    @Select("SELECT perm_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Integer> selectPermIdsByRoleId(Integer roleId);
}