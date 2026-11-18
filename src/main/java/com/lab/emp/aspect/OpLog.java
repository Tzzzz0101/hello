package com.lab.emp.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志声明：标注在需要留痕的 Controller 方法上，由 {@link OpLogAspect} 记录
 * 操作人、模块、动作、参数、IP、耗时与结果。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {

    /** 模块，如“文章管理” */
    String module();

    /** 动作，如“新增文章” */
    String operation();
}