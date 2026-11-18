package com.lab.emp.controller;

import com.lab.emp.common.BusinessException;
import com.lab.emp.common.Result;
import com.lab.emp.common.ResultCode;
import com.lab.emp.dto.LoginDTO;
import com.lab.emp.dto.LoginUser;
import com.lab.emp.interceptor.LoginInterceptor;
import com.lab.emp.service.AuthService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.stream.Collectors;

/**
 * 登录认证接口（路径 /login 在拦截器白名单中）。
 */
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录：校验通过后把登录上下文（含角色与权限）写入 session */
    @PostMapping("/login")
    public Result<LoginUser> login(@Valid @RequestBody LoginDTO loginDTO,
                                   BindingResult bindingResult,
                                   HttpSession session) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("；"));
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
        LoginUser user = authService.login(loginDTO);
        session.setAttribute(LoginInterceptor.SESSION_LOGIN_USER, user);
        return Result.success("登录成功", user);
    }

    /** 退出登录：销毁 session */
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.<Void>success("已退出登录", null);
    }

    /** 当前登录用户及其权限，前端刷新页面后用于回显与按钮控制 */
    @GetMapping("/me")
    public Result<LoginUser> currentUser(HttpSession session) {
        Object loginUser = session.getAttribute(LoginInterceptor.SESSION_LOGIN_USER);
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录，请先登录");
        }
        return Result.success((LoginUser) loginUser);
    }
}