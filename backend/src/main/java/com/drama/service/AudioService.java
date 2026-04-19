package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Audio;
import com.drama.mapper.AudioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 音频服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioService extends ServiceImpl<AudioMapper, Audio> {

    @Transactional
    public Audio create(Audio audio) {
        if (audio.getId() == null || audio.getId().isEmpty()) {
            audio.setId(IdUtils.randomId());
        }
        audio.setStatus(audio.getStatus() != null ? audio.getStatus() : "pending");
        audio.setCreatedAt(LocalDateTime.now());
        audio.setUpdatedAt(LocalDateTime.now());
        audio.setDeleted(0);
        
        this.save(audio);
        log.info("Created audio: {}", audio.getId());
        return audio;
    }

    public List<Audio> listByEpisode(String dramaId, int episodeNumber) {
        LambdaQueryWrapper<Audio> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Audio::getDramaId, dramaId);
        wrapper.eq(Audio::getEpisodeNumber, episodeNumber);
        wrapper.eq(Audio::getDeleted, 0);
        wrapper.orderByAsc(Audio::getCreatedAt);
        return this.list(wrapper);
    }

    public List<Audio> listByStoryboard(String storyboardId) {
        LambdaQueryWrapper<Audio> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Audio::getStoryboardId, storyboardId);
        wrapper.eq(Audio::getDeleted, 0);
        return this.list(wrapper);
    }

    /**
     * 查询剧集下所有配音
     */
    public List<Audio> listByDrama(String dramaId) {
        LambdaQueryWrapper<Audio> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Audio::getDramaId, dramaId)
               .eq(Audio::getDeleted, 0)
               .orderByDesc(Audio::getCreatedAt);
        return this.list(wrapper);
    }

    /**
     * 删除音频（逻辑删除）
     */
    @Transactional
    public void delete(String id) {
        // 先检查记录是否存在（由于@TableLogic，getById会自动过滤已删除记录）
        Audio existing = super.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "音频记录不存在或已被删除");
        }
        // 使用MyBatis-Plus的removeById进行逻辑删除
        boolean success = this.removeById(id);
        if (!success) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "删除失败");
        }
        log.info("Deleted audio: {}", id);
    }

    /**
     * 更新音频信息
     */
    @Transactional
    public Audio update(String id, Map<String, Object> data) {
        Audio audio = super.getById(id);
        if (audio == null || audio.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "音频记录不存在");
        }
        if (data.containsKey("audioUrl")) audio.setAudioUrl((String) data.get("audioUrl"));
        if (data.containsKey("status")) audio.setStatus((String) data.get("status"));
        if (data.containsKey("duration")) {
            Object d = data.get("duration");
            if (d instanceof Number) audio.setDuration(((Number) d).floatValue());
        }
        if (data.containsKey("errorMessage")) audio.setErrorMessage((String) data.get("errorMessage"));
        if (data.containsKey("extraData")) audio.setExtraData(data.get("extraData") != null ? data.get("extraData").toString() : null);

        audio.setUpdatedAt(LocalDateTime.now());
        this.updateById(audio);
        return audio;
    }
}