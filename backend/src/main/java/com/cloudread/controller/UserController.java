package com.cloudread.controller;

import com.cloudread.common.Result;
import com.cloudread.dto.user.UserContributionsVO;
import com.cloudread.dto.user.UserProfileVO;
import com.cloudread.security.JwtUser;
import com.cloudread.security.SecurityUtils;
import com.cloudread.service.FollowService;
import com.cloudread.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "用户主页与关注", description = "公开用户主页、投稿聚合、关注")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService userProfileService;
    private final FollowService followService;

    public UserController(UserProfileService userProfileService, FollowService followService) {
        this.userProfileService = userProfileService;
        this.followService = followService;
    }

    @Operation(summary = "用户公开主页信息")
    @GetMapping("/{id}")
    public Result<UserProfileVO> publicProfile(@PathVariable Long id) {
        Long me = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        return Result.ok(userProfileService.publicProfile(id, me));
    }

    @Operation(summary = "用户的所有投稿（书籍+帖子）")
    @GetMapping("/{id}/contributions")
    public Result<UserContributionsVO> contributions(@PathVariable Long id) {
        Long me = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        return Result.ok(userProfileService.contributions(id, me));
    }

    @Operation(summary = "关注/取消关注用户")
    @PostMapping("/{id}/follow")
    public Result<Map<String, Object>> follow(@PathVariable Long id) {
        return Result.ok(followService.toggle(SecurityUtils.currentUserId(), id));
    }
}
