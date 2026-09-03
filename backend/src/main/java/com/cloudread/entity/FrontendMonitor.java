package com.cloudread.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("frontend_monitor")
public class FrontendMonitor {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String pageUrl;
    private Integer fcpMs;
    private Integer lcpMs;
    private Integer jsErrors;
    private Integer apiTotal;
    private Integer apiFail;
    private String userAgent;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public Integer getFcpMs() {
        return fcpMs;
    }

    public void setFcpMs(Integer fcpMs) {
        this.fcpMs = fcpMs;
    }

    public Integer getLcpMs() {
        return lcpMs;
    }

    public void setLcpMs(Integer lcpMs) {
        this.lcpMs = lcpMs;
    }

    public Integer getJsErrors() {
        return jsErrors;
    }

    public void setJsErrors(Integer jsErrors) {
        this.jsErrors = jsErrors;
    }

    public Integer getApiTotal() {
        return apiTotal;
    }

    public void setApiTotal(Integer apiTotal) {
        this.apiTotal = apiTotal;
    }

    public Integer getApiFail() {
        return apiFail;
    }

    public void setApiFail(Integer apiFail) {
        this.apiFail = apiFail;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
