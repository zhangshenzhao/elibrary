package com.example.elibrary.mapper;

import com.example.elibrary.vo.LoanVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 借阅记录(Loan)表数据库访问层
 *
 * @author makejava
 * @since 2026-11-07
 */
public interface LoanMapper {

    //根据主键查询借阅记录
    LoanVo selectById(@Param("id") Long id);

    //查询某会员对某本书的活跃（未归还）借阅记录，用于重复借阅校验
    LoanVo selectActiveByBookAndMember(@Param("bookId") Long bookId,
                                       @Param("memberId") Long memberId);

    //查询某会员的借阅记录：activeOnly=true 时只返回未归还的，按借出日期倒序
    List<LoanVo> selectByMember(@Param("memberId") Long memberId,
                                @Param("activeOnly") boolean activeOnly);

    //统计活跃（未归还）借阅总数（首页统计用）
    int countActive();

    //新增借阅记录（useGeneratedKeys 回填自增主键）
    int insert(LoanVo loan);

    //归还：将状态置为 RETURNED 并记录归还日期，仅当 status='ACTIVE' 时成功，返回受影响行数
    int markReturned(@Param("id") Long id,
                     @Param("returnDate") LocalDate returnDate);
}
