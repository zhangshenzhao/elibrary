-- 电子图书馆服务 — MySQL 建表 DDL（MyBatis 不自动建表，需手动执行本脚本）
-- 字符集：utf8mb4，所有表与字段均带中文 COMMENT
-- 索引写在 CREATE TABLE 内部，避免脚本重复执行时因索引已存在报错
-- 执行顺序：先 DROP（如已存在旧表），再按 book → member → loan 建表

CREATE DATABASE IF NOT EXISTS elibrary DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE elibrary;

-- ----------------------------
-- 书籍表：馆藏书目与库存信息
-- ----------------------------
CREATE TABLE IF NOT EXISTS book (
    id              BIGINT       AUTO_INCREMENT  COMMENT '主键ID',
    isbn            VARCHAR(13)  NOT NULL        COMMENT 'ISBN 书目编号，唯一',
    title           VARCHAR(200) NOT NULL        COMMENT '书名',
    author          VARCHAR(100)                 COMMENT '作者',
    description     TEXT                         COMMENT '内容简介',
    total_copies    INT          NOT NULL DEFAULT 1  COMMENT '总馆藏册数',
    available_copies INT         NOT NULL DEFAULT 1  COMMENT '当前可借册数',
    version         BIGINT       NOT NULL DEFAULT 0  COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_isbn (isbn),
    CONSTRAINT chk_book_copies CHECK (available_copies BETWEEN 0 AND total_copies)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书籍表：馆藏书目与库存信息';

-- ----------------------------
-- 会员表：借阅人基础信息
-- ----------------------------
CREATE TABLE IF NOT EXISTS member (
    id      BIGINT        AUTO_INCREMENT  COMMENT '主键ID',
    name    VARCHAR(100)  NOT NULL        COMMENT '会员姓名',
    email   VARCHAR(150)  NOT NULL        COMMENT '邮箱，唯一',
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表：借阅人基础信息';

-- ----------------------------
-- 借阅记录表：会员借还书的流水
-- ----------------------------
CREATE TABLE IF NOT EXISTS loan (
    id          BIGINT       AUTO_INCREMENT  COMMENT '主键ID',
    book_id     BIGINT       NOT NULL        COMMENT '关联书籍ID',
    member_id   BIGINT       NOT NULL        COMMENT '关联会员ID',
    loan_date   DATE         NOT NULL        COMMENT '借出日期',
    due_date    DATE         NOT NULL        COMMENT '应还日期',
    return_date DATE                          COMMENT '实际归还日期',
    status      VARCHAR(20)  NOT NULL        COMMENT '借阅状态：ACTIVE/RETURNED',
    version     BIGINT       NOT NULL DEFAULT 0  COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    CONSTRAINT fk_loan_book   FOREIGN KEY (book_id)   REFERENCES book(id),
    CONSTRAINT fk_loan_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT chk_loan_status CHECK (status IN ('ACTIVE', 'RETURNED')),
    INDEX idx_loan_member (member_id),
    INDEX idx_loan_book (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表：会员借还书的流水';

-- ----------------------------
-- 演示种子数据（首次建库时写入，依赖唯一键 uk_book_isbn / uk_member_email，重复执行安全）
-- ----------------------------
INSERT IGNORE INTO book (isbn, title, author, description, total_copies, available_copies) VALUES
('9787111213826', 'Java并发编程实战', 'Brian Goetz', 'Java 并发领域经典著作，深入讲解线程安全、锁、并发工具等。', 3, 3),
('9787115547255', '高性能MySQL', 'Baron Schwartz', 'MySQL 架构、索引、事务与优化的权威指南。', 2, 2),
('9787111641253', '深入理解Java虚拟机', '周志明', 'JVM 内存模型、垃圾回收、类加载机制与性能调优。', 5, 5);

INSERT IGNORE INTO member (name, email) VALUES
('张三', 'zhangsan@example.com'),
('李四', 'lisi@example.com');
