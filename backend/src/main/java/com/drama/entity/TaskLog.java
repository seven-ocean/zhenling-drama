package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务日志实体 - 记录所有异步任务（视频生成、音频生成等）的执行状态
 */
@Data
@TableName("task_logs")
public class TaskLog {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String dramaId;
    private String taskType;       // video_generate, audio_generate, image_generate 等
    private String taskId;         // 厂商返回的任务ID
    private String status;         // pending/running/completed/failed
    private Integer progress;      // 0-100
    private String message;
    private String result;         // 最终结果URL或数据

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
