package com.drama.service.adapter;

import com.drama.entity.AiConfig;
import com.volcengine.ark.runtime.model.content.generation.CreateContentGenerationTaskRequest;
import com.volcengine.ark.runtime.model.content.generation.CreateContentGenerationTaskResult;
import com.volcengine.ark.runtime.model.content.generation.GetContentGenerationTaskRequest;
import com.volcengine.ark.runtime.model.content.generation.GetContentGenerationTaskResponse;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.model.images.generation.ResponseFormat;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    // 图片模型
    private static final String DEFAULT_IMAGE_MODEL = "doubao-seedream-5-0-260128";

    // 视频模型 - Seedance 1.5 Pro (当前接入目标)
    private static final String VIDEO_MODEL_1_5_PRO = "doubao-seedance-1-5-pro-251215";
    // 视频模型 - Seedance 2.0 (未来扩展)
    private static final String VIDEO_MODEL_2_0 = "doubao-seedance-2-0-260128";
    private static final String DEFAULT_VIDEO_MODEL = VIDEO_MODEL_1_5_PRO;

    // 视频默认值
    private static final int DEFAULT_DURATION = 5;   // 默认5秒
    private static final String DEFAULT_RATIO = "16:9";  // 默认16:9

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
                : DEFAULT_IMAGE_MODEL;
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

    // ========== 视频生成（音画同生）==========

    @Override
    public String generateVideo(String imageUrl, String model) {
        return generateVideoWithFrames(imageUrl, null, null, null, null, DEFAULT_DURATION, DEFAULT_RATIO, true, null);
    }

    @Override
    public String generateVideo(String imageUrl, String prompt, String model, Integer duration, String resolution) {
        return generateVideoWithFrames(imageUrl, null, null, null, prompt, duration, resolution, true, null);
    }

    /**
     * 视频生成（音画同生，支持多种参考素材）
     *
     * 豆包 Seedance 1.5 Pro / 2.0 的核心能力是原生音画同生：
     * - 在 prompt 中描述音频元素，模型自动生成同步音频
     * - 支持多种参考素材：reference_image / reference_video / reference_audio
     *
     * @param firstFrameUrl 首帧图片URL（用于图生视频）
     * @param lastFrameUrl 尾帧图片URL（可选，用于首尾帧控制）
     * @param videoReferenceUrl 参考视频URL（可选，用于运镜/动作参考）
     * @param audioReferenceUrl 参考音频URL（可选，用于背景音乐参考）
     * @param prompt 视频+音频描述提示词
     * @param duration 视频时长（秒），支持 4-12s
     * @param ratio 宽高比，如 "16:9", "9:16", "1:1"
     * @param generateAudio 是否生成音频（设为 true 时启用音画同生）
     * @param audioFileUrl 用户上传的音频文件URL（可选，用于精确对口型）
     * @return 任务ID（格式: "task:{taskId}"）
     */
    public String generateVideoWithFrames(String firstFrameUrl, String lastFrameUrl,
                            String videoReferenceUrl, String audioReferenceUrl,
                            String prompt, Integer duration, String ratio,
                            boolean generateAudio, String audioFileUrl) {
        if (!isConfigured()) {
            throw new AiApiException(-1, "", "Doubao 视频生成适配器未配置", "视频生成");
        }

        // Seedance 1.5 Pro 不支持参考视频（r2v 任务类型），仅 2.0 支持
        log.info("[Doubao] generateVideoWithFrames called: videoReferenceUrl={}, audioReferenceUrl={}, audioFileUrl={}",
                videoReferenceUrl, audioReferenceUrl, audioFileUrl);
        if (videoReferenceUrl != null && !videoReferenceUrl.isBlank()) {
            throw new AiApiException(-1, "", "Seedance 1.5 Pro 模型不支持参考视频（运镜/动作参考），请使用 Seedance 2.0 模型或去掉参考视频参数", "视频生成");
        }

        try {
            List<CreateContentGenerationTaskRequest.Content> contents = new ArrayList<>();

            // 1. 文本提示词
            // 音画同生需要在 prompt 中描述音频元素：
            // "首帧为图片1，你的手摘下一颗红苹果... 背景音乐：轻快的钢琴曲"
            StringBuilder fullPrompt = new StringBuilder();
            if (prompt != null && !prompt.isBlank()) {
                fullPrompt.append(prompt);
            }

            contents.add(CreateContentGenerationTaskRequest.Content.builder()
                    .type("text")
                    .text(fullPrompt.toString())
                    .build());

            // 2. 首帧图片
            // 注意：1.5 pro 不支持 reference_image role（会触发 r2v task type 而报错）
            //      应使用 first_frame / last_frame，或不填 role
            boolean hasLastFrame = lastFrameUrl != null && !lastFrameUrl.isBlank();
            if (firstFrameUrl != null && !firstFrameUrl.isBlank()) {
                if (hasLastFrame) {
                    // 首尾帧模式：明确标注 first_frame
                    contents.add(CreateContentGenerationTaskRequest.Content.builder()
                            .type("image_url")
                            .imageUrl(CreateContentGenerationTaskRequest.ImageUrl.builder()
                                    .url(firstFrameUrl)
                                    .build())
                            .role("first_frame")
                            .build());
                } else {
                    // 单图时不填 role，让 API 自动识别为 first_frame
                    contents.add(CreateContentGenerationTaskRequest.Content.builder()
                            .type("image_url")
                            .imageUrl(CreateContentGenerationTaskRequest.ImageUrl.builder()
                                    .url(firstFrameUrl)
                                    .build())
                            .build());
                }
            }

            // 3. 尾帧图片（仅首尾帧模式）
            // 1.5 pro 首尾帧模式必须用 role="last_frame"
            if (hasLastFrame) {
                contents.add(CreateContentGenerationTaskRequest.Content.builder()
                        .type("image_url")
                        .imageUrl(CreateContentGenerationTaskRequest.ImageUrl.builder()
                                .url(lastFrameUrl)
                                .build())
                        .role("last_frame")
                        .build());
            }

            // 4. 参考视频（reference_video）- Seedance 2.0 支持
            // 用于借鉴视频中的运镜方式、人物动作等
            if (videoReferenceUrl != null && !videoReferenceUrl.isBlank()) {
                contents.add(CreateContentGenerationTaskRequest.Content.builder()
                        .type("video_url")
                        .videoUrl(CreateContentGenerationTaskRequest.VideoUrl.builder()
                                .url(videoReferenceUrl)
                                .build())
                        .role("reference_video")
                        .build());
            }

            // 5. 参考音频（reference_audio）- Seedance 2.0 支持
            // 用于使用现成的背景音乐、音效等
            if (audioReferenceUrl != null && !audioReferenceUrl.isBlank()) {
                contents.add(CreateContentGenerationTaskRequest.Content.builder()
                        .type("audio_url")
                        .audioUrl(CreateContentGenerationTaskRequest.AudioUrl.builder()
                                .url(audioReferenceUrl)
                                .build())
                        .role("reference_audio")
                        .build());
            }

            // 6. 用户上传的音频文件（用于精确对口型）
            // 如果提供了，则作为主要音频源，模型会对口型
            if (audioFileUrl != null && !audioFileUrl.isBlank()) {
                contents.add(CreateContentGenerationTaskRequest.Content.builder()
                        .type("audio_url")
                        .audioUrl(CreateContentGenerationTaskRequest.AudioUrl.builder()
                                .url(audioFileUrl)
                                .build())
                        .role("reference_audio")
                        .build());
            }

            // 7. 构建请求
            int useDuration = (duration != null) ? Math.min(Math.max(duration, 4), 12) : DEFAULT_DURATION;
            String useRatio = (ratio != null && !ratio.isBlank()) ? ratio : DEFAULT_RATIO;

            CreateContentGenerationTaskRequest.Builder requestBuilder = CreateContentGenerationTaskRequest.builder()
                    .model(DEFAULT_VIDEO_MODEL)
                    .content(contents)
                    .duration((long) useDuration)
                    .ratio(useRatio)
                    .watermark(false)
                    .generateAudio(generateAudio);

            CreateContentGenerationTaskRequest createRequest = requestBuilder.build();

            log.info("[Doubao] Video generation: firstFrame={}, lastFrame={}, videoRef={}, audioRef={}, audioFile={}, duration={}, ratio={}, generateAudio={}",
                    firstFrameUrl, lastFrameUrl, videoReferenceUrl, audioReferenceUrl, audioFileUrl, useDuration, useRatio, generateAudio);

            CreateContentGenerationTaskResult result = getOrCreateService().createContentGenerationTask(createRequest);

            if (result.getId() != null) {
                return "task:" + result.getId();
            }
            throw new AiApiException(-1, "", "Doubao 视频任务创建失败，未返回taskId", "视频生成");
        } catch (AiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Doubao] Video generation error: {}", e.getMessage(), e);
            throw new AiApiException(-1, e.getMessage(), "Doubao 视频生成异常: " + e.getMessage(), "视频生成");
        }
    }

    /**
     * 轮询视频任务状态
     */
    @Override
    public String pollVideoStatus(String taskId) {
        if (!isConfigured()) return "status:unknown";

        try {
            // 清理可能的 task: 前缀
            String cleanTaskId = taskId.replace("task:", "");

            GetContentGenerationTaskRequest getRequest = GetContentGenerationTaskRequest.builder()
                    .taskId(cleanTaskId)
                    .build();

            GetContentGenerationTaskResponse response = getOrCreateService().getContentGenerationTask(getRequest);

            String status = response.getStatus();
            if ("succeeded".equalsIgnoreCase(status)) {
                // 视频生成成功，从 content 中获取视频URL
                GetContentGenerationTaskResponse.Content content = response.getContent();
                if (content != null) {
                    String videoUrl = content.getVideoUrl();
                    if (videoUrl != null && !videoUrl.isBlank()) {
                        return "completed:" + videoUrl;
                    }
                    // 尝试获取文件URL（某些情况下视频可能通过fileUrl返回）
                    String fileUrl = content.getFileUrl();
                    if (fileUrl != null && !fileUrl.isBlank()) {
                        return "completed:" + fileUrl;
                    }
                }
                return "status:unknown";
            } else if ("failed".equalsIgnoreCase(status)) {
                // 获取错误信息
                String errorMsg = "视频生成失败";
                if (response.getError() != null && response.getError().getMessage() != null) {
                    errorMsg = response.getError().getMessage();
                }
                return "failed:" + errorMsg;
            } else {
                return "status:" + status;
            }
        } catch (Exception e) {
            log.error("[Doubao] Poll video status error: {}", e.getMessage());
            return "status:unknown";
        }
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
