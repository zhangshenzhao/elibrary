package com.example.elibrary.config;

import com.example.elibrary.common.exception.BusinessException;
import com.example.elibrary.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 数据库并发闸门：用公平 Semaphore(10) 限制同一时刻访问 MySQL 的并发数不超过 10。
 * 拦截 MyBatis {@link Executor} 的 query/update（即真正向数据库发 SQL 的位置），
 * 获取许可失败（超时）则抛 DB_BUSY(503)，由全局异常处理器返回。
 * try/finally 保证许可证必定释放，避免泄漏导致整体停滞。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class DbConcurrencyInterceptor implements Interceptor {

    /** 并发访问 MySQL 的最大许可数 */
    private static final int MAX_CONCURRENT_DB = 10;
    /** 等待许可的超时时间（毫秒），超时即返回 503 而非无限阻塞请求线程 */
    private static final long ACQUIRE_TIMEOUT_MS = 3000L;

    // fair=true：先到先得，避免高并发下的饥饿
    private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_DB, true);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SERVER_INNER_ERROR);
        }
        if (!acquired) {
            // 并发已达上限，快速失败
            throw new BusinessException(ResultCode.DB_BUSY);
        }
        try {
            return invocation.proceed();
        } finally {
            // 无论成功失败都释放许可，防止泄漏
            semaphore.release();
        }
    }

    /**
     * MyBatis 3.4.x 中 Interceptor.plugin 仍是抽象方法：用 Plugin.wrap 按 @Signature 生成代理。
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * MyBatis 3.4.x 中 Interceptor.setProperties 仍是抽象方法，必须实现（无属性可配置，空实现即可）。
     */
    @Override
    public void setProperties(Properties properties) {
        // no-op
    }
}
