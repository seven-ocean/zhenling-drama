package com.drama.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drama.common.R;
import com.drama.entity.Asset;
import com.drama.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 素材接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping("/upload")
    public R<Asset> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dramaId", required = false) String dramaId,
            @RequestParam(value = "type", required = false) String type) {
        return R.ok(assetService.upload(file, dramaId, type));
    }

    @GetMapping("/drama/{dramaId}")
    public R<List<Asset>> listByDramaId(
            @PathVariable String dramaId,
            @RequestParam(required = false) String type) {
        return R.ok(assetService.listByDramaId(dramaId, type));
    }

    @GetMapping
    public R<IPage<Asset>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dramaId,
            @RequestParam(required = false) String type) {
        return R.ok(assetService.page(pageNum, pageSize, dramaId, type));
    }

    @GetMapping("/{id}")
    public R<Asset> getById(@PathVariable String id) {
        return R.ok(assetService.getById(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        assetService.delete(id);
        return R.ok();
    }
}