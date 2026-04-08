package com.drama.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:./data/storage}")
    private String uploadPath;

    @Value("${file.static.path:/static}")
    private String staticPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态文件访问
        registry.addResourceHandler(staticPath + "/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}