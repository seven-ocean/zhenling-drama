package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Vision 服务 - AI 图片分析
 * 用于从角色图片中提取外观描述
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisionService {

    private final AiServiceFactory aiServiceFactory;

    /**
     * 从角色图片提取外观提示词
     *
     * @param imageUrl 角色图片 URL
     * @param characterName 角色名称（可选，用于增强描述）
     * @return 外观描述文本（英文，适合 AI 图片生成）
     */
    public String extractAppearancePrompt(String imageUrl, String characterName) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色图片 URL 不能为空");
        }

        String prompt = buildVisionPrompt(characterName);

        log.info("[Vision] Extracting appearance prompt for character: {}, imageUrl: {}",
                characterName, imageUrl);

        try {
            String description = aiServiceFactory.analyzeImage(prompt, imageUrl);
            if (description == null || description.isBlank()) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "图片分析返回为空");
            }
            log.info("[Vision] Extracted description: {}", description.substring(0, Math.min(100, description.length())));
            return description;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Vision] Failed to extract appearance prompt: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "提取外观描述失败: " + e.getMessage());
        }
    }

    /**
     * 构建 Vision 分析提示词（控制在 500 字符以内）
     */
    private String buildVisionPrompt(String characterName) {
        StringBuilder prompt = new StringBuilder();
        if (characterName != null && !characterName.isBlank()) {
            prompt.append("Describe character '").append(characterName).append("': ");
        } else {
            prompt.append("Describe this character: ");
        }
        prompt.append("age/gender, hairstyle & color, facial features, ");
        prompt.append("clothing style & colors, accessories, body build, ");
        prompt.append("style & mood. ");
        prompt.append("Reply in English, concise, for AI image generation. ");
        prompt.append("Ex: 'Young female warrior, mid-20s, long silver hair, gold-trimmed armor, athletic, confident, fantasy'");
        String result = prompt.toString();
        if (result.length() > 500) {
            result = result.substring(0, 495) + "...";
        }
        return result;
    }
}
