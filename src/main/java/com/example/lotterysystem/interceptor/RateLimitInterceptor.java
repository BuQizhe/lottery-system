package com.example.lotterysystem.interceptor;

import com.example.lotterysystem.annotation.RateLimit;
import com.example.lotterysystem.common.result.ApiResponse;
import com.example.lotterysystem.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口限流拦截器
 * <p>
 * 基于"固定时间窗口"算法，通过注解 {@link RateLimit} 为接口配置限流策略。
 * <p>
 * 实现原理：
 * <ol>
 *   <li>以 "客户端IP:注解key" 为维度统计请求次数</li>
 *   <li>窗口内首次请求记录起始时间并计数=1</li>
 *   <li>后续请求检查时间窗口是否过期 → 过期则重置，未过期则累加计数</li>
 *   <li>超过阈值返回 429 Too Many Requests</li>
 * </ol>
 * 所有数据存储在内存 ConcurrentHashMap 中，重启后清零（适合单体应用）。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private final ConcurrentHashMap<String, RateLimitInfo> limitMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;

        RateLimit annotation = ((HandlerMethod) handler).getMethodAnnotation(RateLimit.class);
        if (annotation == null) return true;

        String key = getClientIp(request) + ":" + annotation.key();
        long now = System.currentTimeMillis();
        RateLimitInfo info = limitMap.compute(key, (k, v) -> {
            if (v == null || now - v.firstRequestTime > annotation.time() * 1000L) {
                return new RateLimitInfo(1, now);
            }
            v.count++;
            return v;
        });

        if (info.count > annotation.count()) {
            log.warn("限流触发: key={}, count={}", key, info.count);
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);
            PrintWriter out = response.getWriter();
            out.write(new ObjectMapper().writeValueAsString(
                    ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS, annotation.message())));
            out.flush();
            return false;
        }
        return true;
    }

    /** 获取客户端 IP（支持代理穿透） */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }

    /** 固定时间窗口记录 */
    private static class RateLimitInfo {
        int count;
        long firstRequestTime;
        RateLimitInfo(int count, long firstRequestTime) {
            this.count = count;
            this.firstRequestTime = firstRequestTime;
        }
    }
}
