package com.cloudread.monitor;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.MDC;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * 将 ERROR/WARN 级别日志实时推送到管理端 WebSocket 面板。
 */
public class LogbackWebSocketAppender extends AppenderBase<ILoggingEvent> {

    public static volatile Consumer<LogEvent> sink;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

    @Override
    protected void append(ILoggingEvent event) {
        Consumer<LogEvent> listener = sink;
        if (listener == null) {
            return;
        }
        String level = event.getLevel().toString();
        if (!"ERROR".equals(level) && !"WARN".equals(level)) {
            return;
        }
        String message = event.getFormattedMessage();
        if (message == null || message.length() > 3000) {
            message = message == null ? "" : message.substring(0, 3000);
        }
        LogEvent logEvent = new LogEvent(
                FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())),
                level,
                event.getLoggerName(),
                message,
                event.getMDCPropertyMap() == null ? null : event.getMDCPropertyMap().get("traceId"));
        listener.accept(logEvent);
    }
}
