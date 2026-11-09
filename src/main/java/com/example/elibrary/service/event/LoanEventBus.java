package com.example.elibrary.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 借还事件总线：解耦请求线程与事件落内存，避免借/还接口被事件处理拖慢。
 * <p>用到的并发容器：
 * <ul>
 *   <li>{@link BlockingQueue}（LinkedBlockingQueue）：有界缓冲，请求线程 offer 非阻塞，满则丢弃并告警；</li>
 *   <li>{@link ConcurrentHashMap}：会员ID → 该会员事件队列的映射；</li>
 *   <li>{@link ConcurrentLinkedQueue}：每个会员的事件列表，无锁写、多线程读安全；</li>
 *   <li>{@link CopyOnWriteArrayList}：全局最近事件，读多写少，读不加锁；</li>
 *   <li>{@link Set}（ConcurrentHashMap.newKeySet）：本进程内产生过事件的会员集合。</li>
 * </ul>
 * 单消费者线程运行在 bizExecutor 上，轮询队列并分发。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Slf4j
@Component
public class LoanEventBus {

    /** 事件缓冲队列容量，满则丢弃（offer 返回 false）并记录告警 */
    private static final int QUEUE_CAPACITY = 1024;
    /** 每个会员保留的最近事件数 */
    private static final int PER_MEMBER_MAX = 50;
    /** 全局保留的最近事件数 */
    private static final int GLOBAL_MAX = 100;
    /** 消费者 poll 超时，便于响应停止信号 */
    private static final long POLL_TIMEOUT_SECONDS = 1L;

    // Queue：有界阻塞队列，作为请求线程与消费者之间的缓冲
    private final BlockingQueue<LoanEvent> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    // Map → Queue：会员ID → 该会员的事件队列（无锁并发队列）
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<LoanEvent>> memberEvents = new ConcurrentHashMap<>();

    // List：全局最近事件（读多写少，CopyOnWriteArrayList 读不加锁）
    private final CopyOnWriteArrayList<LoanEvent> recentGlobal = new CopyOnWriteArrayList<>();

    // Set：本进程内产生过事件的会员ID集合（并发安全 Set）
    private final Set<Long> activeMembers = ConcurrentHashMap.newKeySet();

    @Autowired
    @Qualifier("bizExecutor")
    private ExecutorService bizExecutor;

    private volatile boolean running = true;
    private Thread consumerThread;

    @PostConstruct
    public void start() {
        // 提交消费者到业务线程池，长期运行
        bizExecutor.execute(this::consumeLoop);
        log.info("LoanEventBus 消费者已启动");
    }

    /** 发布事件：请求线程调用，offer 非阻塞，不影响接口延迟 */
    public void publish(LoanEvent event) {
        boolean offered = queue.offer(event);
        if (!offered) {
            log.warn("事件缓冲队列已满，丢弃事件: member={} book={} action={}",
                    event.getMemberId(), event.getBookId(), event.getAction());
        }
    }

    /** 消费者循环：从队列取事件，分发到各并发容器 */
    private void consumeLoop() {
        consumerThread = Thread.currentThread();
        while (running) {
            LoanEvent event;
            try {
                event = queue.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (event == null) {
                continue;
            }
            try {
                dispatch(event);
            } catch (Exception e) {
                log.error("事件分发异常", e);
            }
        }
        log.info("LoanEventBus 消费者已停止，队列剩余 {} 条", queue.size());
    }

    private void dispatch(LoanEvent event) {
        // Map：按会员聚合到并发队列，computeIfAbsent 保证只创建一次
        ConcurrentLinkedQueue<LoanEvent> q = memberEvents.computeIfAbsent(
                event.getMemberId(), k -> new ConcurrentLinkedQueue<>());
        q.offer(event);
        // 每个会员只保留最近 PER_MEMBER_MAX 条
        while (q.size() > PER_MEMBER_MAX) {
            q.poll();
        }
        // List：全局最近事件，写时复制
        recentGlobal.add(event);
        while (recentGlobal.size() > GLOBAL_MAX) {
            recentGlobal.remove(0);
        }
        // Set：记录有事件的会员
        activeMembers.add(event.getMemberId());
    }

    /** 读取某会员的最近事件（倒序：最新在前），快照返回，线程安全 */
    public List<LoanEvent> snapshotByMember(Long memberId) {
        ConcurrentLinkedQueue<LoanEvent> q = memberEvents.get(memberId);
        if (q == null) {
            return Collections.emptyList();
        }
        List<LoanEvent> list = new ArrayList<>(q);
        Collections.reverse(list);
        return list;
    }

    /** 读取全局最近事件（倒序：最新在前） */
    public List<LoanEvent> snapshotRecentGlobal() {
        List<LoanEvent> list = new ArrayList<>(recentGlobal);
        Collections.reverse(list);
        return list;
    }

    /** 当前有事件的会员数（演示 Set 的使用） */
    public int activeMemberCount() {
        return activeMembers.size();
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }
}
