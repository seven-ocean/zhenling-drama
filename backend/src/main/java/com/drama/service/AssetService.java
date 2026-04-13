package com.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drama.common.BusinessException;
import com.drama.common.IdUtils;
import com.drama.common.ResultCode;
import com.drama.config.FileConfig;
import com.drama.entity.Asset;
import com.drama.mapper.AssetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 素材服务 - 文件上传/管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService extends ServiceImpl<AssetMapper, Asset> {

    private final FileConfig fileConfig;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 上传文件
     */
    @Transactional
    public Asset upload(MultipartFile file, String dramaId, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        // 生成存储路径
        String datePath = LocalDateTime.now().format(DATE_FORMAT);
        String filename = file.getOriginalFilename();
        String ext = getExtension(filename);
        String newFilename = UUID.randomUUID() + ext;
        
        // 确保目录存在
        String basePath = fileConfig.getPath();
        String relativePath = "assets/" + datePath;
        File directory = new File(basePath + "/" + relativePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 保存文件
        Path fullPath = Paths.get(basePath, relativePath, newFilename);
        try {
            file.transferTo(fullPath.toFile());
        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件保存失败");
        }

        // 获取文件信息
        long fileSize = file.getSize();
        String mimeType = file.getContentType();

        // 获取图片尺寸
        Integer width = null;
        Integer height = null;
        if (mimeType != null && mimeType.startsWith("image/")) {
            try {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(fullPath.toFile());
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (Exception e) {
                log.debug("Failed to read image dimensions: {}", e.getMessage());
            }
        }

        // 保存记录到数据库
        Asset asset = new Asset();
        asset.setId(IdUtils.randomId());
        asset.setDramaId(dramaId);
        asset.setType(type != null ? type : guessType(mimeType));
        asset.setFilename(filename);
        asset.setFilePath(relativePath + "/" + newFilename);
        asset.setFileUrl(fileConfig.getUrlPrefix() + "/" + relativePath + "/" + newFilename);
        asset.setFileSize(fileSize);
        asset.setMimeType(mimeType);
        asset.setWidth(width);
        asset.setHeight(height);
        asset.setSourceType("uploaded");
        asset.setCreatedAt(LocalDateTime.now());
        asset.setDeleted(0);

        this.save(asset);
        log.info("Uploaded asset: {}", asset.getId());

        return asset;
    }

    /**
     * 按剧集查询素材
     */
    public List<Asset> listByDramaId(String dramaId, String type) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Asset::getDramaId, dramaId)
               .eq(Asset::getDeleted, 0);
        
        if (StringUtils.hasText(type)) {
            wrapper.eq(Asset::getType, type);
        }
        wrapper.orderByDesc(Asset::getCreatedAt);
        
        return this.list(wrapper);
    }

    /**
     * 分页查询
     */
    public Page<Asset> page(int pageNum, int pageSize, String dramaId, String type) {
        Page<Asset> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Asset::getDeleted, 0);
        
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
        Asset asset = this.getById(id);
        if (asset == null || asset.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }
        
        // 删除物理文件
        try {
            Path fullPath = Paths.get(fileConfig.getPath(), asset.getFilePath());
            Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            log.warn("Delete file failed: {}", e.getMessage());
        }
        
        asset.setDeleted(1);
        this.updateById(asset);
        log.info("Deleted asset: {}", id);
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }

    private String guessType(String mimeType) {
        if (mimeType == null) return "file";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        return "file";
    }
}
