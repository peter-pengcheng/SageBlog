package com.sage.blog.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    public Result() {
    }

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回结果
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
    }

    /**
     * 成功返回结果
     *
     * @param data 获取的数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功返回结果
     *
     * @param data    获取的数据
     * @param message 提示信息
     */
    public static <T> Result<T> success(T data, String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回结果
     */
    public static <T> Result<T> failed() {
        return new Result<>(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMessage());
    }

    /**
     * 失败返回结果
     *
     * @param message 提示信息
     */
    public static <T> Result<T> failed(String message) {
        return new Result<>(ResultCode.FAILED.getCode(), message);
    }

    /**
     * 失败返回结果
     *
     * @param errorCode 错误码
     */
    public static <T> Result<T> failed(IErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 参数验证失败返回结果
     */
    public static <T> Result<T> validateFailed() {
        return failed(ResultCode.VALIDATE_FAILED);
    }

    /**
     * 参数验证失败返回结果
     *
     * @param message 提示信息
     */
    public static <T> Result<T> validateFailed(String message) {
        return new Result<>(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    /**
     * 未登录返回结果
     */
    public static <T> Result<T> unauthorized() {
        return failed(ResultCode.UNAUTHORIZED);
    }

    /**
     * 未授权返回结果
     */
    public static <T> Result<T> forbidden() {
        return failed(ResultCode.FORBIDDEN);
    }
}