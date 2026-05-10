package com.example.lotterysystem.common.exception;

import com.example.lotterysystem.common.result.ApiResponse;
import com.example.lotterysystem.common.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 拦截所有 Controller 抛出的异常，统一包装为 ApiResponse 返回。
 * 业务异常 {@link BusinessException} 返回对应的业务状态码；
 * 未知异常兜底返回 500，同时打印完整堆栈方便排查。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常 —— 返回对应的状态码和消息 */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: [{}] {}", e.getResultCode().getCode(), e.getMessage());
        return ApiResponse.fail(e.getResultCode(), e.getMessage());
    }

    /** 非法参数异常 —— Spring Validation 抛出的 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return ApiResponse.fail(ResultCode.BAD_REQUEST, e.getMessage());
    }

    /** 未知异常 —— 兜底处理，记录完整堆栈 */
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("未知异常", e);
        return ApiResponse.fail(ResultCode.ERROR, "服务器繁忙，请稍后再试");
    }
}
