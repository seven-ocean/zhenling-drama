package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.entity.Drama;
import com.drama.mapper.DramaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 剧集服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DramaService extends ServiceImpl<DramaMapper, Drama> {

    /**
     * 创建剧集
     */
    @Transactional
    public Drama create(Drama drama) {
        if (!StringUtils.hasText(drama.getId())) {
            drama.setId(IdUtils.randomId());
        }
        drama.setStatus("draft");
        drama.setTotalEpisodes(drama.getTotalEpisodes() != null ? drama.getTotalEpisodes() : 0);
        drama.setCreatedEpisodes(0);
        drama.setCreatedAt(LocalDateTime.now());
        drama.setUpdatedAt(LocalDateTime.now());
        drama.setDeleted(0);
        
        this.save(drama);
        log.info("Created drama: {}", drama.getId());
        return drama;
    }

    /**
     * 分页查询
     */
    public IPage<Drama> page(int pageNum, int pageSize, String status, String keyword) {
        LambdaQueryWrapper<Drama> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Drama::getDeleted, 0);
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(Drama::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Drama::getTitle, keyword);
        }
        wrapper.orderByDesc(Drama::getCreatedAt);
        
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 获取详情
     */
    public Drama getById(String id) {
        Drama drama = super.getById(id);
        if (drama == null || drama.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "剧集不存在");
        }
        return drama;
    }

    /**
     * 更新剧集
     */
    @Transactional
    public Drama update(Drama drama) {
        if (!StringUtils.hasText(drama.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "ID不能为空");
        }
        Drama existing = this.getById(drama.getId());
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "剧集不存在");
        }
        drama.setUpdatedAt(LocalDateTime.now());
        this.updateById(drama);
        log.info("Updated drama: {}", drama.getId());
        return this.getById(drama.getId());
    }

    /**
     * 删除剧集
     */
    @Transactional
    public void delete(String id) {
        Drama drama = this.getById(id);
        if (drama == null || drama.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "剧集不存在");
        }
        drama.setDeleted(1);
        drama.setUpdatedAt(LocalDateTime.now());
        this.updateById(drama);
        log.info("Deleted drama: {}", id);
    }

    /**
     * 更新状态
     */
    @Transactional
    public void updateStatus(String id, String status) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "ID不能为空");
        }
        Drama drama = new Drama();
        drama.setId(id);
        drama.setStatus(status);
        drama.setUpdatedAt(LocalDateTime.now());
        this.updateById(drama);
        log.info("Updated drama status: {} -> {}", id, status);
    }

    /**
     * 更新集数
     */
    @Transactional
    public void updateEpisodeCount(String id, int count) {
        Drama drama = this.getById(id);
        if (drama != null) {
            drama.setCreatedEpisodes(count);
            drama.setUpdatedAt(LocalDateTime.now());
            this.updateById(drama);
        }
    }
}