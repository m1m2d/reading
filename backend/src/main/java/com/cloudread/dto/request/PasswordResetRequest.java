package com.cloudread.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetRequest {

    @NotBlank(message = "电子邮件不能为空")
    @Email(message = "电子邮件格式不正确")
    @Size(max = 128, message = "电子邮件长度不能超过128")
    private String email;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名长度不能超过32")
    private String username;

    @Size(max = 500, message = "详情不能超过500字")
    private String detail;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
