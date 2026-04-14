package com.drama.controller;

import com.drama.common.R;
import com.drama.entity.Scene;
import com.drama.service.SceneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场景接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    /**
     * 创建场景
     */
    @PostMapping
    public R<Scene> create(@RequestBody Scene scene) {
        return R.ok(sceneService.create(scene));
    }

    /**
     * 按剧集ID查询场景列表
     */
    @GetMapping("/drama/{dramaId}")
    public R<List<Scene>> listByDramaId(@PathVariable String dramaId) {
        return R.ok(sceneService.listByDramaId(dramaId));
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    public R<Scene> getById(@PathVariable String id) {
        return R.ok(sceneService.getById(id));
    }

    /**
     * 更新场景
     */
    @PostMapping("/{id}")
    public R<Scene> update(@PathVariable String id, @RequestBody Scene scene) {
        scene.setId(id);
        return R.ok(sceneService.update(scene));
    }

    /**
     * 删除场景
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        sceneService.delete(id);
        return R.ok();
    }
}
