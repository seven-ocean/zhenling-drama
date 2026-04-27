package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Asset;
import com.drama.entity.Character;
import com.drama.entity.Scene;
import com.drama.entity.Storyboard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 分镜参考图服务
 * 在正式视频合成前，为分镜生成"角色×场景"预览图
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageReferenceService {

    private final ImageGenerationService imageGenerationService;
    private final StoryboardService storyboardService;
    private final CharacterService characterService;
    private final SceneService sceneService;
    private final AssetService assetService;

    /**
     * 生成参考图
     *
     * @param storyboardId 分镜ID
     * @param forceRegenerate 是否强制重新生成
     * @return 生成的参考图 Asset
     */
    @Transactional
    public Asset generateReference(String storyboardId, boolean forceRegenerate) {
        // 1. 查询分镜
        Storyboard sb = storyboardService.getById(storyboardId);
        if (sb == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分镜不存在");
        }

        // 2. 查询已有的参考图（未删且未强制刷新）
        if (!forceRegenerate) {
            Asset existing = findExistingReference(storyboardId);
            if (existing != null) {
                log.info("[RefImg] Using existing reference: id={}, storyboardId={}", existing.getId(), storyboardId);
                return existing;
            }
        } else {
            // 物理删除旧参考图记录（保留文件，节省存储）
            deleteReferenceRecord(storyboardId);
        }

        // 3. 获取角色和场景信息
        Character character = null;
        Scene scene = null;
        if (StringUtils.hasText(sb.getCharacterId())) {
            character = characterService.getById(sb.getCharacterId());
        }
        if (StringUtils.hasText(sb.getSceneId())) {
            scene = sceneService.getById(sb.getSceneId());
        }

        // 4. 构造复合提示词
        String prompt = buildReferencePrompt(sb, character, scene);
        log.info("[RefImg] Generated prompt for storyboard {}: {}", storyboardId, prompt.substring(0, Math.min(200, prompt.length())));

        // 5. 调用图片生成服务
        // 复用已有 archiveAiImage 逻辑，归档到 assets 表
        Asset asset = archiveReferenceImage(sb.getDramaId(), storyboardId, prompt, sb.getId());

        log.info("[RefImg] Reference image generated: id={}, storyboardId={}, url={}",
                asset.getId(), storyboardId, asset.getFileUrl());
        return asset;
    }

    /**
     * 获取分镜的参考图
     *
     * @param storyboardId 分镜ID
     * @return 参考图 Asset，若不存在返回 null
     */
    public Asset getReference(String storyboardId) {
        return findExistingReference(storyboardId);
    }

    /**
     * 删除参考图
     *
     * @param storyboardId 分镜ID
     */
    @Transactional
    public void deleteReference(String storyboardId) {
        deleteReferenceRecord(storyboardId);
        log.info("[RefImg] Reference image deleted for storyboardId={}", storyboardId);
    }

    // ==================== 私有方法 ====================

    /**
     * 查找已有的参考图记录
     */
    private Asset findExistingReference(String storyboardId) {
        // 使用 MySQL JSON 函数精确查询 extra_data 中的 storyboardId
        var q = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Asset>();
        q.apply("JSON_EXTRACT(extra_data, '$.type') = 'reference_image'")
         .apply("JSON_EXTRACT(extra_data, '$.storyboardId') = {0}", storyboardId)
         .orderByDesc(Asset::getCreatedAt)
         .last("LIMIT 1");
        var list = assetService.list(q);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * 删除参考图记录
     */
    private void deleteReferenceRecord(String storyboardId) {
        var q = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Asset>();
        q.apply("JSON_EXTRACT(extra_data, '$.type') = 'reference_image'")
         .apply("JSON_EXTRACT(extra_data, '$.storyboardId') = {0}", storyboardId);
        var list = assetService.list(q);
        if (list != null && !list.isEmpty()) {
            for (Asset a : list) {
                assetService.removeById(a.getId());
            }
        }
    }

    /**
     * 构造复合参考图提示词
     * 格式: "In {场景描述}, {角色外观描述}, {动作描述}, cinematic lighting, film still quality, 16:9"
     */
    private String buildReferencePrompt(Storyboard sb, Character character, Scene scene) {
        StringBuilder prompt = new StringBuilder();

        // 场景
        if (scene != null && StringUtils.hasText(scene.getPrompt())) {
            prompt.append("In ").append(scene.getPrompt());
        } else if (scene != null && StringUtils.hasText(scene.getName())) {
            prompt.append("In a scene titled ").append(scene.getName());
        } else {
            prompt.append("In a cinematic scene");
        }

        // 角色
        if (character != null) {
            if (StringUtils.hasText(character.getAppearancePrompt())) {
                prompt.append(", ").append(character.getAppearancePrompt());
            } else if (StringUtils.hasText(character.getName())) {
                prompt.append(", a character named ").append(character.getName());
            }
        }

        // 动作描述（来自分镜的action）
        if (StringUtils.hasText(sb.getAction())) {
            prompt.append(", ").append(sb.getAction());
        }

        // 台词（可作为画面补充）
        if (StringUtils.hasText(sb.getDialogue())) {
            prompt.append(". The character says: \"").append(sb.getDialogue()).append("\"");
        }

        // 镜头类型（增强构图）
        if (StringUtils.hasText(sb.getShotType())) {
            String shotTypePrompt = switch (sb.getShotType()) {
                case "wide" -> ", establishing wide shot";
                case "medium" -> ", medium shot";
                case "close-up" -> ", close-up shot";
                case "extreme-close-up" -> ", extreme close-up";
                default -> "";
            };
            prompt.append(shotTypePrompt);
        }

        // 运镜（可选）
        if (StringUtils.hasText(sb.getShotDirection())) {
            prompt.append(". Camera movement: ").append(sb.getShotDirection());
        }

        // 固定后缀：电影感
        prompt.append(". Cinematic lighting, film still quality, 16:9 aspect ratio, anime style, detailed");

        return prompt.toString();
    }

    /**
     * 归档参考图到 assets 表
     * 复用 imageGenerationService 的 AI 生成 + 归档能力
     */
    private Asset archiveReferenceImage(String dramaId, String storyboardId, String prompt, String shotId) {
        // 调用 AI 图片生成（走 AiServiceFactory 获取供应商配置）
        String imageUrl = imageGenerationService.generateImageDirect(prompt);

        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "参考图生成失败：AI返回为空");
        }

        // 归档到本地/OSS
        String filename = "ref_" + storyboardId + "_" + System.currentTimeMillis() + ".png";
        String extraData = String.format(
                "{\"type\":\"reference_image\",\"storyboardId\":\"%s\",\"shotId\":\"%s\",\"prompt\":\"%s\"}",
                storyboardId, shotId,
                prompt.length() > 500 ? prompt.substring(0, 500) : prompt
        );

        Asset asset = imageGenerationService.archiveImage(dramaId, imageUrl, filename, "image/png", extraData);
        return asset;
    }
}
