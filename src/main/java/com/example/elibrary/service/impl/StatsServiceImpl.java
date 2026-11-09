package com.example.elibrary.service.impl;

import com.example.elibrary.common.dto.StatsDTO;
import com.example.elibrary.common.exception.BusinessException;
import com.example.elibrary.mapper.BookMapper;
import com.example.elibrary.mapper.LoanMapper;
import com.example.elibrary.mapper.MemberMapper;
import com.example.elibrary.service.StatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * 统计服务实现：将三个相互独立的 COUNT 查询提交到 bizExecutor 线程池并行执行，
 * 再用 {@link CompletableFuture#allOf} 汇总。每个查询都会经过
 * {@code DbConcurrencyInterceptor} 的 Semaphore(10) 闸门——三者同时仅占用 3 个许可，
 * 演示线程池与信号量的协同：池负责并行，信号量负责对下游 DB 的并发上限兜底。
 * <p>显式指定 bizExecutor 而非默认 ForkJoinPool.commonPool，避免 I/O 密集任务阻塞公共池。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Slf4j
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private LoanMapper loanMapper;

    //复用业务线程池执行并行查询
    @Autowired
    @Qualifier("bizExecutor")
    private ExecutorService bizExecutor;

    @Override
    public StatsDTO overview() {
        CompletableFuture<Long> books = CompletableFuture.supplyAsync(
                () -> bookMapper.countPage(null), bizExecutor);
        CompletableFuture<Long> members = CompletableFuture.supplyAsync(
                () -> (long) memberMapper.countAll(), bizExecutor);
        CompletableFuture<Long> active = CompletableFuture.supplyAsync(
                () -> (long) loanMapper.countActive(), bizExecutor);

        try {
            CompletableFuture.allOf(books, members, active).join();
        } catch (CompletionException e) {
            //解包：线程内抛出的 BusinessException（如 DB_BUSY=503）需还原，
            //由全局异常处理器按其 httpStatus 返回，否则会被吞成 500
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException) {
                throw (BusinessException) cause;
            }
            throw e;
        }

        return new StatsDTO()
                .setBookCount(books.join())
                .setMemberCount(members.join())
                .setActiveLoanCount(active.join());
    }
}
