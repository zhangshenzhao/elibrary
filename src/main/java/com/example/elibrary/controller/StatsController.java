package com.example.elibrary.controller;

import com.example.elibrary.common.dto.StatsDTO;
import com.example.elibrary.common.response.ResultDto;
import com.example.elibrary.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计概览 REST 接口。
 *
 * @author makejava
 * @since 2026-11-07
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    //首页统计：书籍/会员/活跃借阅三项计数并行聚合
    @GetMapping("/overview")
    public ResultDto<StatsDTO> overview() {
        return ResultDto.success(statsService.overview());
    }
}
