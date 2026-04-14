package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.AiConfig;
import com.drama.mapper.AiConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigService extends ServiceImpl<AiConfigMapper, AiConfig> {

    @Transactional
    public AiConfig create(AiConfig config) {
        if (!StringUtils.hasText(config.getProvider()) || !StringUtils.hasText(config.getApiType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "provider 和 apiType 不能为空");
        }
        config.setId(IdUtils.randomId());
        config.setPriority(config.getPriority() != null ? config.getPriority() : 0);
        config.setEnabled(config.getEnabled() != null ? config.getEnabled() : true);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        config.setDeleted(0);
        
        this.save(config);
        log.info("Created AI config: {} - {}", config.getProvider(), config.getApiType());
        return config;
    }

    public List<AiConfig> listByType(String apiType) {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getApiType, apiType);
        wrapper.eq(AiConfig::getEnabled, true);
        wrapper.eq(AiConfig::getDeleted, 0);
        wrapper.orderByAsc(AiConfig::getPriority);
        return this.list(wrapper);
    }

    /**
     * 查询所有未删除的配置（管理页面使用，包含禁用的）
     */
    public List<AiConfig> listAll() {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getDeleted, 0);
        wrapper.orderByAsc(AiConfig::getPriority);
        return this.list(wrapper);
    }

    public AiConfig getFirstAvailable(String apiType) {
        List<AiConfig> configs = this.listByType(apiType);
        if (configs.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到可用的 AI 配置: " + apiType);
        }
        return configs.get(0);
    }

    @Transactional
    public AiConfig update(AiConfig config) {
        if (!StringUtils.hasText(config.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "ID不能为空");
        }
        config.setUpdatedAt(LocalDateTime.now());
        this.updateById(config);
        log.info("Updated AI config: {}", config.getId());
        return this.getById(config.getId());
    }

    @Transactional
    public void delete(String id) {
        AiConfig config = this.getById(id);
        if (config == null || config.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        config.setDeleted(1);
        config.setUpdatedAt(LocalDateTime.now());
        this.updateById(config);
        log.info("Deleted AI config: {}", id);
    }

    @Transactional
    public void toggle(String id, boolean enabled) {
        AiConfig config = this.getById(id);
        if (config == null || config.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        config.setEnabled(enabled);
        config.setUpdatedAt(LocalDateTime.now());
        this.updateById(config);
        log.info("Toggled AI config: {} -> {}", id, enabled);
    }
}