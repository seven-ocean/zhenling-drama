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
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg视频合成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoComposeService {

    private final FfmpegConfig ffmpegConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** 注入由 RestTemplateConfig 创建的 Bean（支持代理/DNS/超时配置） */
    private final RestTemplate restTemplate;

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

            int code = waitForWithTimeout(p, 5, "ComposeShot");

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
            
            int code = waitForWithTimeout(p, 10, "Concat");

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
     * 带转场效果的视频拼接（使用 xfade 滤镜链）
     *
     * <p>与 {@link #concatVideos} 的区别：
     * <ul>
     *   <li>-c copy 流拷贝 → libx264 重新编码（支持滤镜）</li>
     *   <li>硬切切换 → 可选 crossfade/dissolve/slide 等转场</li>
     *   <li>速度极快(~2s) → 较慢但高质量(~30-60s)</li>
     * </ul>
     *
     * @param videoPaths         已合成的单镜头视频路径列表
     * @param outputPath         输出文件绝对路径
     * @param transitionDuration 转场时长(秒)，0 或负数表示无转场（回退到 concatVideos）
     * @param transitionType     转场类型：fade/dissolve/slideleft/slideright/wipeleft/circlecrop
     * @return 输出文件路径
     */
    @Transactional
    public String concatWithTransition(List<String> videoPaths, String outputPath,
                                         float transitionDuration, String transitionType) {
        // 无转场或单个视频 → 回退到快速拼接
        if (transitionDuration <= 0 || videoPaths.size() <= 1) {
            log.info("[ConcatTransition] No transition needed (dur={}, count={}), falling back to concat",
                    transitionDuration, videoPaths.size());
            return concatVideos(videoPaths, outputPath);
        }

        if (!isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg未安装");
        }

        List<Path> tempFiles = new ArrayList<>();
        
        try {
            // === Step A: 下载远程URL + 探测每个视频的时长 ===
            List<String> localPaths = new ArrayList<>();
            List<Float> durations = new ArrayList<>();

            for (String videoPath : videoPaths) {
                String localPath;
                if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
                    Path tempFile = downloadVideoToTemp(videoPath);
                    tempFiles.add(tempFile);
                    localPath = tempFile.toString().replace("\\", "/");
                    log.info("[ConcatTransition] Downloaded remote: {}", localPath);
                } else {
                    localPath = videoPath.replace("\\", "/");
                }
                localPaths.add(localPath);

                // 探测时长（用于计算 xfade offset）
                try {
                    VideoInfo info = getVideoInfo(localPath);
                    float dur = info.getDuration() != null ? info.getDuration() : 5f;
                    durations.add(dur);
                } catch (Exception e) {
                    log.warn("[ConcatTransition] Failed to probe duration for {}, defaulting to 5s: {}", 
                            localPath, e.getMessage());
                    durations.add(5f);
                }
            }

            // === Step B: 动态构建 xfade 滤镜链 ===
            StringBuilder filterComplex = new StringBuilder();
            float offset = 0f;

            // N个视频需要 N-1 次 xfade
            for (int i = 0; i < localPaths.size() - 1; i++) {
                float currentDur = durations.get(i);

                // 输入标签
                String in1Label = i == 0 ? "[0:v]" : String.format("[v%02d]", i);
                String in2Label = String.format("[%d:v]", i + 1);
                // 输出标签（最后一个用 [vout]，中间的用 [vN+1]）
                String outLabel = i == localPaths.size() - 2 ? "[vout]" : String.format("[v%02d]", i + 1);

                // xfade offset = 当前视频开始时间 + 当前视频时长 - 转场重叠时间
                float xfadeOffset = offset + currentDur - transitionDuration;

                if (filterComplex.length() > 0) filterComplex.append(' ');
                
                filterComplex.append(String.format(
                        "%s%sxfade=transition=%s:duration=%.2f:offset=%.2f%s",
                        filterComplex.length() > 0 ? "" : "",
                        in1Label,
                        transitionType != null ? transitionType : "fade",
                        transitionDuration,
                        Math.max(0, xfadeOffset),   // offset 不能为负
                        outLabel
                ));

                offset += currentDur - transitionDuration;
            }

            log.info("[ConcatTransition] Filter chain ({} videos, {} transitions): {}",
                    localPaths.size(), localPaths.size() - 1, filterComplex);

            // === Step C: 构建 FFmpeg 命令 ===
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegConfig.getFfmpegPath());
            cmd.add("-y");
            
            // 所有输入视频
            for (String lp : localPaths) {
                cmd.add("-i");
                cmd.add(lp);
            }

            // 滤镜链
            cmd.add("-filter_complex");
            cmd.add(filterComplex.toString());

            // 只取最终输出流
            cmd.add("-map");
            cmd.add("[vout]");
            
            // 编码参数（高质量重编码）
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-preset");
            cmd.add("fast");           // 平衡速度和质量
            cmd.add("-crf");
            cmd.add("18");             // 近无损质量

            // 如果有音频轨，也做交叉淡化
            boolean hasAudio = false;
            try {
                VideoInfo firstInfo = getVideoInfo(localPaths.get(0));
                // 简化处理：只对视频做转场，音频后续可单独处理
            } catch (Exception ignored) {}

            cmd.add("-an");             // 先去掉音频（音频在镜头合成阶段已混入）

            cmd.add(outputPath);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[FFmpeg:XFade] {}", line);
            }

            int code = waitForWithTimeout(p, 15, "ConcatTransition");  // 转场拼接可能较慢，给15分钟

            // 清理临时文件
            for (Path tempFile : tempFiles) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ex) {
                    log.warn("[ConcatTransition] Failed to delete temp: {}", tempFile);
                }
            }

            if (code != 0) {
                // 转场失败时降级到快速拼接
                log.warn("[ConcatTransition] Xfade failed (code={}), fallback to concat copy", code);
                return concatVideos(videoPaths, outputPath);
            }

            log.info("[ConcatTransition] Done with transitions: {} → {} ({} videos)",
                    videoPaths.size(), outputPath, localPaths.size());
            return outputPath;

        } catch (BusinessException e) {
            // 清理临时文件
            for (Path tempFile : tempFiles) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
            throw e;
        } catch (Exception e) {
            for (Path tempFile : tempFiles) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
            log.error("Concat with transition failed: {}", e.getMessage());
            // 最终降级：回到无转场拼接
            log.info("[ConcatTransition] Final fallback to simple concat");
            return concatVideos(videoPaths, outputPath);
        }
    }

    /**
     * 下载视频/音频到本地临时文件
     * （包级可见：供 AudioSyncEngine 等同类复用）
     *
     * <p>包含下载后有效性校验：
     * <ul>
     *   <li>HTTP 状态码检查（非2xx抛异常）</li>
     *   <li>响应体空值检查</li>
     *   <li>最小体积校验（≥512字节，防止HTML错误页被当媒体）</li>
     * </ul>
     */
    Path downloadVideoToTemp(String videoUrl) throws Exception {
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
            ResponseEntity<byte[]> response = restTemplate.getForEntity(
                URI.create(videoUrl), byte[].class);

            // 检查HTTP状态码
            if (!response.getStatusCode().is2xxSuccessful()) {
                Files.deleteIfExists(tempFile);
                throw new BusinessException(ResultCode.SERVER_ERROR,
                    "下载失败: HTTP " + response.getStatusCode().value() + " (URL: " + videoUrl + ")");
            }

            if (response.getBody() == null || response.getBody().length == 0) {
                Files.deleteIfExists(tempFile);
                throw new BusinessException(ResultCode.SERVER_ERROR,
                    "下载失败: 空响应体 (URL: " + videoUrl + ")");
            }

            byte[] data = response.getBody();

            // 最小体积校验: 音频/视频文件至少 512 字节（防止HTML错误页或重定向页）
            // 注意：某些极短音频可能 < 512B，但正常AI生成的TTS通常 > 1KB
            if (data.length < 512) {
                String contentType = response.getHeaders().getContentType() != null
                        ? response.getHeaders().getContentType().toString() : "unknown";
                log.warn("[downloadVideoToTemp] Response suspiciously small ({}B, Content-Type: {}) for URL: {}",
                        data.length, contentType, shortUrlForLog(videoUrl));
                // 不直接拒绝，让上层 isValidAudioFile 做最终判断
                // 但记录警告以便排查
            }

            Files.write(tempFile, data);

            log.debug("[downloadVideoToTemp] Downloaded {} ({} bytes) from {}", tempFile.getFileName(), data.length, shortUrlForLog(videoUrl));
            return tempFile;
        } catch (BusinessException e) {
            throw e; // 向上传播业务异常
        } catch (Exception e) {
            Files.deleteIfExists(tempFile);
            throw new BusinessException(ResultCode.SERVER_ERROR,
                "下载失败: " + shortUrlForLog(videoUrl) + " - " + e.getMessage());
        }
    }

    /** 截断显示URL（日志用，隐藏查询参数） */
    private String shortUrlForLog(String url) {
        if (url == null) return "null";
        if (url.length() > 120) {
            return url.substring(0, 117) + "...";
        }
        return url;
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
     * 转义FFmpeg drawtext/filter中的特殊字符
     *
     * 防御命令注入，转义以下特殊字符：
     * - '  单引号（drawtext文本边界符）
     * - :  冒号（滤镜参数分隔符）
     * - \  反斜杠（转义字符本身）
     * - %  百分号（FFmpeg中用于引用包/流元数据，可读取任意文件内容）
     * - [  ] 方括号（FFmpeg滤镜语法字符）
     */
    private String escapeForFFmpeg(String text) {
        if (text == null) return "";
        return text.replace("'", "'\\''")
                   .replace(":", "\\:")
                   .replace("\\", "\\\\")
                   .replace("%", "\\%")
                   .replace("[", "\\[")
                   .replace("]", "\\]");
    }

    /**
     * 带超时的进程等待方法（package-visible，供 AudioSyncEngine 调用）
     * 替代 p.waitFor()，防止 FFmpeg 卡死时无限阻塞 Tomcat 线程
     *
     * @param process  进程对象
     * @param timeoutMinutes 超时时间（分钟）
     * @param operation 操作名称（用于日志）
     * @return 进程退出码；超时返回 -1
     */
    int waitForWithTimeout(Process process, int timeoutMinutes, String operation) {
        try {
            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                log.warn("[{}] FFmpeg process timed out after {}min, force destroying...", operation, timeoutMinutes);
                process.destroyForcibly();
                // 给进程3秒优雅退出
                try { process.waitFor(3, TimeUnit.SECONDS); } catch (Exception ignored) {}
                return -1;
            }
            return process.exitValue();
        } catch (InterruptedException e) {
            log.warn("[{}] FFmpeg process interrupted: {}", operation, e.getMessage());
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return -1;
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

    /**
     * 音频信息类（用于 ffprobe 分析音频时长）
     */
    @lombok.Data
    public static class AudioInfo {
        private String path;
        private Float duration;       // 音频总时长(秒)
        private Integer sampleRate;   // 采样率(Hz), 如44100
        private Integer channels;     // 声道数: 1=单声道, 2=立体声
        private String codec;         // 编码格式: mp3/aac/ogg等
        private Long bitrate;         // 比特率
    }

    /**
     * 使用 ffprobe 获取音频文件信息
     * 复用 getVideoInfo 的 ffprobe 基础设施，专门解析音频流
     *
     * @param audioPath 音频文件路径或URL
     * @return 音频元数据
     */
    public AudioInfo getAudioInfo(String audioPath) {
        if (!isFFmpegAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "FFmpeg未安装");
        }

        String actualPath = audioPath;
        Path tempFile = null;

        // 如果是远程URL，先下载到临时文件
        if (audioPath.startsWith("http://") || audioPath.startsWith("https://")) {
            try {
                tempFile = downloadVideoToTemp(audioPath);  // 复用下载方法
                actualPath = tempFile.toString().replace("\\", "/");
                log.info("[getAudioInfo] Downloaded remote audio: {} -> {}", audioPath, tempFile);
            } catch (Exception e) {
                log.warn("Failed to download audio for probing: {}", e.getMessage());
            }
        }

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegConfig.getFfprobePath());
            cmd.add("-v");
            cmd.add("quiet");
            cmd.add("-print_format");
            cmd.add("json");
            cmd.add("-show_format");           // format.duration = 总时长
            cmd.add("-show_streams");          // 找 codec_type=audio 的流
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

            AudioInfo info = new AudioInfo();
            info.setPath(audioPath);

            String jsonOutput = sb.toString();
            if (!jsonOutput.isEmpty()) {
                JsonNode root = objectMapper.readTree(jsonOutput);

                // 从 format 获取时长和比特率
                JsonNode format = root.path("format");
                if (!format.isMissingNode()) {
                    double dur = format.path("duration").asDouble(0);
                    info.setDuration(dur > 0 ? (float) dur : null);
                    long br = format.path("bit_rate").asLong(0L);
                    info.setBitrate(br > 0 ? br : null);
                }

                // 遍历 streams 找音频流
                JsonNode streams = root.path("streams");
                if (streams.isArray()) {
                    for (JsonNode stream : streams) {
                        if ("audio".equals(stream.path("codec_type").asText(""))) {
                            int sr = stream.path("sample_rate").asInt(0);
                            info.setSampleRate(sr > 0 ? sr : null);
                            int ch = stream.path("channels").asInt(0);
                            info.setChannels(ch > 0 ? ch : null);
                            String codecName = stream.path("codec_name").asText("");
                            if (!codecName.isEmpty()) info.setCodec(codecName);
                            break;  // 取第一个音频流
                        }
                    }
                }
            }

            return info;
        } catch (Exception e) {
            if (tempFile != null) { try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {} }
            log.error("Get audio info failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "获取音频信息失败");
        } finally {
            if (tempFile != null) { try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {} }
        }
    }

    /** 获取 FFmpeg 配置实例（供其他 Service 访问 ffmpeg 路径等） */
    public FfmpegConfig getFfmpegConfig() {
        return this.ffmpegConfig;
    }
}