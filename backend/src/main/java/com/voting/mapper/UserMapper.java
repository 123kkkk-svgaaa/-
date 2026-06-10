package com.voting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voting.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
