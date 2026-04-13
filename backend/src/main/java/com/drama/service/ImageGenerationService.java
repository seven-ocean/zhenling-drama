package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Asset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 图片生成服务 - 调用AI生成图片并保存为素材
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private final AiServiceFactory aiServiceFactory;
    private final AssetService assetService;

    /**
     * 生成角色图片并保存为素材
     */
    @Transactional
    public Asset generateCharacterImage(String dramaId, String characterId, String prompt,
                                         String provider, String model) {
        if (prompt == null || prompt.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提示词不能为空");
        }

        String actualProvider = provider != null ? provider : null;  // null = auto-select
        String actualModel = model != null ? model : "dall-e-3";

        log.info("Generating character image: provider={}, model={}", actualProvider, actualModel);

        try {
            // 调用AI生成图片
            String imageUrl = aiServiceFactory.generateImage(actualProvider, prompt, actualModel);

            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new BusinessException(ResultCode.SERVER_ERROR,
                        "图片生成失败：AI返回为空，请检查图片生成配置");
            }

            // 保存为素材记录到数据库
            Asset asset = new Asset();
            asset.setId(IdUtils.randomId());
            asset.setDramaId(dramaId);
            asset.setType("image");
            asset.setFilename("character_" + characterId + ".png");
            asset.setFileUrl(imageUrl);
            asset.setFileSize(0L);  // AI生成的图片，暂时无法获取大小
            asset.setMimeType("image/png");
            asset.setSourceType("ai_generated");
            asset.setExtraData(String.format(
                    "{\"type\":\"character\",\"characterId\":\"%s\",\"provider\":\"%s\",\"model\":\"%s\"}",
                    characterId, actualProvider != null ? actualProvider : "auto", actualModel));
            asset.setCreatedAt(LocalDateTime.now());
            asset.setDeleted(0);

            assetService.save(asset);
            log.info("Generated character image: {} for character {}", asset.getId(), characterId);
            
            return asset;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Image generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成场景图片并保存为素材
     */
    @Transactional
    public Asset generateSceneImage(String dramaId, String sceneId, String prompt,
                                      String provider, String model) {
        if (prompt == null || prompt.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提示词不能为空");
        }

        String actualProvider = provider != null ? provider : null;
        String actualModel = model != null ? model : "dall-e-3";

        log.info("Generating scene image: provider={}, model={}", actualProvider, actualModel);

        try {
            String imageUrl = aiServiceFactory.generateImage(actualProvider, prompt, actualModel);

            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败：AI返回为空");
            }

            Asset asset = new Asset();
            asset.setId(IdUtils.randomId());
            asset.setDramaId(dramaId);
            asset.setType("image");
            asset.setFilename("scene_" + sceneId + ".png");
            asset.setFileUrl(imageUrl);
            asset.setMimeType("image/png");
            asset.setSourceType("ai_generated");
            asset.setExtraData(String.format(
                    "{\"type\":\"scene\",\"sceneId\":\"%s\",\"provider\":\"%s\",\"model\":\"%s\"}",
                    sceneId, actualProvider != null ? actualProvider : "auto", actualModel));
            asset.setCreatedAt(LocalDateTime.now());
            asset.setDeleted(0);

            assetService.save(asset);
            log.info("Generated scene image: {} for scene {}", asset.getId(), sceneId);

            return asset;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Image generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成宫格图并保存为素材
     */
    @Transactional
    public Asset generateGridImage(String dramaId, String prompt, String provider, String model) {
        if (prompt == null || prompt.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提示词不能为空");
        }

        String actualProvider = provider != null ? provider : null;
        String actualModel = model != null ? model : "dall-e-3";

        log.info("Generating grid image: provider={}, model={}", actualProvider, actualModel);

        try {
            String imageUrl = aiServiceFactory.generateImage(actualProvider, prompt, actualModel);

            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败：AI返回为空");
            }

            Asset asset = new Asset();
            asset.setId(IdUtils.randomId());
            asset.setDramaId(dramaId);
            asset.setType("image");
            asset.setFilename("grid_" + System.currentTimeMillis() + ".png");
            asset.setFileUrl(imageUrl);
            asset.setMimeType("image/png");
            asset.setSourceType("ai_generated");
            asset.setExtraData(String.format(
                    "{\"type\":\"grid\",\"provider\":\"%s\",\"model\":\"%s\"}",
                    actualProvider != null ? actualProvider : "auto", actualModel));
            asset.setCreatedAt(LocalDateTime.now());
            asset.setDeleted(0);

            assetService.save(asset);
            log.info("Generated grid image: {}", asset.getId());

            return asset;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Image generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败: " + e.getMessage());
        }
    }
}
