package com.yupi.yuaiagent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为所有响应添加安全与缓存相关头，满足 webhint 等检查。
 * - X-Content-Type-Options: 防止 MIME 嗅探
 * - Cache-Control: 替代 Expires，静态资源可缓存
 * - Content-Type charset: 由 Spring 默认处理，此处仅补充安全头
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 静态资源带 hash，可长期缓存；HTML 不缓存便于更新
        String path = request.getRequestURI();
        if (path != null && (path.startsWith("/api/assets/") || path.endsWith(".js") || path.endsWith(".css"))) {
            response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        } else if (path != null && path.equals("/api") || path.equals("/api/")) {
            response.setHeader("Cache-Control", "no-cache");
        }
        filterChain.doFilter(request, response);
    }
}
