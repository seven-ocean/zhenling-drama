package com.drama.service.adapter;

import com.drama.entity.AiConfig;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.model.images.generation.ResponseFormat;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 豆包/火山引擎 Doubao/Ark 适配器
 *
 * 使用官方 volcengine-java-sdk-ark-runtime SDK
 * 文档: https://www.volcengine.com/docs/82379/1824121
 *
 * 模型 ID:
 *   - Doubao-Seedream-5.0-lite: doubao-seedream-5-0-260128
 *   - Doubao-Seedream-4.5:      doubao-seedream-4-5-251128
 *   - Doubao-Seedream-4.0:      doubao-seedream-4-0-250828
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoubaoAdapter implements AiAdapter {

    private static final String PROVIDER = "volcengine";

    private static final String BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";
    private static final String DEFAULT_MODEL = "doubao-seedream-5-0-260128";

    private AiConfig config;
    private volatile ArkService arkService;

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public void initConfig(AiConfig config) {
        this.config = config;
        this.arkService = null; // reset, will be lazily built on first call
        log.info("Doubao adapter initialized: model={}, baseUrl={}",
                config.getModel(), getEffectiveBaseUrl());
    }

    @Override
    public boolean isConfigured() {
        return config != null
                && config.getApiKey() != null && !config.getApiKey().isBlank()
                && config.getEnabled();
    }

    private String getEffectiveBaseUrl() {
        return (config != null && config.getBaseUrl() != null && !config.getBaseUrl().isBlank())
                ? config.getBaseUrl().trim()
                : BASE_URL;
    }

    private String getEffectiveApiKey() {
        return (config != null && config.getApiKey() != null)
                ? config.getApiKey()
                : "";
    }

    private String getEffectiveModel() {
        return (config != null && config.getModel() != null && !config.getModel().isBlank())
                ? config.getModel()
                : DEFAULT_MODEL;
    }

    private synchronized ArkService getOrCreateService() {
        if (arkService == null) {
            ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
            Dispatcher dispatcher = new Dispatcher();
            arkService = ArkService.builder()
                    .baseUrl(getEffectiveBaseUrl())
                    .dispatcher(dispatcher)
                    .connectionPool(connectionPool)
                    .apiKey(getEffectiveApiKey())
                    .build();
        }
        return arkService;
    }

    private void ensureSuccess(ImagesResponse response, String operation) {
        if (response.getError() != null) {
            throw new AiApiException(
                    -1,
                    response.getError().getCode(),
                    "Doubao API error: " + response.getError().getMessage(),
                    operation
            );
        }
    }

    // ========== 文本生成（不支持） ==========

    @Override
    public String generateText(String prompt, String model) {
        throw new UnsupportedOperationException("Doubao text generation not implemented yet");
    }

    // ========== 图片生成 ==========

    @Override
    public String generateImage(String prompt, String model) {
        return doGenerateImage(prompt, null, model);
    }

    /**
     * 图片生成（支持多图融合）
     *
     * @param prompt 提示词
     * @param referenceImageUrls 参考图URL列表；多图融合时传入角色图+场景图；为null或空时退化为文生图
     * @param model 模型名称（如 "doubao-seedream-5-0-260128"）
     * @return 生成的图片URL
     */
    @Override
    public String generateImageWithReferences(String prompt, List<String> referenceImageUrls, String model) {
        return doGenerateImage(prompt, referenceImageUrls, model);
    }

    private String doGenerateImage(String prompt, List<String> referenceImageUrls, String model) {
        if (!isConfigured()) {
            log.warn("[Doubao] Image generation skipped: adapter not configured");
            throw new AiApiException(-1, "", "Doubao 图片生成适配器未配置", "图片生成");
        }

        // 过滤空URL
        List<String> validUrls = referenceImageUrls != null
                ? referenceImageUrls.stream().filter(u -> u != null && !u.isBlank()).collect(Collectors.toList())
                : List.of();

        String useModel = (model != null && !model.isBlank()) ? model : getEffectiveModel();

        log.info("[Doubao] Image generation: model={}, prompt={}, refImages={}",
                useModel, prompt.substring(0, Math.min(50, prompt.length())), validUrls.size());

        try {
            GenerateImagesRequest.Builder requestBuilder = GenerateImagesRequest.builder()
                    .model(useModel)
                    .prompt(prompt)
                    .size("2K")
                    .sequentialImageGeneration("disabled")
                    .outputFormat("png")
                    .responseFormat(ResponseFormat.Url)
                    .stream(false)
                    .watermark(false);

            // 多图融合：传入多张参考图（角色图+场景图）
            if (!validUrls.isEmpty()) {
                if (validUrls.size() == 1) {
                    requestBuilder.image(validUrls.get(0));
                } else {
                    requestBuilder.image(validUrls);
                }
            }

            GenerateImagesRequest request = requestBuilder.build();

            ImagesResponse response = getOrCreateService().generateImages(request);
            ensureSuccess(response, "图片生成");

            if (response.getData() != null && !response.getData().isEmpty()) {
                String imageUrl = response.getData().get(0).getUrl();
                if (imageUrl != null && !imageUrl.isBlank()) {
                    log.info("[Doubao] Image generated successfully: {}", imageUrl);
                    return imageUrl;
                }
            }
            throw new AiApiException(-1, "", "Doubao 图片生成返回格式异常：无可用图片URL", "图片生成");
        } catch (AiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Doubao] Image generation error: {}", e.getMessage(), e);
            throw new AiApiException(-1, e.getMessage(), "Doubao 图片生成异常: " + e.getMessage(), "图片生成");
        }
    }

    // ========== 视频生成（不支持） ==========

    @Override
    public String generateVideo(String imageUrl, String model) {
        throw new UnsupportedOperationException("Doubao video generation not implemented yet");
    }

    // ========== TTS（不支持） ==========

    @Override
    public String generateTTS(String text, String voiceId, String model) {
        throw new UnsupportedOperationException("Doubao TTS not implemented yet");
    }

    // ========== 健康检查 ==========

    @Override
    public boolean checkHealth() {
        if (!isConfigured()) return false;
        try {
            GenerateImagesRequest request = GenerateImagesRequest.builder()
                    .model(getEffectiveModel())
                    .prompt("health check")
                    .size("2K")
                    .outputFormat("png")
                    .responseFormat(ResponseFormat.Url)
                    .watermark(false)
                    .stream(false)
                    .build();
            ImagesResponse response = getOrCreateService().generateImages(request);
            return response.getData() != null && !response.getData().isEmpty();
        } catch (Exception e) {
            log.warn("[Doubao] Health check failed: {}", e.getMessage());
            return false;
        }
    }
}
