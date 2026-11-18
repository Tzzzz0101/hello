package com.lab.emp.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户-角色关联表（无独立实体，直接用注解 SQL）。
 */
@Mapper
public interface UserRoleMapper {

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    @Insert("<script>INSERT INTO sys_user_role (user_id, role_id) VALUES "
            + "<foreach collection='roleIds' item='roleId' separator=','>(#{userId}, #{roleId})</foreach></script>")
    int insertBatch(@Param("userId") Integer userId, @Param("roleIds") List<Integer> roleIds);

    /** 批量查多个用户的角色，避免用户列表 N+1 */
    @Select("<script>SELECT ur.user_id AS userId, r.role_id AS roleId, r.role_name AS roleName "
            + "FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id "
            + "WHERE ur.user_id IN <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<Map<String, Object>> selectRolesByUserIds(@Param("userIds") List<Integer> userIds);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Integer> selectRoleIdsByUserId(@Param("userId") Integer userId);
}