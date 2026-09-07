package com.fitness.security;

import com.fitness.common.BusinessException;
import com.fitness.common.ResultCode;
import com.fitness.entity.Admin;
import com.fitness.entity.User;
import com.fitness.mapper.AdminMapper;
import com.fitness.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器
 * - 总是尝试解析 Token 放入上下文（支持匿名接口的可选登录态）
 * - 根据 @RequireLogin / @RequireAdmin 注解强制校验身份
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 可选登录态：尝试解析 Token 并校验状态
        String token = resolveToken(request);
        if (token != null) {
            String role = jwtUtil.getRole(token);
            Long userId = jwtUtil.getUserId(token);
            if (userId != null && "admin".equals(role)) {
                Admin admin = adminMapper.selectById(userId);
                if (admin != null && admin.getStatus() != null && admin.getStatus() == 1) {
                    UserContext.set(userId, "admin");
                }
            } else if (userId != null) {
                User user = userMapper.selectById(userId);
                if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                    UserContext.set(userId, "user");
                }
            }
        }

        boolean needLogin = handlerMethod.hasMethodAnnotation(RequireLogin.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireLogin.class);
        boolean needAdmin = handlerMethod.hasMethodAnnotation(RequireAdmin.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAdmin.class);

        if (needAdmin) {
            if (!UserContext.isAdmin()) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "请使用管理员账号登录");
            }
            return true;
        }
        if (needLogin) {
            if (UserContext.getUserId() == null || UserContext.isAdmin()) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录，请先登录");
            }
            return true;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
