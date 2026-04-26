package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Audio;
import com.drama.entity.EpisodeExport;
import com.drama.entity.Storyboard;
import com.drama.entity.Video;
import com.drama.mapper.EpisodeExportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 整集导出服务
 * 将一集的所有分镜视频按顺序合成为一个完整视频
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeExportService extends ServiceImpl<EpisodeExportMapper, EpisodeExport> {

    private static final String FFMPEG_PATH = "ffmpeg";
    private static final String EXPORT_DIR = "./data/exports";
    
    private final StoryboardService storyboardService;
    private final VideoService videoService;
    private final AudioService audioService;
    private final FileStorageService fileStorageService;
    private final VideoComposeService videoComposeService;

    /**
     * 导出整集视频
     * 流程：1.获取该集所有分镜 2.获取每个分镜的视频 3.按顺序拼接 4.上传并保存记录
     */
    @Transactional
    public EpisodeExport exportEpisode(String dramaId, int episodeNumber) {
        log.info("[Export] Starting episode export: dramaId={}, episodeNumber={}", dramaId, episodeNumber);
        
        // 1. 检查FFmpeg是否可用
        if (!videoComposeService.isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg未安装，无法导出视频");
        }
        
        // 2. 获取该集的所有分镜（按镜头号排序）
        List<Storyboard> storyboards = storyboardService.listByEpisode(dramaId, episodeNumber);
        if (storyboards.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该集没有分镜，无法导出");
        }
        log.info("[Export] Found {} storyboards", storyboards.size());
        
        // 3. 收集所有可用的视频片段
        List<VideoSegment> segments = collectVideoSegments(dramaId, episodeNumber, storyboards);
        if (segments.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "没有可用的视频片段，请先生成视频");
        }
        log.info("[Export] Collected {} video segments", segments.size());
        
        // 4. 创建导出记录（初始状态为处理中）
        EpisodeExport export = createExportRecord(dramaId, episodeNumber, storyboards.size());
        
        try {
            // 5. 下载所有视频片段到本地临时目录
            List<String> localVideoPaths = downloadSegments(segments);
            log.info("[Export] Downloaded {} segments to local", localVideoPaths.size());
            
            // 6. 使用FFmpeg拼接视频
            String localOutputPath = EXPORT_DIR + "/" + export.getId() + ".mp4";
            Files.createDirectories(Paths.get(EXPORT_DIR));
            
            String finalVideoPath = concatVideos(localVideoPaths, localOutputPath);
            log.info("[Export] Video concatenated: {}", finalVideoPath);
            
            // 7. 读取合成后的视频文件
            byte[] videoData = Files.readAllBytes(Paths.get(finalVideoPath));
            
            // 8. 上传到存储（本地或OSS）
            String filename = "episode_" + episodeNumber + "_" + System.currentTimeMillis() + ".mp4";
            var asset = fileStorageService.uploadBytes(videoData, filename, dramaId, "video", "video/mp4");
            log.info("[Export] Uploaded to storage: {}", asset.getFileUrl());
            
            // 9. 获取视频时长
            Float duration = getVideoDuration(finalVideoPath);
            
            // 10. 更新导出记录为完成
            export.setStatus("completed");
            export.setExportUrl(asset.getFileUrl());
            export.setDuration(duration);
            this.updateById(export);
            
            // 11. 清理临时文件
            cleanupTempFiles(localVideoPaths, finalVideoPath);
            log.info("[Export] Export completed successfully: {}", export.getId());
            
            return export;
            
        } catch (Exception e) {
            log.error("[Export] Export failed: {}", e.getMessage(), e);
            export.setStatus("failed");
            export.setErrorMessage(e.getMessage());
            this.updateById(export);
            throw new BusinessException(ResultCode.SERVER_ERROR, "导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 收集视频片段信息
     */
    private List<VideoSegment> collectVideoSegments(String dramaId, int episodeNumber, List<Storyboard> storyboards) {
        List<VideoSegment> segments = new ArrayList<>();
        
        for (Storyboard sb : storyboards) {
            // 查找该分镜的视频
            List<Video> videos = videoService.listByStoryboard(dramaId, episodeNumber, sb.getId());
            
            for (Video video : videos) {
                if (video.getVideoUrl() != null && !video.getVideoUrl().isEmpty() 
                        && "completed".equals(video.getStatus())) {
                    segments.add(new VideoSegment(
                        sb.getShotNumber(),
                        video.getVideoUrl(),
                        video.getDuration()
                    ));
                    break; // 每个分镜只取第一个可用视频
                }
            }
        }
        
        // 按镜头号排序
        segments.sort((a, b) -> Integer.compare(a.shotNumber, b.shotNumber));
        return segments;
    }
    
    /**
     * 下载视频片段到本地
     */
    private List<String> downloadSegments(List<VideoSegment> segments) throws Exception {
        List<String> localPaths = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("episode_export_");
        
        for (int i = 0; i < segments.size(); i++) {
            VideoSegment seg = segments.get(i);
            String localPath = tempDir.toString() + "/shot_" + String.format("%04d", i) + ".mp4";
            
            // 下载视频文件
            downloadFile(seg.videoUrl, localPath);
            localPaths.add(localPath);
            log.debug("[Export] Downloaded segment {} to {}", i, localPath);
        }
        
        return localPaths;
    }
    
    /**
     * 下载文件
     */
    private void downloadFile(String url, String localPath) throws Exception {
        java.net.URI uri = java.net.URI.create(url);
        try (java.io.InputStream in = uri.toURL().openStream();
             java.io.FileOutputStream out = new java.io.FileOutputStream(localPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
    
    /**
     * 使用FFmpeg拼接多个视频
     */
    private String concatVideos(List<String> videoPaths, String outputPath) throws Exception {
        // 创建concat列表文件
        StringBuilder sb = new StringBuilder();
        for (String path : videoPaths) {
            // 使用绝对路径并处理特殊字符
            String absPath = Paths.get(path).toAbsolutePath().toString().replace("'", "'\\''");
            sb.append("file '").append(absPath).append("'\n");
        }
        
        Path listFile = Files.createTempFile("concat_list_", ".txt");
        Files.writeString(listFile, sb.toString());
        
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(FFMPEG_PATH);
            cmd.add("-y");
            cmd.add("-f");
            cmd.add("concat");
            cmd.add("-safe");
            cmd.add("0");
            cmd.add("-i");
            cmd.add(listFile.toString());
            // 重新编码以确保格式一致
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-preset");
            cmd.add("fast");
            cmd.add("-crf");
            cmd.add("23");
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-b:a");
            cmd.add("192k");
            cmd.add("-movflags");
            cmd.add("+faststart");
            cmd.add(outputPath);
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
            
            int code = p.waitFor();
            if (code != 0) {
                throw new RuntimeException("FFmpeg拼接失败，返回码: " + code);
            }
            
            return outputPath;
        } finally {
            // 清理列表文件
            Files.deleteIfExists(listFile);
        }
    }
    
    /**
     * 获取视频时长
     */
    private Float getVideoDuration(String videoPath) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("ffprobe");
            cmd.add("-v");
            cmd.add("error");
            cmd.add("-show_entries");
            cmd.add("format=duration");
            cmd.add("-of");
            cmd.add("default=noprint_wrappers=1:nokey=1");
            cmd.add(videoPath);
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String output = reader.readLine();
            p.waitFor();
            
            if (output != null && !output.trim().isEmpty()) {
                return Float.parseFloat(output.trim());
            }
        } catch (Exception e) {
            log.warn("[Export] Failed to get video duration: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 创建导出记录
     */
    private EpisodeExport createExportRecord(String dramaId, int episodeNumber, int shotCount) {
        EpisodeExport export = new EpisodeExport();
        export.setId(IdUtils.randomId());
        export.setDramaId(dramaId);
        export.setEpisodeNumber(episodeNumber);
        export.setShotCount(shotCount);
        export.setStatus("processing");
        export.setCreatedAt(LocalDateTime.now());
        export.setUpdatedAt(LocalDateTime.now());
        export.setDeleted(0);
        this.save(export);
        return export;
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(List<String> localPaths, String outputPath) {
        try {
            for (String path : localPaths) {
                Files.deleteIfExists(Paths.get(path));
            }
            // 删除临时目录
            if (!localPaths.isEmpty()) {
                Path tempDir = Paths.get(localPaths.get(0)).getParent();
                if (tempDir != null) {
                    Files.deleteIfExists(tempDir);
                }
            }
            Files.deleteIfExists(Paths.get(outputPath));
        } catch (Exception e) {
            log.warn("[Export] Cleanup temp files failed: {}", e.getMessage());
        }
    }
    
    /**
     * 分页查询导出记录
     */
    public Page<EpisodeExport> page(int pageNum, int pageSize, String dramaId, Integer episodeNumber) {
        LambdaQueryWrapper<EpisodeExport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EpisodeExport::getDeleted, 0);
        
        if (dramaId != null && !dramaId.isEmpty()) {
            wrapper.eq(EpisodeExport::getDramaId, dramaId);
        }
        if (episodeNumber != null && episodeNumber > 0) {
            wrapper.eq(EpisodeExport::getEpisodeNumber, episodeNumber);
        }
        
        wrapper.orderByDesc(EpisodeExport::getCreatedAt);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }
    
    /**
     * 视频片段信息
     */
    private record VideoSegment(int shotNumber, String videoUrl, Float duration) {}
}
