package com.example.lotterysystem.common.exception;

import com.example.lotterysystem.common.result.ResultCode;

/**
 * 业务异常
 * <p>
 * 在 Service 层遇到可预见的业务错误（用户不存在、库存不足等）时抛出，
 * 由 {@link GlobalExceptionHandler} 统一捕获并转换为 ApiResponse 返回。
 * 避免在 Controller 层到处 try-catch。
 */
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getDefaultMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.resultCode = ResultCode.ERROR; // 兜底，实际 code 在 handler 中单独处理
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
