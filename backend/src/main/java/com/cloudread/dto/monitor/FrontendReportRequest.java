package com.cloudread.dto.monitor;

public class FrontendReportRequest {

    private String pageUrl;
    private Integer fcpMs;
    private Integer lcpMs;
    private Integer jsErrors;
    private Integer apiTotal;
    private Integer apiFail;
    private String userAgent;

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
}
