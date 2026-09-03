package com.cloudread.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class ConfigItem {

    @NotBlank(message = "配置键不能为空")
    private String configKey;

    private String configValue;

    private String description;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
