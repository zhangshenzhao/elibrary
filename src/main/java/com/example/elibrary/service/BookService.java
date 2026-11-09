package com.example.elibrary.service;

import com.example.elibrary.common.response.PageResult;
import com.example.elibrary.common.dto.BookCreateRequest;
import com.example.elibrary.common.dto.BookDTO;
import com.example.elibrary.common.dto.BookDetailDTO;

/**
 * 书籍(Book)表服务接口
 *
 * @author makejava
 * @since 2026-11-07
 */
public interface BookService {

    //分页浏览书籍：keyword 非空时按书名/作者模糊搜索
    PageResult<BookDTO> listBooks(String keyword, int page, int size);

    //查询单本书籍详情
    BookDetailDTO getBookDetail(Long id);

    //新增书籍
    BookDTO createBook(BookCreateRequest request);
}
