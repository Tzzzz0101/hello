package com.lab.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.emp.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 登录时一次性查出用户拥有的角色 */
    @Select("SELECT r.* FROM sys_role r "
            + "JOIN sys_user_role ur ON ur.role_id = r.role_id "
            + "WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 1 "
            + "ORDER BY r.role_id")
    List<SysRole> selectByUserId(Integer userId);

    @Select("SELECT r.* FROM sys_role r "
            + "JOIN sys_user_role ur ON ur.role_id = r.role_id "
            + "WHERE ur.user_id = #{userId}")
    List<SysRole> selectAllByUserId(Integer userId);
}