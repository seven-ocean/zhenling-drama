package com.drama.controller;

import com.drama.common.R;
import com.drama.entity.Character;
import com.drama.service.CharacterService;
import com.drama.service.VisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;
    private final VisionService visionService;

    @PostMapping
    public R<Character> create(@RequestBody Character character) {
        return R.ok(characterService.create(character));
    }

    @GetMapping("/drama/{dramaId}")
    public R<List<Character>> listByDramaId(@PathVariable String dramaId) {
        return R.ok(characterService.listByDramaId(dramaId));
    }

    @GetMapping("/{id}")
    public R<Character> getById(@PathVariable String id) {
        return R.ok(characterService.getById(id));
    }

    @PostMapping("/{id}")
    public R<Character> update(@PathVariable String id, @RequestBody Character character) {
        character.setId(id);
        return R.ok(characterService.update(character));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        characterService.delete(id);
        return R.ok();
    }

    /**
     * 从角色图片提取外观提示词
     *
     * @param body 包含 imageUrl 和可选的 characterName
     * @return 提取的外观描述文本
     */
    @PostMapping("/extract-prompt")
    public R<String> extractAppearancePrompt(@RequestBody Map<String, String> body) {
        String imageUrl = body.get("imageUrl");
        String characterName = body.get("characterName");
        log.info("[CharacterCtrl] extractAppearancePrompt: imageUrl={}, characterName={}", imageUrl, characterName);
        String prompt = visionService.extractAppearancePrompt(imageUrl, characterName);
        return R.ok(prompt);
    }
}