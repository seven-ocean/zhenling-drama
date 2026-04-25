package com.drama.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drama.common.R;
import com.drama.entity.EpisodeExport;
import com.drama.service.EpisodeExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 整集导出接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/episode-exports")
@RequiredArgsConstructor
public class EpisodeExportController {

    private final EpisodeExportService episodeExportService;

    /**
     * 导出整集视频
     * 将指定剧集的指定集数下的所有分镜视频拼接成一个完整视频
     */
    @PostMapping("/export")
    public R<EpisodeExport> exportEpisode(
            @RequestParam String dramaId,
            @RequestParam int episodeNumber) {
        log.info("Export episode request: dramaId={}, episodeNumber={}", dramaId, episodeNumber);
        return R.ok(episodeExportService.exportEpisode(dramaId, episodeNumber));
    }

    /**
     * 分页查询导出记录
     */
    @GetMapping
    public R<IPage<EpisodeExport>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dramaId,
            @RequestParam(required = false) Integer episodeNumber) {
        return R.ok(episodeExportService.page(pageNum, pageSize, dramaId, episodeNumber));
    }

    /**
     * 获取单个导出记录
     */
    @GetMapping("/{id}")
    public R<EpisodeExport> getById(@PathVariable String id) {
        EpisodeExport export = episodeExportService.getById(id);
        if (export == null || export.getDeleted() == 1) {
            return R.fail(404, "导出记录不存在");
        }
        return R.ok(export);
    }

    /**
     * 删除导出记录
     */
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable String id) {
        episodeExportService.removeById(id);
        return R.ok("删除成功");
    }
}
