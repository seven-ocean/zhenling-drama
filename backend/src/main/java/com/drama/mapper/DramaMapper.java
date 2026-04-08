package com.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drama.entity.Drama;
import org.apache.ibatis.annotations.Mapper;

/**
 * 剧集Mapper
 */
@Mapper
public interface DramaMapper extends BaseMapper<Drama> {
}