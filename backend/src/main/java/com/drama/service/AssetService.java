package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.ResultCode;
import com.drama.entity.Asset;
import com.drama.mapper.AssetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 素材服务 - 文件上传/管理
 * 使用 FileStorageService 进行实际的文件存储（支持本地存储和OSS）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService extends ServiceImpl<AssetMapper, Asset> {

    private final FileStorageService fileStorageService;

    /**
     * 上传文件
     * 委托给 FileStorageService 处理，支持自动选择本地存储或OSS
     */
    @Transactional
    public Asset upload(MultipartFile file, String dramaId, String type) {
        // 直接调用 FileStorageService 的 uploadFile 方法
        // 该方法会自动根据配置选择本地存储或OSS
        return fileStorageService.uploadFile(file, dramaId, type);
    }

    /**
     * 按剧集查询素材
     * 注意：MyBatis-Plus已配置全局逻辑删除，会自动添加 deleted = 0 条件
     */
    public List<Asset> listByDramaId(String dramaId, String type) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Asset::getDramaId, dramaId);
        
        if (StringUtils.hasText(type)) {
            wrapper.eq(Asset::getType, type);
        }
        wrapper.orderByDesc(Asset::getCreatedAt);
        
        return this.list(wrapper);
    }

    /**
     * 分页查询
     * 注意：MyBatis-Plus已配置全局逻辑删除，会自动添加 deleted = 0 条件
     */
    public Page<Asset> page(int pageNum, int pageSize, String dramaId, String type) {
        Page<Asset> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(dramaId)) {
            wrapper.eq(Asset::getDramaId, dramaId);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Asset::getType, type);
        }
        wrapper.orderByDesc(Asset::getCreatedAt);
        
        return this.page(page, wrapper);
    }

    /**
     * 删除素材
     */
    @Transactional
    public void delete(String id) {
        fileStorageService.deleteAsset(id);
    }
}
