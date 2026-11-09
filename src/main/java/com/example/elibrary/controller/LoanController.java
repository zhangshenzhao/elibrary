package com.example.elibrary.controller;

import com.example.elibrary.common.response.ResultDto;
import com.example.elibrary.common.dto.LoanCreateRequest;
import com.example.elibrary.common.dto.LoanDTO;
import com.example.elibrary.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 借阅相关 REST 接口。
 * <p>归还操作通过请求头 {@code X-Member-Id} 校验归属（鉴权简化，见 README）。
 *
 * @author makejava
 * @since 2026-11-07
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /** 借阅书籍，成功返回 201 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultDto<LoanDTO> create(@Valid @RequestBody LoanCreateRequest request) {
        return ResultDto.success(loanService.createLoan(request));
    }

    /** 归还书籍：X-Member-Id 必须与借阅人一致 */
    @PostMapping("/{id}/return")
    public ResultDto<LoanDTO> returnLoan(@PathVariable Long id,
                                         @RequestHeader("X-Member-Id") Long memberId) {
        return ResultDto.success(loanService.returnLoan(id, memberId));
    }

    /** 查看会员的借阅记录：activeOnly=true 只看未归还的 */
    @GetMapping
    public ResultDto<List<LoanDTO>> list(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResultDto.success(loanService.listLoans(memberId, activeOnly));
    }
}
