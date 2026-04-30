package com.drama.service.adapter;

import com.drama.entity.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * MiniMax 适配器 v2
 *
 * 官方文档参考：
 *   - API概览: https://platform.minimaxi.com/docs/api-reference/api-overview
 *   - 文本(OpenAI兼容): https://platform.minimaxi.com/docs/api-reference/text-openai-api
 *   - 图片生成: https://platform.minimaxi.com/docs/api-reference/image-generation-t2i
 *   - 视频生成: https://platform.minimaxi.com/docs/api-reference/video-generation-t2v
 *   - 视频查询: https://platform.minimaxi.com/docs/api-reference/video-generation-query
 *   - TTS语音: https://platform.minimaxi.com/docs/api-reference/speech-t2a-http
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniMaxAdapter implements AiAdapter {

    private static final String PROVIDER = "minimax";

    /**
     * MiniMax 官方 Base URL（2025年更新）
     * 文本支持 OpenAI 兼容模式，其余走原生端点
     */
    private static final String BASE_URL = "https://api.minimaxi.com/v1";

    /** 默认模型（与官方文档对齐） */
    private static final String DEFAULT_TEXT_MODEL = "MiniMax-M2.5";
    private static final String DEFAULT_IMAGE_MODEL = "image-01";
    private static final String DEFAULT_VIDEO_MODEL = "MiniMax-Hailuo-2.3";
    private static final String DEFAULT_TTS_MODEL = "speech-02-hd";

    /** 使用 volatile 保证多线程可见性（initConfig 可能被运行时重新调用） */
    private volatile AiConfig config;

    /** 注入由 RestTemplateConfig 创建的 Bean（支持代理） */
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== MiniMax API 错误码常量 ==========

    /** 余额不足 */
    private static final int ERR_INSUFFICIENT_BALANCE = 1008;
    /** 参数错误 */
    private static final int ERR_INVALID_PARAM = 1004;
    /** API Key 无效 */
    private static final int ERR_UNAUTHORIZED = 1001;
    /** 频率限制 */
    private static final int ERR_RATE_LIMIT = 1013;
    /** 内容安全审核不通过 */
    private static final int ERR_CONTENT_SAFE = 1014;

    /**
     * 解析 MiniMax base_resp 并抛出有意义的异常（替代旧的静默返回空串）
     *
     * MiniMax 标准响应结构: { "base_resp": { "status_code": 0, "status_msg": "success" }, ... }
     * 当 status_code != 0 时，根据错误码给出中文提示
     *
     * @param root       API 响应 JSON 根节点
     * @param operation  操作名称（用于日志和异常消息）
     * @throws AiApiException 当 API 返回业务错误时
     */
    private void ensureSuccess(JsonNode root, String operation) throws AiApiException {
        JsonNode baseResp = root.path("base_resp");
        int code = baseResp.path("status_code").asInt(-1);

        if (code == 0) return; // 成功

        String msg = baseResp.path("status_msg").asText("未知错误");

        // 根据 MiniMax 错误码翻译为用户可理解的中文消息
        String userMessage = translateErrorCode(code, msg, operation);

        log.error("[MiniMax] {} API 失败: code={}, msg={}, 原文={}", operation, code, msg, userMessage);
        throw new AiApiException(code, msg, userMessage, operation);
    }

    /**
     * 将 MiniMax 错误码翻译为用户友好的中文提示
     */
    private String translateErrorCode(int code, String originalMsg, String operation) {
        switch (code) {
            case ERR_INSUFFICIENT_BALANCE:
                return "MiniMax 账户余额不足（错误码:" + code + "），请在 MiniMax 开放平台充值后再试。"
                        + "操作类型: " + operation;
            case ERR_INVALID_PARAM:
                return "MiniMax 参数校验失败（错误码:" + code + "）：" + originalMsg
                        + "。操作类型: " + operation + "，请检查请求参数是否正确";
            case ERR_UNAUTHORIZED:
                return "MiniMax API Key 无效或已过期（错误码:" + code + "），请在 AI 配置页面更新正确的 API Key";
            case ERR_RATE_LIMIT:
                return "MiniMax 请求频率超限（错误码:" + code + "），请稍后重试";
            case ERR_CONTENT_SAFE:
                return "MiniMax 内容安全审核未通过（错误码:" + code + "）：" + originalMsg
                        + "。请调整提示词内容后重试";
            default:
                return "MiniMax API 调用失败（错误码:" + code + "）：" + originalMsg
                        + "。操作类型: " + operation;
        }
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public void initConfig(AiConfig config) {
        this.config = config;
        log.info("MiniMax adapter initialized: model={}, baseUrl={}", config.getModel(), getBaseUrl());
    }

    @Override
    public boolean isConfigured() {
        return config != null && config.getEnabled()
                && config.getApiKey() != null && !config.getApiKey().isEmpty();
    }

    // ========== 工具方法 ==========

    private String getBaseUrl() {
        if (config != null && config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            return config.getBaseUrl().replaceAll("/$", "");
        }
        return BASE_URL;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());
        return headers;
    }

    /**
     * 统一检查 MiniMax 基础响应状态
     * MiniMax 标准返回结构: { "base_resp": { "status_code": 0, "status_msg": "success" }, ... }
     */
    private boolean isSuccess(JsonNode root) {
        JsonNode baseResp = root.path("base_resp");
        int code = baseResp.path("status_code").asInt(-1);
        return code == 0;
    }

    // ========== 文本生成（OpenAI 兼容） ==========

    @Override
    public String generateText(String prompt, String model) {
        if (!isConfigured()) {
            log.warn("MiniMax not configured for text generation");
            throw new AiApiException(-1, "", "MiniMax 文本生成适配器未配置，请在 AI 配置页面添加 text 类型的配置", "文本生成");
        }
        try {
            // OpenAI 兼容接口：POST /v1/chat/completions
            String url = getBaseUrl() + "/chat/completions";
            String useModel = (model != null) ? model : DEFAULT_TEXT_MODEL;

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"model\": \"").append(useModel).append("\", ");
            json.append("\"messages\": [{\"role\": \"user\", \"content\": ").append(objectMapper.writeValueAsString(prompt)).append("}], ");
            json.append("\"temperature\": 0.7");
            json.append("}");

            log.info("[MiniMax] Text generation: model={}", useModel);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(json.toString(), authHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // 检查是否有 MiniMax 错误响应（OpenAI兼容接口可能走 base_resp）
                if (root.has("base_resp") && root.path("base_resp").path("status_code").asInt(0) != 0) {
                    ensureSuccess(root, "文本生成");
                }

                // OpenAI 兼容格式：choices[0].message.content
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String text = choices.get(0).path("message").path("content").asText("");
                    if (!text.isEmpty()) return text;
                }
                // 兜底：尝试直接取 content 字段
                String fallback = root.path("content").asText("");
                if (!fallback.isEmpty()) return fallback;

                // 真的什么都没有
                log.warn("[MiniMax] Text generation returned empty body: {}", response.getBody());
                throw new AiApiException(-1, "", "MiniMax 文本生成返回为空内容，请检查模型是否正确或稍后重试", "文本生成");
            } else {
                // HTTP 层面的错误（非 2xx）
                int httpStatus = response.getStatusCodeValue();
                String body = response.getBody() != null ? response.getBody().substring(0, Math.min(500, response.getBody().length())) : "";
                throw new AiApiException(httpStatus, body,
                        "MiniMax 文本生成请求失败（HTTP " + httpStatus + "），请检查网络或服务状态", "文本生成");
            }
        } catch (AiApiException e) {
            throw e; // 直接透传我们的自定义异常
        } catch (Exception e) {
            log.error("[MiniMax] Text generation error: {}", e.getMessage(), e);
            throw new AiApiException(-1, e.getMessage(),
                    "MiniMax 文本生成异常：" + e.getMessage() + "，请检查后端日志获取详细信息", "文本生成");
        }
    }

    // ========== 图片生成 ==========

    @Override
    public String generateImage(String prompt, String model) {
        if (!isConfigured()) {
            log.warn("[MiniMax] Image generation skipped: adapter not configured");
            throw new AiApiException(-1, "", "MiniMax 图片生成适配器未配置，请在 AI 配置页面添加 image 类型的配置", "图片生成");
        }

        try {
            // 原生接口：POST /v1/image_generation
            String url = getBaseUrl() + "/image_generation";
            String useModel = (model != null) ? model : DEFAULT_IMAGE_MODEL;

            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"model\": \"").append(useModel).append("\", ");
            body.append("\"prompt\": ").append(objectMapper.writeValueAsString(prompt)).append(", ");
            body.append("\"aspect_ratio\": \"16:9\", ");
            body.append("\"response_format\": \"url\", ");
            body.append("\"n\": 1, ");
            // content_safe=false: 放宽MiniMax内容审核，避免正常创作提示词被误判为 sensitive
            body.append("\"content_safe\": false");
            body.append("}");

            log.info("[MiniMax] Image generation: model={}, prompt={}", useModel, prompt.substring(0, Math.min(50, prompt.length())));
            log.debug("[MiniMax] Request body: {}", body);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), authHeaders()),
                    String.class);

            log.debug("[MiniMax] Image response status: {}, body: {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // 使用 ensureSuccess 统一错误检查（替代旧的 isSuccess + 静默返回空）
                ensureSuccess(root, "图片生成");

                // 官方返回格式：{ "data": { "image_urls": ["url1", ...] }, "base_resp": {...} }
                JsonNode data = root.path("data");
                JsonNode imageUrls = data.path("image_urls");
                if (imageUrls.isArray() && imageUrls.size() > 0) {
                    String imageUrl = imageUrls.get(0).asText("");
                    log.info("[MiniMax] Image generated successfully: {}", imageUrl);
                    return imageUrl;
                } else {
                    log.warn("[MiniMax] Image gen response has no image_urls, data: {}", data);
                    throw new AiApiException(-1, "",
                            "MiniMax 图片生成返回数据中无 image_urls，可能是模型或参数问题", "图片生成");
                }
            } else {
                int httpStatus = response.getStatusCodeValue();
                throw new AiApiException(httpStatus,
                        response.getBody() != null ? response.getBody().substring(0, Math.min(500, response.getBody().length())) : "",
                        "MiniMax 图片生成请求失败（HTTP " + httpStatus + "）", "图片生成");
            }
        } catch (AiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MiniMax] Image generation error: {}", e.getMessage(), e);
            throw new AiApiException(-1, e.getMessage(),
                    "MiniMax 图片生成异常：" + e.getMessage() + "，请检查后端日志获取详细信息", "图片生成");
        }
    }

    /**
     * 多图参考图片生成
     * MiniMax-Image-01 支持通过 image_urls 传入多张参考图（角色图×N + 场景图）合成一张图
     * 降级策略：若不支持多图，则取第一张参考图或场景图作为唯一参考图调用 generateImage
     */
    @Override
    public String generateImageWithReferences(String prompt, java.util.List<String> referenceImageUrls, String model) {
        if (!isConfigured()) {
            log.warn("[MiniMax] Image generation with references skipped: adapter not configured");
            throw new AiApiException(-1, "", "MiniMax 图片生成适配器未配置", "多图参考生成");
        }

        // 过滤空 URL
        java.util.List<String> validUrls = referenceImageUrls != null
                ? referenceImageUrls.stream().filter(u -> u != null && !u.isBlank()).collect(java.util.stream.Collectors.toList())
                : new java.util.ArrayList<>();

        if (validUrls.isEmpty()) {
            log.info("[MiniMax] No valid reference images, falling back to prompt-only generation");
            return generateImage(prompt, model);
        }

        try {
            String url = getBaseUrl() + "/image_generation";
            String useModel = (model != null) ? model : DEFAULT_IMAGE_MODEL;

            // 构建多图参考请求体
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", useModel);
            requestBody.put("prompt", prompt);
            requestBody.put("aspect_ratio", "16:9");
            requestBody.put("response_format", "url");
            requestBody.put("n", 1);
            requestBody.put("content_safe", false);
            requestBody.put("image_urls", validUrls);

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            log.info("[MiniMax] Image generation with {} reference images: model={}, prompt={}, imageCount={}",
                    validUrls.size(), useModel, prompt.substring(0, Math.min(50, prompt.length())), validUrls.size());
            log.debug("[MiniMax] Request body: {}", bodyJson);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(bodyJson, authHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                ensureSuccess(root, "多图参考生成");

                JsonNode data = root.path("data");
                JsonNode imageUrls = data.path("image_urls");
                if (imageUrls.isArray() && imageUrls.size() > 0) {
                    String imageUrl = imageUrls.get(0).asText("");
                    log.info("[MiniMax] Image with references generated successfully: {}", imageUrl);
                    return imageUrl;
                } else {
                    log.warn("[MiniMax] Image gen with references response has no image_urls, degrading to prompt-only");
                    return generateImage(prompt, model);
                }
            } else {
                int httpStatus = response.getStatusCodeValue();
                log.warn("[MiniMax] Multi-image reference failed (HTTP {}), degrading to prompt-only: {}",
                        httpStatus, response.getBody());
                // 降级：不用参考图，纯 prompt 生成
                return generateImage(prompt, model);
            }
        } catch (AiApiException e) {
            // API 异常直接透传
            throw e;
        } catch (Exception e) {
            log.error("[MiniMax] Image generation with references error, degrading to prompt-only: {}", e.getMessage());
            try {
                return generateImage(prompt, model);
            } catch (Exception ex) {
                throw new AiApiException(-1, ex.getMessage(),
                        "MiniMax 多图参考生成异常，降级失败：" + ex.getMessage(), "多图参考生成");
            }
        }
    }

    // ========== 视频生成（异步任务） ==========

    @Override
    public String generateVideo(String imageUrl, String model) {
        // 调用新方法，使用默认值
        return generateVideo(imageUrl, null, model, 6, "768P");
    }

    @Override
    public String generateVideo(String imageUrl, String prompt, String model, Integer duration, String resolution) {
        if (!isConfigured()) throw new AiApiException(-1, "", "MiniMax 视频生成适配器未配置", "视频生成");

        try {
            // POST /v1/video_generation（文生视频/图生视频共用）
            String url = getBaseUrl() + "/video_generation";
            String useModel = resolveVideoModel(
                    (imageUrl != null && !imageUrl.isEmpty()) ? "IMAGE_TO_VIDEO" : "TEXT_TO_VIDEO",
                    model
            );

            // 参数校验与默认值
            int useDuration = (duration != null && (duration == 6 || duration == 10)) ? duration : 6;
            String useResolution = (resolution != null && !resolution.isEmpty()) ? resolution : "768P";

            // 根据模型能力校验参数
            if (useModel.contains("T2V-01") && useDuration == 10) {
                useDuration = 6; // T2V-01 不支持 10s
            }
            if (useDuration == 10 && "1080P".equals(useResolution)) {
                useResolution = "768P"; // 10s 视频不支持 1080P
            }

            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"model\": \"").append(useModel).append("\", ");
            body.append("\"duration\": ").append(useDuration).append(", ");
            body.append("\"resolution\": \"").append(useResolution).append("\", ");

            // 使用用户提供的 prompt 或默认 prompt
            String usePrompt = (prompt != null && !prompt.isEmpty()) ? prompt : "[镜头缓慢推进] A cinematic video clip with smooth motion";

            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 图生视频模式：使用官方正确的参数名 first_frame_image
                body.append("\"prompt\": ").append(objectMapper.writeValueAsString(usePrompt)).append(", ");
                body.append("\"first_frame_image\": ").append(objectMapper.writeValueAsString(imageUrl));
            } else {
                // 文生视频模式
                body.append("\"prompt\": ").append(objectMapper.writeValueAsString(usePrompt));
            }
            body.append("}");

            log.info("[MiniMax] Video generation: model={}, duration={}, resolution={}", useModel, useDuration, useResolution);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), authHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // 使用 ensureSuccess 统一错误检查——这里会捕获 insufficient balance 等！
                ensureSuccess(root, "视频生成");

                // 返回 task_id 用于后续轮询
                String taskId = root.path("task_id").asText("");
                if (!taskId.isEmpty()) {
                    return "task:" + taskId;
                }
                log.warn("[MiniMax] Video success but no task_id: {}", response.getBody());
                throw new AiApiException(-1, "",
                        "MiniMax 视频任务创建成功但未返回 task_id", "视频生成");
            } else {
                int httpStatus = response.getStatusCodeValue();
                throw new AiApiException(httpStatus,
                        response.getBody() != null ? response.getBody().substring(0, Math.min(500, response.getBody().length())) : "",
                        "MiniMax 视频生成请求失败（HTTP " + httpStatus + "）", "视频生成");
            }
        } catch (AiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MiniMax] Video generation error: {}", e.getMessage(), e);
            throw new AiApiException(-1, e.getMessage(),
                    "MiniMax 视频生成异常：" + e.getMessage() + "，请检查后端日志获取详细信息", "视频生成");
        }
    }

    // ========== TTS 语音合成 ==========

    @Override
    public String generateTTS(String text, String voiceId, String model) {
        if (!isConfigured())
            throw new AiApiException(-1, "", "MiniMax TTS 适配器未配置，请在 AI 配置页面添加 tts 类型的配置", "语音合成(TTS)");

        try {
            // POST /v1/t2a_v2
            String url = getBaseUrl() + "/t2a_v2";
            String useModel = (model != null) ? model : DEFAULT_TTS_MODEL;
            String useVoiceId = (voiceId != null) ? voiceId : "female-tianmei";

            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"model\": \"").append(useModel).append("\", ");
            body.append("\"text\": ").append(objectMapper.writeValueAsString(text)).append(", ");
            body.append("\"voice_setting\": {");
            body.append("\"voice_id\": \"").append(useVoiceId).append("\"");
            body.append("}, ");
            body.append("\"audio_setting\": {");
            body.append("\"sample_rate\": 32000, ");
            body.append("\"format\": \"mp3\"");
            body.append("}, ");
            body.append("\"output_format\": \"url\"");
            body.append("}");

            log.info("[MiniMax] TTS: voice={}, model={}, len={}", useVoiceId, useModel, text.length());
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), authHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // 统一错误检查
                ensureSuccess(root, "语音合成(TTS)");

                // 官方返回格式：{ "data": { "audio": "<hex或url>", "status": 2 }, ... }
                String audioData = root.path("data").path("audio").asText("");

                if (!audioData.isEmpty()) {
                    if (audioData.startsWith("http")) {
                        return audioData;
                    } else {
                        return "audio:mp3:hex," + audioData;
                    }
                }
                log.warn("[MiniMax] TTS returned empty audio data: {}", response.getBody());
                throw new AiApiException(-1, "", "MiniMax TTS 返回数据为空，可能是模型或文本问题", "语音合成(TTS)");
            } else {
                int httpStatus = response.getStatusCodeValue();
                throw new AiApiException(httpStatus,
                        response.getBody() != null ? response.getBody().substring(0, Math.min(500, response.getBody().length())) : "",
                        "MiniMax TTS 请求失败（HTTP " + httpStatus + "）", "语音合成(TTS)");
            }
        } catch (AiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MiniMax] TTS error: {}", e.getMessage(), e);
            throw new AiApiException(-1, e.getMessage(),
                    "MiniMax TTS 异常：" + e.getMessage() + "，请检查后端日志获取详细信息", "语音合成(TTS)");
        }
    }

    // ========== 健康检查 ==========

    @Override
    public boolean checkHealth() {
        if (!isConfigured()) return false;
        try {
            String result = generateText("hi", DEFAULT_TEXT_MODEL);
            return !result.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ========== 视频任务轮询 ==========

    @Override
    public String pollVideoStatus(String taskId) {
        if (!isConfigured() || taskId == null || taskId.isEmpty()) return "status:failed";

        // 清理可能的 task: 前缀
        String cleanTaskId = taskId.replace("task:", "");

        try {
            // GET /v1/query/video_generation?task_id=xxx
            String url = getBaseUrl() + "/query/video_generation?task_id=" + cleanTaskId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + config.getApiKey());

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // MiniMax 视频查询响应格式:
                // { "task_id": "xxx", "status": "Success/Processing/Queueing/Fail",
                //   "file_id": "xxx"(成功时), "video_width": 1920, "video_height": 1080,
                //   "base_resp": { "status_code": 0, "status_msg": "success" } }

                String status = root.path("status").asText("").toUpperCase();

                switch (status) {
                    case "SUCCESS":
                    case "DONE":
                    case "COMPLETED": {
                        // MiniMax 返回的是 file_id，需通过 /files/retrieve 解析为真实下载URL
                        String fileId = root.path("file_id").asText("");
                        if (!fileId.isEmpty()) {
                            String realUrl = fetchFileDownloadUrl(fileId);
                            if (!realUrl.isEmpty()) {
                                return "completed:" + realUrl;
                            }
                            log.warn("[MiniMax] pollVideo: file_id={} resolved to empty URL, returning raw fileId", fileId);
                        }
                        // 兜底：有些情况可能直接返回 video_url
                        String videoUrl = root.path("video_url").asText("");
                        if (!videoUrl.isEmpty()) {
                            return "completed:" + videoUrl;
                        }
                        // 最后兜底：直接返回 file_id 让上层处理
                        return "completed:" + fileId;
                    }

                    case "FAIL":
                    case "FAILED":
                    case "ERROR":
                        String errMsg = root.path("base_resp").path("status_msg").asText("视频生成失败");
                        return "failed:" + errMsg;

                    case "PREPARING":
                    case "QUEUEING":
                    default:
                        return "status:processing";
                }
            }
        } catch (Exception e) {
            log.error("[MiniMax] Poll video status failed for task {}: {}", taskId, e.getMessage());
        }
        return "status:unknown";
    }

    // ========== 视频生成多模式支持（MiniMax专用） ==========

    /**
     * 根据 file_id 从 MiniMax 文件服务获取视频下载链接
     * MiniMax 视频任务完成后返回的是 file_id，需通过此接口解析为真实 download_url
     *
     * 文档: https://platform.minimaxi.com/docs/api-reference/video-generation
     * 接口: GET https://api.minimaxi.com/v1/files/retrieve?file_id={file_id}
     * 返回: { "file": { "file_id": "...", "download_url": "https://...", ... }, "base_resp": {...} }
     *
     * @param fileId 视频文件ID（如 "389685784670407"）
     * @return 真实的视频下载URL；失败返回空字符串
     */
    public String fetchFileDownloadUrl(String fileId) {
        if (!isConfigured() || fileId == null || fileId.isEmpty()) return "";

        try {
            String url = getBaseUrl() + "/files/retrieve?file_id=" + fileId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + config.getApiKey());

            log.info("[MiniMax] Fetching download URL for file_id: {}", fileId);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // 响应格式: { "file": { "file_id": "...", "download_url": "...", ... }, "base_resp": {...} }
                JsonNode fileNode = root.path("file");
                String downloadUrl = fileNode.path("download_url").asText("");

                if (!downloadUrl.isEmpty()) {
                    log.info("[MiniMax] Got download URL for file_id={}: {}", fileId, downloadUrl);
                    return downloadUrl;
                } else {
                    log.warn("[MiniMax] files/retrieve returned empty download_url for file_id={}, response: {}",
                            fileId, response.getBody());
                    // 兜底：检查是否直接在顶层有 download_url
                    downloadUrl = root.path("download_url").asText("");
                    if (!downloadUrl.isEmpty()) return downloadUrl;
                }
            } else {
                log.warn("[MiniMax] files/retrieve failed: status={}, body={}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("[MiniMax] Failed to fetch download URL for file_id={}: {}", fileId, e.getMessage(), e);
        }

        return "";
    }

    /**
     * MiniMax 视频生成模型映射（官方文档对齐）
     *
     * 模型说明：
     *   - MiniMax-Hailuo-2.3: 文生视频 / 图生视频（主力模型）
     *   - MiniMax-Hailuo-02: 首尾帧生成视频（首尾帧专用）
     *   - S2V-01:           主体参考生成视频（人物一致性模型）
     */
    private static final String VIDEO_MODEL_HAILUO_23 = "MiniMax-Hailuo-2.3";
    private static final String VIDEO_MODEL_HAILUO_02 = "MiniMax-Hailuo-02";
    private static final String VIDEO_MODEL_S2V_01      = "S2V-01";

    /**
     * 根据模式获取推荐的视频模型
     * 如果用户显式传入 model 则优先使用，否则按官方推荐自动选择
     */
    private String resolveVideoModel(String mode, String userProvidedModel) {
        if (userProvidedModel != null && !userProvidedModel.isEmpty()) {
            return userProvidedModel;
        }
        switch (mode) {
            case "FIRST_LAST_FRAME": return VIDEO_MODEL_HAILUO_02;
            case "SUBJECT_REFERENCE": return VIDEO_MODEL_S2V_01;
            default:
                // TEXT_TO_VIDEO / IMAGE_TO_VIDEO / 其他
                return VIDEO_MODEL_HAILUO_23;
        }
    }

    /**
     * 视频生成多模式接口 —— 完全对齐 MiniMax 官方 API 规范
     *
     * 官方文档: https://platform.minimaxi.com/docs/guides/video-generation
     * 端点: POST /v1/video_generation
     *
     * 支持4种模式：
     * 1. TEXT_TO_VIDEO: 文生视频 - 仅需 prompt
     * 2. IMAGE_TO_VIDEO: 图生视频 - 需要 prompt + first_frame_image（起始帧图片URL）
     * 3. FIRST_LAST_FRAME: 首尾帧 - 需要 prompt + first_frame_image + last_frame_image
     * 4. SUBJECT_REFERENCE: 主体参考 - 需要 prompt + subject_reference（数组结构）
     *
     * ⚠️ 必填参数（全部模式通用）: model, prompt, duration, resolution
     *
     * @param mode 生成模式
     * @param prompt 视频描述文本（支持 [运镜] 指令控制镜头运动）
     * @param referenceImageUrl 图生视频的起始帧图片 URL
     * @param firstFrameUrl 首帧图片 URL（首尾帧/图生复用）
     * @param lastFrameUrl 尾帧图片 URL（仅首尾帧模式）
     * @param subjectImageUrl 主体参考图片 URL（人脸照片）
     * @param model 模型名称（为空时按模式自动选择最优模型）
     * @return "task:{taskId}" 异步任务标识；失败返回 ""
     */
    public String generateVideoMultiMode(
            String mode,
            String prompt,
            String referenceImageUrl,
            String firstFrameUrl,
            String lastFrameUrl,
            String subjectImageUrl,
            String model) {
        return generateVideoMultiMode(mode, prompt, referenceImageUrl, firstFrameUrl, lastFrameUrl, subjectImageUrl, model, 6, "768P");
    }

    /**
     * 多模式视频生成（带扩展参数）
     * 支持：TEXT_TO_VIDEO / IMAGE_TO_VIDEO / FIRST_LAST_FRAME / SUBJECT_REFERENCE
     */
    public String generateVideoMultiMode(
            String mode,
            String prompt,
            String referenceImageUrl,
            String firstFrameUrl,
            String lastFrameUrl,
            String subjectImageUrl,
            String model,
            Integer duration,
            String resolution) {
        if (!isConfigured()) throw new AiApiException(-1, "", "MiniMax 视频生成适配器未配置", "视频生成(多模式)");

        try {
            String url = getBaseUrl() + "/video_generation";

            // 按模式自动选模型（用户传了则覆盖）
            String useModel = resolveVideoModel(mode, model);

            // 参数校验与默认值
            int useDuration = (duration != null && (duration == 6 || duration == 10)) ? duration : 6;
            String useResolution = (resolution != null && !resolution.isEmpty()) ? resolution : "768P";

            // 根据模型能力校验参数
            if (useModel.contains("T2V-01") && useDuration == 10) {
                useDuration = 6; // T2V-01 不支持 10s
            }
            if (useDuration == 10 && "1080P".equals(useResolution)) {
                useResolution = "768P"; // 10s 视频不支持 1080P
            }

            // Prompt 处理：空值警告+降级
            String usePrompt = (prompt != null && !prompt.isEmpty())
                    ? prompt
                    : "[镜头缓慢推进] A short video clip with smooth camera motion and high quality visual effects.";

            if (prompt == null || prompt.isEmpty()) {
                log.warn("[MiniMax] Video generation called WITHOUT prompt! "
                        + "The result will be unpredictable. "
                        + "Frontend MUST build a rich prompt combining character/scene/action/mood.");
            }

            // ====== 构建请求体（严格对齐官方参数名）======
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", useModel);
            requestBody.put("prompt", usePrompt);
            requestBody.put("duration", useDuration);
            requestBody.put("resolution", useResolution);

            // 根据模式添加不同参数（⚠️ 参数名必须与官方文档完全一致）
            switch (mode) {
                case "IMAGE_TO_VIDEO": {
                    String imageUrl = (referenceImageUrl != null && !referenceImageUrl.isEmpty())
                            ? referenceImageUrl : firstFrameUrl;
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        requestBody.put("first_frame_image", imageUrl);
                        log.info("[MiniMax] Image-to-Video mode: model={}, imageUrl={}", useModel, imageUrl);
                    } else {
                        log.warn("[MiniMax] IMAGE_TO_VIDEO mode has no image, degrading to TEXT_TO_VIDEO");
                    }
                    break;
                }

                case "FIRST_LAST_FRAME": {
                    if (firstFrameUrl != null && !firstFrameUrl.isEmpty()
                            && lastFrameUrl != null && !lastFrameUrl.isEmpty()) {
                        requestBody.put("first_frame_image", firstFrameUrl);
                        requestBody.put("last_frame_image", lastFrameUrl);
                        log.info("[MiniMax] First-Last-Frame mode: model={} (Hailuo-02 recommended), frames provided",
                                useModel);
                    } else if (firstFrameUrl != null && !firstFrameUrl.isEmpty()) {
                        requestBody.put("first_frame_image", firstFrameUrl);
                        log.info("[MiniMax] FIRST_LAST_FRAME missing last_frame, degrading to Image-to-Video");
                    } else if (lastFrameUrl != null && !lastFrameUrl.isEmpty()) {
                        requestBody.put("first_frame_image", lastFrameUrl);
                        log.info("[MiniMax] FIRST_LAST_FRAME missing first_frame, using last_frame as start");
                    } else {
                        log.warn("[MiniMax] FIRST_LAST_FRAME mode has NO images, degrading to TEXT_TO_VIDEO");
                    }
                    break;
                }

                case "SUBJECT_REFERENCE": {
                    if (subjectImageUrl != null && !subjectImageUrl.isEmpty()) {
                        java.util.Map<String, Object> charRef = new java.util.HashMap<>();
                        charRef.put("type", "character");
                        charRef.put("image", java.util.Arrays.asList(subjectImageUrl));
                        requestBody.put("subject_reference", java.util.Arrays.asList(charRef));
                        log.info("[MiniMax] Subject-Reference mode: model={} (S2V-01 recommended), image provided",
                                useModel);
                    } else {
                        log.warn("[MiniMax] SUBJECT_REFERENCE mode has no face image, degrading to TEXT_TO_VIDEO");
                    }
                    break;
                }

                case "TEXT_TO_VIDEO":
                default:
                    log.info("[MiniMax] Text-to-Video mode: model={}, duration={}, resolution={}", useModel, useDuration, useResolution);
                    break;
            }

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            log.info("[MiniMax] Video generation request: mode={}, model={}, duration={}, resolution={}, promptLen={}",
                    mode, useModel, useDuration, useResolution, usePrompt.length());

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(bodyJson, authHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // ✅ 核心：使用 ensureSuccess 统一检查错误码
                // 这里会捕获 code=1008 insufficient balance 并抛出有意义的异常！
                ensureSuccess(root, "视频生成(多模式/" + mode + ")");

                String taskId = root.path("task_id").asText("");
                if (!taskId.isEmpty()) {
                    log.info("[MiniMax] Video task created: taskId={}", taskId);
                    return "task:" + taskId;
                } else {
                    log.warn("[MiniMax] Success response but no task_id! Body: {}", response.getBody());
                    throw new AiApiException(-1, "",
                            "MiniMax 视频任务创建成功但未返回 task_id，请稍后重试", "视频生成(多模式)");
                }
            } else {
                int httpStatus = response.getStatusCodeValue();
                throw new AiApiException(httpStatus,
                        response.getBody() != null ? response.getBody().substring(0, Math.min(500, response.getBody().length())) : "",
                        "MiniMax 视频生成请求失败（HTTP " + httpStatus + "）", "视频生成(多模式)");
            }
        } catch (AiApiException e) {
            throw e; // 直接透传
        } catch (Exception e) {
            log.error("[MiniMax] Video generation exception: {}", e.getMessage(), e);
            throw new AiApiException(-1, e.getMessage(),
                    "MiniMax 视频生成异常：" + e.getMessage() + "，请检查后端日志获取详细信息", "视频生成(多模式)");
        }
    }

}
