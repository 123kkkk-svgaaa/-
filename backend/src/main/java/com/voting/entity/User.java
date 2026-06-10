package com.voting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体 — 仅存钱包地址和登录 nonce，不存实名信息
 */
@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 钱包地址 (唯一) */
    private String walletAddress;

    /** 登录签名随机数 */
    private String nonce;

    private LocalDateTime createdAt;
}
