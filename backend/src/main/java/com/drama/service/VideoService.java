package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Video;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 视频生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final AiServiceFactory aiServiceFactory;

    /**
     * 生成视频
     */
    @Transactional
    public Video generate(String dramaId, int episodeNumber, String storyboardId,
                        String imageUrl, String provider, String model) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片URL不能为空");
        }

        String actualProvider = provider != null ? provider : "minimax";
        String actualModel = model != null ? model : "video-01";

        log.info("Generating video: provider={}, model={}", actualProvider, actualModel);

        try {
            // 调用AI生成视频
            String videoUrl = aiServiceFactory.generateVideo(actualProvider, imageUrl, actualModel);

            Video video = new Video();
            video.setId(IdUtils.randomId());
            video.setDramaId(dramaId);
            video.setEpisodeNumber(episodeNumber);
            video.setStoryboardId(storyboardId);
            video.setVideoUrl(videoUrl);
            video.setProvider(actualProvider);
            video.setModel(actualModel);
            video.setStatus("completed");
            video.setCreatedAt(LocalDateTime.now());
            video.setDeleted(0);

            return video;
        } catch (Exception e) {
            log.error("Video generation failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "视频生成失败: " + e.getMessage());
        }
    }
}