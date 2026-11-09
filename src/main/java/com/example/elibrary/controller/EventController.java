package com.example.elibrary.controller;

import com.example.elibrary.common.response.ResultDto;
import com.example.elibrary.service.event.LoanEvent;
import com.example.elibrary.service.event.LoanEventBus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作记录（借还事件）查询 REST 接口。
 * 事件由 LoanServiceImpl 发布到 LoanEventBus，异步落入内存并发容器，这里只读快照。
 *
 * @author makejava
 * @since 2026-11-07
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final LoanEventBus eventBus;

    public EventController(LoanEventBus eventBus) {
        this.eventBus = eventBus;
    }

    //全局最近操作记录（最新在前），读 CopyOnWriteArrayList 快照
    @GetMapping("/recent")
    public ResultDto<List<LoanEvent>> recent() {
        return ResultDto.success(eventBus.snapshotRecentGlobal());
    }

    //某会员的操作记录（最新在前），读 ConcurrentLinkedQueue 快照
    @GetMapping("/members/{memberId}")
    public ResultDto<List<LoanEvent>> byMember(@PathVariable Long memberId) {
        return ResultDto.success(eventBus.snapshotByMember(memberId));
    }

    //产生过事件的会员数（演示 ConcurrentHashMap.newKeySet 的使用）
    @GetMapping("/active-count")
    public ResultDto<Integer> activeCount() {
        return ResultDto.success(eventBus.activeMemberCount());
    }
}
