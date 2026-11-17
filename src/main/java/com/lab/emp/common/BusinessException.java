package com.lab.emp.common;

/**
 * 业务异常：可预期的、需要把原因提示给调用方的错误（如"员工不存在""姓名重复"）。
 * <p>由 {@link GlobalExceptionHandler} 统一转换为 Result 响应，不做堆栈打印。</p>
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR.getCode(), message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}