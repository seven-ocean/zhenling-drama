package com.drama.service.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MiniMax 适配器
 */
@Slf4j
@Component
public class MiniMaxAdapter implements AiAdapter {

    private static final String PROVIDER = "minimax";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public String generateText(String prompt, String model) {
        log.info("MiniMax text generation: model={}", model);
        return "";
    }

    @Override
    public String generateImage(String prompt, String model) {
        log.info("MiniMax image generation: model={}", model);
        return "";
    }

    @Override
    public String generateVideo(String prompt, String model) {
        log.info("MiniMax video generation: model={}", model);
        return "";
    }

    @Override
    public String generateTTS(String text, String voiceId, String model) {
        log.info("MiniMax TTS: model={}, voice={}", model, voiceId);
        return "";
    }

    @Override
    public boolean checkHealth() {
        return true;
    }
}