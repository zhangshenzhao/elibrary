package com.example.elibrary.common.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 统一响应包装：{code, msg, data}，code 为字符串，成功为 "0"。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@Accessors(chain = true)
public class ResultDto<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;   // 业务状态码（成功 "0"）
    private String msg;    // 状态描述
    private T data;        // 响应数据

    public ResultDto() {
    }

    public ResultDto(Code result, T data) {
        this.code = result.getNo();
        this.msg = result.getMessage();
        this.data = data;
    }

    public ResultDto(Code result) {
        this.code = result.getNo();
        this.msg = result.getMessage();
    }

    // 构造成功响应
    public static <T> ResultDto<T> success(T data) {
        return new ResultDto<>(ResultCode.SUCCESS, data);
    }

    // 用 Code 构造错误响应
    public static <T> ResultDto<T> error(Code code) {
        return error(code.getNo(), code.getMessage());
    }

    // 用状态码与描述构造错误响应（链式塞入）
    public static <T> ResultDto<T> error(String code, String msg) {
        return new ResultDto<T>().setCode(code).setMsg(msg);
    }
}
