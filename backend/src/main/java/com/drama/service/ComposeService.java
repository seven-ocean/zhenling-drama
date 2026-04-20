package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Audio;
import com.drama.entity.Asset;
import com.drama.entity.EpisodeExport;
import com.drama.entity.Storyboard;
import com.drama.entity.Video;
import com.drama.mapper.AudioMapper;
import com.drama.mapper.StoryboardMapper;
import com.drama.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 视频合成编排服务
 * 在 VideoComposeService（底层FFmpeg调用）之上提供业务编排能力：
 * - 按分镜自动查找视频/音频 → 合成
 * - 整集批量拼接 → 上传 → 记录导出历史
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComposeService {

    private final VideoComposeService videoComposeService;
    private final VideoMapper videoMapper;
    private final AudioMapper audioMapper;
    private final StoryboardMapper storyboardMapper;
    private final EpisodeExportService episodeExportService;
    private final FileStorageService fileStorageService;
    private final AssetService assetService;

    /**
     * 直接合成单镜头（传入原始路径）
     */
    public Map<String, Object> composeShot(String videoPath, String audioPath, String subtitle) {
        // 生成临时输出路径
        String tempDir = System.getProperty("java.io.tmpdir");
        String outputFileName = "composed_" + System.currentTimeMillis() + "_" + IdUtils.randomId().substring(0, 8) + ".mp4";
        String outputPath = tempDir + File.separator + outputFileName;

        videoComposeService.composeShot(videoPath, audioPath, subtitle, outputPath);

        // 获取合成后的视频信息
        try {
            VideoComposeService.VideoInfo info = videoComposeService.getVideoInfo(outputPath);
            return Map.of(
                    "outputPath", outputPath,
                    "success", true,
                    "duration", info.getDuration() != null ? info.getDuration() : 0,
                    "width", info.getWidth() != null ? info.getWidth() : 0,
                    "height", info.getHeight() != null ? info.getHeight() : 0,
                    "codec", info.getCodec() != null ? info.getCodec() : "unknown"
            );
        } catch (Exception e) {
            log.warn("Failed to get composed video info: {}", e.getMessage());
            return Map.of("outputPath", outputPath, "success", true);
        }
    }

    /**
     * 按分镜ID自动查找关联的视频和音频，然后合成
     */
    @Transactional
    public Map<String, Object> composeShotByStoryboard(String dramaId, String storyboardId, int episodeNumber) {
        // 1. 查找该分镜对应的已完成视频
        LambdaQueryWrapper<Video> vWrapper = new LambdaQueryWrapper<>();
        vWrapper.eq(Video::getStoryboardId, storyboardId)
                .eq(Video::getStatus, "completed")
                .eq(Video::getDeleted, 0)
                .orderByDesc(Video::getCreatedAt)
                .last("LIMIT 1");
        Video video = videoMapper.selectOne(vWrapper);

        if (video == null || video.getVideoUrl() == null || video.getVideoUrl().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该分镜尚未生成完成视频");
        }

        // 2. 查找该分镜的配音音频（精确按 storyboardId 匹配）
        String audioPath = null;
        try {
            LambdaQueryWrapper<Audio> aWrapper = new LambdaQueryWrapper<>();
            aWrapper.eq(Audio::getStoryboardId, storyboardId)
                   .eq(Audio::getStatus, "completed")
                   .eq(Audio::getDeleted, 0)
                   .last("LIMIT 1");
            Audio audio = audioMapper.selectOne(aWrapper);
            if (audio != null && audio.getAudioUrl() != null) {
                audioPath = audio.getAudioUrl();
            }
        } catch (Exception e) {
            log.warn("No audio found for storyboard {}: {}", storyboardId, e.getMessage());
        }

        // 3. 提取台词作为字幕（如果有的话）
        String subtitle = null; // 可从 Storyboard 实体获取 dialogue 字段

        // 4. 调用 FFmpeg 合成
        String outputDir = System.getProperty("user.dir") + "/data/composed/" + dramaId + "/";
        new File(outputDir).mkdirs();
        String outputPath = outputDir + "shot_" + storyboardId.substring(0, 8) + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".mp4";

        try {
            videoComposeService.composeShot(video.getVideoUrl(), audioPath, subtitle, outputPath);

            VideoComposeService.VideoInfo info = videoComposeService.getVideoInfo(outputPath);
            return Map.of(
                    "outputPath", outputPath,
                    "success", true,
                    "storyboardId", storyboardId,
                    "videoSource", video.getVideoUrl(),
                    "hasAudio", audioPath != null,
                    "duration", info.getDuration() != null ? info.getDuration() : 0
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Compose by storyboard failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "镜头合成失败: " + e.getMessage());
        }
    }

    /**
     * 整集视频合成（核心改造）：
     *
     * 流程：
     * 1. 查询该集所有分镜 → 按 shotNumber 升序排列
     * 2. 对每个分镜：
     *    a. 查找已完成的视频
     *    b. 查找关联的配音音频
     *    c. 调用 FFmpeg 合成「视频+音频」为单个镜头文件
     * 3. 将所有镜头按 shotNumber 顺序拼接为完整剧集
     * 4. 归档到OSS/本地存储，记录导出历史
     */
    @Transactional
    public EpisodeExport composeEpisode(String dramaId, int episodeNumber) {
        if (!videoComposeService.isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg 未安装，无法进行视频合成");
        }

        // ========== Step 1: 获取分镜列表（按 shotNumber 排序） ==========
        LambdaQueryWrapper<Storyboard> sbWrapper = new LambdaQueryWrapper<>();
        sbWrapper.eq(Storyboard::getDramaId, dramaId)
                 .eq(Storyboard::getEpisodeNumber, episodeNumber)
                 .eq(Storyboard::getDeleted, 0)
                 .orderByAsc(Storyboard::getShotNumber);
        List<Storyboard> storyboards = storyboardMapper.selectList(sbWrapper);

        if (storyboards.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该集暂无分镜数据，请先在剧集详情中 AI 拆解剧本");
        }

        log.info("[ComposeEpisode] Found {} storyboards for drama={} ep={}", storyboards.size(), dramaId, episodeNumber);

        // ========== Step 2: 查询所有已完成的视频 ==========
        LambdaQueryWrapper<Video> vWrapper = new LambdaQueryWrapper<>();
        vWrapper.eq(Video::getDramaId, dramaId)
               .eq(Video::getEpisodeNumber, episodeNumber)
               .eq(Video::getStatus, "completed")
               .eq(Video::getDeleted, 0);
        List<Video> allVideos = videoMapper.selectList(vWrapper);

        // 按 storyboardId 建立索引：storyboardId → Video
        Map<String, Video> videoMap = allVideos.stream()
                .filter(v -> v.getStoryboardId() != null && !v.getStoryboardId().isEmpty())
                .collect(Collectors.toMap(Video::getStoryboardId, v -> v, (a, b) -> a));

        // ========== Step 3: 查询所有已完成配音 ==========
        LambdaQueryWrapper<Audio> aWrapper = new LambdaQueryWrapper<>();
        aWrapper.eq(Audio::getDramaId, dramaId)
               .eq(Audio::getEpisodeNumber, episodeNumber)
               .eq(Audio::getStatus, "completed")
               .eq(Audio::getDeleted, 0);
        List<Audio> allAudios = audioMapper.selectList(aWrapper);

        // 按 storyboardId 建立索引：storyboardId → Audio
        Map<String, Audio> audioMap = new HashMap<>();
        for (Audio audio : allAudios) {
            if (audio.getStoryboardId() != null && !audio.getStoryboardId().isEmpty()) {
                // 同一分镜只取第一个配音
                audioMap.putIfAbsent(audio.getStoryboardId(), audio);
            }
        }

        // ========== Step 4: 按分镜顺序逐个合成镜头（视频+音频合并） ==========
        String outputDir = System.getProperty("user.dir") + "/data/composed/" + dramaId + "/";
        new File(outputDir).mkdirs();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        List<String> composedShotPaths = new ArrayList<>();
        int shotCount = 0;

        for (Storyboard sb : storyboards) {
            Video video = videoMap.get(sb.getId());
            if (video == null || video.getVideoUrl() == null || video.getVideoUrl().isEmpty()) {
                log.info("[ComposeEpisode] Skip storyboard #{} ({}): no completed video",
                        sb.getShotNumber(), sb.getId());
                continue; // 跳过没有完成视频的分镜
            }

            Audio audio = audioMap.get(sb.getId());
            String audioPath = (audio != null && audio.getAudioUrl() != null) ? audio.getAudioUrl() : null;

            // 提取台词作为字幕
            String subtitle = sb.getDialogue();

            try {
                String shotOutputPath = outputDir + String.format("shot_%03d_%s.mp4", sb.getShotNumber(), timestamp);
                videoComposeService.composeShot(video.getVideoUrl(), audioPath, subtitle, shotOutputPath);
                composedShotPaths.add(shotOutputPath);
                shotCount++;
                log.info("[ComposeEpisode] Composed shot #{}: video={}, hasAudio={}, subtitle={}",
                        sb.getShotNumber(), video.getModel(), audioPath != null,
                        subtitle != null ? subtitle.substring(0, Math.min(20, subtitle.length())) : "null");
            } catch (Exception e) {
                log.warn("[ComposeEpisode] Shot #{} composition failed, using original video: {}",
                        sb.getShotNumber(), e.getMessage());
                // 合成失败时降级：直接用原始视频
                composedShotPaths.add(video.getVideoUrl());
                shotCount++;
            }
        }

        if (composedShotPaths.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该集暂无可合成的视频，请确保至少有1个状态为'已完成'的视频");
        }

        // ========== Step 5: 将所有合成后的镜头按顺序拼接 ==========
        String finalOutputPath = outputDir + "episode_" + episodeNumber + "_" + timestamp + ".mp4";
        String finalPath = videoComposeService.concatVideos(composedShotPaths, finalOutputPath);

        // ========== Step 6: 获取合成后视频信息 ==========
        float duration = 0f;
        try {
            VideoComposeService.VideoInfo info = videoComposeService.getVideoInfo(finalPath);
            duration = info.getDuration() != null ? info.getDuration() : 0f;
        } catch (Exception e) {
            log.warn("[ComposeEpisode] Failed to get composed video info: {}", e.getMessage());
        }

        // ========== Step 7: 归档到存储 ==========
        String finalUrl = finalPath;
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(finalPath);
            if (java.nio.file.Files.exists(filePath)) {
                byte[] composedData = java.nio.file.Files.readAllBytes(filePath);
                String filename = "episode_" + dramaId + "_ep" + episodeNumber + "_" + timestamp + ".mp4";
                Asset archivedAsset = fileStorageService.uploadBytes(
                        composedData, filename, dramaId, "video", "video/mp4");
                archivedAsset.setSourceType("composed");
                archivedAsset.setExtraData("{\"type\":\"episode_compose\",\"episodeNumber\":" +
                        episodeNumber + ",\"shotCount\":" + shotCount + ",\"withAudioMerged\":true}");
                assetService.updateById(archivedAsset);
                finalUrl = archivedAsset.getFileUrl();
                log.info("[ComposeEpisode] Archived: {} ({}KB)", finalUrl, composedData.length / 1024);
            }
        } catch (Exception archiveEx) {
            // 归档失败时，生成可通过 /compose/download/** 端点访问的相对URL
            log.warn("[ComposeEpisode] Archive failed, using compose download URL: {}", archiveEx.getMessage());
            // 将绝对路径转换为可访问的下载URL: /api/v1/compose/download/{dramaId}/{filename}
            String filename = "episode_" + episodeNumber + "_" + timestamp + ".mp4";
            finalUrl = "/api/v1/compose/download/" + dramaId + "/" + filename;
        }

        // ========== Step 8: 创建导出记录 ==========
        EpisodeExport exportRecord = new EpisodeExport();
        exportRecord.setId(IdUtils.randomId());
        exportRecord.setDramaId(dramaId);
        exportRecord.setEpisodeNumber(episodeNumber);
        exportRecord.setShotCount(shotCount);
        exportRecord.setDuration(duration);
        exportRecord.setStatus("completed");
        exportRecord.setExportUrl(finalUrl);
        exportRecord.setExtraData("{\"shotCount\":" + shotCount +
                ",\"storyboardCount\":" + storyboards.size() +
                ",\"withAudioMerged\":true" +
                ",\"composedAt\":\"" + LocalDateTime.now().toString() + "\"}");
        exportRecord.setCreatedAt(LocalDateTime.now());
        exportRecord.setUpdatedAt(LocalDateTime.now());
        exportRecord.setDeleted(0);

        episodeExportService.save(exportRecord);
        log.info("[ComposeEpisode] DONE: {} shots from {} storyboards → {} ({}s)",
                shotCount, storyboards.size(), finalUrl, duration);

        return exportRecord;
    }

    /**
     * 分页查询合成记录（复用 EpisodeExport 的表和 Service）
     */
    public Page<EpisodeExport> listRecords(int pageNum, int pageSize, String dramaId, Integer episodeNumber) {
        return episodeExportService.page(pageNum, pageSize, dramaId, episodeNumber);
    }
}
