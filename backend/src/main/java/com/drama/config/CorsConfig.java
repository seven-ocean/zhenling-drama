package com.drama.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS跨域配置
 * 从 application.yml 读取允许的来源列表，不再使用通配符 *
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3013}")
    private String allowedOriginsStr;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // 解析逗号分隔的字符串为列表
        String[] origins = allowedOriginsStr.split(",");
        for (String origin : origins) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                config.addAllowedOrigin(trimmed);
            }
        }

        // 兜底：如果白名单为空（配置缺失），至少允许 localhost 开发环境
        if (config.getAllowedOrigins() == null || config.getAllowedOrigins().isEmpty()) {
            config.addAllowedOrigin("http://localhost:5173");
            config.addAllowedOrigin("http://localhost:3013");
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}