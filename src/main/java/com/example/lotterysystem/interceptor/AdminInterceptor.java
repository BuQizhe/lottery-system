package com.example.lotterysystem.interceptor;

import com.example.lotterysystem.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 管理员权限拦截器
 *
 * 作用：所有 /api/admin/** 的请求都会被这个拦截器拦截
 * 检查用户是否已登录且角色为 ADMIN，否则返回 403 禁止访问
 *
 * 之前的问题：管理后台接口完全没有后端鉴权，任何人直接调 API 就能操作
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 在 Controller 方法执行之前拦截
     * 返回 true 表示放行，返回 false 表示拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        // 从 session 中获取当前登录用户
        User user = (User) request.getSession().getAttribute("user");

        // 未登录或不是管理员，拒绝访问
        if (user == null || !"ADMIN".equals(user.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403
            response.setContentType("application/json;charset=UTF-8");
            // 返回 JSON 格式的错误信息
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            Map.of("code", 403, "message", "无权限访问，请使用管理员账号登录")
                    )
            );
            return false;  // 拦截请求，不继续执行 Controller
        }

        return true;  // 是管理员，放行
    }
}
