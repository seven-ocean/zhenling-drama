package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 素材实体
 */
@Data
@TableName("assets")
public class Asset {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String dramaId;
    private String type;
    private String filename;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Float duration;
    private String sourceType;
    private String extraData;
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}