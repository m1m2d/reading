package com.cloudread.config;

import com.cloudread.monitor.LogStreamWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LogStreamWebSocketHandler logStreamWebSocketHandler;

    public WebSocketConfig(LogStreamWebSocketHandler logStreamWebSocketHandler) {
        this.logStreamWebSocketHandler = logStreamWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logStreamWebSocketHandler, "/ws/logs")
                .setAllowedOrigins("*");
    }
}
