package com.voting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voting.entity.Poll;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PollMapper extends BaseMapper<Poll> {
}
