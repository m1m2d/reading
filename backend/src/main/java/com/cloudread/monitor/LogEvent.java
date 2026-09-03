package com.cloudread.monitor;

public class LogEvent {

    private String time;
    private String level;
    private String logger;
    private String message;
    private String traceId;

    public LogEvent() {
    }

    public LogEvent(String time, String level, String logger, String message, String traceId) {
        this.time = time;
        this.level = level;
        this.logger = logger;
        this.message = message;
        this.traceId = traceId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
