package com.drama.controller;

import com.drama.common.R;
import com.drama.dto.StoryboardRefStatus;
import com.drama.entity.Asset;
import com.drama.service.ImageReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分镜参考图接口
 * 为分镜生成"角色×场景"预览图，用于视频合成前的效果确认
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/image-reference")
@RequiredArgsConstructor
public class ImageReferenceController {

    private final ImageReferenceService imageReferenceService;

    /**
     * 为指定分镜生成参考图
     *
     * @param storyboardId 分镜ID
     * @param forceRegenerate 是否强制重新生成
     * @param model 指定的模型（如 "image-01" 或 "doubao-seedream-5-0-260128"，为 null 则使用默认模型）
     */
    @PostMapping("/generate")
    public R<Asset> generate(
            @RequestParam String storyboardId,
            @RequestParam(required = false, defaultValue = "false") Boolean forceRegenerate,
            @RequestParam(required = false) String model) {
        log.info("[RefImg API] generate: storyboardId={}, forceRegenerate={}, model={}", storyboardId, forceRegenerate, model);
        Asset asset = imageReferenceService.generateReference(storyboardId, forceRegenerate != null && forceRegenerate, model);
        return R.ok(asset);
    }

    /**
     * 获取分镜的参考图
     */
    @GetMapping("/{storyboardId}")
    public R<Asset> get(@PathVariable String storyboardId) {
        Asset asset = imageReferenceService.getReference(storyboardId);
        if (asset == null) {
            return R.ok(null);
        }
        return R.ok(asset);
    }

    /**
     * 删除分镜的参考图
     */
    @DeleteMapping("/{storyboardId}")
    public R<Void> delete(@PathVariable String storyboardId) {
        imageReferenceService.deleteReference(storyboardId);
        return R.ok(null);
    }

    /**
     * 批量获取剧集下所有分镜的参考图状态
     */
    @GetMapping("/list/{dramaId}")
    public R<List<StoryboardRefStatus>> listByDrama(@PathVariable String dramaId) {
        log.info("[RefImg API] listByDrama: dramaId={}", dramaId);
        List<StoryboardRefStatus> list = imageReferenceService.listRefStatusByDrama(dramaId);
        return R.ok(list);
    }
}
