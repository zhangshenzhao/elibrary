package com.example.elibrary.service;

import com.example.elibrary.common.dto.LoanCreateRequest;
import com.example.elibrary.common.dto.LoanDTO;

import java.util.List;

/**
 * 借阅记录(Loan)表服务接口
 *
 * @author makejava
 * @since 2026-11-07
 */
public interface LoanService {

    //借阅书籍
    LoanDTO createLoan(LoanCreateRequest request);

    //归还书籍（memberId 校验归属）
    LoanDTO returnLoan(Long loanId, Long memberId);

    //查询会员的借阅记录：activeOnly=true 只看未归还
    List<LoanDTO> listLoans(Long memberId, boolean activeOnly);
}
