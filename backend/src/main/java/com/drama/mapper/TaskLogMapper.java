package com.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drama.entity.TaskLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {
}
