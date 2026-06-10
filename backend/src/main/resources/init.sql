-- ============================================
-- 去中心化投票系统 — 数据库初始化
-- 使用: MySQL 8
-- ============================================

CREATE DATABASE IF NOT EXISTS dapp_voting DEFAULT CHARSET utf8mb4;

USE dapp_voting;

-- 投票表
CREATE TABLE IF NOT EXISTS polls (
    id BIGINT PRIMARY KEY COMMENT '对应链上 pollId',
    creator_address VARCHAR(42) NOT NULL COMMENT '创建者钱包地址',
    title VARCHAR(200) NOT NULL COMMENT '投票标题',
    description TEXT COMMENT '投票描述',
    options JSON NOT NULL COMMENT '选项列表 JSON',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    tx_hash VARCHAR(66) COMMENT '创建交易哈希',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_creator (creator_address),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票表';

-- 投票结果表
CREATE TABLE IF NOT EXISTS poll_results (
    poll_id BIGINT NOT NULL COMMENT '投票 ID',
    option_index INT NOT NULL COMMENT '选项索引',
    vote_count INT DEFAULT 0 COMMENT '得票数',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (poll_id, option_index),
    INDEX idx_poll_id (poll_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票结果表';

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增 ID',
    wallet_address VARCHAR(42) NOT NULL UNIQUE COMMENT '钱包地址',
    nonce VARCHAR(64) COMMENT '登录随机数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX idx_wallet (wallet_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
