package com.example.elibrary.mapper;

import com.example.elibrary.vo.MemberVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会员(Member)表数据库访问层
 *
 * @author makejava
 * @since 2026-11-07
 */
public interface MemberMapper {

    //根据主键查询会员
    MemberVo selectById(@Param("id") Long id);

    //查询全部会员（供前端下拉选择）
    List<MemberVo> selectAll();

    //统计会员总数（首页统计用）
    int countAll();

    //统计指定邮箱的数量（用于唯一校验）
    int countByEmail(@Param("email") String email);

    //新增会员（useGeneratedKeys 回填自增主键）
    int insert(MemberVo member);
}
