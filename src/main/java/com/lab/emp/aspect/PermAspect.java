package com.lab.emp.aspect;

import com.lab.emp.common.BusinessException;
import com.lab.emp.common.ResultCode;
import com.lab.emp.dto.LoginUser;
import com.lab.emp.interceptor.LoginInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 接口权限切面：在 Controller 方法执行前，按 @RequirePerm 的权限标识做一次内存校验。
 * <p>与登录拦截器分工：拦截器只判断“有没有登录”，本切面判断“有没有这个操作的权限”，
 * 两者都在方法进入前生效，但前者在 DispatcherServlet 分发阶段，后者在 Bean 代理调用阶段。</p>
 */
@Aspect
@Component
public class PermAspect {

    private static final Logger log = LoggerFactory.getLogger(PermAspect.class);

    @Around("@annotation(requirePerm)")
    public Object check(ProceedingJoinPoint pjp, RequirePerm requirePerm) throws Throwable {
        String perm = requirePerm.value();
        LoginUser user = currentUser();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录，请先登录");
        }
        if (!user.has(perm)) {
            log.warn("权限不足：user={}, perm={}, method={}",
                    user.getUsername(), perm, pjp.getSignature().toShortString());
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "没有操作权限：" + perm);
        }
        return pjp.proceed();
    }

    private LoginUser currentUser() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(LoginInterceptor.SESSION_LOGIN_USER);
        return value instanceof LoginUser ? (LoginUser) value : null;
    }
}