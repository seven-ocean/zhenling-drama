package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI配置实体
 */
@Data
@TableName("ai_configs")
public class AiConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String provider;
    private String apiType;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Integer priority;
    private Boolean enabled;
    private String configJson;
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}