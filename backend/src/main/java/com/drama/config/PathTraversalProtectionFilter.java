package com.drama.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 静态资源路径遍历防护过滤器
 * 
 * 拦截 /static/** 请求，检测并拒绝包含目录穿越（../）的恶意请求。
 * 与 WebConfig 配合使用，构成双重防护。
 * 
 * 防护层级：
 * 1. 本 Filter：在 Servlet 层直接拦截，返回 403
 * 2. Spring ResourceHttpRequestHandler：内部已做 normalize() 规范化
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class PathTraversalProtectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PathTraversalProtectionFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 仅检查静态资源路径（可配置）
        if (isStaticResourceRequest(uri)) {
            if (containsPathTraversal(uri)) {
                log.warn("[安全] 拦截路径遍历攻击: IP={}, URI={}, UserAgent={}",
                        getClientIp(request),
                        uri,
                        request.getHeader("User-Agent"));
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "访问被拒绝");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否为静态资源请求
     */
    private boolean isStaticResourceRequest(String uri) {
        return uri != null && (uri.startsWith("/static/") || uri.equals("/static"));
    }

    /**
     * 检测路径遍历特征
     */
    private boolean containsPathTraversal(String path) {
        if (path == null) return false;
        // 检测 URL 编码的 ../ (%2e%2e%2f, %2e%2e/)
        String decoded = path
                .toLowerCase()
                .replace("%2e", ".")
                .replace("%2f", "/")
                .replace("%5c", "\\");
        return decoded.contains("..") || decoded.contains("./") || decoded.contains(".\\");
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri;
        }
        return request.getRemoteAddr();
    }
}
