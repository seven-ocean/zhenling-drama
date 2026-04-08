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
}