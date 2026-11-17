package com.lab.emp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理：把各类异常统一转换为 Result 结构，保证前端只需处理一种响应格式。
 * <p>业务异常只记录 warn 日志（可预期），未知异常记录 error 日志并返回通用提示，
 * 避免把堆栈或 SQL 细节暴露给调用方。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：按异常自带的 code 返回，如 400 参数错误、404 不存在 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        HttpStatus status = resolveStatus(e.getCode());
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /** @Valid 校验失败（@RequestBody 场景） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.PARAM_ERROR.getCode(), message));
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败: {}", message);
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.PARAM_ERROR.getCode(), message));
    }

    /** 方法参数上的约束校验失败（如 @Validated + @Min） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数约束校验失败: {}", message);
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.PARAM_ERROR.getCode(), message));
    }

    /** 路径变量/查询参数类型不匹配，如 /emps/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "参数 " + e.getName() + " 类型不合法：" + e.getValue();
        log.warn(message);
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.PARAM_ERROR.getCode(), message));
    }

    /** 缺少必填的请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = "缺少必填参数：" + e.getParameterName();
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.PARAM_ERROR.getCode(), message));
    }

    /** 请求体不是合法 JSON，或字段类型与目标对象不匹配 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误，请检查 JSON 结构"));
    }

    /** 请求方法不支持，如用 POST 访问 GET 接口 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(405, "不支持的请求方法：" + e.getMethod()));
    }

    /** 兜底：未预期的异常统一脱敏为"系统繁忙"，完整堆栈只写日志 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage()));
    }

    private HttpStatus resolveStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }
}