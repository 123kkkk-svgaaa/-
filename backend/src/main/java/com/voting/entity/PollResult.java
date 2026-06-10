package com.voting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投票结果实体 — 每个选项的得票数快照
 */
@Data
@TableName("poll_results")
public class PollResult {

    private Long pollId;

    private Integer optionIndex;

    private Integer voteCount;

    private LocalDateTime updatedAt;
}
