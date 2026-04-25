package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.config.FileConfig;
import com.drama.entity.Audio;
import com.drama.entity.AiConfig;
import com.drama.entity.Asset;
import com.drama.entity.Character;
import com.drama.service.adapter.AiApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.net.URI;
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
    private final CharacterService characterService;
    private final AiConfigService aiConfigService;
    private final FileStorageService fileStorageService;
    private final AssetService assetService;

    /** 注入由 RestTemplateConfig 创建的 Bean（支持代理/DNS/超时配置） */
    private final RestTemplate restTemplate;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 生成配音并保存记录（音频文件持久化到磁盘）
     * 核心改进：自动从 Character 获取音色，从 AiConfig 获取模型
     */
    @Transactional
    public Audio generate(String dramaId, int episodeNumber, String storyboardId, String characterId,
                      String text, String provider, String voiceId, String model) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "配音文本不能为空");
        }

        // ====== 自动解析：优先使用传入参数，否则从角色/AI配置动态获取 ======
        String actualProvider = provider != null ? provider : "minimax";

        // 1. 如果没有指定 voiceId，尝试通过 characterId 查找角色的专属音色
        String actualVoice = voiceId;
        if ((actualVoice == null || actualVoice.isEmpty()) && characterId != null && !characterId.isEmpty()) {
            try {
                Character ch = characterService.getById(characterId);
                if (ch != null && ch.getVoiceId() != null && !ch.getVoiceId().isEmpty()) {
                    actualVoice = ch.getVoiceId();
                    log.info("Resolved voice from character {}: voiceId={}", characterId, actualVoice);
                }
            } catch (Exception e) {
                log.warn("Failed to resolve voice from character {}: {}", characterId, e.getMessage());
            }
        }
        if (actualVoice == null || actualVoice.isEmpty()) {
            actualVoice = "female-tianmei";
        }

        // 2. 如果没有指定 model，从 AI 配置的 TTS 类型中获取第一个可用模型
        String actualModel = model;
        if (actualModel == null || actualModel.isEmpty()) {
            try {
                List<AiConfig> ttsConfigs = aiConfigService.listByType("tts");
                if (!ttsConfigs.isEmpty() && ttsConfigs.get(0).getModel() != null && !ttsConfigs.get(0).getModel().isEmpty()) {
                    actualModel = ttsConfigs.get(0).getModel();
                    log.info("Resolved TTS model from AiConfig: model={}", actualModel);
                } else {
                    actualModel = "speech-02-hd";
                }
            } catch (Exception e) {
                log.warn("Failed to resolve TTS model from AiConfig, using default: {}", e.getMessage());
                actualModel = "speech-02-hd";
            }
        }

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
        } catch (AiApiException e) {
            log.error("AI API error (TTS): [code={}] {}", e.getVendorCode(), e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("TTS generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "配音生成失败: " + e.getMessage());
        }
    }

    /**
     * 将音频数据保存到文件系统或归档到OSS
     *
     * 支持格式：
     *   - "http://..." / "https://" → 下载音频 → 通过 FileStorageService 上传到 OSS（永久存储）
     *   - "audio:mp3:base64,{data}" → Base64解码 → 写本地文件
     *   - "audio:mp3:hex,{data}"  → Hex解码 → 写本地文件
     */
    private String saveAudioFile(String audioResult, String dramaId, String provider) {
        // ====== 情况1：直接是URL（MiniMax output_format=url 返回的临时链接）======
        // 需要下载并归档到用户配置的OSS，避免24小时后过期
        if (audioResult.startsWith("http://") || audioResult.startsWith("https://")) {
            return archiveAudioFromUrl(audioResult, dramaId, provider);
        }

        // ====== 情况2：Base64/Hex编码数据 ======
        if (audioResult.startsWith("audio:")) {
            try {
                // 解析格式: audio:{format}:{encoding},{data}
                String[] parts = audioResult.split(":", 3);
                String format = parts.length >= 2 ? parts[1] : "mp3";
                String dataPart = parts.length >= 3 ? parts[2] : "";

                byte[] audioBytes;
                if (dataPart.startsWith("hex,")) {
                    // Hex 编码（MiniMax output_format=hex）
                    String hexData = dataPart.substring(4);
                    audioBytes = hexStringToByteArray(hexData);
                    log.info("Decoded hex audio: {} bytes", audioBytes.length);
                } else if (dataPart.startsWith("base64,")) {
                    // Base64 编码
                    String base64Data = dataPart.substring(7);
                    audioBytes = Base64.getDecoder().decode(base64Data);
                    log.info("Decoded base64 audio: {} bytes", audioBytes.length);
                } else {
                    log.warn("Unknown audio encoding format, trying base64 fallback");
                    audioBytes = Base64.getDecoder().decode(dataPart);
                }

                // 尝试通过 FileStorageService 归档到 OSS（如果配置了的话）
                try {
                    String filename = "tts_" + System.currentTimeMillis() + "." + format;
                    Asset archived = fileStorageService.uploadBytes(
                            audioBytes, filename, dramaId, "audio", "audio/" + format);
                    archived.setSourceType("ai_archived");
                    archived.setExtraData("{\"provider\":\"" + provider + "\",\"type\":\"tts\"}");
                    assetService.updateById(archived);
                    log.info("[TTS-Archive] Audio archived to storage: id={}, url={}",
                            archived.getId(), archived.getFileUrl());
                    return archived.getFileUrl();
                } catch (Exception archiveEx) {
                    log.warn("[TTS-Archive] OSS upload failed, falling back to local: {}", archiveEx.getMessage());
                    // 降级：存本地文件系统
                    return saveAudioToLocal(audioBytes, dramaId, format);
                }

            } catch (IllegalArgumentException e) {
                log.warn("Invalid audio data, falling back to URL marker: {}", e.getMessage());
                return "generated:" + provider + ":" + System.currentTimeMillis();
            } catch (Exception e) {
                log.error("Failed to save audio file: {}", e.getMessage(), e);
                throw new BusinessException(ResultCode.SERVER_ERROR, "音频文件保存失败: " + e.getMessage());
            }
        }

        // 兜底：原样返回
        return audioResult;
    }

    /**
     * 从URL下载音频并归档到OSS/本地存储
     */
    private String archiveAudioFromUrl(String tempUrl, String dramaId, String provider) {
        try {
            log.info("[TTS-Archive] Downloading audio from MiniMax临时URL: {}", tempUrl);

            // 使用 URI 处理带签名的 URL，避免 RestTemplate 二次编码导致签名不匹配
            URI uri = URI.create(tempUrl);
            ResponseEntity<byte[]> response = restTemplate.getForEntity(uri, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[TTS-Archive] Failed to download audio, status={}, using original URL",
                        response.getStatusCode());
                return tempUrl; // 降级：返回原始URL（可能会过期）
            }

            byte[] audioData = response.getBody();
            log.info("[TTS-Archive] Downloaded {} bytes from MiniMax", audioData.length);

            // 通过 FileStorageService 上传到配置的存储位置（自动处理 OSS 或本地）
            String filename = "tts_" + System.currentTimeMillis() + ".mp3";
            Asset archived = fileStorageService.uploadBytes(
                    audioData, filename, dramaId, "audio", "audio/mpeg");

            // 补充元数据
            archived.setSourceType("ai_archived");
            archived.setExtraData("{\"provider\":\"" + provider + "\",\"type\":\"tts\",\"sourceUrl\":\"" + tempUrl + "\"}");
            assetService.updateById(archived);

            log.info("[TTS-Archive] Audio archived successfully: id={}, url={}, source={}",
                    archived.getId(), archived.getFileUrl(),
                    archived.getSourceType() != null ? archived.getSourceType() : "unknown");

            return archived.getFileUrl();

        } catch (Exception e) {
            log.error("[TTS-Archive] Failed to archive audio from URL, returning original: {}", e.getMessage());
            // 降级：返回原始临时URL（用户仍可在24小时内播放）
            return tempUrl;
        }
    }

    /**
     * 将音频数据保存到本地文件系统（降级方案）
     */
    private String saveAudioToLocal(byte[] audioBytes, String dramaId, String format) {
        try {
            String datePath = LocalDateTime.now().format(DATE_FORMAT);
            String filename = "tts_" + System.currentTimeMillis() + "." + format;
            String relativePath = "audios/" + datePath;

            Path dirPath = Paths.get(fileConfig.getPath(), relativePath);
            Files.createDirectories(dirPath);

            Path fullPath = dirPath.resolve(filename);
            try (FileOutputStream fos = new FileOutputStream(fullPath.toFile())) {
                fos.write(audioBytes);
            }

            log.info("Saved audio to local filesystem: {} ({} bytes)", fullPath, audioBytes.length);
            return fileConfig.getUrlPrefix() + "/" + relativePath + "/" + filename;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "本地文件保存失败: " + e.getMessage());
        }
    }

    /**
     * Hex字符串转byte数组
     */
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((java.lang.Character.digit(hex.charAt(i), 16) << 4)
                    + java.lang.Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * TTS 预览（保存到文件系统并更新角色预览音频URL）
     * 用于角色配置页面试听音色效果
     */
    @Transactional
    public String preview(String text, String voiceId, String model, String characterId) {
        // 复用 generate 的解析逻辑，但不保存记录
        String actualVoice = voiceId;
        String characterName = null;
        String dramaId = null;
        if (characterId != null && !characterId.isEmpty()) {
            try {
                Character ch = characterService.getById(characterId);
                if (ch != null) {
                    characterName = ch.getName();
                    dramaId = ch.getDramaId();
                    if ((actualVoice == null || actualVoice.isEmpty()) && ch.getVoiceId() != null && !ch.getVoiceId().isEmpty()) {
                        actualVoice = ch.getVoiceId();
                    }
                }
            } catch (Exception ignored) {}
        }
        if (actualVoice == null || actualVoice.isEmpty()) {
            actualVoice = "female-tianmei";
        }

        // 如果文本为空，使用默认文本
        if (text == null || text.isEmpty()) {
            if (characterName != null && !characterName.isEmpty()) {
                text = "你好，我是" + characterName;
            } else {
                text = "你好，我是你的专属配音";
            }
        }

        String actualModel = model;
        if (actualModel == null || actualModel.isEmpty()) {
            try {
                List<AiConfig> ttsConfigs = aiConfigService.listByType("tts");
                if (!ttsConfigs.isEmpty() && ttsConfigs.get(0).getModel() != null && !ttsConfigs.get(0).getModel().isEmpty()) {
                    actualModel = ttsConfigs.get(0).getModel();
                } else {
                    actualModel = "speech-02-hd";
                }
            } catch (Exception ignored) {
                actualModel = "speech-02-hd";
            }
        }

        log.info("TTS preview: model={}, voice={}, textLength={}", actualModel, actualVoice, text.length());

        try {
            String audioResult = aiServiceFactory.generateTTS("minimax", text, actualVoice, actualModel);
            if (audioResult == null || audioResult.isEmpty()) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "TTS返回为空");
            }
            
            // 保存音频到文件系统/OSS
            String audioUrl = saveAudioFile(audioResult, dramaId != null ? dramaId : "preview", "minimax");
            
            // 更新角色的预览音频URL
            if (characterId != null && !characterId.isEmpty()) {
                Character updateChar = new Character();
                updateChar.setId(characterId);
                updateChar.setPreviewAudioUrl(audioUrl);
                characterService.updateById(updateChar);
                log.info("Updated character preview audio: characterId={}, url={}", characterId, audioUrl);
            }
            
            return audioUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (AiApiException e) {
            log.error("AI API error (TTS preview): [code={}] {}", e.getVendorCode(), e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("TTS preview failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "试听失败: " + e.getMessage());
        }
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
