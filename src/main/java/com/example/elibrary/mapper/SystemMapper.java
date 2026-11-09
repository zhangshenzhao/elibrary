package com.example.elibrary.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 系统级诊断 SQL 映射：仅用于并发探针（SELECT SLEEP），不对应业务表。
 *
 * @author makejava
 * @since 2026-11-07
 */
public interface SystemMapper {

    //执行 SELECT SLEEP(sec)，持有数据库连接约 sec 秒，用于压测并发闸门
    int sleep(@Param("sec") double sec);
}
