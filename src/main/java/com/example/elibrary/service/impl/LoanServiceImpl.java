package com.example.elibrary.service.impl;

import com.example.elibrary.common.exception.BusinessException;
import com.example.elibrary.common.response.LoanStatus;
import com.example.elibrary.common.response.ResultCode;
import com.example.elibrary.common.dto.LoanCreateRequest;
import com.example.elibrary.common.dto.LoanDTO;
import com.example.elibrary.mapper.BookMapper;
import com.example.elibrary.mapper.LoanMapper;
import com.example.elibrary.mapper.MemberMapper;
import com.example.elibrary.service.LoanService;
import com.example.elibrary.service.event.LoanEvent;
import com.example.elibrary.service.event.LoanEventBus;
import com.example.elibrary.vo.BookVo;
import com.example.elibrary.vo.LoanVo;
import com.example.elibrary.vo.MemberVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 借阅记录(Loan)表服务实现类
 *
 * @author makejava
 * @since 2026-11-07
 */
@Slf4j
@Service("loanService")
public class LoanServiceImpl implements LoanService {

    /** 借阅期限天数 */
    private static final int LOAN_PERIOD_DAYS = 14;

    @Autowired
    private LoanMapper loanMapper;
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private LoanEventBus eventBus;

    /**
     * 借阅：校验书/会员存在 → 校验未重复借阅 → 条件原子扣减库存 → 建借阅记录。
     * 库存扣减用 UPDATE ... WHERE available_copies > 0，受影响行数 0 即无可借馆藏，
     * 借助数据库行锁避免并发超卖（替代 JPA 的 @Version 乐观锁）。
     */
    @Override
    @Transactional
    public LoanDTO createLoan(LoanCreateRequest request) {
        BookVo book = bookMapper.selectById(request.getBookId());
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        MemberVo member = memberMapper.selectById(request.getMemberId());
        if (member == null) {
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        //重复借阅校验：同一会员对同一本书存在未归还记录
        LoanVo existing = loanMapper.selectActiveByBookAndMember(request.getBookId(), request.getMemberId());
        if (existing != null) {
            throw new BusinessException(ResultCode.ALREADY_BORROWED);
        }
        //原子扣减库存：仅当 available_copies > 0 时成功
        int affected = bookMapper.decrementAvailable(request.getBookId());
        if (affected == 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE);
        }
        LocalDate today = LocalDate.now();
        LoanVo loan = new LoanVo()
                .bookId(request.getBookId())
                .memberId(request.getMemberId())
                .loanDate(today)
                .dueDate(today.plusDays(LOAN_PERIOD_DAYS))
                .status(LoanStatus.ACTIVE);
        //原子写入：NOT EXISTS 保证同一会员同一本书最多一条活跃借阅，并发下受影响行数 0 即为重复借阅
        int inserted = loanMapper.insert(loan);
        if (inserted == 0) {
            throw new BusinessException(ResultCode.ALREADY_BORROWED);
        }
        //发布借阅事件到事件总线（异步落内存，不阻塞接口）
        eventBus.publish(new LoanEvent()
                .setMemberId(member.id()).setMemberName(member.name())
                .setBookId(book.id()).setBookTitle(book.title())
                .setAction(LoanEvent.Action.BORROW)
                .setTime(LocalDateTime.now()));
        return toDTO(loan, book.title());
    }

    /**
     * 归还：校验记录存在 → 校验归属 → 校验活跃状态 → 条件更新归还 → 回补库存。
     * 归还用 UPDATE ... WHERE status = 'ACTIVE'，受影响行数 0 表示已归还或并发冲突。
     */
    @Override
    @Transactional
    public LoanDTO returnLoan(Long loanId, Long memberId) {
        LoanVo loan = loanMapper.selectById(loanId);
        if (loan == null) {
            throw new BusinessException(ResultCode.LOAN_NOT_FOUND);
        }
        //归属校验：只能归还自己的借阅记录
        if (!loan.memberId().equals(memberId)) {
            throw new BusinessException(ResultCode.NOT_OWNER);
        }
        if (loan.status() != LoanStatus.ACTIVE) {
            throw new BusinessException(ResultCode.LOAN_NOT_ACTIVE);
        }
        LocalDate today = LocalDate.now();
        //条件更新：仅活跃记录可被标记归还
        int affected = loanMapper.markReturned(loanId, today);
        if (affected == 0) {
            //并发下已被他人归还
            throw new BusinessException(ResultCode.LOAN_NOT_ACTIVE);
        }
        //回补库存
        bookMapper.incrementAvailable(loan.bookId());
        //同步内存状态，用于 DTO 输出
        loan.status(LoanStatus.RETURNED).returnDate(today);
        BookVo book = bookMapper.selectById(loan.bookId());
        //发布归还事件到事件总线
        MemberVo member = memberMapper.selectById(loan.memberId());
        eventBus.publish(new LoanEvent()
                .setMemberId(loan.memberId())
                .setMemberName(member != null ? member.name() : null)
                .setBookId(loan.bookId())
                .setBookTitle(book != null ? book.title() : null)
                .setAction(LoanEvent.Action.RETURN)
                .setTime(LocalDateTime.now()));
        return toDTO(loan, book != null ? book.title() : null);
    }

    /**
     * 查询会员的借阅记录：activeOnly=true 只返回未归还（ACTIVE）的记录。
     */
    @Override
    public List<LoanDTO> listLoans(Long memberId, boolean activeOnly) {
        List<LoanVo> loans = loanMapper.selectByMember(memberId, activeOnly);
        return loans.stream().map(loan -> {
            BookVo book = bookMapper.selectById(loan.bookId());
            return toDTO(loan, book != null ? book.title() : null);
        }).collect(Collectors.toList());
    }

    // 实体 → DTO，含逾期判断，链式塞入
    private LoanDTO toDTO(LoanVo loan, String bookTitle) {
        return new LoanDTO()
                .setId(loan.id())
                .setBookId(loan.bookId())
                .setBookTitle(bookTitle)
                .setMemberId(loan.memberId())
                .setLoanDate(loan.loanDate())
                .setDueDate(loan.dueDate())
                .setReturnDate(loan.returnDate())
                .setStatus(loan.status())
                .setOverdue(loan.status() == LoanStatus.ACTIVE
                        && LocalDate.now().isAfter(loan.dueDate()));
    }
}
