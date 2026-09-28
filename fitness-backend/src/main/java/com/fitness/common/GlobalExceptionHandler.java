package com.fitness.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** 请求体类型错误（如 status 传 "abc"），收敛为 400 而非 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "请求参数格式错误");
    }

    /**
     * 静态资源 / 未知路径不存在（Spring Boot 3.2+ 由 ResourceHttpRequestHandler 抛出）。
     * 属于客户端侧的正常 404，不能落进下面的 Exception 兜底被记成「服务器异常」并刷 ERROR 堆栈。
     * 常见触发：favicon.ico、/、/druid 等浏览器/扫描器探测。
     * 注意：根路径的 resourcePath 为空串，故日志改用真实请求 URI（否则会打印成「GET 」）。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("资源不存在: {} {}", request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ResultCode.NOT_FOUND, "资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器异常", e);
        // ISS-013：不向客户端回传原始异常信息（避免泄露 SQL/表结构/堆栈）
        return Result.fail(ResultCode.SERVER_ERROR, "服务器繁忙，请稍后重试");
    }
}
