package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.emp.common.BusinessException;
import com.lab.emp.common.ResultCode;
import com.lab.emp.dto.LoginDTO;
import com.lab.emp.entity.SysUser;
import com.lab.emp.mapper.SysUserMapper;
import com.lab.emp.service.AuthService;
import com.lab.emp.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 认证业务实现。
 * <p>只比对口令摘要，日志中不出现任何口令内容；登录成功后返回对象前清空 password 字段。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final SysUserMapper sysUserMapper;

    public AuthServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public SysUser login(LoginDTO loginDTO) {
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

        SysUser safeUser = new SysUser();
        safeUser.setUserId(user.getUserId());
        safeUser.setUsername(user.getUsername());
        safeUser.setRealName(user.getRealName());
        safeUser.setRole(user.getRole());
        safeUser.setStatus(user.getStatus());
        log.info("登录成功：username={}, role={}", user.getUsername(), user.getRole());
        return safeUser;
    }
}