package com.cloudread.common;

import com.cloudread.entity.SystemLog;
import com.cloudread.mapper.SystemLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncLogWriter {

    private static final Logger log = LoggerFactory.getLogger(AsyncLogWriter.class);

    private final SystemLogMapper systemLogMapper;

    public AsyncLogWriter(SystemLogMapper systemLogMapper) {
        this.systemLogMapper = systemLogMapper;
    }

    @Async("logExecutor")
    public void write(SystemLog entry) {
        try {
            systemLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("写入 system_log 失败: {}", e.getMessage());
        }
    }
}
