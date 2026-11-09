package com.example.elibrary.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 书籍详情 DTO：含简介与总册数。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@Accessors(chain = true)
public class BookDetailDTO {

    private Long id;             // 主键ID
    private String isbn;         // ISBN 书目编号
    private String title;        // 书名
    private String author;       // 作者
    private String description;  // 内容简介
    private Integer totalCopies; // 总馆藏册数
    private boolean available;   // 是否可借
}
