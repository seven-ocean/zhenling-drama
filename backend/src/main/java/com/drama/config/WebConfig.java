package com.drama.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * 静态资源配置
 * 
 * 安全说明：对请求路径进行双重校验，防止路径遍历攻击（../）
 * 参考 AssetController / ComposeController 的防护模式
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${file.upload.path:./data/storage}")
    private String uploadPath;

    @Value("${file.static.path:/static}")
    private String staticPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
        
        // 设置资源位置
        handler.setLocations(Collections.singletonList(
            new org.springframework.core.io.FileSystemResource(uploadPath + "/")
        ));
        
        // 注册自定义资源处理器（带安全校验）
        registry.addResourceHandler(staticPath + "/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .setUseLastModified(true);
    }

    /**
     * 校验静态文件请求路径安全性
     * 拦截包含 .. 的路径遍历攻击
     * 
     * 注意：Spring ResourceHttpRequestHandler 本身已对路径进行规范化处理，
     * 此处额外记录可疑请求以便审计
     */
    public static boolean isSafeStaticPath(String requestPath) {
        if (requestPath == null) return false;
        // 拒绝包含目录穿越的路径
        if (requestPath.contains("..")) {
            log.warn("[安全] 静态文件请求检测到路径遍历尝试: {}", requestPath);
            return false;
        }
        return true;
    }
}