package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.ResultCode;
import com.drama.entity.AiConfig;
import com.drama.service.adapter.AiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI服务工厂
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceFactory {

    private final Map<String, AiAdapter> adapters = new ConcurrentHashMap<>();

    public AiServiceFactory(List<AiAdapter> adapterList) {
        for (AiAdapter adapter : adapterList) {
            adapters.put(adapter.getProvider().toLowerCase(), adapter);
        }
    }

    /**
     * 获取适配器
     */
    public AiAdapter getAdapter(String provider) {
        AiAdapter adapter = adapters.get(provider.toLowerCase());
        if (adapter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "不支持的 AI 厂商: " + provider);
        }
        return adapter;
    }

    /**
     * 文本生成
     */
    public String generateText(String provider, String prompt, String model) {
        return getAdapter(provider).generateText(prompt, model);
    }

    /**
     * 图片生成
     */
    public String generateImage(String provider, String prompt, String model) {
        return getAdapter(provider).generateImage(prompt, model);
    }

    /**
     * 视频生成
     */
    public String generateVideo(String provider, String prompt, String model) {
        return getAdapter(provider).generateVideo(prompt, model);
    }

    /**
     * TTS
     */
    public String generateTTS(String provider, String text, String voiceId, String model) {
        return getAdapter(provider).generateTTS(text, voiceId, model);
    }

    /**
     * 健康检查
     */
    public boolean checkHealth(String provider) {
        try {
            return getAdapter(provider).checkHealth();
        } catch (Exception e) {
            log.warn("Health check failed for {}: {}", provider, e.getMessage());
            return false;
        }
    }
}