package com.drama.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drama.common.R;
import com.drama.entity.Drama;
import com.drama.service.DramaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 剧集接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dramas")
@RequiredArgsConstructor
public class DramaController {

    private final DramaService dramaService;

    /**
     * 创建剧集
     */
    @PostMapping
    public R<Drama> create(@RequestBody Drama drama) {
        return R.ok(dramaService.create(drama));
    }

    /**
     * 分页查询
     */
    @GetMapping
    public R<IPage<Drama>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(dramaService.page(pageNum, pageSize, status, keyword));
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    public R<Drama> getById(@PathVariable String id) {
        return R.ok(dramaService.getById(id));
    }

    /**
     * 更新剧集
     */
    @PutMapping("/{id}")
    public R<Drama> update(@PathVariable String id, @RequestBody Drama drama) {
        drama.setId(id);
        return R.ok(dramaService.update(drama));
    }

    /**
     * 删除剧集
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        dramaService.delete(id);
        return R.ok();
    }

    /**
     * 更新状态
     */
    @PatchMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable String id, @RequestParam String status) {
        dramaService.updateStatus(id, status);
        return R.ok();
    }
}