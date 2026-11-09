package com.example.elibrary.common.dto;

import com.example.elibrary.common.response.LoanStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 借阅记录 DTO。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@Accessors(chain = true)
public class LoanDTO {

    private Long id;              // 主键ID
    private Long bookId;          // 关联书籍ID
    private String bookTitle;     // 书名（冗余展示，避免二次查询）
    private Long memberId;        // 关联会员ID
    private LocalDate loanDate;   // 借出日期
    private LocalDate dueDate;    // 应还日期
    private LocalDate returnDate; // 实际归还日期
    private LoanStatus status;    // 借阅状态：ACTIVE/RETURNED
    private boolean overdue;      // 是否已逾期
}
