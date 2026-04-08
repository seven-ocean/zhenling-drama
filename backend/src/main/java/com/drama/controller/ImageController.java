package com.drama.controller;

import com.drama.common.R;
import com.drama.entity.Asset;
import com.drama.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 图片生成接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageGenerationService imageGenerationService;

    /**
     * 生成角色图片
     */
    @PostMapping("/character")
    public R<Asset> generateCharacterImage(
            @RequestParam String dramaId,
            @RequestParam String characterId,
            @RequestParam String prompt,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model) {
        return R.ok(imageGenerationService.generateCharacterImage(dramaId, characterId, prompt, provider, model));
    }

    /**
     * 生成场景图片
     */
    @PostMapping("/scene")
    public R<Asset> generateSceneImage(
            @RequestParam String dramaId,
            @RequestParam String sceneId,
            @RequestParam String prompt,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model) {
        return R.ok(imageGenerationService.generateSceneImage(dramaId, sceneId, prompt, provider, model));
    }

    /**
     * 生成宫格图
     */
    @PostMapping("/grid")
    public R<Asset> generateGridImage(
            @RequestParam String dramaId,
            @RequestParam String prompt,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model) {
        return R.ok(imageGenerationService.generateGridImage(dramaId, prompt, provider, model));
    }
}