package com.cloudread.controller;

import com.cloudread.common.Result;
import com.cloudread.common.PageResult;
import com.cloudread.dto.auth.UserVO;
import com.cloudread.dto.post.ReceivedCommentVO;
import com.cloudread.security.SecurityUtils;
import com.cloudread.service.FollowService;
import com.cloudread.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "个人中心", description = "收到的评论等个人聚合信息")
@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final FollowService followService;

    public UserProfileController(UserProfileService userProfileService, FollowService followService) {
        this.userProfileService = userProfileService;
        this.followService = followService;
    }

    @Operation(summary = "别人对我的帖子/书籍的评论")
    @GetMapping("/received-comments")
    public Result<List<ReceivedCommentVO>> receivedComments() {
        return Result.ok(userProfileService.receivedComments(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "上传/更新我的头像")
    @PostMapping("/avatar")
    public Result<UserVO> uploadAvatar(@RequestPart("avatar") MultipartFile avatar) {
        return Result.ok(userProfileService.updateAvatar(SecurityUtils.currentUserId(), avatar));
    }

    @Operation(summary = "我的关注列表")
    @GetMapping("/following")
    public Result<PageResult<UserVO>> following(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.ok(followService.following(SecurityUtils.currentUserId(), page, size));
    }

}
