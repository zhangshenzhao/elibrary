package com.example.elibrary.service.impl;

import com.example.elibrary.common.exception.BusinessException;
import com.example.elibrary.common.response.PageResult;
import com.example.elibrary.common.response.ResultCode;
import com.example.elibrary.common.dto.BookCreateRequest;
import com.example.elibrary.common.dto.BookDTO;
import com.example.elibrary.common.dto.BookDetailDTO;
import com.example.elibrary.mapper.BookMapper;
import com.example.elibrary.service.BookService;
import com.example.elibrary.vo.BookVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 书籍(Book)表服务实现类
 *
 * @author makejava
 * @since 2026-11-07
 */
@Slf4j
@Service("bookService")
public class BookServiceImpl implements BookService {

    @Autowired
    private BookMapper bookMapper;

    /**
     * 分页浏览书籍：keyword 非空时按书名/作者模糊匹配，按 id 倒序。
     * 列表 DTO 仅暴露 available 布尔，不泄露具体库存册数。
     */
    @Override
    public PageResult<BookDTO> listBooks(String keyword, int page, int size) {
        //分页参数兜底：page 从 0 开始，size 限制在 [1,100]，避免负偏移与全表拉取
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = safePage * safeSize;
        String kw = (keyword == null) ? null : keyword.trim();
        //查询当前页数据
        List<BookVo> list = bookMapper.selectPage(kw, offset, safeSize);
        //查询总记录数
        long total = bookMapper.countPage(kw);
        List<BookDTO> dtoList = list.stream().map(this::toDTO).collect(Collectors.toList());
        return new PageResult<>(dtoList, total, safePage, safeSize);
    }

    /**
     * 查询书籍详情，不存在抛 BOOK_NOT_FOUND。
     */
    @Override
    public BookDetailDTO getBookDetail(Long id) {
        BookVo book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        return toDetailDTO(book);
    }

    /**
     * 新增书籍：先校验 ISBN 唯一，再落库。新增时可借册数 = 总册数。
     */
    @Override
    @Transactional
    public BookDTO createBook(BookCreateRequest request) {
        if (bookMapper.countByIsbn(request.getIsbn()) > 0) {
            throw new BusinessException(ResultCode.ISBN_DUPLICATE);
        }
        Integer total = request.getTotalCopies() != null ? request.getTotalCopies() : 1;
        BookVo book = new BookVo()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .description(request.getDescription())
                .totalCopies(total)
                .availableCopies(total);
        bookMapper.insert(book);
        return toDTO(book);
    }

    // 实体 → 列表 DTO（隐藏库存册数），链式塞入
    private BookDTO toDTO(BookVo book) {
        return new BookDTO()
                .setId(book.id())
                .setIsbn(book.isbn())
                .setTitle(book.title())
                .setAuthor(book.author())
                .setAvailable(book.availableCopies() != null && book.availableCopies() > 0);
    }

    // 实体 → 详情 DTO（含简介与总册数），链式塞入
    private BookDetailDTO toDetailDTO(BookVo book) {
        return new BookDetailDTO()
                .setId(book.id())
                .setIsbn(book.isbn())
                .setTitle(book.title())
                .setAuthor(book.author())
                .setDescription(book.description())
                .setTotalCopies(book.totalCopies())
                .setAvailable(book.availableCopies() != null && book.availableCopies() > 0);
    }
}
