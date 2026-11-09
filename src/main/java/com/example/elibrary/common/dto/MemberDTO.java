package com.example.elibrary.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 会员 DTO：对外暴露的会员信息（下拉选择用）。
 * 不直接使用 MemberVo（其 fluent 访问器无 get 前缀，Jackson 无法序列化）。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@Accessors(chain = true)
public class MemberDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;      // 主键ID
    private String name;  // 会员姓名
    private String email; // 邮箱
}
