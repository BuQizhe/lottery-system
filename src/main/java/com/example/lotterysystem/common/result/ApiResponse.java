package com.example.lotterysystem.common.result;

/**
 * 统一 API 响应包装类
 * <p>
 * 所有 Controller 接口统一返回此类型，前端可据此做统一的错误处理和 Toast 提示。
 * 使用静态工厂方法创建，避免到处 new 对象。
 *
 * @param <T> data 字段的具体类型
 */
public class ApiResponse<T> {

    /** 业务状态码（对齐 HTTP 语义） */
    private int code;
    /** 提示信息（前端可直接展示） */
    private String message;
    /** 响应数据（可为 null） */
    private T data;

    private ApiResponse() {}

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ======================== 工厂方法 ========================

    /** 成功（无数据） */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getDefaultMessage(), null);
    }

    /** 成功（带数据） */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getDefaultMessage(), data);
    }

    /** 成功（自定义数据 + 消息），data 在前方便类型推断 */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /** 失败（使用预定义状态码） */
    public static <T> ApiResponse<T> fail(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getDefaultMessage(), null);
    }

    /** 失败（自定义消息） */
    public static <T> ApiResponse<T> fail(ResultCode resultCode, String message) {
        return new ApiResponse<>(resultCode.getCode(), message, null);
    }

    /** 失败（完全自定义） */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // ======================== Getter / Setter ========================

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
