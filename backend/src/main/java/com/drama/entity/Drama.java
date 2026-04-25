package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 剧集实体
 */
@Data
@TableName("dramas")
public class Drama {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String title;
    private String description;
    private String coverImage;
    private String status;
    private Integer totalEpisodes;
    private Integer createdEpisodes;
    private String settings;
    
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}