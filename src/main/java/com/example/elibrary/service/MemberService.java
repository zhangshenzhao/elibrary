package com.example.elibrary.service;

import com.example.elibrary.common.dto.MemberDTO;

import java.util.List;

/**
 * 会员(Member)表服务接口
 *
 * @author makejava
 * @since 2026-11-07
 */
public interface MemberService {

    //查询全部会员
    List<MemberDTO> listAll();
}
