package com.drama.controller;

import com.drama.common.R;
import com.drama.entity.Video;
import com.drama.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 视频接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * 生成视频
     */
    @PostMapping("/generate")
    public R<Video> generate(@RequestParam String dramaId,
                          @RequestParam int episodeNumber,
                          @RequestParam String storyboardId,
                          @RequestParam String imageUrl,
                          @RequestParam(required = false) String provider,
                          @RequestParam(required = false) String model) {
        return R.ok(videoService.generate(dramaId, episodeNumber, storyboardId, imageUrl, provider, model));
    }

    /**
     * 批量生成视频
     */
    @PostMapping("/batch")
    public R<String> batchGenerate(@RequestParam String dramaId,
                                 @RequestParam int episodeNumber) {
        // 简化实现
        return R.ok("batch generation started");
    }
}