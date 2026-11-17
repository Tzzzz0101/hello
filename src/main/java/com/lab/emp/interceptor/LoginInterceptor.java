package com.lab.emp.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器：校验 session 中的登录用户，未登录时直接返回 401 JSON，不再进入 Controller。
 * <p>注意：拦截器在 DispatcherServlet 分发阶段执行，早于 Controller 方法；
 * 而事务、AOP 切面发生在 Spring 容器内的 Bean 调用上，两者所处容器与时机均不同。</p>
 */
public class LoginInterceptor implements HandlerInterceptor {

    /** session 中保存登录用户的键名，登录/退出/拦截三处共用同一常量 */
    public static final String SESSION_LOGIN_USER = "loginUser";

    private static final Logger log = LoggerFactory.getLogger(LoginInterceptor.class);

    private static final String UNAUTHORIZED_JSON = "{\"code\":401,\"message\":\"未登录，请先登录\",\"data\":null}";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 预检请求（CORS preflight）不带 Cookie，必须放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        Object loginUser = session == null ? null : session.getAttribute(SESSION_LOGIN_USER);
        if (loginUser != null) {
            return true;
        }
        log.warn("拦截未登录请求: {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(UNAUTHORIZED_JSON);
        return false;
    }
}