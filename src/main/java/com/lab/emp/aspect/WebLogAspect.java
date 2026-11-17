package com.lab.emp.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Web 请求日志切面：记录 Controller 的入参、URI、耗时与返回值，异常时记录堆栈。
 * <p>切面加载在 SpringMVC 子容器中（SpringMvcConfig 显式开启 @EnableAspectJAutoProxy），
 * 与业务层的事务切面分属不同容器，互不干扰。</p>
 */
@Aspect
@Component
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);

    /** Controller 包下所有 public 方法 */
    @Pointcut("execution(public * com.lab.emp.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String target = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        String args = formatArgs(joinPoint.getArgs());
        String uri = currentUri();

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[WebLog] {} {} -> {} 参数={} 耗时={}ms 结果={}",
                    uri, target, "OK", args, cost, abbreviate(String.valueOf(result), 200));
            return result;
        } catch (Throwable throwable) {
            long cost = System.currentTimeMillis() - start;
            log.error("[WebLog] {} {} -> EXCEPTION 参数={} 耗时={}ms 异常={}: {}",
                    uri, target, args, cost, throwable.getClass().getSimpleName(), throwable.getMessage());
            throw throwable;
        }
    }

    /** 异常出口再记一条，便于在日志中按异常类型检索 */
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "ex")
    public void afterThrowing(Throwable ex) {
        log.error("[WebLog] Controller 抛出异常: {}", ex.getClass().getName(), ex);
    }

    private String currentUri() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "-";
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getMethod() + " " + request.getRequestURI();
    }

    /** 过滤 Servlet 对象与校验结果，避免日志膨胀或序列化失败 */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        String text = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest)
                        && !(arg instanceof HttpServletResponse)
                        && !(arg instanceof BindingResult))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return "[" + abbreviate(text, 300) + "]";
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}