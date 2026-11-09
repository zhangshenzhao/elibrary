package com.example.elibrary.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 书籍列表 DTO：仅暴露可借布尔，不泄露具体库存册数。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@Accessors(chain = true)
public class BookDTO {

    private Long id;            // 主键ID
    private String isbn;        // ISBN 书目编号
    private String title;       // 书名
    private String author;      // 作者
    private boolean available;  // 是否可借（隐藏具体库存册数）
}
