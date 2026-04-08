package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 音频实体
 */
@Data
@TableName("audios")
public class Audio {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String dramaId;
    private Integer episodeNumber;
    private String storyboardId;
    private String characterId;
    private String audioUrl;
    private String text;
    private String provider;
    private String voiceId;
    private Float duration;
    private String status;
    private String taskId;
    private String errorMessage;
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}