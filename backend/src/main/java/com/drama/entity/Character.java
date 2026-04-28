package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@TableName("characters")
public class Character {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String dramaId;
    private String name;
    private String description;
    private String imageUrl;
    private String voiceId;
    private String voiceProvider;
    private String appearancePrompt;
    private String dialogueStyle;
    private String previewAudioUrl;
    private Integer sortOrder;
    private String extraData;
    
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}