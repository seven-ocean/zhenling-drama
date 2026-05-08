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
import java.nio.file.Path;
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
    private final AudioSyncEngine audioSyncEngine;       // 新增：音画对齐引擎
    private final SubtitleRenderer subtitleRenderer;      // 新增：ASS字幕渲染器
    private final VideoMapper videoMapper;
    private final AudioMapper audioMapper;
    private final StoryboardMapper storyboardMapper;
    private final EpisodeExportService episodeExportService;
    private final FileStorageService fileStorageService;
    private final AssetService assetService;

    /**
     * 直接合成单镜头（传入原始路径）— 使用智能音画对齐引擎
     */
    public Map<String, Object> composeShot(String videoPath, String audioPath, String subtitle) {
        // 生成临时输出路径
        String tempDir = System.getProperty("java.io.tmpdir");
        String outputFileName = "composed_" + System.currentTimeMillis() + "_" + IdUtils.randomId().substring(0, 8) + ".mp4";
        String outputPath = tempDir + File.separator + outputFileName;

        try {
            // 生成 ASS 字幕文件（如果有台词）
            String assPath = null;
            if (subtitle != null && !subtitle.isBlank()) {
                Path assFile = subtitleRenderer.generateForShot(subtitle, 5f, outputPath);
                if (assFile != null) assPath = assFile.toString();
            }

            // 使用音画对齐引擎进行智能混流
            AudioSyncEngine.SyncResult syncResult = audioSyncEngine.composeWithSync(
                    videoPath, audioPath, assPath, outputPath);

            return Map.of(
                    "outputPath", outputPath,
                    "success", true,
                    "duration", syncResult.getOutputDuration(),
                    "strategy", syncResult.getStrategy(),
                    "videoDuration", syncResult.getVideoDuration(),
                    "audioDuration", syncResult.getAudioDuration()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[composeShot] Smart sync failed, falling back: {}", e.getMessage());
            // 降级到简单模式
            try {
                videoComposeService.composeShot(videoPath, audioPath, null, outputPath);
                VideoComposeService.VideoInfo info = videoComposeService.getVideoInfo(outputPath);
                return Map.of(
                        "outputPath", outputPath,
                        "success", true,
                        "duration", info.getDuration() != null ? info.getDuration() : 0,
                        "strategy", "FALLBACK_SIMPLE"
                );
            } catch (Exception fallbackEx) {
                log.error("[composeShot] Fallback also failed: {}", fallbackEx.getMessage());
                throw new BusinessException(ResultCode.SERVER_ERROR, "视频合成失败: " + e.getMessage());
            }
        }
    }

    /**
     * 按分镜ID自动查找关联的视频和音频，然后合成（使用音画对齐引擎）
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
            // 豆包/火山引擎生成的视频已自带音画同步音频，合成时不再混入 TTS 配音
            if (audio != null && audio.getAudioUrl() != null && !("volcengine".equalsIgnoreCase(video.getProvider()))) {
                audioPath = audio.getAudioUrl();
            }
        } catch (Exception e) {
            log.warn("No audio found for storyboard {}: {}", storyboardId, e.getMessage());
        }

        // 3. 提取台词作为字幕
        String subtitle = null; // 从 Storyboard 实体获取 dialogue 字段（在 composeEpisode 中传入）

        // 4. 调用智能音画对齐引擎合成
        String outputDir = System.getProperty("user.dir") + "/data/composed/" + dramaId + "/";
        new File(outputDir).mkdirs();
        String outputPath = outputDir + "shot_" + storyboardId.substring(0, 8) + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".mp4";

        try {
            // 生成 ASS 字幕文件
            String assPath = null;

            // 5. 调用 AudioSyncEngine
            AudioSyncEngine.SyncResult syncResult = audioSyncEngine.composeWithSync(
                    video.getVideoUrl(), audioPath, assPath, outputPath);

            return Map.of(
                    "outputPath", outputPath,
                    "success", true,
                    "storyboardId", storyboardId,
                    "videoSource", video.getVideoUrl(),
                    "hasAudio", audioPath != null,
                    "duration", syncResult.getOutputDuration(),
                    "strategy", syncResult.getStrategy()
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

        // ========== Step 4: 按分镜顺序逐个合成镜头（智能音画对齐+字幕） ==========
        String outputDir = System.getProperty("user.dir") + "/data/composed/" + dramaId + "/";
        new File(outputDir).mkdirs();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        List<String> composedShotPaths = new ArrayList<>();
        List<Map<String, Object>> shotDetails = new ArrayList<>();  // 新增：记录每个镜头详情
        int shotCount = 0;

        for (Storyboard sb : storyboards) {
            Video video = videoMap.get(sb.getId());
            if (video == null || video.getVideoUrl() == null || video.getVideoUrl().isEmpty()) {
                log.info("[ComposeEpisode] Skip storyboard #{} ({}): no completed video",
                        sb.getShotNumber(), sb.getId());
                continue; // 跳过没有完成视频的分镜
            }

            Audio audio = audioMap.get(sb.getId());
            // 豆包/火山引擎生成的视频已自带音画同步音频，合成时不再混入 TTS 配音
            String audioPath = ("volcengine".equalsIgnoreCase(video.getProvider()))
                    ? null
                    : ((audio != null && audio.getAudioUrl() != null) ? audio.getAudioUrl() : null);

            // 提取台词作为字幕
            String dialogue = sb.getDialogue();

            try {
                String shotOutputPath = outputDir + String.format("shot_%03d_%s.mp4", sb.getShotNumber(), timestamp);

                // BUG00036-A Fix: 先探测视频/音频时长，动态计算ASS字幕时间轴长度
                // （旧代码硬编码 10f，导致PAD_SILENCE等长输出时字幕时间轴不够）
                float estimatedDuration = 10f; // 默认值（兜底）
                try {
                    VideoComposeService.VideoInfo vInfo = videoComposeService.getVideoInfo(video.getVideoUrl());
                    float vDur = vInfo.getDuration() != null ? vInfo.getDuration() : 5f;
                    float aDur = 0f;
                    if (audioPath != null) {
                        try {
                            VideoComposeService.AudioInfo aInfo = videoComposeService.getAudioInfo(audioPath);
                            aDur = aInfo.getDuration() != null ? aInfo.getDuration() : 0f;
                        } catch (Exception ignored) {}
                    }
                    // 取最大值 × 1.2 倍余量 + 2秒缓冲，覆盖所有策略的输出范围
                    estimatedDuration = Math.max(vDur, aDur) * 1.2f + 2f;
                    // 合理边界：最小5秒，最大5分钟
                    estimatedDuration = Math.max(5f, Math.min(estimatedDuration, 300f));
                } catch (Exception probeEx) {
                    log.warn("[ComposeEpisode] Duration probe failed for shot #{}, using default 10s: {}",
                            sb.getShotNumber(), probeEx.getMessage());
                }

                // 生成 ASS 字幕文件（使用动态计算的时长）
                Path assFile = subtitleRenderer.generateForShot(dialogue, estimatedDuration, shotOutputPath);
                String assPath = (assFile != null) ? assFile.toString() : null;

                // 使用智能音画对齐引擎
                AudioSyncEngine.SyncResult syncResult = audioSyncEngine.composeWithSync(
                        video.getVideoUrl(), audioPath, assPath, shotOutputPath);

                composedShotPaths.add(shotOutputPath);
                shotCount++;

                // 记录镜头详情
                Map<String, Object> detail = new java.util.HashMap<>();
                detail.put("shotNumber", sb.getShotNumber());
                detail.put("storyboardId", sb.getId());
                detail.put("strategy", syncResult.getStrategy());
                detail.put("videoDuration", syncResult.getVideoDuration());
                detail.put("audioDuration", syncResult.getAudioDuration());
                detail.put("outputDuration", syncResult.getOutputDuration());
                detail.put("hasSubtitle", syncResult.isHasSubtitle());
                detail.put("dialogue", dialogue != null && dialogue.length() > 20 ?
                        dialogue.substring(0, 20) + "..." : dialogue);
                shotDetails.add(detail);

                log.info("[ComposeEpisode] Composed shot #{}: strategy={}, v={}s a={}s → out={}s sub={}",
                        sb.getShotNumber(), syncResult.getStrategy(),
                        syncResult.getVideoDuration(), syncResult.getAudioDuration(),
                        syncResult.getOutputDuration(), syncResult.isHasSubtitle());
            } catch (Exception e) {
                log.warn("[ComposeEpisode] Shot #{} composition failed ({}): {}",
                        sb.getShotNumber(), e.getClass().getSimpleName(), e.getMessage());
                // BUG00036-D Fix: 降级时先下载到本地，避免远程URL直接传入concat导致拼接失败
                String fallbackPath = video.getVideoUrl();
                try {
                    // 尝试下载视频到本地临时文件（composeWithSync 内部也会下载）
                    java.nio.file.Path localVideo = videoComposeService.downloadVideoToTemp(video.getVideoUrl());
                    if (localVideo != null && java.nio.file.Files.exists(localVideo)) {
                        fallbackPath = localVideo.toString();
                        log.info("[ComposeEpisode] Shot #{} fallback: downloaded to local file", sb.getShotNumber());
                    }
                } catch (Exception dlEx) {
                    log.warn("[ComposeEpisode] Shot #{} fallback download failed, will try raw URL: {}",
                            sb.getShotNumber(), dlEx.getMessage());
                }
                composedShotPaths.add(fallbackPath);

                Map<String, Object> failDetail = new java.util.HashMap<>();
                failDetail.put("shotNumber", sb.getShotNumber());
                failDetail.put("storyboardId", sb.getId());
                failDetail.put("strategy", "FAILED_FALLBACK");
                failDetail.put("error", e.getMessage());
                shotDetails.add(failDetail);

                shotCount++;
            }
        }

        if (composedShotPaths.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该集暂无可合成的视频，请确保至少有1个状态为'已完成'的视频");
        }

        // ========== Step 5: 将所有合成后的镜头按顺序拼接 ==========
        String finalOutputPath = outputDir + "episode_" + episodeNumber + "_" + timestamp + ".mp4";
        
        // 转场拼接：目前默认使用快速模式（concat copy），后续可通过参数启用转场
        // TODO: 前端传入 useTransition / transitionType 参数后，改用 videoComposeService.concatWithTransition()
        boolean useTransition = false;  // 默认关闭转场（快速模式）
        float transitionDuration = 0.5f;
        String transitionType = "fade";
        
        String finalPath;
        if (useTransition && composedShotPaths.size() > 1) {
            finalPath = videoComposeService.concatWithTransition(
                    composedShotPaths, finalOutputPath, transitionDuration, transitionType);
            log.info("[ComposeEpisode] Used xfade transition (type={}, dur={})", transitionType, transitionDuration);
        } else {
            finalPath = videoComposeService.concatVideos(composedShotPaths, finalOutputPath);
        }

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
        // 将镜头对齐详情序列化到 extraData
        try {
            String shotDetailsJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter().writeValueAsString(shotDetails);
            exportRecord.setExtraData("{\"shotCount\":" + shotCount +
                    ",\"storyboardCount\":" + storyboards.size() +
                    ",\"withAudioSync\":true" +
                    ",\"withSubtitleRender\":true" +
                    ",\"composedAt\":\"" + LocalDateTime.now().toString() + "\"" +
                    ",\"shotDetails\":" + shotDetailsJson + "}");
        } catch (Exception jsonEx) {
            log.warn("Failed to serialize shot details: {}", jsonEx.getMessage());
            exportRecord.setExtraData("{\"shotCount\":" + shotCount +
                    ",\"storyboardCount\":" + storyboards.size() +
                    ",\"withAudioSync\":true" +
                    ",\"composedAt\":\"" + LocalDateTime.now().toString() + "\"}");
        }
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
