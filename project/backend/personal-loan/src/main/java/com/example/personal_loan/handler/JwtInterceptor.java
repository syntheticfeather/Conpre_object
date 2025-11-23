package com.example.personal_loan.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.personal_loan.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        log.info("【JwtInterceptor】请求路径: " + uri);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.info("【JwtInterceptor】OPTIONS 请求，不做任何校验");
            return true; // 不做任何校验
        }
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.error("Missing or invalid Authorization header");
            sendUnauthorizedJson(response, 401, "Missing or invalid Authorization header");
            return false;
        }
        String token = authorizationHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            log.error("JWT 验证失败");
            sendUnauthorizedJson(response, 401, "JWT 验证失败");
            return false;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        String userPhone = jwtUtil.getPhoneFromToken(token);
        /*
         * 可以直接controller中传入request参数，获取userId和userPhone     
         */
        request.setAttribute("userId", userId);
        request.setAttribute("userPhone", userPhone);
        log.info("【JwtInterceptor】请求成功，userId: " + userId + ", userPhone: " + userPhone);
        return true;
    }

    /**
     * 向前端返回统一的 JSON 错误响应，包含 HTTP 状态码和错误信息。 这里同时设置了一些常见的 CORS 头，方便前端在跨域场景下读取响应。
     */
    private void sendUnauthorizedJson(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            // 常用的 CORS 头（如果你已经在全局配置 CORS，可以去掉这些）
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");

            String json = String.format("{\"code\": %d, \"message\": \"%s\"}", status, escapeJson(message));
            response.getWriter().write(json);
            response.getWriter().flush();
        } catch (Exception e) {
            // 如果写响应失败，尽量用 sendError 退回基础信息
            try {
                response.sendError(status, message);
            } catch (Exception ignored) {
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
