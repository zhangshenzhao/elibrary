package com.example.elibrary.common.response;

/**
 * 统一业务状态码常量。
 * 成功码为 "0"；业务异常码沿用 HTTP 状态码语义（no 即状态码字符串），并携带 httpStatus 供异常处理器使用。
 *
 * @author makejava
 * @since 2026-11-07
 */
public class ResultCode {

    // ===== 通用 =====
    public static final Code SUCCESS = new Code("0", "success", 200);
    public static final Code SERVER_INNER_ERROR = new Code("500", "服务器内部错误", 500);
    public static final Code DB_BUSY = new Code("503", "数据库繁忙，并发访问已达上限，请稍后重试", 503);

    // ===== 资源不存在 404 =====
    public static final Code BOOK_NOT_FOUND = new Code("404", "书籍不存在", 404);
    public static final Code MEMBER_NOT_FOUND = new Code("404", "会员不存在", 404);
    public static final Code LOAN_NOT_FOUND = new Code("404", "借阅记录不存在", 404);

    // ===== 业务冲突 409 =====
    public static final Code BOOK_NOT_AVAILABLE = new Code("409", "该书暂无可借馆藏", 409);
    public static final Code ALREADY_BORROWED = new Code("409", "您已借阅该书且未归还", 409);
    public static final Code ISBN_DUPLICATE = new Code("409", "ISBN 已存在", 409);
    public static final Code DATA_CONFLICT = new Code("409", "数据冲突，违反唯一约束", 409);

    // ===== 请求非法 400 =====
    public static final Code LOAN_NOT_ACTIVE = new Code("400", "该借阅记录不是活跃状态，无法归还", 400);

    // ===== 无权限 403 =====
    public static final Code NOT_OWNER = new Code("403", "无权操作他人的借阅记录", 403);
}
