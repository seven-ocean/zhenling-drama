package com.drama.service;

import com.aliyun.oss.OSSClient;
import com.drama.common.BusinessException;
import com.drama.common.ResultCode;
import com.drama.config.OssProperties;
import com.drama.entity.Asset;
import com.drama.mapper.AssetMapper;
import com.drama.common.IdUtils;
import lombok.Data;
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
import java.util.stream.Collectors;

/**
 * 文件存储服务 - 支持 本地存储和阿里云OSS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AssetMapper assetMapper;
    private final OssProperties ossProps;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 上传文件（自动选择存储方式）
     */
    @Transactional
    public Asset uploadFile(MultipartFile file, String dramaId, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        String filename = file.getOriginalFilename();
        String ext =getExtension(filename);
        String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;
        String datePath = LocalDateTime.now().format(DATE_FORMAT);
        String relativePath = ossProps.getBasePath() + "/" + datePath + "/" + newFilename;

        long fileSize = file.getSize();
        String mimeType = file.getContentType();

        // 图片尺寸
        Integer width = null;
        Integer height = null;
        if (mimeType != null && mimeType.startsWith("image/")) {
            try {
                // 先保存到临时文件获取尺寸
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file.getInputStream());
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (Exception e) {
                log.debug("Failed to read image dimensions: {}", e.getMessage());
            }
        }

        String finalUrl;
        String filePath;

        if (ossProps.isEnabled()) {
            // 使用 OSS 存储
            filePath = uploadToOss(file, datePath, newFilename);
            if (StringUtils.hasText(ossProps.getCustomDomain())) {
                finalUrl = "https://" + ossProps.getCustomDomain() + "/" + relativePath;
            } else {
                // 构建标准OSS访问URL: https://bucket-name.endpoint/object-key
                String endpoint = ossProps.getEndpoint().replace("https://", "").replace("http://", "");
                finalUrl = "https://" + ossProps.getBucketName() + "." + endpoint + "/" + relativePath;
            }
        } else {
            // 使用本地文件系统
            filePath = saveToLocal(file, datePath, newFilename);
            finalUrl = "/api/v1/assets/download/" + relativePath;
        }

        // 保存记录
        Asset asset = new Asset();
        asset.setId(IdUtils.randomId());
        asset.setDramaId(dramaId);
        asset.setType(type != null ? type : guessType(mimeType));
        asset.setFilename(filename);
        asset.setFilePath(relativePath);
        asset.setFileUrl(finalUrl);
        asset.setFileSize(fileSize);
        asset.setMimeType(mimeType);
        asset.setWidth(width);
        asset.setHeight(height);
        asset.setSourceType(ossProps.isEnabled() ? "oss" : "local");
        asset.setDeleted(0);

        assetMapper.insert(asset);
        log.info("Uploaded asset: {} via {}", asset.getId(), asset.getSourceType());

        return asset;
    }

    /**
     * 批量上传文件
     */
    public List<Asset> batchUpload(MultipartFile[] files, String dramaId, String type) {
        return List.of(files).stream()
            .map(f -> uploadFile(f, dramaId, type))
            .collect(Collectors.toList());
    }

    /**
     * 删除文件及数据库记录
     */
    @Transactional
    public void deleteAsset(String id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null || asset.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }

        if ("oss".equals(asset.getSourceType())) {
            // 从 OSS Bucket 真正删除对象
            com.aliyun.oss.OSSClient client = null;
            try {
                client = createOssClient();
                client.deleteObject(ossProps.getBucketName(), asset.getFilePath());
                log.info("[OSS] Deleted object: {} from bucket: {}", asset.getFilePath(), ossProps.getBucketName());
            } catch (Exception e) {
                log.warn("[OSS] Delete failed for {}: {}", asset.getFilePath(), e.getMessage());
                // 不抛异常，允许数据库记录继续标记删除（最终一致性）
            } finally {
                if (client != null) client.shutdown();
            }
        } else {
            try {
                Path fullPath = Paths.get("./data/storage", asset.getFilePath());
                Files.deleteIfExists(fullPath);
            } catch (IOException e) {
                log.warn("Delete local file failed: {}", e.getMessage());
            }
        }

        asset.setDeleted(1);
        assetMapper.updateById(asset);
        log.info("Deleted asset: {}", id);
    }

    /**
     * 获取存储状态信息
     */
    public StorageStatus getStatus() {
        StorageStatus status = new StorageStatus();
        status.setEnabled(ossProps.isEnabled());
        status.setProvider(ossProps.getProvider());
        status.setBucketName(ossProps.getBucketName());
        status.setEndpoint(ossProps.getEndpoint());

        // 统计本地存储使用情况
        File storageDir = new File("./data/storage");
        if (storageDir.exists()) {
            long totalBytes = getDirSize(storageDir.toPath());
            status.setLocalUsedBytes(totalBytes);
            status.setLocalUsedMB(totalBytes / (1024 * 1024));
        }
        return status;
    }

    private String uploadToOss(MultipartFile file, String datePath, String newFilename) {
        // 阿里云 OSS SDK 上传（设置公开读取权限，否则浏览器访问会 AccessDenied）
        com.aliyun.oss.OSSClient client = null;
        try {
            client = createOssClient();
            String objectKey = ossProps.getBasePath() + "/" + datePath + "/" + newFilename;

            // 设置 Object 元数据：公开读权限
            com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            // 上传时直接设为 public-read，确保通过 URL 可直接访问
            com.aliyun.oss.model.PutObjectRequest putReq =
                new com.aliyun.oss.model.PutObjectRequest(ossProps.getBucketName(), objectKey, file.getInputStream(), metadata);
            client.putObject(putReq);

            // 确保对象 ACL 为公开可读
            client.setObjectAcl(ossProps.getBucketName(), objectKey,
                com.aliyun.oss.model.CannedAccessControlList.PublicRead);

            return objectKey;
        } catch (Exception e) {
            log.error("OSS upload failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件上传失败: " + e.getMessage());
        } finally {
            if (client != null) client.shutdown();
        }
    }

    private com.aliyun.oss.OSSClient createOssClient() {
        return (OSSClient) new com.aliyun.oss.OSSClientBuilder()
            .build(ossProps.getEndpoint(), ossProps.getAccessKeyId(), ossProps.getAccessKeySecret());
    }

    private String saveToLocal(MultipartFile file, String datePath, String newFilename) {
        String basePath = "./data/storage";
        String dirPath = basePath + "/" + ossProps.getBasePath() + "/" + datePath;
        File directory = new File(dirPath);
        if (!directory.exists()) directory.mkdirs();

        Path fullPath = Paths.get(dirPath, newFilename);
        try {
            file.transferTo(fullPath.toFile());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件保存失败");
        }
        return ossProps.getBasePath() + "/" + datePath + "/" + newFilename;
    }

    private long getDirSize(Path path) {
        try {
            return Files.walk(path).filter(p -> !Files.isDirectory(p)).mapToLong(p -> {
                try { return Files.size(p); } catch (IOException e) { return 0L; }
            }).sum();
        } catch (IOException e) { return 0; }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : "";
    }

    private String guessType(String mimeType) {
        if (mimeType == null) return "file";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        return "file";
    }

    /**
     * 存储状态 DTO
     */
    @Data
    public static class StorageStatus {
        private boolean enabled;
        private String provider;
        private String bucketName;
        private String endpoint;
        private long localUsedBytes;
        private long localUsedMB;
    }
}
