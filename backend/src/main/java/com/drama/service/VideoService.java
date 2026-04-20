package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.AiConfig;
import com.drama.entity.Asset;
import com.drama.entity.TaskLog;
import com.drama.entity.Video;
import com.drama.mapper.VideoMapper;
import com.drama.service.adapter.AiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 视频生成服务 - 支持异步任务轮询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService extends ServiceImpl<VideoMapper, Video> {

    private final AiServiceFactory aiServiceFactory;
    private final TaskLogService taskLogService;
    private final AiConfigService aiConfigService;
    private final FileStorageService fileStorageService;
    private final AssetService assetService;

    // 用于下载视频URL
    private final RestTemplate downloadRestTemplate = new RestTemplate();

    /**
     * 生成视频并保存记录（支持多模式，自动注册任务日志）
     * 模式：TEXT_TO_VIDEO(文生视频), IMAGE_TO_VIDEO(图生视频), FIRST_LAST_FRAME(首尾帧), SUBJECT_REFERENCE(主体参考)
     */
    @Transactional
    public Video generate(String dramaId, int episodeNumber, String storyboardId,
                        String mode, String prompt,
                        String imageUrl, String firstFrameUrl, String lastFrameUrl, String subjectImageUrl,
                        String provider, String model) {
        // 验证必填项
        if ("TEXT_TO_VIDEO".equals(mode)) {
            if (prompt == null || prompt.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "文生视频模式需要提供视频描述");
            }
        } else if ("IMAGE_TO_VIDEO".equals(mode)) {
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "图生视频模式需要提供参考图片URL");
            }
        } else if ("FIRST_LAST_FRAME".equals(mode)) {
            if (firstFrameUrl == null || firstFrameUrl.isEmpty() || lastFrameUrl == null || lastFrameUrl.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "首尾帧模式需要提供首帧和尾帧图片URL");
            }
        } else if ("SUBJECT_REFERENCE".equals(mode)) {
            if (subjectImageUrl == null || subjectImageUrl.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "主体参考模式需要提供主体参考图片URL");
            }
        }

        // ====== 自动解析 provider/model，优先使用传入参数否则从 AiConfig 动态获取 ======
        String actualProvider = provider != null ? provider : "minimax";
        String actualModel = model;
        if (actualModel == null || actualModel.isEmpty()) {
            try {
                List<AiConfig> videoConfigs = aiConfigService.listByType("video");
                if (!videoConfigs.isEmpty() && videoConfigs.get(0).getModel() != null && !videoConfigs.get(0).getModel().isEmpty()) {
                    actualModel = videoConfigs.get(0).getModel();
                    log.info("Resolved video model from AiConfig: model={}", actualModel);
                } else {
                    actualModel = "MiniMax-Hailuo-2.3";
                }
            } catch (Exception e) {
                log.warn("Failed to resolve video model from AiConfig, using default: {}", e.getMessage());
                actualModel = "MiniMax-Hailuo-2.3";
            }
        }

        log.info("Generating video: mode={}, provider={}, model={}", mode, actualProvider, actualModel);

        try {
            // 调用AI生成视频（使用多模式接口）
            String videoResult = aiServiceFactory.generateVideoMultiMode(
                    actualProvider, mode, prompt, imageUrl, firstFrameUrl, lastFrameUrl, subjectImageUrl, actualModel);

            Video video = new Video();
            video.setId(IdUtils.randomId());
            video.setDramaId(dramaId);
            video.setEpisodeNumber(episodeNumber);
            video.setStoryboardId(storyboardId);
            video.setProvider(actualProvider);
            video.setModel(actualModel);

            if (videoResult != null && videoResult.startsWith("task:")) {
                // 异步任务，记录TaskLog并标记处理中
                String vendorTaskId = videoResult.substring(5);
                video.setStatus("processing");
                video.setTaskId(vendorTaskId);

                // 创建任务日志用于后续轮询
                taskLogService.create(dramaId, "video_generate", vendorTaskId);
                log.info("Video generation started as async task: {}", vendorTaskId);
            } else if (videoResult != null && !videoResult.isEmpty()) {
                // 同步返回了视频URL → 归档到OSS
                String archivedUrl = archiveVideoToStorage(videoResult, dramaId);
                video.setVideoUrl(archivedUrl);
                video.setStatus("completed");
            } else {
                // 返回空
                video.setStatus("failed");
                video.setErrorMessage("视频生成返回为空");
            }

            video.setCreatedAt(LocalDateTime.now());
            video.setDeleted(0);

            this.save(video);
            log.info("Generated video: {} for storyboard {}", video.getId(), storyboardId);

            return video;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Video generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "视频生成失败: " + e.getMessage());
        }
    }

    /**
     * 轮询所有正在处理的视频任务状态
     * 可由定时任务调用
     */
    public void pollPendingTasks() {
        List<TaskLog> runningTasks = taskLogService.listRunningTasks();

        for (TaskLog taskLog : runningTasks) {
            if (!"video_generate".equals(taskLog.getTaskType())) continue;

            try {
                // 获取对应的视频记录
                LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Video::getTaskId, taskLog.getTaskId())
                       .eq(Video::getStatus, "processing")
                       .eq(Video::getDeleted, 0)
                       .last("LIMIT 1");
                Video video = this.getOne(wrapper);

                if (video == null) {
                    // 视频已不存在或已更新，标记任务完成
                    taskLogService.complete(taskLog.getId(), "视频记录已移除");
                    continue;
                }

                // 通过适配器轮询状态
                AiAdapter adapter = aiServiceFactory.getAdapter(video.getProvider());
                String pollResult = adapter.pollVideoStatus(video.getTaskId());

                if (pollResult.startsWith("completed:")) {
                    String videoUrl = pollResult.substring(10);
                    // 归档视频到OSS（避免临时链接过期）
                    String archivedUrl = archiveVideoToStorage(videoUrl, video.getDramaId());
                    video.setVideoUrl(archivedUrl);
                    video.setStatus("completed");
                    video.setUpdatedAt(LocalDateTime.now());
                    this.updateById(video);

                    taskLogService.complete(taskLog.getId(), archivedUrl);
                    log.info("Video task completed & archived: {} -> {}", taskLog.getTaskId(), archivedUrl);

                } else if (pollResult.startsWith("failed:")) {
                    String errorMsg = pollResult.substring(7);
                    video.setStatus("failed");
                    video.setErrorMessage(errorMsg);
                    video.setUpdatedAt(LocalDateTime.now());
                    this.updateById(video);

                    taskLogService.fail(taskLog.getId(), errorMsg);
                    log.warn("Video task failed: {} - {}", taskLog.getTaskId(), errorMsg);

                } else {
                    // 仍在处理中，更新进度
                    int progress = estimateProgress(taskLog.getCreatedAt());
                    taskLogService.updateProgress(taskLog.getId(), "running", progress, "等待厂商处理...");
                }

            } catch (Exception e) {
                log.error("Error polling video task {}: {}", taskLog.getTaskId(), e.getMessage());
            }
        }
    }

    /**
     * 根据创建时间估算进度值（简单线性估算）
     */
    private int estimateProgress(LocalDateTime createdAt) {
        long minutes = java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        return Math.min(90, (int)(minutes * 10)); // 每分钟+10%，最高90%
    }

    /**
     * 查询某集的视频列表
     */
    public List<Video> listByEpisode(String dramaId, int episodeNumber) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getDramaId, dramaId)
               .eq(Video::getEpisodeNumber, episodeNumber)
               .eq(Video::getDeleted, 0)
               .orderByAsc(Video::getCreatedAt);
        return this.list(wrapper);
    }

    /**
     * 查询分镜对应的视频
     */
    public Video getByStoryboardId(String storyboardId) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getStoryboardId, storyboardId)
               .eq(Video::getDeleted, 0)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    /**
     * 查询指定分镜的所有视频
     */
    public List<Video> listByStoryboard(String dramaId, int episodeNumber, String storyboardId) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getDramaId, dramaId)
               .eq(Video::getEpisodeNumber, episodeNumber)
               .eq(Video::getStoryboardId, storyboardId)
               .eq(Video::getDeleted, 0)
               .orderByDesc(Video::getCreatedAt);
        return this.list(wrapper);
    }

    /**
     * 删除视频
     */
    @Transactional
    public void delete(String id) {
        // 先检查记录是否存在（由于@TableLogic，getById会自动过滤已删除记录）
        Video existing = super.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "视频不存在或已被删除");
        }
        // 使用MyBatis-Plus的removeById进行逻辑删除
        boolean success = this.removeById(id);
        if (!success) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "删除失败");
        }
    }

    /**
     * 分页查询
     * 注意：MyBatis-Plus已配置全局逻辑删除，会自动添加 deleted = 0 条件
     */
    public Page<Video> page(int pageNum, int pageSize, String dramaId, Integer episodeNumber, String status) {
        Page<Video> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(dramaId)) {
            wrapper.eq(Video::getDramaId, dramaId);
        }
        if (episodeNumber != null && episodeNumber > 0) {
            wrapper.eq(Video::getEpisodeNumber, episodeNumber);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Video::getStatus, status);
        }
        wrapper.orderByDesc(Video::getCreatedAt);

        return this.page(page, wrapper);
    }

    /**
     * 查询剧集下的所有视频
     */
    public List<Video> listByDrama(String dramaId) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getDramaId, dramaId)
               .eq(Video::getDeleted, 0)
               .orderByDesc(Video::getCreatedAt);
        return this.list(wrapper);
    }

    /**
     * 更新视频信息（支持 Map 灵活更新）
     */
    @Transactional
    public void update(String id, Map<String, Object> data) {
        Video video = super.getById(id);
        if (video == null || video.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "视频不存在");
        }
        // 只允许更新特定字段
        if (data.containsKey("videoUrl")) video.setVideoUrl((String) data.get("videoUrl"));
        if (data.containsKey("status")) video.setStatus((String) data.get("status"));
        if (data.containsKey("errorMessage")) video.setErrorMessage((String) data.get("errorMessage"));
        if (data.containsKey("duration")) video.setDuration(((Number) data.get("duration")).floatValue());
        if (data.containsKey("extraData")) video.setExtraData(data.get("extraData") != null ? data.get("extraData").toString() : null);

        video.setUpdatedAt(LocalDateTime.now());
        this.updateById(video);
    }

    /**
     * 归档视频到OSS/本地存储
     * 下载AI返回的临时视频URL → 通过FileStorageService上传到用户配置的存储位置
     *
     * @param tempUrl  AI返回的临时视频URL
     * @param dramaId  剧集ID（用于OSS路径组织）
     * @return 归档后的永久URL
     */
    private String archiveVideoToStorage(String tempUrl, String dramaId) {
        // 如果已经是本地/OSS路径（不以http开头），直接返回
        if (!tempUrl.startsWith("http://") && !tempUrl.startsWith("https://")) {
            log.info("[VideoArchive] URL is already local/storage path: {}", tempUrl);
            return tempUrl;
        }

        try {
            log.info("[VideoArchive] Downloading video from: {}", tempUrl);

            // 使用 URI 处理带签名的 URL，避免 RestTemplate 二次编码导致签名不匹配
            URI uri = URI.create(tempUrl);
            ResponseEntity<byte[]> response = downloadRestTemplate.getForEntity(uri, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[VideoArchive] Failed to download, status={}, returning original URL",
                        response.getStatusCode());
                return tempUrl;
            }

            byte[] videoData = response.getBody();
            log.info("[VideoArchive] Downloaded {} bytes from {}", videoData.length, tempUrl);

            // 通过 FileStorageService 上传到配置的存储位置（自动处理 OSS 或本地）
            String filename = "video_" + System.currentTimeMillis() + ".mp4";
            Asset archivedAsset = fileStorageService.uploadBytes(
                    videoData, filename, dramaId, "video", "video/mp4");

            // 补充元数据
            archivedAsset.setSourceType("ai_archived");
            archivedAsset.setExtraData("{\"type\":\"video\",\"sourceUrl\":\"" + tempUrl + "\"}");
            assetService.updateById(archivedAsset);

            log.info("[VideoArchive] Video archived: id={}, url={}, size={}KB",
                    archivedAsset.getId(), archivedAsset.getFileUrl(), videoData.length / 1024);

            return archivedAsset.getFileUrl();

        } catch (Exception e) {
            log.error("[VideoArchive] Failed to archive video, returning original URL: {}", e.getMessage());
            return tempUrl; // 降级：返回原始URL
        }
    }
}
