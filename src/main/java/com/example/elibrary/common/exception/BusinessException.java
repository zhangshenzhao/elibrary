package com.example.elibrary.common.exception;

import com.example.elibrary.common.response.Code;

/**
 * 业务异常：携带一个 {@link Code}，由 GlobalExceptionHandler 统一转为对应 HTTP 状态与 ResultDto。
 *
 * @author makejava
 * @since 2026-11-07
 */
public class BusinessException extends RuntimeException {

    private final Code code;   // 业务状态码

    public BusinessException(Code code) {
        super(code.getMessage());
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
