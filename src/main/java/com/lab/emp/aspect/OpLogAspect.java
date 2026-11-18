package com.lab.emp.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.emp.dto.LoginUser;
import com.lab.emp.entity.SysLog;
import com.lab.emp.interceptor.LoginInterceptor;
import com.lab.emp.service.LogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面：环绕标注了 @OpLog 的接口，记录操作人、模块、动作、参数、IP、耗时与成败。
 * <p>日志落库失败不能影响主流程，因此 save 包在 try/catch 中。</p>
 */
@Aspect
@Component
public class OpLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OpLogAspect.class);

    private static final int PARAM_LIMIT = 900;

    private final LogService logService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpLogAspect(LogService logService) {
        this.logService = logService;
    }

    @Pointcut("@annotation(opLog)")
    public void opLogPointcut(OpLog opLog) {
    }

    @Around(value = "opLogPointcut(opLog)", argNames = "pjp,opLog")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysLog entity = new SysLog();
        entity.setModule(opLog.module());
        entity.setOperation(opLog.operation());
        entity.setMethod(pjp.getSignature().toShortString());

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setIp(clientIp(request));
        }
        LoginUser user = currentUser();
        if (user != null) {
            entity.setUserId(user.getUserId());
            entity.setUsername(user.getUsername());
        }
        entity.setParams(describeArgs(pjp.getArgs()));

        try {
            Object result = pjp.proceed();
            entity.setStatus(1);
            return result;
        } catch (Throwable ex) {
            entity.setStatus(0);
            entity.setErrorMsg(truncate(ex.getClass().getSimpleName() + ": " + ex.getMessage(), 500));
            throw ex;
        } finally {
            entity.setCostMs((int) (System.currentTimeMillis() - start));
            try {
                logService.save(entity);
            } catch (Exception ex) {
                log.warn("操作日志落库失败，不影响主流程: {}", ex.getMessage());
            }
        }
    }

    private String describeArgs(Object[] args) {
        List<String> parts = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof HttpServletRequest || arg instanceof HttpSession
                    || arg instanceof org.springframework.validation.BindingResult) {
                continue;
            }
            try {
                parts.add(objectMapper.writeValueAsString(arg));
            } catch (Exception ignored) {
                parts.add(String.valueOf(arg));
            }
        }
        String text = String.join(", ", parts);
        // 参数里可能带口令，落库前统一打码
        text = text.replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*", "$1***");
        return truncate(text, PARAM_LIMIT);
    }

    private String truncate(String text, int limit) {
        if (text == null) {
            return null;
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private LoginUser currentUser() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(LoginInterceptor.SESSION_LOGIN_USER);
        return value instanceof LoginUser ? (LoginUser) value : null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}