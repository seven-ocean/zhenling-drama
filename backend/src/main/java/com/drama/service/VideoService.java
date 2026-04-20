package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.TaskLog;
import com.drama.entity.Video;
import com.drama.mapper.VideoMapper;
import com.drama.service.adapter.AiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    /**
     * 生成视频并保存记录（自动注册任务日志）
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
            String videoResult = aiServiceFactory.generateVideo(actualProvider, imageUrl, actualModel);

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
                // 同步返回了视频URL
                video.setVideoUrl(videoResult);
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
                    video.setVideoUrl(videoUrl);
                    video.setStatus("completed");
                    video.setUpdatedAt(LocalDateTime.now());
                    this.updateById(video);

                    taskLogService.complete(taskLog.getId(), videoUrl);
                    log.info("Video task completed: {} -> {}", taskLog.getTaskId(), videoUrl);

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
}
