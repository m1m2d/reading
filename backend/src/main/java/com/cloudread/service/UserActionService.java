package com.cloudread.service;

import com.cloudread.entity.UserActionLog;
import com.cloudread.mapper.UserActionLogMapper;
import org.springframework.stereotype.Service;

@Service
public class UserActionService {

    private final UserActionLogMapper actionLogMapper;

    public UserActionService(UserActionLogMapper actionLogMapper) {
        this.actionLogMapper = actionLogMapper;
    }

    public void record(Long userId, String action, String detail) {
        try {
            UserActionLog log = new UserActionLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setDetail(detail);
            actionLogMapper.insert(log);
        } catch (Exception ignored) {
            // 行为日志失败不影响主流程
        }
    }

    public void record(Long userId, String action, String detail, String ip) {
        try {
            UserActionLog log = new UserActionLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setDetail(detail);
            log.setIp(ip);
            actionLogMapper.insert(log);
        } catch (Exception ignored) {
        }
    }
}
