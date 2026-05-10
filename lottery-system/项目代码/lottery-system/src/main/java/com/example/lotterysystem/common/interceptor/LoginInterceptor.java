package com.example.lotterysystem.common.interceptor;

import com.example.lotterysystem.common.utils.JWTUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    // 放行路径列表（不需要登录就能访问）
    private final List<String> excludes = Arrays.asList(
            "/register",
            "/password/login",
            "/email-code/send",
            "/email-code/login",
            "/winning-records/export",      // 添加导出接口放行
            "/winning-records/export-data", // 添加导出接口放行
            "/blogin.html",
            "/login.html",
            "/register.html",
            "/css/",
            "/js/",
            "/pic/",
            "/favicon.ico"
    );

    /**
     * 判断请求路径是否需要放行
     */
    private boolean isExcluded(String uri) {
        for (String exclude : excludes) {
            if (uri.startsWith(exclude)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        log.info("请求路径：{}", uri);

        // 放行不需要登录的接口
        if (isExcluded(uri)) {
            log.info("放行路径：{}", uri);
            return true;
        }

        // 获取请求头中的 token
        String token = request.getHeader("user_token");
        log.info("获取token：{}", token);

        // 令牌解析
        Claims claims = JWTUtil.parseJWT(token);
        if (null == claims) {
            log.error("解析JWT令牌失败！");
            response.setStatus(401);
            return false;
        }

        log.info("解析JWT令牌成功！放行");
        return true;
    }
}