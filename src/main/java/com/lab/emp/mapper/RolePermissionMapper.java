package com.lab.emp.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联表。
 */
@Mapper
public interface RolePermissionMapper {

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Integer roleId);

    @Insert("<script>INSERT INTO sys_role_permission (role_id, perm_id) VALUES "
            + "<foreach collection='permIds' item='permId' separator=','>(#{roleId}, #{permId})</foreach></script>")
    int insertBatch(@Param("roleId") Integer roleId, @Param("permIds") List<Integer> permIds);

    @Select("SELECT COUNT(*) FROM sys_role_permission WHERE role_id = #{roleId}")
    int countByRoleId(@Param("roleId") Integer roleId);
}