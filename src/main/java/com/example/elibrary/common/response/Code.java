package com.example.elibrary.common.response;

/**
 * 业务状态码封装：code(no) + message + httpStatus。
 * httpStatus 供 GlobalExceptionHandler 设置 HTTP 响应状态码。
 *
 * @author makejava
 * @since 2026-11-07
 */
public class Code {

    private String no;        // 业务状态码（成功为 "0"）
    private String message;   // 状态描述信息
    private int httpStatus;   // 对应的 HTTP 响应状态码

    public Code() {
    }

    public Code(String no, String message) {
        this(no, message, 200);
    }

    public Code(String no, String message, int httpStatus) {
        this.no = no;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getNo() {
        return this.no;
    }

    public String getMessage() {
        return this.message;
    }

    public int getHttpStatus() {
        return this.httpStatus;
    }
}
