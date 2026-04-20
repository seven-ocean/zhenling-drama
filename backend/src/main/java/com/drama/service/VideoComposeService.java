package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Video;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FFmpeg视频合成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoComposeService {

    private final FfmpegConfig ffmpegConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate downloadRestTemplate = new RestTemplate();

    /**
     * FFmpeg 路径配置
     * 支持通过 application.yml 配置 ffmpeg/ffprobe 的完整路径
     */
    @Data
    @Component
    @ConfigurationProperties(prefix = "ffmpeg")
    public static class FfmpegConfig {
        /** FFmpeg 可执行文件路径，留空则使用系统PATH中的 "ffmpeg" */
        private String path = "";
        /** ffprobe 可执行文件路径，留空则使用系统PATH中的 "ffprobe" */
        private String probePath = "";

        /** 获取实际FFmpeg路径（带默认值） */
        public String getFfmpegPath() {
            return (path != null && !path.isBlank()) ? path.trim() : "ffmpeg";
        }
        /** 获取实际ffprobe路径（带默认值） */
        public String getFfprobePath() {
            return (probePath != null && !probePath.isBlank()) ? probePath.trim() : "ffprobe";
        }
    }

    /**
     * 检查FFmpeg是否可用
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegConfig.getFfmpegPath(), "-version");
            Process p = pb.start();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available: path={}, error={}", ffmpegConfig.getFfmpegPath(), e.getMessage());
            return false;
        }
    }

    /**
     * 单镜头合成：视频 + 音频 + 字幕
     * 支持HTTP/HTTPS远程URL（会自动下载到本地临时文件）
     */
    @Transactional
    public String composeShot(String videoPath, String audioPath, String subtitle, String outputPath) {
        if (!isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg未安装");
        }

        List<Path> tempFiles = new ArrayList<>();
        String localVideoPath = videoPath;
        String localAudioPath = audioPath;

        try {
            // 如果视频是远程URL，下载到本地临时文件
            if (videoPath != null && (videoPath.startsWith("http://") || videoPath.startsWith("https://"))) {
                Path tempVideo = downloadVideoToTemp(videoPath);
                tempFiles.add(tempVideo);
                localVideoPath = tempVideo.toString().replace("\\", "/");
                log.info("[ComposeShot] Downloaded remote video: {} -> {}", videoPath, tempVideo);
            }

            // 如果音频是远程URL，下载到本地临时文件
            if (audioPath != null && (audioPath.startsWith("http://") || audioPath.startsWith("https://"))) {
                Path tempAudio = downloadVideoToTemp(audioPath);  // 复用下载方法，音频也支持
                tempFiles.add(tempAudio);
                localAudioPath = tempAudio.toString().replace("\\", "/");
                log.info("[ComposeShot] Downloaded remote audio: {} -> {}", audioPath, tempAudio);
            }

            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegConfig.getFfmpegPath());
            cmd.add("-y"); // 覆盖输出

            // 视频输入（使用本地路径）
            if (localVideoPath != null && !localVideoPath.isEmpty()) {
                cmd.add("-i");
                cmd.add(localVideoPath);
            }

            // 音频输入（使用本地路径）
            if (localAudioPath != null && !localAudioPath.isEmpty()) {
                cmd.add("-i");
                cmd.add(localAudioPath);
            }

            // 字幕
            if (subtitle != null && !subtitle.isEmpty()) {
                cmd.add("-vf");
                cmd.add("drawtext=text='" + escapeForFFmpeg(subtitle) + "':fontcolor=white:fontsize=24:x=(w-text_w)/2:y=h-50");
            }

            // 编码参数
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-shortest");
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

            // 清理临时文件
            for (Path tempFile : tempFiles) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }

            if (code != 0) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "视频合成失败(退出码:" + code + ")");
            }

            log.info("Composed shot: {}", outputPath);
            return outputPath;
        } catch (Exception e) {
            // 清理临时文件
            for (Path tempFile : tempFiles) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
            log.error("Compose shot failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "视频合成失败: " + e.getMessage());
        }
    }

    /**
     * 拼接多个视频
     * 支持HTTP/HTTPS URL，会自动下载到本地临时文件
     */
    @Transactional
    public String concatVideos(List<String> videoPaths, String outputPath) {
        if (!isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg未安装");
        }

        if (videoPaths == null || videoPaths.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "视频列表为空");
        }

        List<Path> tempFiles = new ArrayList<>();
        
        try {
            // 下载远程URL到本地临时文件
            List<String> localPaths = new ArrayList<>();
            for (String videoPath : videoPaths) {
                if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
                    // 下载远程文件
                    Path tempFile = downloadVideoToTemp(videoPath);
                    tempFiles.add(tempFile);
                    localPaths.add(tempFile.toString().replace("\\", "/"));
                    log.info("[Concat] Downloaded remote video: {} -> {}", videoPath, tempFile);
                } else {
                    // 本地文件直接使用
                    localPaths.add(videoPath.replace("\\", "/"));
                }
            }

            // 创建临时文件列表
            StringBuilder sb = new StringBuilder();
            for (String path : localPaths) {
                sb.append("file '").append(path).append("'\n");
            }

            // 写入临时文件
            Path listFile = Files.createTempFile("concat", ".txt");
            tempFiles.add(listFile);
            Files.writeString(listFile, sb.toString());

            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegConfig.getFfmpegPath());
            cmd.add("-y");
            cmd.add("-f");
            cmd.add("concat");
            cmd.add("-safe");
            cmd.add("0");
            cmd.add("-i");
            cmd.add(listFile.toString());
            cmd.add("-c");
            cmd.add("copy");
            cmd.add(outputPath);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
            
            int code = p.waitFor();
            
            // 清理临时文件
            for (Path tempFile : tempFiles) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ex) {
                    log.warn("[Concat] Failed to delete temp file: {}", tempFile);
                }
            }
            
            if (code != 0) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "视频拼接失败");
            }
            
            log.info("Concatenated videos: {}", outputPath);
            return outputPath;
        } catch (Exception e) {
            // 清理临时文件
            for (Path tempFile : tempFiles) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ex) {
                    log.warn("[Concat] Failed to delete temp file: {}", tempFile);
                }
            }
            log.error("Concat failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "视频拼接失败: " + e.getMessage());
        }
    }

    /**
     * 下载视频到本地临时文件
     */
    private Path downloadVideoToTemp(String videoUrl) throws Exception {
        String extension = ".mp4";
        // 尝试从URL推断扩展名
        if (videoUrl.contains(".")) {
            String urlExt = videoUrl.substring(videoUrl.lastIndexOf('.'));
            if (urlExt.contains("?")) {
                urlExt = urlExt.substring(0, urlExt.indexOf('?'));
            }
            if (urlExt.length() <= 5) {
                extension = urlExt;
            }
        }
        
        Path tempFile = Files.createTempFile("video_", extension);
        
        try {
            ResponseEntity<byte[]> response = downloadRestTemplate.getForEntity(
                URI.create(videoUrl), byte[].class);
            
            if (response.getBody() != null) {
                Files.write(tempFile, response.getBody());
            } else {
                throw new BusinessException(ResultCode.SERVER_ERROR, "下载视频失败：空响应");
            }
            
            return tempFile;
        } catch (Exception e) {
            Files.deleteIfExists(tempFile);
            throw new BusinessException(ResultCode.SERVER_ERROR, 
                "下载视频失败: " + videoUrl + " - " + e.getMessage());
        }
    }

    /**
     * 获取视频信息
     * 支持HTTP/HTTPS远程URL（会自动下载到本地临时文件后分析）
     */
    public VideoInfo getVideoInfo(String videoPath) {
        if (!isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg未安装");
        }

        // 如果是远程URL，先下载到临时文件
        String actualPath = videoPath;
        Path tempFile = null;
        if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
            try {
                tempFile = downloadVideoToTemp(videoPath);
                actualPath = tempFile.toString().replace("\\", "/");
                log.info("[getVideoInfo] Downloaded remote file for analysis: {} -> {}", videoPath, tempFile);
            } catch (Exception e) {
                log.warn("Failed to download for ffprobe, attempting direct: {}", e.getMessage());
            }
        }

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegConfig.getFfprobePath());
            cmd.add("-v");
            cmd.add("quiet");
            cmd.add("-print_format");
            cmd.add("json");
            cmd.add("-show_format");
            cmd.add("-show_streams");
            cmd.add(actualPath);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            // 解析 ffprobe JSON 输出
            String jsonOutput = sb.toString();
            VideoInfo info = new VideoInfo();
            info.setPath(videoPath);

            if (!jsonOutput.isEmpty()) {
                try {
                    JsonNode root = objectMapper.readTree(jsonOutput);

                    // 从 format 获取 duration / bitrate
                    JsonNode format = root.path("format");
                    if (!format.isMissingNode()) {
                        double dur = format.path("duration").asDouble(0);
                        info.setDuration(dur > 0 ? (float) dur : null);
                        long bitRate = format.path("bit_rate").asLong(0L);
                        info.setBitrate(bitRate > 0 ? bitRate : null);
                    }

                    // 遍历 streams 找到视频流
                    JsonNode streams = root.path("streams");
                    if (streams.isArray()) {
                        for (JsonNode stream : streams) {
                            String codecType = stream.path("codec_type").asText("");
                            if ("video".equals(codecType)) {
                                int w = stream.path("width").asInt(0);
                                int h = stream.path("height").asInt(0);
                                info.setWidth(w > 0 ? w : null);
                                info.setHeight(h > 0 ? h : null);
                                String codec = stream.path("codec_name").asText("");
                                if (!codec.isEmpty()) info.setCodec(codec);
                                break; // 取第一个视频流即可
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse ffprobe JSON: {}", e.getMessage());
                }
            }
            return info;
        } catch (Exception e) {
            // 清理下载的临时文件
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
            log.error("Get video info failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "获取视频信息失败");
        } finally {
            // 确保临时文件被清理
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 转义FFmpeg drawtext中的特殊字符
     */
    private String escapeForFFmpeg(String text) {
        if (text == null) return "";
        return text.replace("'", "'\\''")
                   .replace(":", "\\:")
                   .replace("\\", "\\\\");
    }

    /**
     * 视频信息类
     */
    @lombok.Data
    public static class VideoInfo {
        private String path;
        private Float duration;
        private Integer width;
        private Integer height;
        private String codec;
        private Long bitrate;
    }
}