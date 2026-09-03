package com.cloudread.dto.admin;

import jakarta.validation.constraints.NotNull;

public class ReviewRequest {

    @NotNull(message = "审核状态不能为空")
    private Integer status;

    private String reason;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
