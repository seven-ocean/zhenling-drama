package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分镜实体
 */
@Data
@TableName("storyboards")
public class Storyboard {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String dramaId;
    private Integer episodeNumber;
    private Integer sceneNumber;
    private Integer shotNumber;
    private String shotType;
    private String shotDirection;
    private String action;
    private String dialogue;
    private String characterId;
    private String sceneId;
    private String characterImageUrl;
    private String sceneImageUrl;
    private String gridImageUrl;
    private String gridPrompt;
    private String status;
    private String extraData;
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}