package com.example.elibrary.controller;

import com.example.elibrary.common.dto.MemberDTO;
import com.example.elibrary.common.response.ResultDto;
import com.example.elibrary.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员相关 REST 接口。
 *
 * @author makejava
 * @since 2026-11-07
 */
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    //查询全部会员（供前端下拉选择当前操作人）
    @GetMapping
    public ResultDto<List<MemberDTO>> list() {
        return ResultDto.success(memberService.listAll());
    }
}
