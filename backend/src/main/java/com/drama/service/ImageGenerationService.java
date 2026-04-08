package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Asset;
import com.drama.service.adapter.AiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 图片生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private final AiServiceFactory aiServiceFactory;
    private final AssetService assetService;

    /**
     * 生成角色图片
     */
    @Transactional
    public Asset generateCharacterImage(String dramaId, String characterId, String prompt, String provider, String model) {
        if (prompt == null || prompt.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提示词不能为空");
        }

        String actualProvider = provider != null ? provider : "openai";
        String actualModel = model != null ? model : "dall-e-3";

        log.info("Generating character image: provider={}, model={}", actualProvider, actualModel);

        try {
            // 调用AI生成图片
            String imageUrl = aiServiceFactory.generateImage(actualProvider, prompt, actualModel);

            // 保存为素材
            // 这里简化处理，实际应该下载图片到本地
            Asset asset = new Asset();
            asset.setId(IdUtils.randomId());
            asset.setDramaId(dramaId);
            asset.setType("image");
            asset.setFileUrl(imageUrl);
            asset.setSourceType("generated");
            asset.setExtraData(String.format("{\"characterId\":\"%s\",\"provider\":\"%s\",\"model\":\"%s\"}", 
                characterId, actualProvider, actualModel));
            asset.setCreatedAt(java.time.LocalDateTime.now());
            asset.setDeleted(0);

            return asset;
        } catch (Exception e) {
            log.error("Image generation failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成场景图片
     */
    @Transactional
    public Asset generateSceneImage(String dramaId, String sceneId, String prompt, String provider, String model) {
        if (prompt == null || prompt.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提示词不能为空");
        }

        String actualProvider = provider != null ? provider : "openai";
        String actualModel = model != null ? model : "dall-e-3";

        log.info("Generating scene image: provider={}, model={}", actualProvider, actualModel);

        try {
            String imageUrl = aiServiceFactory.generateImage(actualProvider, prompt, actualModel);

            Asset asset = new Asset();
            asset.setId(IdUtils.randomId());
            asset.setDramaId(dramaId);
            asset.setType("image");
            asset.setFileUrl(imageUrl);
            asset.setSourceType("generated");
            asset.setExtraData(String.format("{\"sceneId\":\"%s\",\"provider\":\"%s\",\"model\":\"%s\"}",
                sceneId, actualProvider, actualModel));
            asset.setCreatedAt(java.time.LocalDateTime.now());
            asset.setDeleted(0);

            return asset;
        } catch (Exception e) {
            log.error("Image generation failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成宫格图
     */
    @Transactional
    public Asset generateGridImage(String dramaId, String prompt, String provider, String model) {
        if (prompt == null || prompt.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提示词不能为空");
        }

        String actualProvider = provider != null ? provider : "openai";
        String actualModel = model != null ? model : "dall-e-3";

        log.info("Generating grid image: provider={}, model={}", actualProvider, actualModel);

        try {
            String imageUrl = aiServiceFactory.generateImage(actualProvider, prompt, actualModel);

            Asset asset = new Asset();
            asset.setId(IdUtils.randomId());
            asset.setDramaId(dramaId);
            asset.setType("image");
            asset.setFileUrl(imageUrl);
            asset.setSourceType("generated");
            asset.setExtraData(String.format("{\"type\":\"grid\",\"provider\":\"%s\",\"model\":\"%s\"}",
                actualProvider, actualModel));
            asset.setCreatedAt(java.time.LocalDateTime.now());
            asset.setDeleted(0);

            return asset;
        } catch (Exception e) {
            log.error("Image generation failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败: " + e.getMessage());
        }
    }
}