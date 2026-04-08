package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Audio;
import com.drama.service.adapter.AiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * TTS配音服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    private final AiServiceFactory aiServiceFactory;
    private final AudioService audioService;

    /**
     * 生成配音
     */
    @Transactional
    public Audio generate(String dramaId, int episodeNumber, String storyboardId, String characterId, 
                      String text, String provider, String voiceId, String model) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "配音文本不能为空");
        }

        String actualProvider = provider != null ? provider : "minimax";
        String actualModel = model != null ? model : "speech-01";
        String actualVoice = voiceId != null ? voiceId : "male-qn-qingse";

        log.info("Generating TTS: provider={}, model={}, voice={}", actualProvider, actualModel, actualVoice);

        try {
            // 调用AI生成配音
            String audioUrl = aiServiceFactory.generateTTS(actualProvider, text, actualVoice, actualModel);

            // 保存音频记录
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

            return audio;
        } catch (Exception e) {
            log.error("TTS generation failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "配音生成失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成角色配音
     */
    @Transactional
    public java.util.List<Audio> batchGenerate(String dramaId, int episodeNumber, 
                                       java.util.List<java.util.Map<String, String>> dialogues) {
        java.util.List<Audio> audios = new java.util.ArrayList<>();
        
        for (java.util.Map<String, String> dialog : dialogues) {
            Audio audio = generate(
                dramaId,
                episodeNumber,
                dialog.get("storyboardId"),
                dialog.get("characterId"),
                dialog.get("text"),
                dialog.get("provider"),
                dialog.get("voiceId"),
                dialog.get("model")
            );
            audios.add(audio);
        }
        
        return audios;
    }
}