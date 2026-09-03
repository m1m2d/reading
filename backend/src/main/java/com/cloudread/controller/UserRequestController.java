package com.cloudread.controller;

import com.cloudread.common.IpUtils;
import com.cloudread.common.Result;
import com.cloudread.dto.request.PasswordResetRequest;
import com.cloudread.security.JwtUser;
import com.cloudread.security.SecurityUtils;
import com.cloudread.service.UserRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户请求", description = "忘记密码等用户请求提交")
@RestController
@RequestMapping("/api/v1/user-requests")
public class UserRequestController {

    private final UserRequestService userRequestService;

    public UserRequestController(UserRequestService userRequestService) {
        this.userRequestService = userRequestService;
    }

    @Operation(summary = "提交忘记密码请求（邮箱+用户名+详情）")
    @PostMapping("/password-reset")
    public Result<Void> passwordReset(@Valid @RequestBody PasswordResetRequest request,
                                      HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        userRequestService.createPasswordReset(request, userId);
        return Result.ok();
    }
}
