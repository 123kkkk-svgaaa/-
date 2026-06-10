package com.voting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投票实体 — 对应链上 Poll 结构
 */
@Data
@TableName(value = "polls", autoResultMap = true)
public class Poll {

    /** 使用链上 pollId 作为主键 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 创建者钱包地址 */
    private String creatorAddress;

    private String title;

    private String description;

    /** 选项列表 JSON 存储，如 ["张三","李四","王五"] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 创建投票的链上交易哈希 */
    private String txHash;

    private LocalDateTime createdAt;
}
