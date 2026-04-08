package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Character;
import com.drama.mapper.CharacterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterService extends ServiceImpl<CharacterMapper, Character> {

    @Transactional
    public Character create(Character character) {
        if (!StringUtils.hasText(character.getId())) {
            character.setId(IdUtils.randomId());
        }
        character.setSortOrder(character.getSortOrder() != null ? character.getSortOrder() : 0);
        character.setCreatedAt(LocalDateTime.now());
        character.setUpdatedAt(LocalDateTime.now());
        character.setDeleted(0);
        
        this.save(character);
        log.info("Created character: {}", character.getId());
        return character;
    }

    public List<Character> listByDramaId(String dramaId) {
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Character::getDramaId, dramaId);
        wrapper.eq(Character::getDeleted, 0);
        wrapper.orderByAsc(Character::getSortOrder);
        return this.list(wrapper);
    }

    public Character getById(String id) {
        Character character = this.getById(id);
        if (character == null || character.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return character;
    }

    @Transactional
    public Character update(Character character) {
        if (!StringUtils.hasText(character.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "ID不能为空");
        }
        Character existing = this.getById(character.getId());
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        character.setUpdatedAt(LocalDateTime.now());
        this.updateById(character);
        log.info("Updated character: {}", character.getId());
        return this.getById(character.getId());
    }

    @Transactional
    public void delete(String id) {
        Character character = this.getById(id);
        if (character == null || character.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        character.setDeleted(1);
        character.setUpdatedAt(LocalDateTime.now());
        this.updateById(character);
        log.info("Deleted character: {}", id);
    }
}