package com.drama.controller;

import com.drama.common.R;
import com.drama.entity.Character;
import com.drama.entity.Scene;
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
     * AI自动拆解剧本为分镜
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
    public R<String> generateGridPrompt(@RequestBody Map<String, String> params) {
        String storyboardId = params.get("storyboardId");
        String characterId = params.get("characterId");
        String sceneId = params.get("sceneId");
        
        // 获取分镜、角色、场景信息
        Storyboard sb = null;
        if (storyboardId != null) {
            try { sb = storyboardService.getById(storyboardId); } catch (Exception ignored) {}
        }
        
        // TODO: 根据ID加载角色和场景实体，当前简化处理
        Character character = null;
        Scene scene = null;
        
        if (sb == null) {
            return R.ok("");
        }
        
        return R.ok(storyboardService.generateGridPrompt(sb, null, null));
    }

    /**
     * 获取某集的分镜列表
     */
    @GetMapping("/drama/{dramaId}/episode/{episodeNumber}")
    public R<List<Storyboard>> listByEpisode(
            @PathVariable String dramaId,
            @PathVariable int episodeNumber) {
        return R.ok(storyboardService.listByEpisode(dramaId, episodeNumber));
    }

    /**
     * 更新单个分镜
     */
    @PutMapping("/{id}")
    public R<Storyboard> update(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return R.ok(storyboardService.updateShot(id, updates));
    }

    /**
     * 删除分镜
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        storyboardService.deleteShot(id);
        return R.ok();
    }

    /**
     * 批量重排序
     */
    @PostMapping("/reorder")
    public R<Void> reorder(@RequestBody List<Map<String, Object>> orders) {
        storyboardService.reorderShots(orders);
        return R.ok();
    }

    /**
     * 获取单个分镜详情
     */
    @GetMapping("/{id}")
    public R<Storyboard> getById(@PathVariable String id) {
        Storyboard sb = storyboardService.getById(id);
        if (sb == null || sb.getDeleted() == 1) {
            throw new com.drama.common.BusinessException(com.drama.common.ResultCode.NOT_FOUND, "分镜不存在");
        }
        return R.ok(sb);
    }
}
