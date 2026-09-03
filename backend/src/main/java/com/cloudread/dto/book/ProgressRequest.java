package com.cloudread.dto.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProgressRequest {

    @NotBlank(message = "阅读进度不能为空")
    @Size(max = 500, message = "阅读进度内容过长")
    private String position;

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
