package com.cloudread.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LogEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LogEventPublisher.class);
    private static final int HISTORY_SIZE = 1000;

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedDeque<LogEvent> history = new ConcurrentLinkedDeque<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publish(LogEvent event) {
        history.addLast(event);
        while (history.size() > HISTORY_SIZE) {
            history.pollFirst();
        }
        String json = toJson("event", event);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    log.debug("推送日志失败: {}", e.getMessage());
                    sessions.remove(session);
                }
            } else {
                sessions.remove(session);
            }
        }
    }

    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    public List<LogEvent> recentHistory() {
        return List.copyOf(history);
    }

    public String toJson(String type, Object payload) {
        try {
            return objectMapper.writeValueAsString(new WsMessage(type, payload));
        } catch (Exception e) {
            return "{\"type\":\"event\",\"payload\":{}}";
        }
    }

    public static class WsMessage {
        private final String type;
        private final Object payload;

        public WsMessage(String type, Object payload) {
            this.type = type;
            this.payload = payload;
        }

        public String getType() {
            return type;
        }

        public Object getPayload() {
            return payload;
        }
    }
}
