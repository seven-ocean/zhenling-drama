package com.drama.controller;

import com.drama.common.R;
import com.drama.common.ResultCode;
import com.drama.entity.Audio;
import com.drama.service.AudioService;
import com.drama.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配音接口 - 完整 CRUD + TTS 生成
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audios")
@RequiredArgsConstructor
public class AudioController {

    private final TtsService ttsService;
    private final AudioService audioService;

    /**
     * 生成配音 (TTS)
     */
    @PostMapping("/generate")
    public R<Audio> generate(@RequestParam String dramaId,
                             @RequestParam int episodeNumber,
                             @RequestParam(required = false) String storyboardId,
                             @RequestParam(required = false) String characterId,
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

    /**
     * 按剧集获取全部配音
     */
    @GetMapping("/drama/{dramaId}")
    public R<List<Audio>> listByDrama(@PathVariable String dramaId) {
        return R.ok(audioService.listByDrama(dramaId));
    }

    /**
     * 获取单个音频详情
     */
    @GetMapping("/{id}")
    public R<Audio> getById(@PathVariable String id) {
        Audio audio = audioService.getById(id);
        if (audio == null || audio.getDeleted() == 1) {
            throw new com.drama.common.BusinessException(ResultCode.NOT_FOUND, "音频记录不存在");
        }
        return R.ok(audio);
    }

    /**
     * 删除音频（逻辑删除）
     */
    @PostMapping("/{id}/delete")
    public R<Void> delete(@PathVariable String id) {
        audioService.delete(id);
        return R.ok();
    }

    /**
     * TTS 预览（不保存到数据库，用于角色配置页面试听音色）
     * 注意：路径用 /tts-preview 而非 /preview，避免被下面的 /{id} 路由吞掉
     */
    @PostMapping("/tts-preview")
    public R<Map<String, Object>> preview(
            @RequestParam String text,
            @RequestParam(required = false) String voiceId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String characterId) {
        // 限制预览文本长度，防止滥用
        if (text.length() > 200) {
            text = text.substring(0, 200);
        }
        String audioResult = ttsService.preview(text, voiceId, model, characterId);
        return R.ok(Map.of(
                "audioData", audioResult,
                "voiceId", voiceId != null ? voiceId : "default",
                "text", text
        ));
    }

    /**
     * 更新音频信息
     */
    @PostMapping("/{id}")
    public R<Audio> update(@PathVariable String id, @RequestBody Map<String, Object> data) {
        return R.ok(audioService.update(id, data));
    }

    /**
     * 按分镜ID查询已完成的配音列表
     * 用于视频生成时自动匹配音频时长
     */
    @GetMapping("/by-storyboard/{storyboardId}")
    public R<List<Audio>> listByStoryboard(@PathVariable String storyboardId) {
        return R.ok(audioService.listByStoryboard(storyboardId));
    }
}
