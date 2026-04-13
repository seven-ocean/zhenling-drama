package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Video;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    private static final String FFMPEG_PATH = "ffmpeg";
    private static final String FFPROBE_PATH = "ffprobe";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查FFmpeg是否可用
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(FFMPEG_PATH, "-version");
            Process p = pb.start();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 单镜头合成：视频 + 音频 + 字幕
     */
    @Transactional
    public String composeShot(String videoPath, String audioPath, String subtitle, String outputPath) {
        if (!isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg未安装");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(FFMPEG_PATH);
        cmd.add("-y"); // 覆盖输出

        // 视频输入
        if (videoPath != null && !videoPath.isEmpty()) {
            cmd.add("-i");
            cmd.add(videoPath);
        }

        // 音频输入
        if (audioPath != null && !audioPath.isEmpty()) {
            cmd.add("-i");
            cmd.add(audioPath);
        }

        // 字幕
        if (subtitle != null && !subtitle.isEmpty()) {
            cmd.add("-vf");
            cmd.add("drawtext=text='" + subtitle + "':fontcolor=white:fontsize=24:x=(w-text_w)/2:y=h-50");
        }

        // 编码参数
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-shortest");
        cmd.add(outputPath);

        try {
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
                throw new BusinessException(ResultCode.SERVER_ERROR, "视频合成失败");
            }
            
            log.info("Composed shot: {}", outputPath);
            return outputPath;
        } catch (Exception e) {
            log.error("Compose failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "视频合成失败: " + e.getMessage());
        }
    }

    /**
     * 拼接多个视频
     */
    @Transactional
    public String concatVideos(List<String> videoPaths, String outputPath) {
        if (!isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg未安装");
        }

        if (videoPaths == null || videoPaths.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "视频列表为空");
        }

        // 创建临时文件列表
        StringBuilder sb = new StringBuilder();
        for (String path : videoPaths) {
            sb.append("file '").append(path).append("'\n");
        }

        try {
            // 写入临时文件
            java.nio.file.Path listFile = java.nio.file.Files.createTempFile("concat", ".txt");
            java.nio.file.Files.writeString(listFile, sb.toString());

            List<String> cmd = new ArrayList<>();
            cmd.add(FFMPEG_PATH);
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
            java.nio.file.Files.deleteIfExists(listFile);
            
            if (code != 0) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "视频拼接失败");
            }
            
            log.info("Concatenated videos: {}", outputPath);
            return outputPath;
        } catch (Exception e) {
            log.error("Concat failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "视频拼接失败: " + e.getMessage());
        }
    }

    /**
     * 获取视频信息
     */
    public VideoInfo getVideoInfo(String videoPath) {
        if (!isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg未安装");
        }

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(FFPROBE_PATH);
            cmd.add("-v");
            cmd.add("quiet");
            cmd.add("-print_format");
            cmd.add("json");
            cmd.add("-show_format");
            cmd.add("-show_streams");
            cmd.add(videoPath);

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
            log.error("Get video info failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "获取视频信息失败");
        }
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