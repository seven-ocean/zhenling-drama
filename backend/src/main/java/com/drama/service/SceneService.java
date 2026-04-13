package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Scene;
import com.drama.mapper.SceneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场景服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneService extends ServiceImpl<SceneMapper, Scene> {

    @Transactional
    public Scene create(Scene scene) {
        if (!StringUtils.hasText(scene.getId())) {
            scene.setId(IdUtils.randomId());
        }
        scene.setCreatedAt(LocalDateTime.now());
        scene.setUpdatedAt(LocalDateTime.now());
        scene.setDeleted(0);
        
        this.save(scene);
        log.info("Created scene: {}", scene.getId());
        return scene;
    }

    public List<Scene> listByDramaId(String dramaId) {
        LambdaQueryWrapper<Scene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Scene::getDramaId, dramaId);
        wrapper.eq(Scene::getDeleted, 0);
        wrapper.orderByAsc(Scene::getName);
        return this.list(wrapper);
    }

    public Scene getById(String id) {
        Scene scene = super.getById(id);
        if (scene == null || scene.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "场景不存在");
        }
        return scene;
    }

    @Transactional
    public Scene update(Scene scene) {
        if (!StringUtils.hasText(scene.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "ID不能为空");
        }
        Scene existing = this.getById(scene.getId());
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "场景不存在");
        }
        scene.setUpdatedAt(LocalDateTime.now());
        this.updateById(scene);
        log.info("Updated scene: {}", scene.getId());
        return this.getById(scene.getId());
    }

    @Transactional
    public void delete(String id) {
        Scene scene = this.getById(id);
        if (scene == null || scene.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "场景不存在");
        }
        scene.setDeleted(1);
        scene.setUpdatedAt(LocalDateTime.now());
        this.updateById(scene);
        log.info("Deleted scene: {}", id);
    }
}