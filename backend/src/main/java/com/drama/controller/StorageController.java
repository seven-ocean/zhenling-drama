package com.drama.controller;

import com.drama.common.R;
import com.drama.config.OssProperties;
import com.drama.entity.Asset;
import com.drama.service.FileStorageService;
import com.drama.service.OssConfigPersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件存储管理接口（含OSS配置）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final FileStorageService fileStorageService;
    private final OssProperties ossProperties;
    private final OssConfigPersistence configPersistence;

    /**
     * 获取存储状态
     */
    @GetMapping("/status")
    public R<FileStorageService.StorageStatus> getStatus() {
        return R.ok(fileStorageService.getStatus());
    }

    /**
     * 上传文件（通用）
     */
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dramaId", required = false) String dramaId,
            @RequestParam(value = "type", required = false) String type) {
        Asset asset = fileStorageService.uploadFile(file, dramaId, type);
        Map<String, Object> result = new HashMap<>();
        result.put("id", asset.getId());
        result.put("filename", asset.getFilename());
        result.put("fileUrl", asset.getFileUrl());
        result.put("filePath", asset.getFilePath());
        result.put("mimeType", asset.getMimeType());
        result.put("fileSize", asset.getFileSize());
        result.put("width", asset.getWidth());
        result.put("height", asset.getHeight());
        return R.ok(result);
    }

    /**
     * 批量上传
     */
    @PostMapping("/batch-upload")
    public R<List<Map<String, Object>>> batchUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "dramaId") String dramaId,
            @RequestParam(value = "type", required = false) String type) {
        List<Asset> assets = fileStorageService.batchUpload(files, dramaId, type);
        List<Map<String, Object>> results = assets.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("filename", a.getFilename());
            m.put("fileUrl", a.getFileUrl());
            m.put("mimeType", a.getMimeType());
            m.put("fileSize", a.getFileSize());
            return m;
        }).toList();
        return R.ok(results);
    }

    /**
     * 删除文件
     */
    @PostMapping("/{id}/delete")
    public R<Void> delete(@PathVariable String id) {
        fileStorageService.deleteAsset(id);
        return R.ok();
    }

    /**
     * 更新 OSS 配置（内存 + 文件持久化）
     */
    @PostMapping("/oss-config")
    public R<Void> updateOssConfig(@RequestBody Map<String, String> config) {
        // 动态更新内存中的配置
        if (config.containsKey("enabled")) ossProperties.setEnabled(Boolean.parseBoolean(config.get("enabled")));
        if (config.containsKey("provider")) ossProperties.setProvider(config.get("provider"));
        if (config.containsKey("endpoint")) ossProperties.setEndpoint(config.get("endpoint"));
        if (config.containsKey("accessKeyId")) ossProperties.setAccessKeyId(config.get("accessKeyId"));
        if (config.containsKey("accessKeySecret")) ossProperties.setAccessKeySecret(config.get("accessKeySecret"));
        if (config.containsKey("bucketName")) ossProperties.setBucketName(config.get("bucketName"));
        if (config.containsKey("customDomain")) ossProperties.setCustomDomain(config.get("customDomain"));

        // 持久化到文件（重启后自动恢复）
        configPersistence.persist(config);

        log.info("OSS config updated: provider={}, bucket={}, enabled={} [persisted]",
                ossProperties.getProvider(), ossProperties.getBucketName(), ossProperties.isEnabled());

        return R.ok();
    }

    /**
     * 获取当前 OSS 配置（脱敏）
     */
    @GetMapping("/oss-config")
    public R<Map<String, String>> getOssConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("provider", ossProperties.getProvider());
        config.put("endpoint", ossProperties.getEndpoint());
        config.put("bucketName", ossProperties.getBucketName());
        config.put("customDomain", ossProperties.getCustomDomain() != null ? ossProperties.getCustomDomain() : "");
        config.put("basePath", ossProperties.getBasePath());
        config.put("enabled", String.valueOf(ossProperties.isEnabled()));
        // 脱敏 key 信息
        if (StringUtils.hasText(ossProperties.getAccessKeyId())) {
            config.put("accessKeyId", maskKey(ossProperties.getAccessKeyId()));
        }
        config.put("hasCredentials", String.valueOf(StringUtils.hasText(ossProperties.getAccessKeyId())));
        return R.ok(config);
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 8) return "***";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
