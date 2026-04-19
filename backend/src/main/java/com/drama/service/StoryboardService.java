package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Character;
import com.drama.entity.Scene;
import com.drama.entity.Storyboard;
import com.drama.mapper.StoryboardMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 分镜服务 - AI自动拆解 + CRUD
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryboardService extends ServiceImpl<StoryboardMapper, Storyboard> {

    private final AiServiceFactory aiServiceFactory;
    private final CharacterService characterService;
    private final SceneService sceneService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        String aiResult = aiServiceFactory.generateText(null, prompt, null);
        
        if (!StringUtils.hasText(aiResult) || aiResult.length() < 10) {
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "AI返回结果为空，请检查AI文本生成配置是否正确（需要在AI配置页面添加text类型的配置）");
        }

        // 解析AI返回的JSON
        List<Map<String, Object>> shots = parseAiResult(aiResult);
        
        if (shots.isEmpty()) {
            log.warn("AI解析结果为空，原始返回长度: {}", aiResult.length());
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "AI解析分镜失败，返回内容: " + aiResult.substring(0, Math.min(200, aiResult.length())) + "...");
        }

        // 获取该集已有分镜数量
        int existingCount = countByEpisode(dramaId, episodeNumber);

        // 创建分镜记录
        List<Storyboard> storyboards = new ArrayList<>();
        int shotNum = existingCount + 1;
        LocalDateTime now = LocalDateTime.now();
        
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
            
            // 尝试匹配角色和场景
            String characterName = (String) shot.getOrDefault("characterName", "");
            String sceneName = (String) shot.getOrDefault("sceneName", "");
            if (StringUtils.hasText(characterName)) {
                Character ch = findCharacterByName(dramaId, characterName);
                if (ch != null) sb.setCharacterId(ch.getId());
            }
            if (StringUtils.hasText(sceneName)) {
                Scene sc = findSceneByName(dramaId, sceneName);
                if (sc != null) sb.setSceneId(sc.getId());
            }
            
            sb.setStatus("pending");
            sb.setCreatedAt(now);
            sb.setUpdatedAt(now);
            sb.setDeleted(0);
            
            storyboards.add(sb);
        }
        
        // 批量保存
        this.saveBatch(storyboards);
        log.info("Generated {} storyboards for drama {}, episode {}", storyboards.size(), dramaId, episodeNumber);
        
        return storyboards;
    }

    /**
     * 查询某集的所有分镜
     */
    public List<Storyboard> listByEpisode(String dramaId, int episodeNumber) {
        LambdaQueryWrapper<Storyboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Storyboard::getDramaId, dramaId)
               .eq(Storyboard::getEpisodeNumber, episodeNumber)
               .eq(Storyboard::getDeleted, 0)
               .orderByAsc(Storyboard::getShotNumber);
        return this.list(wrapper);
    }

    /**
     * 更新单个分镜
     */
    @Transactional
    public Storyboard updateShot(String id, Map<String, Object> updates) {
        Storyboard existing = this.getById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分镜不存在");
        }

        if (updates.containsKey("shotType")) existing.setShotType((String) updates.get("shotType"));
        if (updates.containsKey("shotDirection")) existing.setShotDirection((String) updates.get("shotDirection"));
        if (updates.containsKey("action")) existing.setAction((String) updates.get("action"));
        if (updates.containsKey("dialogue")) existing.setDialogue((String) updates.get("dialogue"));
        if (updates.containsKey("characterId")) existing.setCharacterId((String) updates.get("characterId"));
        if (updates.containsKey("sceneId")) existing.setSceneId((String) updates.get("sceneId"));
        if (updates.containsKey("status")) existing.setStatus((String) updates.get("status"));
        if (updates.containsKey("characterImageUrl")) existing.setCharacterImageUrl((String) updates.get("characterImageUrl"));
        if (updates.containsKey("sceneImageUrl")) existing.setSceneImageUrl((String) updates.get("sceneImageUrl"));
        if (updates.containsKey("gridImageUrl")) existing.setGridImageUrl((String) updates.get("gridImageUrl"));
        if (updates.containsKey("gridPrompt")) existing.setGridPrompt((String) updates.get("gridPrompt"));

        existing.setUpdatedAt(LocalDateTime.now());
        this.updateById(existing);
        return this.getById(id);
    }

    /**
     * 删除分镜
     */
    @Transactional
    public void deleteShot(String id) {
        // 先检查记录是否存在（由于@TableLogic，getById会自动过滤已删除记录）
        Storyboard existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分镜不存在或已被删除");
        }
        // 使用MyBatis-Plus的removeById进行逻辑删除
        boolean success = this.removeById(id);
        if (!success) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "删除失败");
        }
    }

    /**
     * 批量更新分镜顺序
     */
    @Transactional
    public void reorderShots(List<Map<String, Object>> orders) {
        for (Map<String, Object> order : orders) {
            String id = (String) order.get("id");
            Integer shotNumber = (Integer) order.get("shotNumber");
            if (id != null && shotNumber != null) {
                Storyboard sb = new Storyboard();
                sb.setId(id);
                sb.setShotNumber(shotNumber);
                sb.setUpdatedAt(LocalDateTime.now());
                this.updateById(sb);
            }
        }
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
        if (StringUtils.hasText(storyboard.getDialogue())) {
            prompt.append("The character says: '").append(storyboard.getDialogue()).append("'. ");
        }
        if (StringUtils.hasText(storyboard.getShotDirection())) {
            prompt.append("Camera: ").append(storyboard.getShotDirection()).append(".");
        }
        
        prompt.append(" Style: anime storyboard, detailed, cinematic lighting.");
        
        return prompt.toString();
    }

    /**
     * 生成角色提示词
     */
    public String generateCharacterPrompt(Character character) {
        if (character == null || !StringUtils.hasText(character.getAppearancePrompt())) {
            return "";
        }
        return String.format(
            "Create a consistent character portrait of %s. %s. " +
            "Style: anime, detailed face, high quality, clean background. " +
            "Keep the same appearance across all images.",
            character.getName(),
            character.getAppearancePrompt()
        );
    }

    /**
     * 生成场景提示词
     */
    public String generateScenePrompt(Scene scene) {
        if (scene == null || !StringUtils.hasText(scene.getPrompt())) {
            return "";
        }
        return String.format(
            "Create a detailed background scene: %s. %s. Time of day: %s. " +
            "Style: anime, detailed environment, cinematic composition, wide angle view.",
            scene.getName(),
            scene.getPrompt(),
            scene.getTimeOfDay() != null ? scene.getTimeOfDay() : "day"
        );
    }

    /**
     * 统计某集分镜数量
     */
    private int countByEpisode(String dramaId, int episodeNumber) {
        LambdaQueryWrapper<Storyboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Storyboard::getDramaId, dramaId)
               .eq(Storyboard::getEpisodeNumber, episodeNumber)
               .eq(Storyboard::getDeleted, 0);
        return Math.toIntExact(this.count(wrapper));
    }

    private Character findCharacterByName(String dramaId, String name) {
        List<Character> characters = characterService.listByDramaId(dramaId);
        for (Character c : characters) {
            if (c.getName().equalsIgnoreCase(name.trim()) ||
                c.getName().contains(name.trim()) ||
                name.trim().contains(c.getName())) {
                return c;
            }
        }
        return null;
    }

    private Scene findSceneByName(String dramaId, String name) {
        List<Scene> scenes = sceneService.listByDramaId(dramaId);
        for (Scene s : scenes) {
            if (s.getName().equalsIgnoreCase(name.trim()) ||
                s.getName().contains(name.trim()) ||
                name.trim().contains(s.getName())) {
                return s;
            }
        }
        return null;
    }

    private String buildSplitPrompt(String script, int episodeNumber) {
        return String.format(
            """ 
            你是一个专业的影视分镜师。请将以下第%d集的剧本拆解为分镜列表。

            要求：
            1. 每个分镜是一个独立的镜头
            2. 包含字段：sceneNumber(场景序号), shotType(镜头类型:wide/medium/close-up/extreme-close-up), shotDirection(运镜方式), action(动作描述), dialogue(台词), characterName(说话角色名), sceneName(场景名)
            3. 台词要保留原文
            4. 动作描述要详细具体
            
            请严格以JSON数组格式返回，不要包含任何其他文字：
            [{"sceneNumber":1,"shotType":"medium","shotDirection":"固定镜头","action":"动作描述","dialogue":"台词","characterName":"角色名","sceneName":"场景名"}, ...]
            
            剧本内容：
            %s
            """,
            episodeNumber, script
        );
    }

    /**
     * 解析AI返回的JSON结果
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAiResult(String aiResult) {
        try {
            // 尝试直接解析
            String jsonStr = aiResult.trim();
            
            // 如果被包裹在```json中，提取出来
            int start = jsonStr.indexOf("[");
            int end = jsonStr.lastIndexOf("]");
            if (start >= 0 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            } else {
                // 可能是单对象或格式不对
                start = jsonStr.indexOf("{");
                end = jsonStr.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    jsonStr = "[" + jsonStr.substring(start, end + 1) + "]";
                }
            }
            
            return objectMapper.readValue(jsonStr, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Parse AI result failed: {}", e.getMessage());
            // 尝试更宽松的解析
            try {
                JsonNode root = objectMapper.readTree(aiResult);
                if (root.isArray()) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (JsonNode node : root) {
                        result.add(objectMapper.convertValue(node, Map.class));
                    }
                    return result;
                } else if (root.isObject()) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    result.add(objectMapper.convertValue(root, Map.class));
                    return result;
                }
            } catch (Exception e2) {
                log.error("Second parse attempt also failed: {}", e2.getMessage());
            }
        }
        return new ArrayList<>();
    }
}
