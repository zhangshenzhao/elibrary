package com.example.elibrary.common.exception;

import com.example.elibrary.common.response.Code;
import com.example.elibrary.common.response.ResultCode;
import com.example.elibrary.common.response.ResultDto;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理：业务异常 / 参数校验 / 唯一键冲突 / 兜底，统一转为 ResultDto。
 *
 * @author makejava
 * @since 2026-11-07
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常：按 Code 携带的 httpStatus 返回
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResultDto<Void>> handleBusiness(BusinessException e) {
        Code code = e.getCode();
        return ResponseEntity.status(code.getHttpStatus()).body(ResultDto.error(code));
    }

    // 参数校验异常：聚合字段错误信息，返回 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultDto<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ResultDto.error("400", message));
    }

    // 唯一键冲突兜底：按数据库报错区分 ISBN 重复与其他唯一约束冲突，统一返回 409
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ResultDto<Void>> handleDuplicateKey(DuplicateKeyException e) {
        String detail = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage() : "";
        Code code = detail.toLowerCase().contains("isbn")
                ? ResultCode.ISBN_DUPLICATE : ResultCode.DATA_CONFLICT;
        return ResponseEntity.status(409).body(ResultDto.error(code));
    }

    // 兜底异常：返回 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultDto<Void>> handleAll(Exception e) {
        return ResponseEntity.status(500)
                .body(ResultDto.error("500", "服务器内部错误: " + e.getMessage()));
    }
}
