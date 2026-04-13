package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.config.FileConfig;
import com.drama.entity.Audio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * TTS配音服务 - 支持Base64音频持久化到文件系统
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    private final AiServiceFactory aiServiceFactory;
    private final AudioService audioService;
    private final FileConfig fileConfig;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 生成配音并保存记录（音频文件持久化到磁盘）
     */
    @Transactional
    public Audio generate(String dramaId, int episodeNumber, String storyboardId, String characterId,
                      String text, String provider, String voiceId, String model) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "配音文本不能为空");
        }

        String actualProvider = provider != null ? provider : "minimax";
        String actualModel = model != null ? model : "speech-02-hd";
        String actualVoice = voiceId != null ? voiceId : "female-tianmei";

        log.info("Generating TTS: provider={}, model={}, voice={}, textLength={}",
                actualProvider, actualModel, actualVoice, text.length());

        try {
            // 调用AI生成配音
            String audioResult = aiServiceFactory.generateTTS(actualProvider, text, actualVoice, actualModel);

            if (audioResult == null || audioResult.isEmpty()) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "TTS返回为空");
            }

            // 解析音频结果，保存到文件系统
            String audioUrl = saveAudioFile(audioResult, dramaId, actualProvider);

            // 保存音频记录到数据库
            Audio audio = new Audio();
            audio.setId(IdUtils.randomId());
            audio.setDramaId(dramaId);
            audio.setEpisodeNumber(episodeNumber);
            audio.setStoryboardId(storyboardId);
            audio.setCharacterId(characterId);
            audio.setText(text);
            audio.setAudioUrl(audioUrl);
            audio.setProvider(actualProvider);
            audio.setVoiceId(actualVoice);
            audio.setStatus("completed");
            audio.setCreatedAt(LocalDateTime.now());
            audio.setDeleted(0);

            audioService.save(audio);
            log.info("Generated TTS: {} for storyboard {}", audio.getId(), storyboardId);

            return audio;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("TTS generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "配音生成失败: " + e.getMessage());
        }
    }

    /**
     * 将音频数据保存到文件系统
     * 支持格式：
     *   - "audio:mp3:base64,{data}" → Base64解码后写文件
     *   - "http://..." / "https://" → 直接作为URL存储
     */
    private String saveAudioFile(String audioResult, String dramaId, String provider) {
        // Base64格式：audio:mp3:base64,{base64data}
        if (audioResult.startsWith("audio:")) {
            try {
                // 解析格式: audio:{format}:base64,{data}
                String[] parts = audioResult.split(":", 3);
                String format = parts.length >= 2 ? parts[1] : "mp3";
                String base64Data = parts.length >= 3 && parts[2].startsWith("base64,")
                        ? parts[2].substring(7)
                        : audioResult.substring(audioResult.indexOf(",") + 1);

                byte[] audioBytes = Base64.getDecoder().decode(base64Data);
                log.info("Decoded base64 audio: {} bytes", audioBytes.length);

                // 构建存储路径
                String datePath = LocalDateTime.now().format(DATE_FORMAT);
                String filename = "tts_" + System.currentTimeMillis() + "." + format;
                String relativePath = "audios/" + datePath;

                Path dirPath = Paths.get(fileConfig.getPath(), relativePath);
                Files.createDirectories(dirPath);

                Path fullPath = dirPath.resolve(filename);
                try (FileOutputStream fos = new FileOutputStream(fullPath.toFile())) {
                    fos.write(audioBytes);
                }

                log.info("Saved audio file: {} ({} bytes)", fullPath, audioBytes.length);
                return fileConfig.getUrlPrefix() + "/" + relativePath + "/" + filename;

            } catch (IllegalArgumentException e) {
                log.warn("Invalid base64 audio data, falling back to URL marker: {}", e.getMessage());
                return "generated:" + provider + ":" + System.currentTimeMillis();
            } catch (Exception e) {
                log.error("Failed to save audio file: {}", e.getMessage(), e);
                throw new BusinessException(ResultCode.SERVER_ERROR, "音频文件保存失败: " + e.getMessage());
            }
        }

        // 直接是URL的情况
        return audioResult;
    }

    /**
     * 批量生成角色配音
     */
    @Transactional
    public List<Audio> batchGenerate(String dramaId, int episodeNumber,
                                       List<Map<String, String>> dialogues) {
        List<Audio> audios = new ArrayList<>();
        
        for (Map<String, String> dialog : dialogues) {
            Audio audio = generate(
                dramaId,
                episodeNumber,
                dialog.getOrDefault("storyboardId", ""),
                dialog.getOrDefault("characterId", ""),
                dialog.getOrDefault("text", ""),
                dialog.getOrDefault("provider", null),
                dialog.getOrDefault("voiceId", null),
                dialog.getOrDefault("model", null)
            );
            audios.add(audio);
        }
        
        return audios;
    }
}
