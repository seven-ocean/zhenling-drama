package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.ResultCode;
import com.drama.dto.StoryboardRefStatus;
import com.drama.entity.Asset;
import com.drama.entity.Character;
import com.drama.entity.Scene;
import com.drama.entity.Storyboard;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        // 3. 收集所有角色图 URL（支持多角色：characterIds JSON数组 + 兼容 characterId）
        List<String> characterImageUrls = new ArrayList<>();
        List<Character> characters = new ArrayList<>();
        // 新字段：characterIds（JSON 数组，如 ["id1","id2"]）
        if (StringUtils.hasText(sb.getCharacterIds())) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.List<String> ids = mapper.readValue(sb.getCharacterIds(), java.util.List.class);
                for (String id : ids) {
                    if (StringUtils.hasText(id)) {
                        Character ch = characterService.getById(id);
                        if (ch != null && StringUtils.hasText(ch.getImageUrl())) {
                            characterImageUrls.add(ch.getImageUrl());
                            characters.add(ch);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[RefImg] Failed to parse characterIds JSON: {}", sb.getCharacterIds());
            }
        }
        // 兼容旧字段：单个 characterId
        if (characterImageUrls.isEmpty() && StringUtils.hasText(sb.getCharacterId())) {
            Character ch = characterService.getById(sb.getCharacterId());
            if (ch != null && StringUtils.hasText(ch.getImageUrl())) {
                characterImageUrls.add(ch.getImageUrl());
                characters.add(ch);
            }
        }

        // 4. 收集场景图 URL
        String sceneUrl = null;
        Scene scene = null;
        if (StringUtils.hasText(sb.getSceneId())) {
            scene = sceneService.getById(sb.getSceneId());
            if (scene != null && StringUtils.hasText(scene.getImageUrl())) {
                sceneUrl = scene.getImageUrl();
            }
        }

        // 5. 构造复合提示词（支持多角色）
        String prompt = buildReferencePrompt(sb, characters, scene);
        log.info("[RefImg] Generated prompt for storyboard {}: {}", storyboardId, prompt.substring(0, Math.min(200, prompt.length())));

        // 6. 构建参考图 URL 列表（角色图×N + 场景图）
        List<String> referenceImageUrls = new ArrayList<>(characterImageUrls);
        if (sceneUrl != null) {
            referenceImageUrls.add(sceneUrl);
        }

        // 7. 调用多图参考生成（或降级为纯文字生成）
        Asset asset = archiveReferenceImageWithReferences(sb.getDramaId(), storyboardId, prompt, referenceImageUrls, sb.getId());

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

    /**
     * 批量获取剧集下所有分镜的参考图状态
     *
     * @param dramaId 剧集ID
     * @return 分镜参考图状态列表
     */
    public List<StoryboardRefStatus> listRefStatusByDrama(String dramaId) {
        var q = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Storyboard>();
        q.eq(Storyboard::getDramaId, dramaId)
         .orderByAsc(Storyboard::getEpisodeNumber)
         .orderByAsc(Storyboard::getShotNumber);
        List<Storyboard> storyboards = storyboardService.list(q);
        List<StoryboardRefStatus> result = new ArrayList<>();

        for (Storyboard sb : storyboards) {
            StoryboardRefStatus status = new StoryboardRefStatus();
            status.setStoryboardId(sb.getId());
            status.setShotNumber(sb.getShotNumber());
            status.setAction(sb.getAction());

            Asset refAsset = findExistingReference(sb.getId());
            if (refAsset != null && StringUtils.hasText(refAsset.getFileUrl())) {
                status.setHasRefImage(true);
                status.setRefImageUrl(refAsset.getFileUrl());
            } else {
                status.setHasRefImage(false);
            }
            result.add(status);
        }
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 查找已有的参考图记录
     */
    private Asset findExistingReference(String storyboardId) {
        // JSON_EXTRACT 在 extra_data 为非法 JSON 时会抛出异常（MySQL 8.x）
        // 使用 JSON_VALID 先行过滤，只对有效的 JSON 执行 JSON_EXTRACT
        var q = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Asset>();
        q.apply("JSON_VALID(extra_data) = 1")
         .apply("JSON_EXTRACT(extra_data, '$.type') = 'reference_image'")
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
        q.apply("JSON_VALID(extra_data) = 1")
         .apply("JSON_EXTRACT(extra_data, '$.type') = 'reference_image'")
         .apply("JSON_EXTRACT(extra_data, '$.storyboardId') = {0}", storyboardId);
        var list = assetService.list(q);
        if (list != null && !list.isEmpty()) {
            for (Asset a : list) {
                assetService.removeById(a.getId());
            }
        }
    }

    /**
     * 构造复合参考图提示词（支持多角色）
     * 格式: "In {场景描述}, {角色1外观} and {角色2外观}, {动作描述}, cinematic lighting, film still quality, 16:9"
     */
    private String buildReferencePrompt(Storyboard sb, List<Character> characters, Scene scene) {
        StringBuilder prompt = new StringBuilder();

        // 场景
        if (scene != null && StringUtils.hasText(scene.getPrompt())) {
            prompt.append("In ").append(scene.getPrompt());
        } else if (scene != null && StringUtils.hasText(scene.getName())) {
            prompt.append("In a scene titled ").append(scene.getName());
        } else {
            prompt.append("In a cinematic scene");
        }

        // 所有角色（用 and 连接）
        if (characters != null && !characters.isEmpty()) {
            List<String> charDescs = new ArrayList<>();
            for (Character ch : characters) {
                if (StringUtils.hasText(ch.getAppearancePrompt())) {
                    charDescs.add(ch.getAppearancePrompt());
                } else if (StringUtils.hasText(ch.getName())) {
                    charDescs.add("a character named " + ch.getName());
                }
            }
            if (!charDescs.isEmpty()) {
                prompt.append(", with ").append(String.join(" and ", charDescs));
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
     * 归档参考图到 assets 表（支持多图参考生成）
     */
    private Asset archiveReferenceImageWithReferences(String dramaId, String storyboardId, String prompt,
                                                       List<String> referenceImageUrls, String shotId) {
        // 调用 AI 多图参考图片生成
        String imageUrl;
        if (referenceImageUrls != null && !referenceImageUrls.isEmpty()) {
            log.info("[RefImg] Generating with {} reference images", referenceImageUrls.size());
            imageUrl = imageGenerationService.generateImageWithReferences(prompt, referenceImageUrls);
        } else {
            log.info("[RefImg] No reference images, generating with prompt only");
            imageUrl = imageGenerationService.generateImageDirect(prompt);
        }

        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "参考图生成失败：AI返回为空");
        }

        // 归档到本地/OSS
        String filename = "ref_" + storyboardId + "_" + System.currentTimeMillis() + ".png";
        // 使用 ObjectMapper 构建合法的 JSON，避免 prompt 中的引号/换行等破坏 JSON 格式
        ObjectMapper mapper = new ObjectMapper();
        String safePrompt = prompt.length() > 500 ? prompt.substring(0, 500) : prompt;
        String extraData;
        try {
            extraData = mapper.writeValueAsString(new java.util.LinkedHashMap<String, String>() {{
                put("type", "reference_image");
                put("storyboardId", storyboardId);
                put("shotId", shotId);
                put("prompt", safePrompt);
            }});
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "构建参考图元数据失败: " + e.getMessage());
        }

        Asset asset = imageGenerationService.archiveImage(dramaId, imageUrl, filename, "image/png", extraData);
        return asset;
    }
}