package com.example.elibrary.common.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 轻量分页结果，替代 Spring Data 的 Page（本项目不依赖 spring-data-commons）。
 * MyBatis 手写 count + limit/offset 后组装。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list;  // 当前页数据
    private long total;    // 总记录数
    private int page;      // 当前页（从 0 开始）
    private int size;      // 每页大小

    public PageResult() {
    }

    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
