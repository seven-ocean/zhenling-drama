package com.drama.service.adapter;

import com.drama.entity.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * MiniMax 适配器 - 支持文本/图片/视频/TTS生成
 */
@Slf4j
@Component
public class MiniMaxAdapter implements AiAdapter {

    private static final String PROVIDER = "minimax";
    private static final String DEFAULT_BASE_URL = "https://api.minimax.chat/v1";

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
        log.info("MiniMax adapter initialized: model={}", config.getModel());
    }

    @Override
    public boolean isConfigured() {
        return config != null && config.getEnabled()
                && config.getApiKey() != null && !config.getApiKey().isEmpty();
    }

    @Override
    public String generateText(String prompt, String model) {
        if (!isConfigured()) {
            log.warn("MiniMax not configured");
            return "";
        }

        try {
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL) + "/text/chatcompletion_v2";
            
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            jsonBuilder.append("\"model\": \"").append(model != null ? model : "abab6.5-chat").append("\", ");
            jsonBuilder.append("\"messages\": [{\"role\": \"user\", \"content\": ").append(objectMapper.writeValueAsString(prompt)).append("}], ");
            jsonBuilder.append("\"temperature\": 0.7, \"tokens_to_generate\": 4096");
            jsonBuilder.append("}");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(jsonBuilder.toString(), headers);
            
            log.info("MiniMax text generation: model={}", model);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String result = root.path("choice").asText("");
                if (!result.isEmpty()) {
                    // 尝试解析嵌套结构
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        result = choices.get(0).path("text").asText(result);
                        result = choices.get(0).path("message").path("content").asText(result);
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.error("MiniMax text generation error: {}", e.getMessage(), e);
        }
        return "";
    }

    @Override
    public String generateImage(String prompt, String model) {
        if (!isConfigured()) return "";

        try {
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL) + "/text/image_generation_v2";
            
            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"model\": \"").append(model != null ? model : "image-01").append("\", ");
            body.append("\"prompt\": ").append(objectMapper.writeValueAsString(prompt)).append(", ");
            body.append("\"aspect_ratio\": \"16:9\"");
            body.append("}");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            log.info("MiniMax image generation: model={}", model);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), headers), String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                // MiniMax图片返回格式
                String imageUrl = root.path("image_path").asText(root.path("data").path(0).path("url").asText(""));
                if (!imageUrl.isEmpty()) {
                    return imageUrl.startsWith("http") ? imageUrl :
                            (config.getBaseUrl() != null ? config.getBaseUrl().replace("/v1", "") : "https://api.minimax.chat") + imageUrl;
                }
            }
        } catch (Exception e) {
            log.error("MiniMax image generation error: {}", e.getMessage(), e);
        }
        return "";
    }

    @Override
    public String generateVideo(String imageUrl, String model) {
        if (!isConfigured()) return "";

        try {
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL) + "/video_generation";
            
            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"model\": \"").append(model != null ? model : "video-01").append("\", ");
            body.append("\"prompt\": \"Generate a video from this image\", ");
            body.append("\"reference_image_url\": ").append(objectMapper.writeValueAsString(imageUrl));
            body.append("}");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            log.info("MiniMax video generation: model={}", model);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), headers), String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                // 可能返回task_id或video_url（异步任务）
                String taskId = root.path("base_resp").path("task_id").asText(root.path("task_id").asText(""));
                String videoUrl = root.path("video_url").asText("");
                
                if (!videoUrl.isEmpty()) {
                    return videoUrl;
                } else if (!taskId.isEmpty()) {
                    return "task:" + taskId;  // 异步任务，需要轮询
                }
            }
        } catch (Exception e) {
            log.error("MiniMax video generation error: {}", e.getMessage(), e);
        }
        return "";
    }

    @Override
    public String generateTTS(String text, String voiceId, String model) {
        if (!isConfigured()) return "";

        try {
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL) + "/t2a_v2";
            
            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"model\": \"").append(model != null ? model : "speech-02-hd").append("\", ");
            body.append("\"text\": ").append(objectMapper.writeValueAsString(text)).append(", ");
            body.append("\"voice_setting\": {\"voice_id\": \"").append(voiceId != null ? voiceId : "female-tianmei").append("\"}, ");
            body.append("\"audio_sample_rate\": 32000");
            body.append("}");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            log.info("MiniMax TTS: voice={}, textLength={}", voiceId, text.length());
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), headers), byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] audioData = response.getBody();
                String base64 = java.util.Base64.getEncoder().encodeToString(audioData);
                return "audio:mp3:base64," + base64;
            }
        } catch (Exception e) {
            log.error("MiniMax TTS error: {}", e.getMessage(), e);
        }
        return "";
    }

    @Override
    public boolean checkHealth() {
        if (!isConfigured()) return false;
        try {
            // 简单检查：调用一个简单的文本请求
            String testResult = generateText("hi", "abab6.5-chat");
            return !testResult.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String pollVideoStatus(String taskId) {
        if (!isConfigured() || taskId == null || taskId.isEmpty()) return "status:failed";

        try {
            // MiniMax 查询视频任务状态的 API 端点
            String url = (config.getBaseUrl() != null ? config.getBaseUrl().replace("/v1", "") : "https://api.minimax.chat")
                    + "/v1/video_generation/query?task_id=" + taskId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + config.getApiKey());

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // 解析 base_resp 中的 status
                String status = root.path("base_resp").path("status_code").asText("");
                if ("0".equals(status)) {
                    // 任务完成，获取视频URL
                    String videoUrl = root.path("video_url").asText("");
                    if (!videoUrl.isEmpty()) {
                        return "completed:" + videoUrl;
                    }
                }

                // 检查任务状态
                String taskStatus = root.path("base_resp").path("status").asText("");
                if (taskStatus.isEmpty()) {
                    // 尝试其他字段名
                    taskStatus = root.path("status").asText("processing");
                }

                switch (taskStatus.toLowerCase()) {
                    case "completed":
                    case "done":
                        return "completed:" + root.path("video_url").asText("");
                    case "failed":
                    case "error":
                        return "failed:" + root.path("base_resp").path("status_msg").asText("视频生成失败");
                    default:
                        return "status:processing";
                }
            }
        } catch (Exception e) {
            log.error("Poll video status failed for task {}: {}", taskId, e.getMessage());
        }
        return "status:unknown";
    }

}
