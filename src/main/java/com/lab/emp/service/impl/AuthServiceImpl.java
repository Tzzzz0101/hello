package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.emp.common.BusinessException;
import com.lab.emp.common.ResultCode;
import com.lab.emp.dto.LoginDTO;
import com.lab.emp.dto.LoginUser;
import com.lab.emp.entity.SysPermission;
import com.lab.emp.entity.SysRole;
import com.lab.emp.entity.SysUser;
import com.lab.emp.mapper.SysPermissionMapper;
import com.lab.emp.mapper.SysRoleMapper;
import com.lab.emp.mapper.SysUserMapper;
import com.lab.emp.service.AuthService;
import com.lab.emp.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证业务实现。
 * <p>只比对口令摘要，日志中不出现任何口令内容；登录成功后一次性装配角色与权限，
 * 后续每个请求的鉴权都只读 session 中的内存集合，不再访问数据库。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           SysRoleMapper sysRoleMapper,
                           SysPermissionMapper sysPermissionMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public LoginUser login(LoginDTO loginDTO) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername().trim())
                .last("LIMIT 1"));

        if (user == null || !PasswordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            log.warn("登录失败：用户名或口令错误，username={}", loginDTO.getUsername());
            // 统一提示，避免泄露"用户名是否存在"
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或口令错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号已被禁用");
        }

        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getUserId());
        List<SysPermission> perms = sysPermissionMapper.selectByUserId(user.getUserId());

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRoleCodes(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        loginUser.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));

        Set<String> codes = new LinkedHashSet<>();
        for (SysPermission p : perms) {
            codes.add(p.getPermCode());
        }
        loginUser.setPermissions(codes);

        log.info("登录成功：username={}, roles={}, permissions={}",
                user.getUsername(), loginUser.getRoleCodes(), codes.size());
        return loginUser;
    }
}