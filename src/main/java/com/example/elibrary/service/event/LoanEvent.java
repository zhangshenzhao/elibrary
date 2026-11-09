package com.example.elibrary.service.event;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借还事件：请求线程发布到事件总线，异步落入内存并发容器，供前端实时展示操作记录。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@Accessors(chain = true)
public class LoanEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 操作类型 */
    public enum Action {
        BORROW,  // 借阅
        RETURN   // 归还
    }

    private Long memberId;        // 会员ID
    private String memberName;    // 会员姓名（冗余，避免读时二次查库）
    private Long bookId;          // 书籍ID
    private String bookTitle;     // 书名（冗余展示）
    private Action action;        // 操作类型
    private LocalDateTime time;   // 发生时间
}
