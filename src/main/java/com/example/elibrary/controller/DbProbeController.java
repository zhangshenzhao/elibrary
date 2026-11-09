package com.example.elibrary.controller;

import com.example.elibrary.common.response.ResultDto;
import com.example.elibrary.mapper.SystemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据库并发探针（诊断用，非业务接口）。
 * <p>调用 {@code SELECT SLEEP(sec)} 持有一个数据库连接（以及 Semaphore 许可）约 sec 秒，
 * 用于确定性地证明 DbConcurrencyInterceptor 的并发上限为 10：
 * 并发发起 15+ 个请求，前 10 个应返回 200，其余在 3 秒内拿不到许可即返回 503 DB_BUSY。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Slf4j
@RestController
@RequestMapping("/api/probe")
public class DbProbeController {

    private final SystemMapper systemMapper;

    public DbProbeController(SystemMapper systemMapper) {
        this.systemMapper = systemMapper;
    }

    //持有 DB 连接 sec 秒，验证 Semaphore(10) 并发闸门
    @GetMapping("/db")
    public ResultDto<String> probeDb(@RequestParam(defaultValue = "0.5") double sec) {
        long start = System.currentTimeMillis();
        //SELECT SLEEP 会占用一个连接 + 一个信号量许可，持续约 sec 秒
        int ret = systemMapper.sleep(sec);
        long cost = System.currentTimeMillis() - start;
        log.info("DbProbe SLEEP({}s) 返回 {}，耗时 {}ms", sec, ret, cost);
        return ResultDto.success("ok:" + cost + "ms");
    }
}
