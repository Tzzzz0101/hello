package com.lab.emp.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级权限声明：标注在 Controller 方法上，由 {@link PermAspect} 在方法执行前校验
 * 当前登录用户是否拥有指定权限标识（如 article:audit）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePerm {

    /** 权限标识，与 sys_permission.perm_code 对应 */
    String value();
}