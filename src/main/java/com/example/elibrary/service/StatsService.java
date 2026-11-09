package com.example.elibrary.service;

import com.example.elibrary.common.dto.StatsDTO;

/**
 * 统计服务接口。
 *
 * @author makejava
 * @since 2026-11-07
 */
public interface StatsService {

    //并行聚合书籍/会员/活跃借阅三项计数
    StatsDTO overview();
}
