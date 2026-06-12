package com.voting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voting.entity.Poll;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PollMapper extends BaseMapper<Poll> {

    /** 获取数据库中最大的 poll ID，用于增量同步判断 */
    @Select("SELECT COALESCE(MAX(id), -1) FROM polls")
    Long selectMaxPollId();
}
