package com.cloudread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudread.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
