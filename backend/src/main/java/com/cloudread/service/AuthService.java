package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudread.common.BusinessException;
import com.cloudread.common.Result;
import com.cloudread.dto.auth.LoginRequest;
import com.cloudread.dto.auth.LoginResponse;
import com.cloudread.dto.auth.UserVO;
import com.cloudread.entity.SysUser;
import com.cloudread.mapper.SysUserMapper;
import com.cloudread.security.JwtUtil;
import com.cloudread.security.SecurityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ConfigService configService;
    private final UserActionService userActionService;

    public AuthService(SysUserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       ConfigService configService,
                       UserActionService userActionService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.configService = configService;
        this.userActionService = userActionService;
    }

    public LoginResponse login(LoginRequest request, String ip) {
        String username = request.getUsername().trim();
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        boolean newUser = false;
        if (user == null) {
            if (!configService.bool(ConfigService.KEY_REGISTER_ENABLED, true)) {
                throw new BusinessException(Result.CODE_FORBIDDEN, "系统已关闭新用户注册");
            }
            user = new SysUser();
            user.setUsername(username);
            user.setNickname(username);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(SysUser.ROLE_USER);
            user.setStatus(SysUser.STATUS_NORMAL);
            userMapper.insert(user);
            newUser = true;
        }
        if (user.getStatus() == null || user.getStatus() != SysUser.STATUS_NORMAL) {
            throw new BusinessException(Result.CODE_FORBIDDEN, "账号已被封禁，请联系管理员");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(Result.CODE_BAD_REQUEST, "用户名或密码错误");
        }
        userActionService.record(user.getId(), "LOGIN", "用户登录", ip);
        return buildResponse(user, newUser);
    }

    public LoginResponse refresh(String token) {
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (ExpiredJwtException e) {
            claims = e.getClaims();
            if (!jwtUtil.withinRefreshWindow(claims)) {
                throw new BusinessException(Result.CODE_UNAUTHORIZED, "登录已过期，请重新登录");
            }
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(Result.CODE_UNAUTHORIZED, "无效的令牌");
        }
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, claims.getSubject()));
        if (user == null || user.getStatus() != SysUser.STATUS_NORMAL) {
            throw new BusinessException(Result.CODE_UNAUTHORIZED, "账号状态异常，请重新登录");
        }
        return buildResponse(user, false);
    }

    public UserVO me() {
        Long userId = SecurityUtils.currentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(Result.CODE_UNAUTHORIZED, "用户不存在");
        }
        return toVO(user);
    }

    private LoginResponse buildResponse(SysUser user, boolean newUser) {
        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()));
        response.setExpiresIn(jwtUtil.getExpireMillis() / 1000);
        response.setNewUser(newUser);
        response.setUser(toVO(user));
        return response;
    }

    public static UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
