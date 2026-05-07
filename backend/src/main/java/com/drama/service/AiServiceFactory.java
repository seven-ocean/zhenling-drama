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

        // 尝试从数据库加载/刷新配置（遍历所有类型，找对应provider的配置）
        if (!adapter.isConfigured()) {
            String[] apiTypes = {"text", "image", "video", "tts"};
            for (String type : apiTypes) {
                List<AiConfig> configs = aiConfigService.listByType(type);
                for (AiConfig config : configs) {
                    if (config.getProvider().equalsIgnoreCase(provider)) {
                        adapter.initConfig(config);
                        return adapter;
                    }
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
     *
     * @param provider 指定厂商（可为null）
     * @param prompt 提示词
     * @param model 模型名称；传入 Seedream 模型时自动路由到 DoubaoAdapter
     */
    public String generateImage(String provider, String prompt, String model) {
        AiAdapter adapter;
        if (provider != null) {
            adapter = getAdapter(provider);
        } else if (model != null && (model.startsWith("doubao-seedream") || model.startsWith("vc-seedream"))) {
            adapter = getAdapter("volcengine");
        } else if (model != null && model.startsWith("image-")) {
            adapter = getAdapter("minimax");
        } else {
            adapter = getAdapterByType("image");
        }
        return adapter.generateImage(prompt, model);
    }

    /**
     * 多图参考图片生成
     *
     * @param provider 指定厂商（可为null）
     * @param prompt 提示词
     * @param referenceImageUrls 参考图URL列表
     * @param model 模型名称；传入 Seedream 模型时自动路由到 DoubaoAdapter
     */
    public String generateImageWithReferences(String provider, String prompt, java.util.List<String> referenceImageUrls, String model) {
        AiAdapter adapter;
        if (provider != null) {
            adapter = getAdapter(provider);
        } else if (model != null && (model.startsWith("doubao-seedream") || model.startsWith("vc-seedream"))) {
            adapter = getAdapter("volcengine");
        } else if (model != null && model.startsWith("image-")) {
            // MiniMax image 模型路由
            adapter = getAdapter("minimax");
        } else {
            adapter = getAdapterByType("image");
        }
        return adapter.generateImageWithReferences(prompt, referenceImageUrls, model);
    }

    /**
     * 视频生成（基础版，向后兼容）
     */
    public String generateVideo(String provider, String imageUrl, String model) {
        AiAdapter adapter = provider != null ? getAdapter(provider) : getAdapterByType("video");
        return adapter.generateVideo(imageUrl, model);
    }

    /**
     * 视频生成（多模式支持）
     * 模式：TEXT_TO_VIDEO(文生视频), IMAGE_TO_VIDEO(图生视频), FIRST_LAST_FRAME(首尾帧), SUBJECT_REFERENCE(主体参考)
     */
    public String generateVideoMultiMode(String provider, String mode, String prompt,
                                         String imageUrl, String firstFrameUrl, String lastFrameUrl,
                                         String subjectImageUrl, String model) {
        return generateVideoMultiMode(provider, mode, prompt, imageUrl, firstFrameUrl, lastFrameUrl, subjectImageUrl, model, 6, "768P");
    }

    /**
     * 视频生成（多模式支持，带扩展参数）
     * 模式：TEXT_TO_VIDEO(文生视频), IMAGE_TO_VIDEO(图生视频), FIRST_LAST_FRAME(首尾帧), SUBJECT_REFERENCE(主体参考)
     */
    public String generateVideoMultiMode(String provider, String mode, String prompt,
                                         String imageUrl, String firstFrameUrl, String lastFrameUrl,
                                         String subjectImageUrl, String model, Integer duration, String resolution) {
        // 目前只有 MiniMax 支持多模式
        if ("minimax".equalsIgnoreCase(provider)) {
            com.drama.service.adapter.MiniMaxAdapter miniMaxAdapter =
                    (com.drama.service.adapter.MiniMaxAdapter) getAdapter("minimax");
            return miniMaxAdapter.generateVideoMultiMode(mode, prompt, imageUrl, firstFrameUrl, lastFrameUrl, subjectImageUrl, model, duration, resolution);
        }
        // 其他厂商使用基础版（图生视频）
        AiAdapter adapter = getAdapter(provider);
        if ("TEXT_TO_VIDEO".equals(mode)) {
            // 文生视频：传入空图片，让适配器处理
            return adapter.generateVideo("", prompt, model, duration, resolution);
        }
        // 其他模式默认使用图生视频
        String refImage = imageUrl;
        if (refImage == null || refImage.isEmpty()) {
            refImage = firstFrameUrl;
        }
        if (refImage == null || refImage.isEmpty()) {
            refImage = subjectImageUrl;
        }
        return adapter.generateVideo(refImage, prompt, model, duration, resolution);
    }

    /**
     * TTS
     */
    public String generateTTS(String provider, String text, String voiceId, String model) {
        AiAdapter adapter = provider != null ? getAdapter(provider) : getAdapterByType("tts");
        return adapter.generateTTS(text, voiceId, model);
    }

    /**
     * 图片分析（Vision）
     * 调用 AI 多模态模型分析图片内容
     *
     * @param prompt 分析提示词
     * @param imageUrl 图片 URL
     * @return 分析结果文本
     */
    public String analyzeImage(String prompt, String imageUrl) {
        // 目前只有 MiniMax 支持 Vision，后续可扩展其他厂商
        AiAdapter adapter = getAdapterByType("text");
        if (adapter instanceof com.drama.service.adapter.MiniMaxAdapter) {
            return ((com.drama.service.adapter.MiniMaxAdapter) adapter).analyzeImage(prompt, imageUrl);
        }
        throw new com.drama.common.BusinessException(
                com.drama.common.ResultCode.SERVER_ERROR,
                "当前配置的 AI 不支持图片分析，请使用 MiniMax 的 text 类型配置");
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
