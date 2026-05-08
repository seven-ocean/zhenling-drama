package com.drama.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drama.common.R;
import com.drama.common.ResultCode;
import com.drama.dto.VideoGenerateRequest;
import com.drama.entity.Video;
import com.drama.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 视频接口 - 完整 CRUD + 生成
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * 生成视频（支持多模式+音频对口型）
     * 接收 JSON Body，统一前后端传参方式
     * 模式：TEXT_TO_VIDEO(文生视频), IMAGE_TO_VIDEO(图生视频), FIRST_LAST_FRAME(首尾帧), SUBJECT_REFERENCE(主体参考)
     *
     * 豆包/火山引擎扩展参数：
     * - audioUrl: 用户上传的音频文件（用于对口型）
     * - videoReferenceUrl: 参考视频（用于运镜/动作参考）
     * - audioReferenceUrl: 参考音频（用于背景音乐）
     * - ratio: 宽高比
     * - generateAudio: 是否生成音频（默认 true）
     */
    @PostMapping("/generate")
    public R<Video> generate(@Valid @RequestBody VideoGenerateRequest request) {
        // 参数校验：dramaId 和 episodeNumber 必填
        if (request.getDramaId() == null || request.getDramaId().isBlank()) {
            throw new com.drama.common.BusinessException(ResultCode.BAD_REQUEST, "dramaId 不能为空");
        }
        if (request.getEpisodeNumber() == null || request.getEpisodeNumber() <= 0) {
            throw new com.drama.common.BusinessException(ResultCode.BAD_REQUEST, "episodeNumber 必须大于 0");
        }
        // 默认值兜底
        String mode = request.getMode() != null ? request.getMode() : "IMAGE_TO_VIDEO";
        Integer duration = request.getDuration() != null ? request.getDuration() : 6;
        String resolution = request.getResolution() != null ? request.getResolution() : "768P";
        Boolean generateAudio = request.getGenerateAudio() != null ? request.getGenerateAudio() : true;

        return R.ok(videoService.generate(
                request.getDramaId(),
                request.getEpisodeNumber(),
                request.getStoryboardId(),
                mode,
                request.getPrompt(),
                request.getImageUrl(),
                request.getFirstFrameUrl(),
                request.getLastFrameUrl(),
                request.getSubjectImageUrl(),
                request.getProvider(),
                request.getModel(),
                duration,
                resolution,
                request.getAudioUrl(),
                request.getVideoReferenceUrl(),
                request.getAudioReferenceUrl(),
                generateAudio
        ));
    }

    /**
     * 批量生成视频（按剧集+集数）
     */
    @PostMapping("/batch")
    public R<String> batchGenerate(@RequestParam String dramaId,
                                    @RequestParam int episodeNumber) {
        return R.ok("batch generation started for drama=" + dramaId + " ep=" + episodeNumber);
    }

    /**
     * 分页查询视频列表
     */
    @GetMapping
    public R<IPage<Video>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dramaId,
            @RequestParam(required = false) Integer episodeNumber,
            @RequestParam(required = false) String status) {
        return R.ok(videoService.page(pageNum, pageSize, dramaId, episodeNumber, status));
    }

    /**
     * 按剧集查询视频列表
     */
    @GetMapping("/drama/{dramaId}")
    public R<List<Video>> listByDrama(
            @PathVariable String dramaId,
            @RequestParam(required = false) Integer episodeNumber) {
        if (episodeNumber != null && episodeNumber > 0) {
            return R.ok(videoService.listByEpisode(dramaId, episodeNumber));
        }
        // 查询剧集下所有视频（需要全量查询方法）
        return R.ok(videoService.listByDrama(dramaId));
    }

    /**
     * 获取单个视频详情
     */
    @GetMapping("/{id}")
    public R<Video> getById(@PathVariable String id) {
        Video video = videoService.getById(id);
        if (video == null || video.getDeleted() == 1) {
            throw new com.drama.common.BusinessException(ResultCode.NOT_FOUND, "视频不存在");
        }
        return R.ok(video);
    }

    /**
     * 删除视频（逻辑删除）
     */
    @PostMapping("/{id}/delete")
    public R<Void> delete(@PathVariable String id) {
        videoService.delete(id);
        return R.ok();
    }

    /**
     * 更新视频信息（如手动修改URL等）
     */
    @PostMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody Map<String, Object> data) {
        videoService.update(id, data);
        return R.ok();
    }

    /**
     * 轮询处理中的任务（供前端定时调用）
     */
    @PostMapping("/poll")
    public R<Void> poll() {
        videoService.pollPendingTasks();
        return R.ok();
    }
}
