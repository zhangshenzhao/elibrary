package com.example.elibrary.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 书籍(Book)实体类
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@NoArgsConstructor
@Accessors(fluent = true)
public class BookVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;               // 主键ID
    private String isbn;           // ISBN 书目编号，唯一
    private String title;          // 书名
    private String author;         // 作者
    private String description;    // 内容简介
    private Integer totalCopies;   // 总馆藏册数
    private Integer availableCopies; // 当前可借册数
    private Long version;          // 乐观锁版本号
}
