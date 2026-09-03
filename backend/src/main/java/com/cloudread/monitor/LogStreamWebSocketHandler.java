package com.cloudread.monitor;

import com.cloudread.entity.SysUser;
import com.cloudread.security.JwtUtil;
import com.cloudread.security.UserCache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

@Component
public class LogStreamWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LogStreamWebSocketHandler.class);

    private final JwtUtil jwtUtil;
    private final UserCache userCache;
    private final LogEventPublisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LogStreamWebSocketHandler(JwtUtil jwtUtil, UserCache userCache, LogEventPublisher publisher) {
        this.jwtUtil = jwtUtil;
        this.userCache = userCache;
        this.publisher = publisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 兼容 query 参数携带 token 的方式
        String token = tokenFromQuery(session);
        if (token != null && isAdminToken(token)) {
            activate(session);
        }
        // 否则等待客户端在首条消息中发送 {"token":"..."}
    }

    private void activate(WebSocketSession session) {
        try {
            publisher.register(session);
            session.sendMessage(new TextMessage(publisher.toJson("history", publisher.recentHistory())));
            log.info("日志流 WebSocket 已连接: {}", session.getId());
        } catch (Exception e) {
            log.error("日志流初始化失败: {}", e.getMessage(), e);
        }
    }

    private String tokenFromQuery(WebSocketSession session) {
        try {
            String query = session.getUri() == null ? null : session.getUri().getQuery();
            if (query != null && query.startsWith("token=")) {
                return query.substring("token=".length());
            }
        } catch (Exception e) {
            log.debug("解析 token query 失败: {}", e.getMessage());
        }
        return null;
    }

    private boolean isAdminToken(String token) {
        SysUser user = authenticate(token);
        return user != null && SysUser.ROLE_ADMIN.equals(user.getRole());
    }

    private SysUser authenticate(String token) {
        if (token == null || token.isBlank()) {
            log.warn("WS 鉴权: token 为空");
            return null;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            SysUser user = userCache.getByUsername(claims.getSubject());
            log.warn("WS 鉴权: subject={} userExists={} role={}", claims.getSubject(), user != null,
                    user == null ? null : user.getRole());
            return user;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("WS 鉴权: JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            java.util.Map<?, ?> payload = objectMapper.readValue(message.getPayload(), java.util.Map.class);
            Object token = payload.get("token");
            if (token != null) {
                if (isAdminToken(String.valueOf(token))) {
                    activate(session);
                } else {
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("未授权"));
                }
            }
        } catch (Exception e) {
            log.debug("日志流消息处理失败: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        publisher.unregister(session);
        log.info("日志流 WebSocket 已断开: {}", session.getId());
    }
}
