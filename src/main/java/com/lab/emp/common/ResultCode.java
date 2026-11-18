package com.lab.emp.common;

/**
 * 统一响应状态码。
 * <p>约定：200 成功、400 参数错误、401 未登录/认证失败、404 资源不存在、500 业务或系统失败。</p>
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "请求参数不合法"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(500, "业务处理失败"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}