package com.example.elibrary.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 首页统计概览 DTO：三项计数并行聚合返回。
 * 注意必须是普通 @Data（getXxx 风格），不可用 fluent VO，否则 Jackson 无法序列化。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@Accessors(chain = true)
public class StatsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long bookCount;       // 书籍总数
    private long memberCount;     // 会员总数
    private long activeLoanCount; // 借阅中（未归还）数量
}
