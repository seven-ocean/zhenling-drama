package com.drama.service;

import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Character;
import com.drama.entity.Scene;
import com.drama.entity.Storyboard;
import com.drama.service.adapter.AiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分镜服务 - AI自动拆解
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryboardService {

    private final AiServiceFactory aiServiceFactory;
    private final CharacterService characterService;
    private final SceneService sceneService;

    /**
     * AI自动拆解剧本为分镜
     */
    @Transactional
    public List<Storyboard> generateFromScript(String dramaId, String script, int episodeNumber) {
        if (!StringUtils.hasText(script)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "剧本不能为空");
        }

        // 调用AI拆分剧本
        String prompt = buildSplitPrompt(script, episodeNumber);
        String aiResult = aiServiceFactory.generateText("openai", prompt, "gpt-4o");
        
        // 解析AI返回结果
        List<Map<String, Object>> shots = parseAiResult(aiResult);
        
        // 创建分镜记录
        List<Storyboard> storyboards = new ArrayList<>();
        int shotNum = 1;
        
        for (Map<String, Object> shot : shots) {
            Storyboard sb = new Storyboard();
            sb.setId(IdUtils.randomId());
            sb.setDramaId(dramaId);
            sb.setEpisodeNumber(episodeNumber);
            sb.setSceneNumber((Integer) shot.getOrDefault("sceneNumber", 1));
            sb.setShotNumber(shotNum++);
            sb.setShotType((String) shot.getOrDefault("shotType", "medium"));
            sb.setShotDirection((String) shot.getOrDefault("shotDirection", ""));
            sb.setAction((String) shot.getOrDefault("action", ""));
            sb.setDialogue((String) shot.getOrDefault("dialogue", ""));
            sb.setStatus("pending");
            sb.setCreatedAt(LocalDateTime.now());
            sb.setUpdatedAt(LocalDateTime.now());
            sb.setDeleted(0);
            
            storyboards.add(sb);
        }
        
        // 批量保存
        // storyboardMapper.insertBatch(storyboards);
        log.info("Generated {} storyboards for drama {}", storyboards.size(), dramaId);
        
        return storyboards;
    }

    /**
     * AI生成宫格图提示词
     */
    public String generateGridPrompt(Storyboard storyboard, Character character, Scene scene) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a storyboard frame showing: ");
        
        if (scene != null && StringUtils.hasText(scene.getPrompt())) {
            prompt.append(scene.getPrompt()).append(". ");
        }
        if (character != null && StringUtils.hasText(character.getAppearancePrompt())) {
            prompt.append("Character: ").append(character.getAppearancePrompt()).append(". ");
        }
        if (StringUtils.hasText(storyboard.getAction())) {
            prompt.append("Action: ").append(storyboard.getAction()).append(". ");
        }
        if (StringUtils.hasText(storyboard.getShotDirection())) {
            prompt.append("Camera: ").append(storyboard.getShotDirection()).append(".");
        }
        
        return prompt.toString();
    }

    /**
     * 生成角色提示词
     */
    public String generateCharacterPrompt(Character character) {
        if (character == null || !StringUtils.hasText(character.getAppearancePrompt())) {
            return "";
        }
        
        String prompt = String.format(
            "Create a character portrait of %s. %s. Style: anime, detailed, high quality.",
            character.getName(),
            character.getAppearancePrompt()
        );
        
        return prompt;
    }

    /**
     * 生成场景提示词
     */
    public String generateScenePrompt(Scene scene) {
        if (scene == null || !StringUtils.hasText(scene.getPrompt())) {
            return "";
        }
        
        String prompt = String.format(
            "Create a background scene: %s. %s. Time: %s. Style: anime, detailed.",
            scene.getName(),
            scene.getPrompt(),
            scene.getTimeOfDay()
        );
        
        return prompt;
    }

    private String buildSplitPrompt(String script, int episodeNumber) {
        return String.format(
            """"
            请将以下剧本拆解为分镜列表。每个分镜需要包含：
            - sceneNumber: 场景序号
            - shotType: 镜头类型 (wide/medium/close-up/extreme-close-up)
            - shotDirection: 运镜方式
            - action: 动作描述
            - dialogue: 台词
            
            返回JSON数组格式。
            剧本：%s
            """,
            script
        );
    }

    private List<Map<String, Object>> parseAiResult(String aiResult) {
        // 简化JSON解析
        List<Map<String, Object>> shots = new ArrayList<>();
        
        try {
            // 实际应该解析AI返回的JSON
            // 这里返回空列表待完善
        } catch (Exception e) {
            log.error("Parse AI result failed: {}", e.getMessage());
        }
        
        return shots;
    }
}