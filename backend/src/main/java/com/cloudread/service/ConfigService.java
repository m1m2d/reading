package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudread.dto.admin.ConfigItem;
import com.cloudread.entity.SystemConfig;
import com.cloudread.mapper.SystemConfigMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfigService {

    public static final String KEY_MAX_SIZE_MB = "upload.maxSizeMb";
    public static final String KEY_ALLOWED_FORMATS = "allowedFormats";
    public static final String KEY_REVIEW_ENABLED = "reviewEnabled";
    public static final String KEY_REGISTER_ENABLED = "registerEnabled";
    public static final String KEY_COVER_MAX_SIZE_MB = "cover.maxSizeMb";
    public static final String KEY_ALLOWED_COVER_FORMATS = "allowedCoverFormats";
    public static final String KEY_CHUNK_THRESHOLD_MB = "chunkThresholdMb";

    private final SystemConfigMapper configMapper;

    public ConfigService(SystemConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Cacheable(cacheNames = "sysConfig", key = "#key")
    public String get(String key) {
        SystemConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
            return config.getConfigValue();
        }
        return defaults().get(key);
    }

    public Map<String, String> all() {
        Map<String, String> result = new LinkedHashMap<>(defaults());
        List<SystemConfig> rows = configMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getId));
        for (SystemConfig row : rows) {
            result.put(row.getConfigKey(), row.getConfigValue());
        }
        return result;
    }

    @CacheEvict(cacheNames = "sysConfig", allEntries = true)
    public void update(List<ConfigItem> items) {
        for (ConfigItem item : items) {
            SystemConfig existing = configMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, item.getConfigKey()));
            if (existing == null) {
                existing = new SystemConfig();
                existing.setConfigKey(item.getConfigKey());
                existing.setConfigValue(item.getConfigValue());
                existing.setDescription(item.getDescription());
                configMapper.insert(existing);
            } else {
                existing.setConfigValue(item.getConfigValue());
                if (item.getDescription() != null) {
                    existing.setDescription(item.getDescription());
                }
                existing.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                configMapper.updateById(existing);
            }
        }
    }

    private Map<String, String> defaults() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(KEY_MAX_SIZE_MB, "200");
        map.put(KEY_ALLOWED_FORMATS, "pdf,epub,txt,mobi");
        map.put(KEY_REVIEW_ENABLED, "true");
        map.put(KEY_REGISTER_ENABLED, "true");
        map.put(KEY_COVER_MAX_SIZE_MB, "10");
        map.put(KEY_ALLOWED_COVER_FORMATS, "jpg,png");
        map.put(KEY_CHUNK_THRESHOLD_MB, "100");
        return map;
    }

    public boolean bool(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    public int intValue(String key, int defaultValue) {
        String value = get(key);
        try {
            return value == null ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
