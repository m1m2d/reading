package com.cloudread.controller;

import com.cloudread.common.IpUtils;
import com.cloudread.common.Result;
import com.cloudread.dto.auth.LoginRequest;
import com.cloudread.dto.auth.LoginResponse;
import com.cloudread.dto.auth.UserVO;
import com.cloudread.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "认证模块", description = "登录/自动注册、Token 续签、当前用户")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录或自动注册")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request, IpUtils.clientIp(httpRequest)));
    }

    @Operation(summary = "无感续签 Token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return Result.fail(Result.CODE_BAD_REQUEST, "缺少 token");
        }
        return Result.ok(authService.refresh(token));
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.me());
    }
}
