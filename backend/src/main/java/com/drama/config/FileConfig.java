package com.drama.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件上传配置
 */
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileConfig {

    private String path = "./data/storage";
    private String maxSize = "100MB";
    private String staticPath = "/static";
    private String urlPrefix = "/static";

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getMaxSize() { return maxSize; }
    public void setMaxSize(String maxSize) { this.maxSize = maxSize; }
    public String getStaticPath() { return staticPath; }
    public void setStaticPath(String staticPath) { this.staticPath = staticPath; }
    public String getUrlPrefix() { return urlPrefix; }
    public void setUrlPrefix(String urlPrefix) { this.urlPrefix = urlPrefix; }

    public long getMaxSizeBytes() {
        String size = maxSize.toUpperCase().replace("MB", "").replace("KB", "").replace("GB", "");
        if (maxSize.toUpperCase().contains("MB")) {
            return Long.parseLong(size) * 1024 * 1024;
        } else if (maxSize.toUpperCase().contains("KB")) {
            return Long.parseLong(size) * 1024;
        } else if (maxSize.toUpperCase().contains("GB")) {
            return Long.parseLong(size) * 1024 * 1024 * 1024;
        }
        return 100 * 1024 * 1024;
    }
}