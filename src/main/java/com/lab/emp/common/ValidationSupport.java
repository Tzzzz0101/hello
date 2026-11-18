package com.lab.emp.common;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.stream.Collectors;

/**
 * 参数校验辅助：把 BindingResult 里的字段错误拼成一条业务异常抛出。
 * <p>由 @RestControllerAdvice 统一捕获后转成 {"code":400,...} 响应。</p>
 */
public final class ValidationSupport {

    private ValidationSupport() {
    }

    public static void check(BindingResult bindingResult) {
        if (bindingResult != null && bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("；"));
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }
}