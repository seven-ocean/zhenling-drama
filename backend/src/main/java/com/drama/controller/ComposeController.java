package com.drama.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drama.common.R;
import com.drama.entity.EpisodeExport;
import com.drama.service.ComposeService;
import com.drama.service.VideoComposeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 视频合成接口
 * 提供单镜头合成（视频+音频+字幕）和整集拼接能力
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/compose")
@RequiredArgsConstructor
public class ComposeController {

    private final VideoComposeService videoComposeService;
    private final ComposeService composeService;

    /** 允许的文件扩展名白名单（防止传入非法文件类型被处理后执行） */
    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._\\-/\\\\:]+$"
    );

    /**
     * 检查 FFmpeg 是否可用
     */
    @GetMapping("/ffmpeg-status")
    public R<Map<String, Object>> ffmpegStatus() {
        boolean available = videoComposeService.isFFmpegAvailable();
        return R.ok(Map.of(
                "available", available,
                "message", available ? "FFmpeg 已就绪" : "FFmpeg 未安装或不在 PATH 中"
        ));
    }

    /**
     * 单镜头合成：视频 + 音频 + 字幕 → 合成后的视频路径
     */
    @PostMapping("/shot")
    public R<Map<String, Object>> composeShot(
            @RequestParam String videoPath,
            @RequestParam(required = false) String audioPath,
            @RequestParam(required = false) String subtitle) {

        // 输入校验
        if (videoPath == null || videoPath.isBlank()) {
            return R.fail(400, "视频路径不能为空");
        }
        if (videoPath.length() > 2048) {
            return R.fail(400, "视频路径过长");
        }
        // 校验路径只包含安全字符（防止命令注入）
        if (!SAFE_PATH_PATTERN.matcher(videoPath).matches() ||
                (audioPath != null && !audioPath.isBlank() && !SAFE_PATH_PATTERN.matcher(audioPath).matches())) {
            return R.fail(400, "路径包含非法字符");
        }

        log.info("Compose shot: video={}, audio={}, hasSubtitle={}", videoPath, audioPath, subtitle != null);
        try {
            String outputPath = composeService.composeShot(videoPath, audioPath, subtitle).toString();
            return R.ok(Map.of("outputPath", outputPath, "success", true));
        } catch (Exception e) {
            log.error("Compose shot failed", e);
            return R.fail(500, "单镜头合成失败: " + e.getMessage());
        }
    }

    /**
     * 按分镜ID合成单镜头（自动查找视频和音频）
     */
    @PostMapping("/shot/storyboard")
    public R<Map<String, Object>> composeShotByStoryboard(
            @RequestParam String dramaId,
            @RequestParam String storyboardId,
            @RequestParam int episodeNumber) {
        log.info("Compose shot by storyboard: dramaId={}, sbId={}", dramaId, storyboardId);
        try {
            Map<String, Object> result = composeService.composeShotByStoryboard(dramaId, storyboardId, episodeNumber);
            return R.ok(result);
        } catch (Exception e) {
            log.error("Compose shot by storyboard failed", e);
            return R.fail(500, "合成失败: " + e.getMessage());
        }
    }

    /**
     * 整集视频拼接：将某集所有已完成的视频按镜头顺序拼接为完整剧集
     */
    @PostMapping("/episode")
    public R<EpisodeExport> composeEpisode(
            @RequestParam String dramaId,
            @RequestParam int episodeNumber) {
        log.info("Compose episode: dramaId={}, ep={}", dramaId, episodeNumber);
        return R.ok(composeService.composeEpisode(dramaId, episodeNumber));
    }

    /**
     * 查询合成记录列表
     */
    @GetMapping("/records")
    public R<IPage<EpisodeExport>> listRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dramaId,
            @RequestParam(required = false) Integer episodeNumber) {
        return R.ok(composeService.listRecords(pageNum, pageSize, dramaId, episodeNumber));
    }

    /**
     * 合成文件下载/预览（本地存储的合成视频通过此端点访问）
     * 路径格式: /api/v1/compose/download/{dramaId}/{filename}
     */
    @GetMapping("/download/**")
    public ResponseEntity<Resource> serveComposedFile(HttpServletRequest request) {
        try {
            String requestUri = request.getRequestURI();
            // 获取 /download/ 之后的部分
            String relativePath = requestUri.substring(requestUri.indexOf("/download/") + "/download/".length());

            // 安全检查
            if (relativePath.contains("..")) {
                log.warn("Blocked path traversal in compose download: {}", relativePath);
                return ResponseEntity.status(403).build();
            }

            // 合成目录: {project}/data/composed/
            Path filePath = Paths.get("./data/composed", relativePath).normalize();
            Path baseDir = Paths.get("./data/composed").normalize().toAbsolutePath();

            if (!filePath.toAbsolutePath().startsWith(baseDir)) {
                log.warn("Compose path escape attempt: {}", relativePath);
                return ResponseEntity.status(403).build();
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.warn("Compose file not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "video/mp4";
            }

            long contentLength = Files.size(filePath);

            log.info("Serving compose file: {} ({}, {}bytes)", relativePath, contentType, contentLength);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving compose file", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
