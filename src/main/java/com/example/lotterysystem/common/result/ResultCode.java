package com.example.lotterysystem.common.result;

/**
 * 统一响应状态码枚举
 * <p>
 * 规范项目中所有接口的返回码，避免魔术数字散落在各 Controller 中。
 * 状态码设计遵循 HTTP 语义：2xx 成功、4xx 客户端错误、5xx 服务端错误。
 */
public enum ResultCode {

    // ---- 成功 ----
    SUCCESS(200, "操作成功"),

    // ---- 客户端错误 4xx ----
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    BAD_REQUEST(400, "请求参数错误"),

    // ---- 服务端错误 5xx ----
    ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用");

    private final int code;
    private final String defaultMessage;

    ResultCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
