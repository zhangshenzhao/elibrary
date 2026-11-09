package com.example.elibrary.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 新增书籍请求参数（入参校验）。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@Accessors(chain = true)
public class BookCreateRequest {

    @NotBlank(message = "ISBN 不能为空")
    private String isbn;             // ISBN 书目编号，唯一

    @NotBlank(message = "书名不能为空")
    private String title;            // 书名

    private String author;           // 作者

    private String description;      // 内容简介

    @Min(value = 1, message = "总册数至少为 1")
    private Integer totalCopies = 1; // 总馆藏册数，默认 1
}
