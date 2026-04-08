package com.drama.controller;

import com.drama.common.R;
import com.drama.entity.Audio;
import com.drama.service.AudioService;
import com.drama.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配音接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audios")
@RequiredArgsConstructor
public class AudioController {

    private final TtsService ttsService;
    private final AudioService audioService;

    /**
     * 生成配音
     */
    @PostMapping("/generate")
    public R<Audio> generate(@RequestParam String dramaId,
                           @RequestParam int episodeNumber,
                           @RequestParam String storyboardId,
                           @RequestParam String characterId,
                           @RequestParam String text,
                           @RequestParam(required = false) String provider,
                           @RequestParam(required = false) String voiceId,
                           @RequestParam(required = false) String model) {
        return R.ok(ttsService.generate(dramaId, episodeNumber, storyboardId, characterId, text, provider, voiceId, model));
    }

    /**
     * 批量生成配音
     */
    @PostMapping("/batch")
    public R<List<Audio>> batchGenerate(@RequestParam String dramaId,
                                      @RequestParam int episodeNumber,
                                      @RequestBody List<Map<String, String>> dialogues) {
        return R.ok(ttsService.batchGenerate(dramaId, episodeNumber, dialogues));
    }

    /**
     * 获取集数配音列表
     */
    @GetMapping("/drama/{dramaId}/episode/{episodeNumber}")
    public R<List<Audio>> listByEpisode(@PathVariable String dramaId,
                                     @PathVariable int episodeNumber) {
        return R.ok(audioService.listByEpisode(dramaId, episodeNumber));
    }
}