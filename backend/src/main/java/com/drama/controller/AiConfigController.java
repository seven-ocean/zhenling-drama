package com.drama.controller;

import com.drama.common.R;
import com.drama.entity.AiConfig;
import com.drama.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI配置接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai-configs")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @PostMapping
    public R<AiConfig> create(@RequestBody AiConfig config) {
        return R.ok(aiConfigService.create(config));
    }

    /**
     * 获取所有未删除配置（管理页使用，包含禁用的）
     */
    @GetMapping
    public R<List<AiConfig>> listAll() {
        return R.ok(aiConfigService.listAll());
    }

    @GetMapping("/type/{apiType}")
    public R<List<AiConfig>> listByType(@PathVariable String apiType) {
        return R.ok(aiConfigService.listByType(apiType));
    }

    @GetMapping("/{id}")
    public R<AiConfig> getById(@PathVariable String id) {
        return R.ok(aiConfigService.getById(id));
    }

    @PostMapping("/{id}")
    public R<AiConfig> update(@PathVariable String id, @RequestBody AiConfig config) {
        config.setId(id);
        return R.ok(aiConfigService.update(config));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        aiConfigService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/toggle")
    public R<Void> toggle(@PathVariable String id, @RequestParam boolean enabled) {
        aiConfigService.toggle(id, enabled);
        return R.ok();
    }
}