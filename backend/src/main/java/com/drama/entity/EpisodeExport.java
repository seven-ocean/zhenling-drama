package com.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 整集导出记录实体
 */
@Data
@TableName("episode_exports")
public class EpisodeExport {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String dramaId;
    private Integer episodeNumber;
    private String exportUrl;
    private Float duration;
    private String status;
    private Integer shotCount;
    private String errorMessage;
    private String extraData;
    
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
