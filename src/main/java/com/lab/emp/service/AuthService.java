package com.lab.emp.service;

import com.lab.emp.dto.LoginDTO;
import com.lab.emp.dto.LoginUser;

/**
 * 认证业务接口。
 */
public interface AuthService {

    /**
     * 校验用户名与口令，并装配角色、权限。
     *
     * @return 登录上下文（不含口令，含角色与权限标识）
     * @throws com.lab.emp.common.BusinessException 用户名或口令错误、账号被禁用
     */
    LoginUser login(LoginDTO loginDTO);
}