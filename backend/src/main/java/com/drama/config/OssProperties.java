package com.drama.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS 存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /**
     * 是否启用 OSS 存储（false 则使用本地文件系统）
     */
    private boolean enabled = false;

    /**
     * 存储提供商: aliyun / volcengine / tencentcloud
     */
    private String provider = "aliyun";

    /**
     * 阿里云配置
     */
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 自定义域名（可选，用于 CDN 加速）
     */
    private String customDomain;

    /**
     * 文件访问路径前缀（如 drama-assets）
     */
    private String basePath = "drama-assets";
}
