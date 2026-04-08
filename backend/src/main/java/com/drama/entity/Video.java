package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频实体
 */
@Data
@TableName("videos")
public class Video {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String dramaId;
    private Integer episodeNumber;
    private String storyboardId;
    private String videoUrl;
    private Float duration;
    private String provider;
    private String model;
    private String status;
    private String taskId;
    private String errorMessage;
    private String extraData;
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}