package com.drama.dto;

import lombok.Data;

/**
 * 视频生成请求 DTO
 * 支持多模式：TEXT_TO_VIDEO(文生视频), IMAGE_TO_VIDEO(图生视频),
 * FIRST_LAST_FRAME(首尾帧), SUBJECT_REFERENCE(主体参考)
 */
@Data
public class VideoGenerateRequest {

    /** 剧集ID (必填) */
    private String dramaId;

    /** 集数 (必填) */
    private Integer episodeNumber;

    /** 关联分镜ID */
    private String storyboardId;

    /**
     * 生成模式（默认 IMAGE_TO_VIDEO）
     * TEXT_TO_VIDEO / IMAGE_TO_VIDEO / FIRST_LAST_FRAME / SUBJECT_REFERENCE
     */
    private String mode = "IMAGE_TO_VIDEO";

    /** 文本提示词（文生视频时使用） */
    private String prompt;

    /** 参考图片URL（图生视频时使用） */
    private String imageUrl;

    /** 首帧图片URL（首尾帧模式） */
    private String firstFrameUrl;

    /** 尾帧图片URL（首尾帧模式） */
    private String lastFrameUrl;

    /** 主体参考图片URL（主体参考模式，如人脸照片） */
    private String subjectImageUrl;

    /** AI 提供商（默认 minimax） */
    private String provider;

    /** 模型名称（为空则从 AI 配置动态读取） */
    private String model;

    /** 视频时长（秒），默认 6，可选 6 或 10 */
    private Integer duration = 6;

    /** 视频分辨率，默认 768P，可选 512P / 768P / 1080P */
    private String resolution = "768P";
}
