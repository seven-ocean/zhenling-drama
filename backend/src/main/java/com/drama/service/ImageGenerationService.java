package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.config.OssProperties;
import com.drama.entity.Asset;
import com.drama.service.adapter.AiApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final FileStorageService fileStorageService;
    private final OssProperties ossProps;
    private final StoryboardService storyboardService;

    // 用于下载AI返回的临时图片
    private final RestTemplate downloadRestTemplate = new RestTemplate();

    /**
     * 生成角色图片并保存为素材
     */
    @Transactional
    public Asset generateCharacterImage(String dramaId, String characterId, String prompt,
                                         String provider, String model) {
        if (prompt == null || prompt.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提示词不能为空");
        }

        String actualProvider = provider != null ? provider : null;
        String actualModel = model;

        log.info("Generating character image: provider={}, model={}", actualProvider, actualModel);

        try {
            // 调用AI生成图片
            String imageUrl = aiServiceFactory.generateImage(actualProvider, prompt, actualModel);

            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new BusinessException(ResultCode.SERVER_ERROR,
                        "图片生成失败：AI返回为空，请检查图片生成配置");
            }

            // 归档AI生成的临时图片到本地/OSS存储（避免链接过期）
            Asset asset = archiveAiImage(dramaId, imageUrl,
                    "character_" + characterId + ".png",
                    "image/png",
                    String.format("{\"type\":\"character\",\"characterId\":\"%s\",\"provider\":\"%s\",\"model\":\"%s\"}",
                            characterId, actualProvider != null ? actualProvider : "auto", actualModel));

            // 更新所有关联该角色的分镜记录的 characterImageUrl
            updateStoryboardCharacterImage(dramaId, characterId, asset.getFileUrl());

            log.info("Generated character image: {} for character {}", asset.getId(), characterId);
            return asset;
        } catch (BusinessException e) {
            throw e;
        } catch (AiApiException e) {
            log.error("AI API error (character image): [code={}] {}", e.getVendorCode(), e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, e.getMessage());
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
        String actualModel = model;

        log.info("Generating scene image: dramaId={}, sceneId={}, provider={}, model={}", 
                dramaId, sceneId, actualProvider, actualModel);

        try {
            String imageUrl = aiServiceFactory.generateImage(actualProvider, prompt, actualModel);

            if (imageUrl == null || imageUrl.isEmpty()) {
                log.error("[SceneImage] AI returned empty URL. provider={}, model={}, prompt={}", 
                        actualProvider, actualModel, prompt.substring(0, Math.min(100, prompt.length())));
                throw new BusinessException(ResultCode.SERVER_ERROR, 
                        "图片生成失败：AI返回为空，请检查AI配置是否正确，或查看后端日志获取详细信息");
            }

            // 归档AI生成的临时图片到本地/OSS存储（避免链接过期）
            Asset asset = archiveAiImage(dramaId, imageUrl,
                    "scene_" + sceneId + ".png",
                    "image/png",
                    String.format("{\"type\":\"scene\",\"sceneId\":\"%s\",\"provider\":\"%s\",\"model\":\"%s\"}",
                            sceneId, actualProvider != null ? actualProvider : "auto", actualModel));

            // 更新所有关联该场景的分镜记录的 sceneImageUrl
            updateStoryboardSceneImage(dramaId, sceneId, asset.getFileUrl());

            log.info("Generated scene image: {} for scene {}", asset.getId(), sceneId);
            return asset;
        } catch (BusinessException e) {
            throw e;
        } catch (AiApiException e) {
            log.error("AI API error (scene image): [code={}] {}", e.getVendorCode(), e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, e.getMessage());
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
        String actualModel = model;

        log.info("Generating grid image: provider={}, model={}", actualProvider, actualModel);

        try {
            String imageUrl = aiServiceFactory.generateImage(actualProvider, prompt, actualModel);

            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败：AI返回为空");
            }

            // 归档AI生成的临时图片到本地/OSS存储（避免链接过期）
            Asset asset = archiveAiImage(dramaId, imageUrl,
                    "grid_" + System.currentTimeMillis() + ".png",
                    "image/png",
                    String.format("{\"type\":\"grid\",\"provider\":\"%s\",\"model\":\"%s\"}",
                            actualProvider != null ? actualProvider : "auto", actualModel));

            log.info("Generated grid image: {}", asset.getId());
            return asset;
        } catch (BusinessException e) {
            throw e;
        } catch (AiApiException e) {
            log.error("AI API error (grid image): [code={}] {}", e.getVendorCode(), e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("Image generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片生成失败: " + e.getMessage());
        }
    }

    /**
     * 归档AI生成的临时图片到本地存储或OSS
     * 下载AI返回的URL → 转为MultipartFile → 通过FileStorageService上传到用户配置的存储位置
     *
     * @param dramaId    剧集ID
     * @param tempUrl    AI返回的临时图片URL
     * @param filename   目标文件名
     * @param mimeType   MIME类型
     * @param extraData  额外元数据JSON
     * @return 归档后的Asset记录（fileUrl指向本地/OSS的永久地址）
     */
    private Asset archiveAiImage(String dramaId, String tempUrl, String filename,
                                   String mimeType, String extraData) {
        try {
            log.info("[Archive] Downloading AI image from: {} (dramaId={})", tempUrl, dramaId);

            // 1. 下载AI生成的临时图片
            // 使用URI处理带签名的URL，避免URL编码问题导致403签名不匹配
            java.net.URI uri = java.net.URI.create(tempUrl);
            ResponseEntity<byte[]> response = downloadRestTemplate.getForEntity(uri, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[Archive] Failed to download from {}, status={}", tempUrl, response.getStatusCode());
                return saveAiUrlFallback(dramaId, tempUrl, filename, mimeType, extraData);
            }

            byte[] imageData = response.getBody();
            log.info("[Archive] Downloaded {} bytes from {}", imageData.length, tempUrl);

            // 2. 通过 FileStorageService.uploadBytes 上传到配置的存储（本地 or OSS）
            //    自动处理 OSS 上传 + ACL 设置 + 本地存储
            Asset archivedAsset = fileStorageService.uploadBytes(
                    imageData, filename, dramaId, "image", mimeType);

            // 3. 补充 AI 相关元数据
            archivedAsset.setSourceType("ai_archived");
            archivedAsset.setExtraData(extraData);
            assetService.updateById(archivedAsset);

            log.info("[Archive] Successfully archived AI image → id={}, url={}, sourceType={}",
                    archivedAsset.getId(), archivedAsset.getFileUrl(), archivedAsset.getSourceType());

            return archivedAsset;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Archive] Failed to archive AI image, using fallback URL: {}", e.getMessage());
            return saveAiUrlFallback(dramaId, tempUrl, filename, mimeType, extraData);
        }
    }

    /**
     * 降级方案：归档失败时直接保存AI返回的URL（可能过期）
     */
    private Asset saveAiUrlFallback(String dramaId, String tempUrl, String filename,
                                      String mimeType, String extraData) {
        Asset asset = new Asset();
        asset.setId(IdUtils.randomId());
        asset.setDramaId(dramaId);
        asset.setType("image");
        asset.setFilename(filename);
        asset.setFileUrl(tempUrl);
        asset.setFileSize(0L);
        asset.setMimeType(mimeType);
        asset.setSourceType("ai_generated_fallback");
        asset.setExtraData(extraData);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setDeleted(0);
        assetService.save(asset);
        log.warn("[Fallback] Saved AI image as temporary URL: {} -> id={}", tempUrl, asset.getId());
        return asset;
    }

    /**
     * 更新关联该角色的所有分镜记录的 characterImageUrl
     */
    private void updateStoryboardCharacterImage(String dramaId, String characterId, String imageUrl) {
        try {
            storyboardService.updateCharacterImageUrl(dramaId, characterId, imageUrl);
            log.info("[Storyboard] Updated character image URL for character {} in drama {}", characterId, dramaId);
        } catch (Exception e) {
            log.warn("[Storyboard] Failed to update character image URL for character {}: {}", characterId, e.getMessage());
            // 不抛出异常，避免影响图片生成的主流程
        }
    }

    /**
     * 更新关联该场景的所有分镜记录的 sceneImageUrl
     */
    private void updateStoryboardSceneImage(String dramaId, String sceneId, String imageUrl) {
        try {
            storyboardService.updateSceneImageUrl(dramaId, sceneId, imageUrl);
            log.info("[Storyboard] Updated scene image URL for scene {} in drama {}", sceneId, dramaId);
        } catch (Exception e) {
            log.warn("[Storyboard] Failed to update scene image URL for scene {}: {}", sceneId, e.getMessage());
            // 不抛出异常，避免影响图片生成的主流程
        }
    }
}
