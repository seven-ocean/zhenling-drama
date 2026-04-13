package com.drama.service.adapter;

import com.drama.entity.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * OpenAI 适配器 - 支持文本/图片生成
 * 兼容所有OpenAI协议的API（包括阿里通义、DeepSeek等）
 */
@Slf4j
@Component
public class OpenAiAdapter implements AiAdapter {

    private static final String PROVIDER = "openai";

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
        log.info("OpenAI adapter initialized: model={}", config.getModel());
    }

    @Override
    public boolean isConfigured() {
        return config != null && config.getEnabled()
                && config.getApiKey() != null && !config.getApiKey().isEmpty();
    }

    @Override
    public String generateText(String prompt, String model) {
        if (!isConfigured()) {
            log.warn("OpenAI not configured");
            return "";
        }

        try {
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1") + "/chat/completions";
            
            // 构建请求体
            String requestBody = buildChatBody(prompt, model);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("OpenAI text generation: model={}, url={}", model, url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String result = choices.get(0).path("message").path("content").asText("");
                    log.debug("OpenAI text result length: {}", result.length());
                    return result;
                }
            }
            log.warn("OpenAI text generation failed: status={}", response.getStatusCode());
        } catch (Exception e) {
            log.error("OpenAI text generation error: {}", e.getMessage(), e);
        }
        return "";
    }

    @Override
    public String generateImage(String prompt, String model) {
        if (!isConfigured()) {
            log.warn("OpenAI not configured");
            return "";
        }

        try {
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1") + "/images/generations";
            
            String requestBody = buildImageBody(prompt, model);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("OpenAI image generation: model={}", model);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                if (data.isArray() && data.size() > 0) {
                    // 支持b64_json和url两种返回格式
                    String b64Json = data.get(0).path("b64_json").asText("");
                    String imageUrl = data.get(0).path("url").asText("");
                    
                    if (!b64Json.isEmpty()) {
                        // Base64格式，需要保存为文件（由调用方处理）
                        return "base64:" + b64Json.substring(0, Math.min(100, b64Json.length())) + "...";
                    }
                    return imageUrl;
                }
            }
            log.warn("OpenAI image generation failed: status={}", response.getStatusCode());
        } catch (Exception e) {
            log.error("OpenAI image generation error: {}", e.getMessage(), e);
        }
        return "";
    }

    @Override
    public String generateVideo(String imageUrl, String model) {
        if (!isConfigured()) {
            log.warn("OpenAI not configured");
            return "";
        }

        try {
            // 尝试调用兼容 OpenAI 协议的视频生成 API
            // 支持如通义万相、智谱等兼容 OpenAI 格式的视频 API
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1") + "/video/generations";

            // 构建请求体
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            jsonBuilder.append("\"model\": \"").append(model != null ? model : "sora-1.0-turbo").append("\", ");
            jsonBuilder.append("\"prompt\": \"Generate a video from this image\", ");
            jsonBuilder.append("\"image_url\": ").append(objectMapper.writeValueAsString(imageUrl));
            jsonBuilder.append("}");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(jsonBuilder.toString(), headers);

            log.info("OpenAI video generation: model={}", model);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                if (data.isArray() && data.size() > 0) {
                    // 可能返回视频URL或task_id
                    String videoUrl = data.get(0).path("url").asText("");
                    String taskId = data.get(0).path("id").asText("");

                    if (!videoUrl.isEmpty()) {
                        return videoUrl;
                    } else if (!taskId.isEmpty()) {
                        return "task:" + taskId;
                    }
                }
                // 也可能直接在根级别返回
                String outputUrl = root.path("output_url").asText(root.path("video_url").asText(""));
                if (!outputUrl.isEmpty()) return outputUrl;
            }
            log.warn("OpenAI video generation failed: status={}", response.getStatusCode());
        } catch (Exception e) {
            log.error("OpenAI video generation error: {} (Note: OpenAI Sora may not be available on your plan)", e.getMessage());
        }
        return "";
    }

    @Override
    public String pollVideoStatus(String taskId) {
        if (!isConfigured() || taskId == null || taskId.isEmpty()) return "status:failed";

        try {
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1")
                    + "/video/generations/" + taskId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String status = root.path("status").asText("");

                switch (status.toLowerCase()) {
                    case "completed":
                    case "succeeded":
                        return "completed:" + root.path("output_url")
                                .asText(root.path("video_url").asText(""));
                    case "failed":
                    case "error":
                        return "failed:" + root.path("error").path("message").asText("视频生成失败");
                    default:
                        return "status:" + status;
                }
            }
        } catch (Exception e) {
            log.error("Poll OpenAI video status failed: {}", e.getMessage());
        }
        return "status:unknown";
    }

    @Override
    public String generateTTS(String text, String voiceId, String model) {
        if (!isConfigured()) {
            log.warn("OpenAI not configured");
            return "";
        }

        try {
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1") + "/audio/speech";
            
            // TTS请求体
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            jsonBuilder.append("\"model\": \"").append(model != null ? model : "tts-1").append("\", ");
            jsonBuilder.append("\"input\": ").append(objectMapper.writeValueAsString(text)).append(", ");
            jsonBuilder.append("\"voice\": \"").append(voiceId != null ? voiceId : "alloy").append("\"");
            jsonBuilder.append("}");
            
            String requestBody = jsonBuilder.toString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("OpenAI TTS: voice={}, textLength={}", voiceId, text.length());
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 返回Base64音频数据
                byte[] audioData = response.getBody();
                String base64 = java.util.Base64.getEncoder().encodeToString(audioData);
                return "audio:mp3:base64," + base64;
            }
        } catch (Exception e) {
            log.error("OpenAI TTS error: {}", e.getMessage(), e);
        }
        return "";
    }

    @Override
    public boolean checkHealth() {
        if (!isConfigured()) return false;
        try {
            String url = (config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.openai.com/v1") + "/models";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(config.getApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("OpenAI health check failed: {}", e.getMessage());
            return false;
        }
    }

    private String buildChatBody(String prompt, String model) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"model\": \"").append(model != null ? model : "gpt-4o").append("\", ");
        builder.append("\"messages\": [{\"role\": \"user\", \"content\": ").append(objectMapper.writeValueAsString(prompt)).append("}], ");
        builder.append("\"temperature\": 0.7, \"max_tokens\": 4096");
        builder.append("}");
        return builder.toString();
    }

    private String buildImageBody(String prompt, String model) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"model\": \"").append(model != null ? model : "dall-e-3").append("\", ");
        builder.append("\"prompt\": ").append(objectMapper.writeValueAsString(prompt)).append(", ");
        builder.append("\"n\": 1, ");
        builder.append("\"size\": \"1024x1024\", ");
        builder.append("\"response_format\": \"url\"");
        builder.append("}");
        return builder.toString();
    }
}
