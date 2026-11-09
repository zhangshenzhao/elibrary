package com.example.elibrary.vo;

import com.example.elibrary.common.response.LoanStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 借阅记录(Loan)实体类
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@NoArgsConstructor
@Accessors(fluent = true)
public class LoanVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;               // 主键ID
    private Long bookId;           // 关联书籍ID
    private Long memberId;         // 关联会员ID
    private LocalDate loanDate;    // 借出日期
    private LocalDate dueDate;     // 应还日期
    private LocalDate returnDate;  // 实际归还日期
    private LoanStatus status;     // 借阅状态：ACTIVE/RETURNED
    private Long version;          // 乐观锁版本号
}
