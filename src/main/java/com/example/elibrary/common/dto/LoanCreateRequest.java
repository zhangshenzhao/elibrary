package com.example.elibrary.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 借阅请求参数（入参校验）。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@Accessors(chain = true)
public class LoanCreateRequest {

    @NotNull(message = "bookId 不能为空")
    private Long bookId;    // 关联书籍ID

    @NotNull(message = "memberId 不能为空")
    private Long memberId;  // 关联会员ID
}
