package com.example.elibrary.controller;

import com.example.elibrary.common.response.PageResult;
import com.example.elibrary.common.response.ResultDto;
import com.example.elibrary.common.dto.BookCreateRequest;
import com.example.elibrary.common.dto.BookDTO;
import com.example.elibrary.common.dto.BookDetailDTO;
import com.example.elibrary.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 书籍相关 REST 接口。
 * <p>所有响应统一包装为 {@link ResultDto}，HTTP 状态码与业务码的映射见 GlobalExceptionHandler。
 *
 * @author makejava
 * @since 2026-11-07
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** 浏览书籍：分页 + 书名/作者模糊搜索 */
    @GetMapping
    public ResultDto<PageResult<BookDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResultDto.success(bookService.listBooks(keyword, page, size));
    }

    /** 查询单本书籍详情 */
    @GetMapping("/{id}")
    public ResultDto<BookDetailDTO> detail(@PathVariable Long id) {
        return ResultDto.success(bookService.getBookDetail(id));
    }

    /** 新增书籍，成功返回 201 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultDto<BookDTO> create(@Valid @RequestBody BookCreateRequest request) {
        return ResultDto.success(bookService.createBook(request));
    }
}
