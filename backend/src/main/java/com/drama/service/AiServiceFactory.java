package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.ResultCode;
import com.drama.entity.AiConfig;
import com.drama.service.adapter.AiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI服务工厂 - 管理所有AI适配器，按类型自动选择
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceFactory {

    private final List<AiAdapter> adapterList;
    private final AiConfigService aiConfigService;
    
    private final Map<String, AiAdapter> adapters = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 注册所有适配器
        for (AiAdapter adapter : adapterList) {
            adapters.put(adapter.getProvider().toLowerCase(), adapter);
            log.info("Registered AI adapter: {}", adapter.getProvider());
        }
        // 初始化配置
        loadAllConfigs();
    }

    /**
     * 从数据库加载所有AI配置到适配器
     */
    public void loadAllConfigs() {
        try {
            String[] apiTypes = {"text", "image", "video", "tts"};
            for (String type : apiTypes) {
                List<AiConfig> configs = aiConfigService.listByType(type);
                for (AiConfig config : configs) {
                    AiAdapter adapter = adapters.get(config.getProvider().toLowerCase());
                    if (adapter != null) {
                        adapter.initConfig(config);
                        log.info("Loaded {} config: provider={}, model={}", type, config.getProvider(), config.getModel());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load AI configs on init: {}", e.getMessage());
        }
    }

    /**
     * 获取指定类型的第一个可用适配器
     */
    public AiAdapter getAdapterByType(String apiType) {
        try {
            List<AiConfig> configs = aiConfigService.listByType(apiType);
            if (configs.isEmpty()) {
                throw new BusinessException(ResultCode.NOT_FOUND,
                        "未找到可用的 AI 配置 (type=" + apiType + ")，请先在 AI配置 页面添加");
            }
            // 找第一个已启用的适配器
            for (AiConfig config : configs) {
                AiAdapter adapter = adapters.get(config.getProvider().toLowerCase());
                if (adapter != null && adapter.isConfigured()) {
                    log.info("Using configured adapter: provider={}, type={}", config.getProvider(), apiType);
                    return adapter;
                }
            }
            // 如果都没配置，尝试用第一个配置初始化适配器
            AiConfig first = configs.get(0);
            AiAdapter adapter = adapters.get(first.getProvider().toLowerCase());
            if (adapter != null) {
                log.info("Initializing adapter with config: provider={}, type={}", first.getProvider(), apiType);
                adapter.initConfig(first);
                if (adapter.isConfigured()) {
                    return adapter;
                }
            }
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "AI适配器未正确配置 (type=" + apiType + ", provider=" + first.getProvider() + ")");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting adapter by type {}: {}", apiType, e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "获取AI适配器失败");
        }
    }

    /**
     * 获取指定provider的适配器
     */
    public AiAdapter getAdapter(String provider) {
        AiAdapter adapter = adapters.get(provider.toLowerCase());
        if (adapter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "不支持的 AI 厂商: " + provider);
        }
        
        // 尝试从数据库加载/刷新配置
        if (!adapter.isConfigured()) {
            List<AiConfig> configs = aiConfigService.listByType("text");
            for (AiConfig config : configs) {
                if (config.getProvider().equalsIgnoreCase(provider)) {
                    adapter.initConfig(config);
                    break;
                }
            }
        }
        return adapter;
    }

    /**
     * 文本生成 - 自动选择text类型的适配器
     */
    public String generateText(String prompt, String model) {
        return generateText(null, prompt, model);
    }

    /**
     * 文本生成 - 使用指定的provider
     */
    public String generateText(String provider, String prompt, String model) {
        AiAdapter adapter = provider != null ? getAdapter(provider) : getAdapterByType("text");
        return adapter.generateText(prompt, model);
    }

    /**
     * 图片生成
     */
    public String generateImage(String provider, String prompt, String model) {
        AiAdapter adapter = provider != null ? getAdapter(provider) : getAdapterByType("image");
        return adapter.generateImage(prompt, model);
    }

    /**
     * 视频生成
     */
    public String generateVideo(String provider, String imageUrl, String model) {
        AiAdapter adapter = provider != null ? getAdapter(provider) : getAdapterByType("video");
        return adapter.generateVideo(imageUrl, model);
    }

    /**
     * TTS
     */
    public String generateTTS(String provider, String text, String voiceId, String model) {
        AiAdapter adapter = provider != null ? getAdapter(provider) : getAdapterByType("tts");
        return adapter.generateTTS(text, voiceId, model);
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

    /**
     * 获取所有已注册的适配器名称
     */
    public java.util.Set<String> getAvailableProviders() {
        return adapters.keySet();
    }
}
