package com.drama.dto;

import lombok.Data;

/**
 * 分镜参考图状态 DTO
 * 用于批量查询剧集下所有分镜的参考图生成状态
 */
@Data
public class StoryboardRefStatus {

    /**
     * 分镜ID
     */
    private String storyboardId;

    /**
     * 分镜序号
     */
    private Integer shotNumber;

    /**
     * 动作描述（用于展示）
     */
    private String action;

    /**
     * 是否有参考图
     */
    private boolean hasRefImage;

    /**
     * 参考图 URL（有参考图时才有值）
     */
    private String refImageUrl;
}