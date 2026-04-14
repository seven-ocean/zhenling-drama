package com.drama.service.adapter;

import com.drama.entity.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private AiConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            return "";
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
                // OpenAI 兼容格式：choices[0].message.content
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String text = choices.get(0).path("message").path("content").asText("");
                    if (!text.isEmpty()) return text;
                }
                // 兜底：尝试直接取 content 字段
                return root.path("content").asText("");
            }
        } catch (Exception e) {
            log.error("[MiniMax] Text generation error: {}", e.getMessage(), e);
        }
        return "";
    }

    // ========== 图片生成 ==========

    @Override
    public String generateImage(String prompt, String model) {
        if (!isConfigured()) return "";

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
            body.append("\"n\": 1");
            body.append("}");

            log.info("[MiniMax] Image generation: model={}", useModel);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), authHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                if (!isSuccess(root)) {
                    log.warn("[MiniMax] Image gen failed: {}",
                            root.path("base_resp").path("status_msg").asText("unknown"));
                    return "";
                }

                // 官方返回格式：{ "data": { "image_urls": ["url1", ...] }, "base_resp": {...} }
                JsonNode data = root.path("data");
                JsonNode imageUrls = data.path("image_urls");
                if (imageUrls.isArray() && imageUrls.size() > 0) {
                    return imageUrls.get(0).asText("");
                }
            }
        } catch (Exception e) {
            log.error("[MiniMax] Image generation error: {}", e.getMessage(), e);
        }
        return "";
    }

    // ========== 视频生成（异步任务） ==========

    @Override
    public String generateVideo(String imageUrl, String model) {
        if (!isConfigured()) return "";

        try {
            // POST /v1/video_creation  （文生视频/图生视频共用）
            // 如果 imageUrl 非空，作为 reference_image_url 使用；否则纯文本 prompt
            String url = getBaseUrl() + "/video_generation";
            String useModel = (model != null) ? model : DEFAULT_VIDEO_MODEL;

            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"model\": \"").append(useModel).append("\", ");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 图生视频模式
                body.append("\"prompt\": \"Generate a video from this image\", ");
                body.append("\"reference_image_url\": ").append(objectMapper.writeValueAsString(imageUrl));
            } else {
                // 文生视频模式（需要外部传入 prompt，这里用占位）
                body.append("\"prompt\": \"Generate a short video clip\"");
            }
            body.append(", \"duration\": 6");
            body.append("}");

            log.info("[MiniMax] Video generation: model={}", useModel);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), authHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                if (isSuccess(root)) {
                    // 返回 task_id 用于后续轮询
                    String taskId = root.path("task_id").asText("");
                    if (!taskId.isEmpty()) {
                        return "task:" + taskId;
                    }
                } else {
                    log.warn("[MiniMax] Video creation failed: {}",
                            root.path("base_resp").path("status_msg").asText(""));
                }
            }
        } catch (Exception e) {
            log.error("[MiniMax] Video generation error: {}", e.getMessage(), e);
        }
        return "";
    }

    // ========== TTS 语音合成 ==========

    @Override
    public String generateTTS(String text, String voiceId, String model) {
        if (!isConfigured()) return "";

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

                if (isSuccess(root)) {
                    // 官方返回格式：{ "data": { "audio": "<hex或url>", "status": 2 }, ... }
                    // output_format=url 时 audio 字段直接是下载链接（24小时有效）
                    // output_format=hex 时 audio 是十六进制编码的音频数据
                    String audioData = root.path("data").path("audio").asText("");

                    if (!audioData.isEmpty()) {
                        if (audioData.startsWith("http")) {
                            // 直接返回音频下载URL
                            return audioData;
                        } else {
                            // Hex 编码数据 → 转为 base64 给前端使用
                            return "audio:mp3:hex," + audioData;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[MiniMax] TTS error: {}", e.getMessage(), e);
        }
        return "";
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
                    case "COMPLETED":
                        String fileId = root.path("file_id").asText("");
                        if (!fileId.isEmpty()) {
                            // 成功后 file_id 可用于获取下载地址
                            // 注意：实际下载需调用 GET /file/download/{file_id}
                            return "completed:" + fileId;
                        }
                        // 兜底：有些情况可能直接返回 video_url
                        String videoUrl = root.path("video_url").asText("");
                        if (!videoUrl.isEmpty()) {
                            return "completed:" + videoUrl;
                        }
                        return "completed";

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

}
