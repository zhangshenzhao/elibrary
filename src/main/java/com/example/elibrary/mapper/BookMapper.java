package com.example.elibrary.mapper;

import com.example.elibrary.vo.BookVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 书籍(Book)表数据库访问层
 *
 * @author makejava
 * @since 2026-11-07
 */
public interface BookMapper {

    //根据主键查询书籍
    BookVo selectById(@Param("id") Long id);

    //统计指定 ISBN 的数量（用于唯一校验）
    int countByIsbn(@Param("isbn") String isbn);

    //分页查询书籍：keyword 非空时按书名/作者模糊匹配，按 id 倒序
    List<BookVo> selectPage(@Param("keyword") String keyword,
                            @Param("offset") int offset,
                            @Param("size") int size);

    //统计分页查询的总记录数
    long countPage(@Param("keyword") String keyword);

    //新增书籍（useGeneratedKeys 回填自增主键）
    int insert(BookVo book);

    //借出一册：可借数 -1，仅当 available_copies > 0 时成功，返回受影响行数
    int decrementAvailable(@Param("id") Long id);

    //归还一册：可借数 +1，仅当 available_copies < total_copies 时成功，返回受影响行数
    int incrementAvailable(@Param("id") Long id);
}
