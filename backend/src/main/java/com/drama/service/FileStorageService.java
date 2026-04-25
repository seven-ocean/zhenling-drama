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

import java.io.ByteArrayInputStream;
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
        // 优先根据 MIME 类型判断文件类型
        String detectedType = guessType(mimeType);
        // 如果MIME类型无法准确判断（如application/octet-stream），用扩展名二次检测
        if ("file".equals(detectedType) && filename != null) {
            detectedType = guessTypeByFilename(filename);
        }
        // 最终兜底：如果前端传了type且检测结果为file，以前端传的为准（但image/video/audio以检测为准）
        if ("file".equals(detectedType) && type != null) {
            detectedType = type;
        }
        asset.setType(detectedType);
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
     * 上传字节数据（用于AI生成的图片归档等场景，无需 MultipartFile）
     * 内部将 byte[] 包装为 InputStream 进行上传
     */
    @Transactional
    public Asset uploadBytes(byte[] data, String filename, String dramaId, String type, String mimeType) {
        if (data == null || data.length == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件数据不能为空");
        }

        String ext = getExtension(filename);
        String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;
        String datePath = LocalDateTime.now().format(DATE_FORMAT);
        String relativePath = ossProps.getBasePath() + "/" + datePath + "/" + newFilename;

        long fileSize = data.length;
        // 使用传入的 mimeType 或根据扩展名猜测
        String effectiveMime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : guessTypeByFilename(filename);

        // 图片尺寸检测（仅对图片类型）
        Integer width = null;
        Integer height = null;
        if ((type != null && type.startsWith("image")) || effectiveMime.startsWith("image/")) {
            try {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(data));
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (Exception e) {
                log.debug("Failed to read image dimensions from bytes: {}", e.getMessage());
            }
        }

        String finalUrl;
        String filePath;

        if (ossProps.isEnabled()) {
            filePath = uploadBytesToOss(data, datePath, newFilename, effectiveMime);
            if (StringUtils.hasText(ossProps.getCustomDomain())) {
                finalUrl = "https://" + ossProps.getCustomDomain() + "/" + relativePath;
            } else {
                String endpoint = ossProps.getEndpoint().replace("https://", "").replace("http://", "");
                finalUrl = "https://" + ossProps.getBucketName() + "." + endpoint + "/" + relativePath;
            }
        } else {
            filePath = saveBytesToLocal(data, datePath, newFilename);
            finalUrl = "/api/v1/assets/download/" + relativePath;
        }

        Asset asset = new Asset();
        asset.setId(IdUtils.randomId());
        asset.setDramaId(dramaId);
        asset.setType(type != null ? type : guessType(effectiveMime));
        asset.setFilename(filename);
        asset.setFilePath(relativePath);
        asset.setFileUrl(finalUrl);
        asset.setFileSize(fileSize);
        asset.setMimeType(effectiveMime);
        asset.setWidth(width);
        asset.setHeight(height);
        asset.setSourceType(ossProps.isEnabled() ? "oss" : "local");
        asset.setDeleted(0);

        assetMapper.insert(asset);
        log.info("[UploadBytes] Uploaded asset: {} via {}, size={}bytes", asset.getId(), asset.getSourceType(), fileSize);

        return asset;
    }

    private String uploadBytesToOss(byte[] data, String datePath, String newFilename, String contentType) {
        com.aliyun.oss.OSSClient client = null;
        try {
            client = createOssClient();
            String objectKey = ossProps.getBasePath() + "/" + datePath + "/" + newFilename;

            com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
            metadata.setContentLength(data.length);
            metadata.setContentType(contentType);

            com.aliyun.oss.model.PutObjectRequest putReq =
                new com.aliyun.oss.model.PutObjectRequest(
                    ossProps.getBucketName(), objectKey,
                    new ByteArrayInputStream(data), metadata);
            client.putObject(putReq);

            // 确保对象 ACL 为公开可读
            client.setObjectAcl(ossProps.getBucketName(), objectKey,
                com.aliyun.oss.model.CannedAccessControlList.PublicRead);

            return objectKey;
        } catch (Exception e) {
            log.error("[OSS] Upload bytes failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件上传失败(OSS): " + e.getMessage());
        } finally {
            if (client != null) client.shutdown();
        }
    }

    private String saveBytesToLocal(byte[] data, String datePath, String newFilename) {
        String basePath = "./data/storage";
        String dirPath = basePath + "/" + ossProps.getBasePath() + "/" + datePath;
        File directory = new File(dirPath);
        if (!directory.exists()) directory.mkdirs();

        Path fullPath = Paths.get(dirPath, newFilename);
        try {
            Files.write(fullPath, data);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件保存失败");
        }
        return ossProps.getBasePath() + "/" + datePath + "/" + newFilename;
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
        log.info("[DeleteAsset] Starting delete for id: {}", id);
        
        // 由于@TableLogic，selectById会自动过滤已删除的记录
        // 如果需要查询包括已删除的记录，需要使用selectById(id, false)
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            log.warn("[DeleteAsset] Asset not found or already deleted: {}", id);
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在或已被删除");
        }
        
        log.info("[DeleteAsset] Found asset: {}, type: {}, source: {}", 
                 asset.getFilename(), asset.getType(), asset.getSourceType());

        // 检查文件路径是否存在（AI生成等外部URL可能没有file_path）
        String filePath = asset.getFilePath();
        if (filePath != null && !filePath.isEmpty()) {
            if ("oss".equals(asset.getSourceType())) {
                // 从 OSS Bucket 真正删除对象
                com.aliyun.oss.OSSClient client = null;
                try {
                    client = createOssClient();
                    client.deleteObject(ossProps.getBucketName(), filePath);
                    log.info("[OSS] Deleted object: {} from bucket: {}", filePath, ossProps.getBucketName());
                } catch (Exception e) {
                    log.warn("[OSS] Delete failed for {}: {}", filePath, e.getMessage());
                    // 不抛异常，允许数据库记录继续标记删除（最终一致性）
                } finally {
                    if (client != null) client.shutdown();
                }
            } else {
                try {
                    Path fullPath = Paths.get("./data/storage", filePath);
                    Files.deleteIfExists(fullPath);
                    log.info("[Local] Deleted file: {}", fullPath);
                } catch (IOException e) {
                    log.warn("Delete local file failed: {}", e.getMessage());
                }
            }
        } else {
            log.info("[DeleteAsset] No physical file to delete (file_path is null/empty), skipping storage deletion");
        }

        // 使用MyBatis-Plus的逻辑删除（会自动设置deleted=1）
        int result = assetMapper.deleteById(id);
        log.info("[DeleteAsset] Database delete result: {}, asset id: {}", result, id);
        
        if (result > 0) {
            log.info("[DeleteAsset] Successfully deleted asset: {}", id);
        } else {
            log.error("[DeleteAsset] Failed to delete asset from database: {}", id);
            throw new BusinessException(ResultCode.SERVER_ERROR, "删除失败，数据库更新失败");
        }
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
            if (totalBytes >= 0) {
                status.setLocalUsedBytes(totalBytes);
                status.setLocalUsedMB(totalBytes / (1024 * 1024));
            } else {
                // getDirSize 返回 -1 表示权限/IO异常，标记为未知
                status.setLocalUsedBytes(0);
                status.setLocalUsedMB(-1); // 前端可据此显示"未知"
                log.warn("Storage size calculation failed, reporting as unknown");
            }
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
        if (!Files.exists(path)) {
            return 0L; // 目录不存在，返回0是合理行为
        }
        try {
            return Files.walk(path).filter(p -> !Files.isDirectory(p)).mapToLong(p -> {
                try { return Files.size(p); } catch (IOException e) { return 0L; }
            }).sum();
        } catch (IOException e) {
            // 目录存在但无法遍历（权限不足/IO错误），记录日志并返回 -1 标记异常
            log.warn("Failed to calculate directory size for {}: {}", path, e.getMessage());
            return -1L;
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : "";
    }

    private String guessType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) return "file";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        // application/octet-stream 等通用MIME无法判断时返回file，由调用方根据扩展名二次判断
        return "file";
    }

    /**
     * 根据文件名扩展名判断类型（当MIME类型不可靠时的兜底方案）
     */
    private String guessTypeByFilename(String filename) {
        if (filename == null || filename.isEmpty()) return "file";
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) return "file";
        String ext = filename.substring(dot + 1).toLowerCase();
        // 视频格式
        if (ext.matches("mp4|mkv|avi|mov|wmv|flv|webm|m4v|ts|mts")) return "video";
        // 音频格式
        if (ext.matches("mp3|wav|ogg|aac|flac|m4a|wma|opus")) return "audio";
        // 图片格式
        if (ext.matches("jpg|jpeg|png|gif|bmp|webp|svg|ico|tiff|heic")) return "image";
        return "file";
    }

    /**
     * 获取文件的可访问URL
     * - 本地文件：返回相对路径
     * - OSS文件：如果配置了自定义域名返回自定义URL，否则返回标准OSS URL
     * 注意：OSS文件需要确保Bucket ACL为公共读或文件已设置PublicRead权限
     */
    public String getAccessibleUrl(Asset asset) {
        if (asset == null) return null;
        
        if ("local".equals(asset.getSourceType())) {
            return "/api/v1/assets/download/" + asset.getFilePath();
        }
        
        // OSS文件直接返回存储的URL
        return asset.getFileUrl();
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
