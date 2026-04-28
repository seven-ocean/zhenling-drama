package com.drama.service;

import com.drama.config.OssProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * OSS 配置持久化服务
 * 将用户在页面上修改的 OSS 配置写入本地文件，重启后自动恢复。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssConfigPersistence {

    private final OssProperties ossProps;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Path CONFIG_PATH = Path.of("./data", "oss-config.json");

    /**
     * 启动时加载持久化的 OSS 配置，覆盖 application.yml 默认值
     */
    @PostConstruct
    public void loadPersistedConfig() {
        try {
            File configFile = CONFIG_PATH.toFile();
            if (!configFile.exists()) return;

            String json = Files.readString(CONFIG_PATH);
            Map<String, Object> saved = objectMapper.readValue(json, Map.class);
            if (saved == null || saved.isEmpty()) return;

            // 覆盖到 OssProperties bean
            if (saved.containsKey("enabled")) ossProps.setEnabled(Boolean.parseBoolean(String.valueOf(saved.get("enabled"))));
            if (saved.containsKey("provider")) ossProps.setProvider(String.valueOf(saved.get("provider")));
            if (saved.containsKey("endpoint")) ossProps.setEndpoint(String.valueOf(saved.get("endpoint")));
            if (saved.containsKey("accessKeyId")) ossProps.setAccessKeyId(String.valueOf(saved.get("accessKeyId")));
            if (saved.containsKey("accessKeySecret")) ossProps.setAccessKeySecret(String.valueOf(saved.get("accessKeySecret")));
            if (saved.containsKey("bucketName")) ossProps.setBucketName(String.valueOf(saved.get("bucketName")));
            if (saved.containsKey("customDomain")) {
                Object v = saved.get("customDomain");
                ossProps.setCustomDomain(v != null && !"null".equals(String.valueOf(v)) ? String.valueOf(v) : null);
            }

            log.info("[OSS] Loaded persisted config from file: enabled={}, provider={}, bucket={}",
                    ossProps.isEnabled(), ossProps.getProvider(), ossProps.getBucketName());
        } catch (Exception e) {
            log.warn("[OSS] Failed to load persisted config: {}", e.getMessage());
        }
    }

    /**
     * 将当前 OSS 配置持久化到文件
     */
    public void persist(Map<String, String> config) {
        try {
            File dir = CONFIG_PATH.getParent().toFile();
            if (!dir.exists()) dir.mkdirs();

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(CONFIG_PATH.toFile(), config);
            log.info("[OSS] Config persisted to file");
        } catch (Exception e) {
            log.error("[OSS] Failed to persist config: {}", e.getMessage());
        }
    }

    /**
     * 清除持久化文件（重置为 application.yml 值）
     */
    public void clear() {
        try {
            Files.deleteIfExists(CONFIG_PATH);
            log.info("[OSS] Persisted config cleared");
        } catch (Exception e) {
            log.warn("[OSS] Failed to clear persisted config: {}", e.getMessage());
        }
    }
}
