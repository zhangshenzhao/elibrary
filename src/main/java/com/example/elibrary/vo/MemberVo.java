package com.example.elibrary.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 会员(Member)实体类
 *
 * @author makejava
 * @since 2026-11-07
 */
@Data
@NoArgsConstructor
@Accessors(fluent = true)
public class MemberVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;       // 主键ID
    private String name;   // 会员姓名
    private String email;  // 邮箱，唯一
}
