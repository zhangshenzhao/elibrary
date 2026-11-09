package com.example.elibrary.common.response;

/**
 * 借阅状态枚举，作为 LoanDTO.status / loan.status 列的取值，属于 API 响应数据契约的一部分。
 * ACTIVE 借阅中 / RETURNED 已归还。
 *
 * @author makejava
 * @since 2026-11-07
 */
public enum LoanStatus {
    ACTIVE,    // 借阅中
    RETURNED   // 已归还
}
