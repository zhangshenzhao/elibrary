package com.example.elibrary.service.impl;

import com.example.elibrary.common.dto.MemberDTO;
import com.example.elibrary.mapper.MemberMapper;
import com.example.elibrary.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会员(Member)表服务实现类
 *
 * @author makejava
 * @since 2026-11-07
 */
@Slf4j
@Service("memberService")
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MemberMapper memberMapper;

    //查询全部会员，VO → DTO 映射（VO 为库内实体，不直接对外序列化）
    @Override
    public List<MemberDTO> listAll() {
        return memberMapper.selectAll().stream()
                .map(m -> new MemberDTO().setId(m.id()).setName(m.name()).setEmail(m.email()))
                .collect(Collectors.toList());
    }
}
