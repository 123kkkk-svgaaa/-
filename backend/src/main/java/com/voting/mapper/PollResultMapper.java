package com.voting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voting.entity.PollResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PollResultMapper extends BaseMapper<PollResult> {

    /** 插入或更新票数 (ON DUPLICATE KEY UPDATE) */
    @Update("INSERT INTO poll_results (poll_id, option_index, vote_count) " +
            "VALUES (#{pollId}, #{optionIndex}, #{voteCount}) " +
            "ON DUPLICATE KEY UPDATE vote_count = #{voteCount}, updated_at = NOW()")
    int upsertVoteCount(@Param("pollId") Long pollId,
                        @Param("optionIndex") Integer optionIndex,
                        @Param("voteCount") Integer voteCount);
}
