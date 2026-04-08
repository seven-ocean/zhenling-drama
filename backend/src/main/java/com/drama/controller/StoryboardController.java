package com.drama.controller;

import com.drama.common.R;
import com.drama.entity.Storyboard;
import com.drama.service.StoryboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分镜接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/storyboards")
@RequiredArgsConstructor
public class StoryboardController {

    private final StoryboardService storyboardService;

    /**
     * AI自动拆解剧本
     */
    @PostMapping("/generate")
    public R<List<Storyboard>> generateFromScript(
            @RequestParam String dramaId,
            @RequestParam String script,
            @RequestParam(defaultValue = "1") int episodeNumber) {
        return R.ok(storyboardService.generateFromScript(dramaId, script, episodeNumber));
    }

    /**
     * 生成宫格图提示词
     */
    @PostMapping("/grid-prompt")
    public R<String> generateGridPrompt(
            @RequestBody Map<String, String> params) {
        // 简化实现
        return R.ok("");
    }

    /**
     * 获取分镜列表
     */
    @GetMapping("/drama/{dramaId}/episode/{episodeNumber}")
    public R<List<Storyboard>> listByEpisode(
            @PathVariable String dramaId,
            @PathVariable int episodeNumber) {
        return R.ok(List.of());
    }
}