package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场景实体
 */
@Data
@TableName("scenes")
public class Scene {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String dramaId;
    private String name;
    private String description;
    private String imageUrl;
    private String location;
    private String timeOfDay;
    private String prompt;
    private String extraData;
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}