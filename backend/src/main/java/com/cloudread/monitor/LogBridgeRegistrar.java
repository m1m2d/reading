package com.cloudread.monitor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class LogBridgeRegistrar {

    private final LogEventPublisher publisher;

    public LogBridgeRegistrar(LogEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostConstruct
    public void register() {
        LogbackWebSocketAppender.sink = publisher::publish;
    }

    @PreDestroy
    public void unregister() {
        LogbackWebSocketAppender.sink = null;
    }
}
