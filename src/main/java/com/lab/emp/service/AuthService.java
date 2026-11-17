package com.lab.emp.service;

import com.lab.emp.dto.LoginDTO;
import com.lab.emp.entity.SysUser;

/**
 * 认证业务接口。
 */
public interface AuthService {

    /**
     * 校验用户名与口令。
     *
     * @return 登录成功后的用户信息（口令字段已清空）
     * @throws com.lab.emp.common.BusinessException 用户名或口令错误、账号被禁用
     */
    SysUser login(LoginDTO loginDTO);
}