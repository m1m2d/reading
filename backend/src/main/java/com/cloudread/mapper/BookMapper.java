package com.cloudread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudread.entity.Book;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
