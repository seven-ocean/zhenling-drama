package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.ResultCode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 音画时间轴对齐引擎
 *
 * <p>解决 AI 视频固定时长（MiniMax/豆包通常 5 秒）与 TTS 配音可变时长（2~15 秒）不匹配的问题。
 *
 * <h3>四种对齐策略</h3>
 * <ul>
 *   <li>{@code DIRECT_BLEND} — 时差 ≤1s，直接 -shortest 混流（理想情况）</li>
 *   <li>{@code EXTEND_VIDEO_FREEZE} — 配音长于视频，冻结视频末帧延展到配音结束</li>
 *   <li>{@code PAD_AUDIO_SILENCE} — 视频长于配音，在音频后补静音段匹配视频长度</li>
 *   <li>{@code VIDEO_ONLY} — 无配音，纯画面 + 字幕渲染</li>
 * </ul>
 *
 * <p>降级策略：任何策略执行失败时自动回退到 {@link VideoComposeService#composeShot} 的简单模式。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioSyncEngine {

    private final VideoComposeService videoComposeService;

    // ========== 策略常量 ==========
    public static final String STRATEGY_DIRECT_BLEND = "DIRECT_BLEND";
    public static final String STRATEGY_EXTEND_FREEZE = "EXTEND_VIDEO_FREEZE";
    public static final String STRATEGY_PAD_SILENCE = "PAD_AUDIO_SILENCE";
    public static final String STRATEGY_VIDEO_ONLY = "VIDEO_ONLY";

    /** 策略选择阈值：时差 ≤ 此值视为基本匹配（秒） */
    private static final float MATCH_THRESHOLD_SECONDS = 1.0f;

    /** 冻结帧延展视频的 CRF 质量参数（越小质量越高，18≈近无损） */
    private static final int FREEZE_CRF = 18;

    /** 静音音频比特率 */
    private static final String SILENCE_BITRATE = "128k";

    /**
     * 合成结果 DTO
     */
    @Data
    public static class SyncResult {
        private String outputPath;       // 最终输出文件绝对路径
        private float outputDuration;    // 实际输出时长(秒)
        private String strategy;         // 使用的策略名
        private float videoDuration;     // 原始视频时长(秒)
        private float audioDuration;     // 原始音频时长(秒)
        private boolean hasSubtitle;     // 是否包含字幕
    }

    // ====================================================================
    // 核心入口
    // ====================================================================

    /**
     * 智能音画混流（主入口）
     *
     * @param videoPath      视频路径（本地或远程 URL）
     * @param audioPath      音频路径（本地或远程 URL），可为 null
     * @param subtitleAssPath ASS 字幕文件绝对路径，可为 null
     * @param outputPath     输出 MP4 文件绝对路径
     * @return 对齐结果详情
     */
    public SyncResult composeWithSync(
            String videoPath,
            String audioPath,
            String subtitleAssPath,
            String outputPath) {

        log.info("[AudioSync] Start: video={}, audio={}, subtitle={}",
                shortPath(videoPath), shortPath(audioPath),
                subtitleAssPath != null ? shortPath(subtitleAssPath) : "null");

        // === Phase 0: 远程文件预下载（FFmpeg 对远程URL+滤镜组合支持不稳定）===
        List<Path> downloadedFiles = new ArrayList<>();
        String localVideoPath;
        String localAudioPath;
        try {
            localVideoPath = resolveLocalPath(videoPath, downloadedFiles);
            localAudioPath = (audioPath != null && !audioPath.isEmpty())
                    ? resolveLocalPath(audioPath, downloadedFiles) : audioPath;

            // === Phase 0.5: 下载后音频有效性校验 + 预转码 ===
            // BUG00037-R10-Fix1 根治方案：
            // MiniMax TTS 生成的 MP3 文件缺少标准 ID3 帧头，FFmpeg mp3float 解码器会报 "Header missing"
            // 但实际上能成功解码出有效音频（只是 warning 级别）。
            // 方案：下载后立刻用 FFmpeg 转码为干净 AAC 格式，后续所有步骤不再接触原始 MP3。
            //
            // R10-Fix1b: 原方案用 WAV(24kHz/mono)，但和 createSilencePad 生成的 AAC(44.1kHz/stereo)
            // 格式不匹配，concatTwoAudios 用 -f concat 拼接时导致时长计算异常（4669s vs 实际5.9s）。
            // 改为 AAC(44.1kHz/stereo/128k)，和静音段完全一致。
            if (localAudioPath != null && !localAudioPath.isEmpty()
                    && !localAudioPath.startsWith("http")) {
                Path audioFile = Path.of(localAudioPath);
                
                // Step A: 魔数校验（R5遗留）
                if (!isValidAudioFile(audioFile)) {
                    log.warn("[AudioSync] Downloaded audio file INVALID (bad magic or too small): {} size={}B, falling back to VIDEO_ONLY",
                            shortPath(localAudioPath),
                            Files.exists(audioFile) ? Files.size(audioFile) : 0);
                    localAudioPath = null; // 降级：视为无音频
                } else {
                    // Step B: 预转码为 AAC（消灭 mp3float Header missing + 格式统一）
                    try {
                        Path aacFile = audioFile.resolveSibling(
                                audioFile.getFileName().toString().replaceAll("\\.[^.]+$", "_clean.aac"));
                        transcodeToAac(audioFile, aacFile);
                        if (Files.exists(aacFile) && Files.size(aacFile) > 1000) {
                            downloadedFiles.add(aacFile); // 注册清理
                            localAudioPath = aacFile.toString();
                            log.info("[AudioSync] Audio pre-transcoded to AAC: {} ({}B)",
                                    shortPath(localAudioPath), Files.size(aacFile));
                        } else {
                            log.warn("[AudioSync] AAC transcoding produced invalid file, using original audio");
                        }
                    } catch (Exception transEx) {
                        log.warn("[AudioSync] AAC pre-transcode failed (non-fatal), using original audio: {}",
                                transEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            cleanupTempFiles(downloadedFiles);
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "音画同步预处理失败（文件下载）: " + e.getMessage());
        }

        // === Phase 1: 时长检测 ===
        VideoComposeService.VideoInfo videoInfo = videoComposeService.getVideoInfo(localVideoPath);
        float videoDur = videoInfo.getDuration() != null ? videoInfo.getDuration() : 0f;

        float audioDur = 0f;
        if (localAudioPath != null && !localAudioPath.isEmpty()) {
            try {
                VideoComposeService.AudioInfo audioInfo = videoComposeService.getAudioInfo(localAudioPath);
                audioDur = audioInfo.getDuration() != null ? audioInfo.getDuration() : 0f;
            } catch (Exception e) {
                log.warn("[AudioSync] Failed to probe audio duration, treating as 0: {}", e.getMessage());
            }
        }

        float diff = Math.abs(videoDur - audioDur);
        log.info("[AudioSync] Duration analysis: video={}s, audio={}s, diff={}s",
                videoDur, audioDur, diff);

        // === Phase 2: 策略选择 ===
        // BUG00037-Fix2: 策略选择修正
        // 旧逻辑：diff ≤ 1s → DIRECT_BLEND(-shortest) — 但如果 audio>video，-shortest会截断音频尾部！
        // 新逻辑：
        //   ① 无音频 → VIDEO_ONLY
        //   ② 音频比视频长(哪怕只长0.01s) → EXTEND_FREEZE（冻结帧延展，不丢任何音频数据）
        //   ③ 视频比音频长且diff≤1s → DIRECT_BLEND（此时-shortest取的是视频长度，音频不会被截）
        //   ④ 视频明显长于音频(diff>1s) → PAD_SILENCE
        String strategy;
        if (localAudioPath == null || localAudioPath.isEmpty() || audioDur <= 0f) {
            strategy = STRATEGY_VIDEO_ONLY;
        } else if (audioDur > videoDur) {
            // 音频比视频长 → 冻结帧延展（绝对不用 -shortest 截断音频！）
            strategy = STRATEGY_EXTEND_FREEZE;
        } else if (diff <= MATCH_THRESHOLD_SECONDS) {
            // 视频更长但差距在阈值内 → 直接混流（-shortest取视频长度，音频完整保留）
            strategy = STRATEGY_DIRECT_BLEND;
        } else {
            strategy = STRATEGY_PAD_SILENCE;
        }

        log.info("[AudioSync] Selected strategy: {}", strategy);

        // === Phase 3: 执行对应 FFmpeg 命令 ===
        try {
            switch (strategy) {
                case STRATEGY_VIDEO_ONLY ->
                    executeVideoOnly(localVideoPath, subtitleAssPath, outputPath);
                case STRATEGY_DIRECT_BLEND ->
                    executeDirectBlend(localVideoPath, localAudioPath, subtitleAssPath, outputPath);
                case STRATEGY_EXTEND_FREEZE ->
                    executeExtendFreeze(localVideoPath, localAudioPath, subtitleAssPath,
                            outputPath, videoDur, audioDur);
                case STRATEGY_PAD_SILENCE ->
                    executePadSilence(localVideoPath, localAudioPath, subtitleAssPath,
                            outputPath, videoDur, audioDur);
                default -> throw new BusinessException(ResultCode.SERVER_ERROR,
                        "Unknown sync strategy: " + strategy);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AudioSync] Strategy {} failed, falling back to simple mix: {}",
                    strategy, e.getMessage());
            // 降级：回退到原始简单合成（使用本地路径）
            fallbackToSimpleMix(localVideoPath, localAudioPath, outputPath);
            strategy = "FALLBACK_SIMPLE";
        } finally {
            // 清理所有预下载的临时文件
            cleanupTempFiles(downloadedFiles);
        }

        // === Phase 4: 收集结果 ===
        SyncResult result = new SyncResult();
        result.setOutputPath(outputPath);
        result.setStrategy(strategy);
        result.setVideoDuration(videoDur);
        result.setAudioDuration(audioDur);
        result.setHasSubtitle(subtitleAssPath != null);

        try {
            VideoComposeService.VideoInfo outInfo = videoComposeService.getVideoInfo(outputPath);
            result.setOutputDuration(outInfo.getDuration() != null ? outInfo.getDuration() : 0f);
        } catch (Exception e) {
            log.warn("[AudioSync] Failed to probe output duration: {}", e.getMessage());
            result.setOutputDuration(Math.max(videoDur, audioDur));
        }

        log.info("[AudioSync] DONE: strategy={}, v={}s, a={}s → out={}s",
                strategy, videoDur, audioDur, result.getOutputDuration());
        return result;
    }

    // ====================================================================
    // 策略实现：VIDEO_ONLY — 纯画面+字幕
    // ====================================================================

    private void executeVideoOnly(String videoPath, String subtitleAssPath, String outputPath) throws IOException {
        List<String> cmd = buildBaseFfmpegCmd();
        cmd.add("-i");
        cmd.add(videoPath);
        // 复制ASS到输出目录，获取纯文件名
        String subFileName = prepareSubtitleForFFmpeg(subtitleAssPath, outputPath);
        if (subFileName != null) {
            cmd.add("-vf");
            cmd.add("subtitles='" + subFileName + "'");
        }
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-an");              // 去除所有音频轨
        cmd.add(outputPath);
        Path workDir = Path.of(outputPath).getParent();
        executeFf(cmd, "VideoOnly", workDir);
    }

    // ====================================================================
    // 策略实现：DIRECT_BLEND — 直接混流（-shortest）
    // ====================================================================

    private void executeDirectBlend(String videoPath, String audioPath,
                                    String subtitleAssPath, String outputPath) throws IOException {
        List<String> cmd = buildBaseFfmpegCmd();
        cmd.add("-i");
        cmd.add(videoPath);
        cmd.add("-i");
        cmd.add(audioPath);
        String subFileName = prepareSubtitleForFFmpeg(subtitleAssPath, outputPath);
        if (subFileName != null) {
            cmd.add("-vf");
            cmd.add("subtitles='" + subFileName + "'");
        }
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-c:a");
        cmd.add("aac");
        // BUG00037-Fix3: 防止音频拼接后产生负时间戳 → concat时时间轴漂移
        cmd.add("-avoid_negative_ts");
        cmd.add("make_zero");
        cmd.add("-shortest");        // 取较短者
        cmd.add("-map");
        cmd.add("0:v:0");            // 显式取视频流
        cmd.add("-map");
        cmd.add("1:a:0");            // 显式取音频流
        cmd.add(outputPath);
        Path workDir = Path.of(outputPath).getParent();
        executeFf(cmd, "DirectBlend", workDir);
    }

    // ====================================================================
    // 策略实现：EXTEND_VIDEO_FREEZE — 视频末帧冻结延展
    //
    // 场景: video=5s, audio=8.3s → 需要把视频延长到8.3s
    // 步骤:
    //   A) 截取视频最后一帧 → last_frame.png
    //   B) 用单帧循环生成延展段 (audioDur - videoDur)秒
    //   C) concat [原视频] + [延展段] → extended_video.mp4
    //   D) 最终混流 extended_video + audio + subtitles
    // ====================================================================

    private void executeExtendFreeze(String videoPath, String audioPath,
                                     String subtitleAssPath, String outputPath,
                                     float videoDur, float audioDur) throws Exception {
        List<Path> tempFiles = new ArrayList<>();
        try {
            float extendDur = audioDur - videoDur;
            log.info("[ExtendFreeze] Extending video by {}s ({}s → {}s)",
                    extendDur, videoDur, audioDur);

            Path tempDir = Files.createTempDirectory("sync_freeze_");

            // Step A: 截取最后一帧
            Path lastFrameFile = tempDir.resolve("last_frame.png");
            tempFiles.add(lastFrameFile);
            extractLastFrame(videoPath, lastFrameFile);

            // Step B: 从末帧生成延展视频段
            Path extendSegmentFile = tempDir.resolve("extend_segment.mp4");
            tempFiles.add(extendSegmentFile);
            createFreezeSegment(lastFrameFile, extendSegmentFile, extendDur);

            // Step C: 拼接原视频 + 延展段
            Path extendedVideoFile = tempDir.resolve("extended_video.mp4");
            tempFiles.add(extendedVideoFile);
            concatTwoVideos(videoPath, extendSegmentFile.toString(), extendedVideoFile.toString());

            // Step D: 最终混流（延展后的视频 + 完整音频 + 字幕）
            List<String> cmd = buildBaseFfmpegCmd();
            cmd.add("-i");
            cmd.add(extendedVideoFile.toString());
            cmd.add("-i");
            cmd.add(audioPath);
            String subFileName = prepareSubtitleForFFmpeg(subtitleAssPath, outputPath);
            if (subFileName != null) {
                cmd.add("-vf");
                cmd.add("subtitles='" + subFileName + "'");
            }
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-c:a");
            cmd.add("aac");
            // BUG00037-Fix3: 防止延展拼接后产生负时间戳 → 整集音画漂移
            cmd.add("-avoid_negative_ts");
            cmd.add("make_zero");
            cmd.add("-map");
            cmd.add("0:v:0");
            cmd.add("-map");
            cmd.add("1:a:0");
            cmd.add(outputPath);
            Path workDir = Path.of(outputPath).getParent();
            executeFf(cmd, "ExtendFreeze-FinalMix", workDir);

            log.info("[ExtendFreeze] Successfully extended and mixed");
        } finally {
            cleanupTempFiles(tempFiles);
        }
    }

    // ====================================================================
    // 策略实现：PAD_AUDIO_SILENCE — 音频补静音
    //
    // 场景: video=10s, audio=2.1s → 需要把音频延长到10s
    // 步骤:
    //   A) 用 lavfi anullsrc 生成静音段 (videoDur - audioDur)秒
    //   B) concat [原音频] + [静音段] → padded_audio.m4a
    //   C) 最终混流 video + padded_audio + subtitles
    // ====================================================================

    private void executePadSilence(String videoPath, String audioPath,
                                   String subtitleAssPath, String outputPath,
                                   float videoDur, float audioDur) throws Exception {
        List<Path> tempFiles = new ArrayList<>();
        try {
            float silenceDur = videoDur - audioDur;
            log.info("[PadSilence] Padding audio with {}s of silence ({}s → {}s)",
                    silenceDur, audioDur, videoDur);

            Path tempDir = Files.createTempDirectory("sync_silence_");

            // Step A: 生成静音音频段
            Path silencePadFile = tempDir.resolve("silence_pad.aac");
            tempFiles.add(silencePadFile);
            createSilencePad(silencePadFile, silenceDur);

            // Step B: 拼接原音频 + 静音段
            Path paddedAudioFile = tempDir.resolve("padded_audio.m4a");
            tempFiles.add(paddedAudioFile);
            concatTwoAudios(audioPath, silencePadFile.toString(), paddedAudioFile.toString());

            // Step C: 最终混流（完整视频 + 补静音后的音频 + 字幕）
            List<String> cmd = buildBaseFfmpegCmd();
            cmd.add("-i");
            cmd.add(videoPath);
            cmd.add("-i");
            cmd.add(paddedAudioFile.toString());
            String subFileName = prepareSubtitleForFFmpeg(subtitleAssPath, outputPath);
            if (subFileName != null) {
                cmd.add("-vf");
                cmd.add("subtitles='" + subFileName + "'");
            }
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-c:a");
            cmd.add("aac");
            // BUG00037-Fix3: 防止concat拼接音频后产生负时间戳 → 整集音画漂移
            cmd.add("-avoid_negative_ts");
            cmd.add("make_zero");
            cmd.add("-map");
            cmd.add("0:v:0");
            cmd.add("-map");
            cmd.add("1:a:0");
            cmd.add(outputPath);
            Path workDir = Path.of(outputPath).getParent();
            executeFf(cmd, "PadSilence-FinalMix", workDir);

            log.info("[PadSilence] Successfully padded and mixed");
        } finally {
            cleanupTempFiles(tempFiles);
        }
    }

    // ====================================================================
    // 辅助方法：FFmpeg 命令构建与执行
    // ====================================================================

    /** 构建基础 FFmpeg 命令前缀（-y 覆盖输出） */
    private List<String> buildBaseFfmpegCmd() {
        List<String> cmd = new ArrayList<>();
        cmd.add(videoComposeService.getFfmpegConfig().getFfmpegPath());
        cmd.add("-y");
        return cmd;
    }

    /** 添加字幕滤镜（如果有 ASS 文件）
     *
     * <p>Windows 下 FFmpeg subtitles 滤镜对绝对路径的解析极其脆弱：
     * 反斜杠转义、盘符冒号被当选项分隔符、路径含特殊字符等都会导致崩溃。
     *
     * <p><b>终极解决方案</b>：不传绝对路径！将 ASS 文件复制到输出文件同目录，
     * 传纯文件名给 FFmpeg。这样字幕路径中不含任何驱动器号、冒号或反斜杠。
     */
    private String prepareSubtitleForFFmpeg(String assPath, String outputPath) throws IOException {
        if (assPath == null || assPath.isEmpty()) return null;

        Path sourceAss = Path.of(assPath);
        if (!Files.exists(sourceAss)) {
            log.warn("[AudioSync] ASS file not found: {}", assPath);
            return null;
        }

        // 将 ASS 复制到输出视频同目录，使用固定名称避免冲突
        Path outputDir = Path.of(outputPath).getParent();
        if (outputDir == null) outputDir = Path.of(".");
        if (!Files.exists(outputDir)) Files.createDirectories(outputDir);

        // 用时间戳避免并发冲突（同一镜头多次合成）
        String simpleName = "sub_" + System.currentTimeMillis() + ".ass";
        Path destAss = outputDir.resolve(simpleName);
        Files.copy(sourceAss, destAss, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        log.debug("[AudioSync] Copied ASS {} -> {} (relative name for FFmpeg)",
                sourceAss.getFileName(), simpleName);
        // 返回纯文件名（不含任何路径分隔符），让 FFmpeg 在 cwd 中查找
        return simpleName;
    }

    /** 执行 FFmpeg 命令并等待完成（无工作目录版本，用于 concat 等纯文件操作）
     * @see #executeFf(List, String, Path)
     */
    private void executeFf(List<String> cmd, String operation) {
        executeFf(cmd, operation, null);
    }

    /** 执行 FFmpeg 命令并等待完成
     *
     * @param cmd FFmpeg 命令参数列表
     * @param operation 操作名称（用于日志）
     * @param workDir 工作目录（FFmpeg 在此目录下查找相对路径的文件，如字幕文件）
     */
    private void executeFf(List<String> cmd, String operation, Path workDir) {
        log.debug("[AudioSync:{}] Executing in dir={}: {}", operation, workDir, String.join(" ", cmd));
        // BUG00037-Fix1: 收集全部stderr输出，用于致命错误检测
        StringBuilder allOutput = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (workDir != null && Files.exists(workDir)) {
                pb.directory(workDir.toFile());
            }
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // 读取输出日志
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[FFmpeg:{}]", line);
                allOutput.append(line).append("\n");
            }

            int code = videoComposeService.waitForWithTimeout(p, 5, operation);

            // BUG00037-R10-Fix2: 致命错误检测策略调整
            //
            // R9(上一轮): exitCode=0 + stderr含 "Header missing" → 直接抛异常
            // 问题: MiniMax TTS 的 MP3 缺少 ID3 头，mp3float 报 warning 但实际能成功解码出有效音频！
            // 结果: 5/6 镜头全走 FAILED_FALLBACK，实际上 FFmpeg 产出了有效音频（audio:57KiB time=4.08s）
            //
            // R10(本轮) 新策略: 分级处理
            //   - exitCode != 0 → 仍然是硬错误，直接抛异常
            //   - exitCode=0 + 含警告关键词 → 记录 WARN 日志，但不抛异常（让下游继续用输出文件）
            //   - 真正的兜底：Phase 0.5 已预转码为 WAV，这些警告应该不会再出现
            if (code == 0 && containsFatalFFmpegError(allOutput.toString())) {
                log.warn("[AudioSync:{}] FFmpeg completed (code=0) but with decode warnings:\n{}" +
                        "\n→ Output file may still be usable. Continuing.",
                        operation, truncateOutput(extractErrorLines(allOutput.toString()), 500));
                // 不再抛异常！只记录警告。如果输出真的有问题（空音频），会在后续 probe 时发现。
            }

            if (code != 0) {
                throw new BusinessException(ResultCode.SERVER_ERROR,
                        "FFmpeg " + operation + " 失败 (退出码: " + code + ")");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "FFmpeg " + operation + " 执行异常: " + e.getMessage());
        }
    }

    /**
     * 检测 FFmpeg 输出中是否包含致命错误关键词。
     *
     * <p>场景：某些情况下 FFmpeg exitCode=0 但实际解码/编码过程中遇到了无法恢复的错误，
     * 输出的文件不完整（如缺少音频轨、视频帧损坏等）。
     *
     * <p>检测的关键词（大小写不敏感）：
     * <ul>
     *   <li>{@code Header missing} — 解码器找不到有效帧头（音频格式错误/文件损坏）</li>
     *   <li>{@code Invalid data found} — 输入数据无效</li>
     *   <li>{@code Error submitting packet} — 解码器拒绝数据包</li>
     * </ul>
     */
    private boolean containsFatalFFmpegError(String output) {
        if (output == null || output.isEmpty()) return false;
        String lower = output.toLowerCase();
        return lower.contains("header missing")
                || lower.contains("invalid data found when processing input")
                || lower.contains("error submitting packet to decoder");
    }

    /** 截断过长输出（日志用） */
    private String truncateOutput(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "\n... [truncated, total " + s.length() + " chars]";
    }

    /** 只提取包含错误关键词的行（用于 WARN 日志，减少噪音） */
    private String extractErrorLines(String output) {
        if (output == null || output.isEmpty()) return "(empty)";
        StringBuilder sb = new StringBuilder();
        for (String line : output.split("\n")) {
            String lower = line.toLowerCase();
            if (lower.contains("error") || lower.contains("warning")
                    || lower.contains("header missing") || lower.contains("invalid data")) {
                sb.append(line.trim()).append("\n");
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? "(no error lines found)" : result;
    }

    // ====================================================================
    // 辅助方法：视频操作（截帧/拼接）
    // ====================================================================

    /**
     * 截取视频最后一帧为 PNG 图片
     * 使用 -sseof -0.5 定位到倒数 0.5 秒处，提取1帧
     */
    private void extractLastFrame(String videoPath, Path outputFrame) throws Exception {
        List<String> cmd = buildBaseFfmpegCmd();
        cmd.add("-sseof");
        cmd.add("-0.5");                    // 从结尾往前 0.5 秒
        cmd.add("-i");
        cmd.add(videoPath);
        cmd.add("-frames:v");
        cmd.add("1");                       // 只取1帧
        cmd.add("-update");
        cmd.add("1");                       // 覆盖已存在文件
        cmd.add(outputFrame.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // 消费输出防止阻塞
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (reader.readLine() != null) { /* drain */ }
        }

        boolean finished = p.waitFor(30, TimeUnit.SECONDS);
        if (!finished) p.destroyForcibly();

        if (!Files.exists(outputFrame) || Files.size(outputFrame) == 0) {
            // 备选方案：从开头取第一帧
            log.warn("[ExtractFrame] Last frame extraction failed, trying first frame");
            List<String> cmd2 = buildBaseFfmpegCmd();
            cmd2.add("-i");
            cmd2.add(videoPath);
            cmd2.add("-frames:v");
            cmd2.add("1");
            cmd2.add(outputFrame.toString());

            ProcessBuilder pb2 = new ProcessBuilder(cmd2);
            pb2.redirectErrorStream(true);
            Process p2 = pb2.start();
            try (BufferedReader r2 = new BufferedReader(new InputStreamReader(p2.getInputStream()))) {
                while (r2.readLine() != null) { /* drain */ }
            }
            p2.waitFor(30, TimeUnit.SECONDS);
        }

        log.debug("[ExtractFrame] Extracted frame: {} ({} bytes)",
                outputFrame, Files.exists(outputFrame) ? Files.size(outputFrame) : 0);
    }

    /**
     * 从单张静态图片创建指定时长的视频片段（用于冻结帧延展）
     *
     * @param imageFile   输入图片
     * @param outputFile  输出视频
     * @param durationSec 目标时长（秒）
     */
    private void createFreezeSegment(Path imageFile, Path outputFile, float durationSec) throws Exception {
        List<String> cmd = buildBaseFfmpegCmd();
        cmd.add("-loop");
        cmd.add("1");                           // 循环输入图片
        cmd.add("-t");
        cmd.add(String.format("%.3f", durationSec)); // 精确到毫秒
        cmd.add("-i");
        cmd.add(imageFile.toString());
        cmd.add("-vf");
        cmd.add("fps=24,format=yuv420p");        // 24fps + 兼容格式
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("fast");                         // 编码速度
        cmd.add("-crf");
        cmd.add(String.valueOf(FREEZE_CRF));     // 高质量
        cmd.add("-pix_fmt");
        cmd.add("yuv420p");                      // 确保兼容性
        cmd.add(outputFile.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[CreateFreeze:{}", line);
            }
        }

        boolean finished = p.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new BusinessException(ResultCode.SERVER_ERROR, "冻结帧视频生成超时(60s)");
        }
        if (p.exitValue() != 0) {
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "冻结帧视频生成失败 (退出码: " + p.exitValue() + ")");
        }
    }

    /**
     * 拼接两个视频文件（使用 concat demuxer + copy 流）
     */
    private void concatTwoVideos(String video1, String video2, String outputPath) throws Exception {
        Path listFile = Files.createTempFile("concat_list", ".txt");
        Files.writeString(listFile,
                "file '" + video1.replace("\\", "/") + "'\n" +
                "file '" + video2.replace("\\", "/") + "'\n");

        try {
            List<String> cmd = buildBaseFfmpegCmd();
            cmd.add("-f");
            cmd.add("concat");
            cmd.add("-safe");
            cmd.add("0");
            cmd.add("-i");
            cmd.add(listFile.toString());
            cmd.add("-c");
            cmd.add("copy");                   // 流拷贝，快速
            cmd.add(outputPath);

            executeFf(cmd, "ConcatVideos");
        } finally {
            try { Files.deleteIfExists(listFile); } catch (Exception ignored) {}
        }
    }

    // ====================================================================
    // 辅助方法：音频操作（静音/拼接）
    // ====================================================================

    /**
     * 生成指定时长的静音 AAC 音频文件
     */
    private void createSilencePad(Path outputFile, float durationSec) throws Exception {
        List<String> cmd = buildBaseFfmpegCmd();
        cmd.add("-f");
        cmd.add("lavfi");                       // libavfilter 虚拟输入
        cmd.add("-i");
        cmd.add("anullsrc=r=44100:cl=stereo");  // 44.1kHz 双声道静音源
        cmd.add("-t");
        cmd.add(String.format("%.3f", durationSec));
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add(SILENCE_BITRATE);
        cmd.add(outputFile.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (reader.readLine() != null) { /* drain */ }
        }

        boolean finished = p.waitFor(15, TimeUnit.SECONDS);
        if (!finished) p.destroyForcibly();
        if (p.exitValue() != 0) {
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "静音音频生成失败 (退出码: " + p.exitValue() + ")");
        }
    }

    /**
     * 拼接两个音频文件
     *
     * <p>注意：concat demuxer 不支持 HTTPS 协议白名单（仅 file,crypto,data），
     * 因此远程音频必须先下载到本地临时文件再拼接。
     */
    private void concatTwoAudios(String audio1, String audio2, String outputPath) throws Exception {
        Path listFile = Files.createTempFile("concat_audio", ".txt");

        // 如果是远程URL，先下载到本地（concat demuxer 不支持 https:// 协议）
        List<Path> downloaded = new ArrayList<>();
        try {
            String localAudio1 = resolveLocalPath(audio1, downloaded);
            String localAudio2 = resolveLocalPath(audio2, downloaded);

            Files.writeString(listFile,
                    "file '" + localAudio1 + "'\n" +
                    "file '" + localAudio2 + "'\n");

            try {
                List<String> cmd = buildBaseFfmpegCmd();
                cmd.add("-f");
                cmd.add("concat");
                cmd.add("-safe");
                cmd.add("0");
                cmd.add("-i");
                cmd.add(listFile.toString());
                cmd.add("-c:a");
                cmd.add("aac");
                cmd.add("-b:a");
                cmd.add(SILENCE_BITRATE);
                cmd.add(outputPath);

                executeFf(cmd, "ConcatAudios");
            } finally {
                try { Files.deleteIfExists(listFile); } catch (Exception ignored) {}
            }
        } finally {
            cleanupTempFiles(downloaded);
        }
    }

    /**
     * 解析音频路径为本地路径：远程URL则先下载到临时文件
     */
    private String resolveLocalPath(String audioPath, List<Path> tempFiles) throws Exception {
        if (audioPath != null && (audioPath.startsWith("http://") || audioPath.startsWith("https://"))) {
            log.debug("[AudioSync] Downloading remote audio for concat: {}", shortPath(audioPath));
            Path tempFile = videoComposeService.downloadVideoToTemp(audioPath);
            tempFiles.add(tempFile);
            return tempFile.toString().replace("\\", "/");
        }
        return audioPath.replace("\\", "/");
    }

    // ====================================================================
    // 降级回退
    // ====================================================================

    /**
     * 回退到简单的 composeShot（无对齐逻辑，保证至少能出片）
     */
    private void fallbackToSimpleMix(String videoPath, String audioPath, String outputPath) {
        log.warn("[AudioSync] Falling back to simple composeShot for: {}", shortPath(outputPath));
        videoComposeService.composeShot(videoPath, audioPath, null, outputPath);
    }

    // ====================================================================
    // 工具方法
    // ====================================================================

    /** 截断显示路径（日志用） */
    private String shortPath(String path) {
        if (path == null) return "null";
        if (path.length() > 80) {
            return path.substring(0, 77) + "...";
        }
        return path;
    }

    /**
     * 校验下载的音频文件是否包含有效的音频数据。
     *
     * <p>防御场景：OSS 临时 URL 过期 → RestTemplate 下载到 HTML 错误页 / 空内容 /
     * 重定向目标丢失 → FFmpeg 拿到假文件报 "mp3float Header missing"。
     *
     * <p>校验规则：
     * <ol>
     *   <li>文件必须存在且 ≥ 256 字节（最小有效音频帧）</li>
     *   <li>文件头魔数必须是已知音频格式（MP3/AAC/M4A/OGG/WAV/FLAC）</li>
     * </ol>
     *
     * @param audioFile 音频文件路径
     * @return true 表示文件包含有效音频数据，false 表示无效（应降级为 VIDEO_ONLY）
     */
    private boolean isValidAudioFile(Path audioFile) {
        try {
            // 规则1: 文件存在且最小体积
            if (!Files.exists(audioFile) || Files.size(audioFile) < 256) {
                log.debug("[AudioValidate] File too small or missing: {} bytes",
                        Files.exists(audioFile) ? Files.size(audioFile) : 0);
                return false;
            }

            // 规则2: 魔数检测（读取前12字节）
            byte[] header = new byte[12];
            try (var is = java.nio.file.Files.newInputStream(audioFile)) {
                int read = is.read(header);
                if (read < 3) return false; // 至少3字节才能判断格式
            }

            // MP3: ID3v2 标签 (0x49 0x44 0x33) 或 MPEG 帧同步 (0xFF 0xFB/0xF3/0xF2)
            if ((header[0] == 0x49 && header[1] == 0x44 && header[2] == 0x33)
                    || (header[0] == (byte)0xFF && (header[1] & 0xE0) == 0xE0)) {
                log.debug("[AudioValidate] Detected MP3 format");
                return true;
            }

            // AAC / M4A: ftyp box (66 74 79 70)
            if (header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
                log.debug("[AudioValidate] Detected AAC/M4A format (ftyp)");
                return true;
            }

            // OGG: "OggS" (4F 67 67 53)
            if (header[0] == 0x4F && header[1] == 0x67 && header[2] == 0x67 && header[3] == 0x53) {
                log.debug("[AudioValidate] Detected OGG format");
                return true;
            }

            // WAV: RIFF (52 49 46 46) + WAVE (57 41 56 45)
            if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                    && header[8] == 0x57 && header[9] == 0x41 && header[10] == 0x56 && header[11] == 0x45) {
                log.debug("[AudioValidate] Detected WAV format");
                return true;
            }

            // FLAC: fLaC (66 4C 61 43)
            if (header[0] == 0x66 && header[1] == 0x4C && header[2] == 0x61 && header[3] == 0x43) {
                log.debug("[AudioValidate] Detected FLAC format");
                return true;
            }

            // WebM: EBML (1A 45 DF A3)
            if (header[0] == 0x1A && header[1] == 0x45 && header[2] == (byte)0xDF && header[3] == 0xA3) {
                log.debug("[AudioValidate] Detected WebM format");
                return true;
            }

            // 未识别的格式 → 打印前16字节帮助诊断
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < Math.min(16, header.length); i++) {
                hex.append(String.format("%02X ", header[i]));
            }
            log.warn("[AudioValidate] UNKNOWN audio format! Magic hex: {} file={}", hex, audioFile.getFileName());
            return false;

        } catch (Exception e) {
            log.error("[AudioValidate] Error checking audio file {}: {}", audioFile, e.getMessage());
            return false; // 校验异常时也视为无效，安全降级
        }
    }

    /**
     * BUG00037-R10-Fix1b: 将音频文件预转码为 AAC 格式。
     *
     * <p>MiniMax TTS 生成的 MP3 文件缺少标准 ID3 帧头，FFmpeg mp3float 解码器会报
     * "Header missing" warning。此方法在策略执行前将音频转为干净 AAC 格式，彻底消除兼容性问题。
     *
     * <p>转码参数（与 {@link #createSilencePad} 格式一致，确保 concat 兼容）：
     * <ul>
     *   <li>-ar 44100: 采样率44.1kHz（和静音段一致）</li>
     *   <li>-ac 2: 立体声（和静音段一致）</li>
     *   <li>-c:a aac -b:a 128k: AAC编码128kbps</li>
     * </ul>
     *
     * @param source 源音频文件（MP3/AAC等任意格式）
     * @param target 输出的AAC文件路径
     */
    private void transcodeToAac(Path source, Path target) {
        List<String> cmd = buildBaseFfmpegCmd();
        cmd.add("-v");
        cmd.add("error");
        cmd.add("-i");
        cmd.add(source.toString());
        cmd.add("-y");
        cmd.add("-ar");
        cmd.add("44100");                       // 和 createSilencePad 一致
        cmd.add("-ac");
        cmd.add("2");                            // 立体声，和静音段一致
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add(SILENCE_BITRATE);                // 128k，和静音段一致
        cmd.add(target.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        try {
            log.debug("[AudioSync:Transcode] {} → AAC ({}B source)",
                    source.getFileName(), Files.exists(source) ? Files.size(source) : 0);

            Process p = pb.start();
            int code = videoComposeService.waitForWithTimeout(p, 10, "TranscodeToAac");
            if (code != 0) {
                throw new RuntimeException("Transcode exit code: " + code);
            }
            long outSize = Files.exists(target) ? Files.size(target) : 0;
            log.info("[AudioSync:Transcode] Done → {} ({}B)", target.getFileName(), outSize);
        } catch (Exception e) {
            throw new RuntimeException("Audio transcoding to AAC failed: " + e.getMessage(), e);
        }
    }


    /** 清理临时文件 */
    private void cleanupTempFiles(List<Path> files) {
        for (Path f : files) {
            try {
                if (Files.exists(f)) Files.delete(f);
            } catch (Exception ignored) {}
        }
        // 尝试删除临时目录本身
        if (!files.isEmpty()) {
            try {
                Path parent = files.get(0).getParent();
                if (parent != null && Files.exists(parent)) Files.delete(parent);
            } catch (Exception ignored) {}
        }
    }
}
