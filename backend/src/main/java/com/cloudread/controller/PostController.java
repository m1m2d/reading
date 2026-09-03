package com.cloudread.controller;

import com.cloudread.common.IpUtils;
import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.post.PostCommentRequest;
import com.cloudread.dto.post.PostCommentVO;
import com.cloudread.dto.post.PostVO;
import com.cloudread.security.JwtUser;
import com.cloudread.security.SecurityUtils;
import com.cloudread.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "讨论模块", description = "帖子、带图发布、评论/回复/点赞")
@RestController
@RequestMapping("/api/v1")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "帖子列表（分页/搜索）")
    @GetMapping("/posts")
    public Result<PageResult<PostVO>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword) {
        Long userId = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        return Result.ok(postService.list(page, size, keyword, userId));
    }

    @Operation(summary = "发布帖子（支持多图，5秒防刷屏）")
    @PostMapping("/posts")
    public Result<PostVO> create(@RequestParam("title") String title,
                                 @RequestParam(value = "content", required = false) String content,
                                 @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                 HttpServletRequest request) {
        JwtUser user = SecurityUtils.requireUser();
        return Result.ok(postService.create(user.getId(), title, content, images, IpUtils.clientIp(request)));
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/posts/{id}")
    public Result<PostVO> detail(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        return Result.ok(postService.detail(id, userId));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/posts/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.delete(id, SecurityUtils.requireUser());
        return Result.ok();
    }

    @Operation(summary = "点赞/取消点赞帖子")
    @PostMapping("/posts/{id}/like")
    public Result<Map<String, Object>> like(@PathVariable Long id) {
        return Result.ok(postService.like(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "获取帖子评论（两级嵌套）")
    @GetMapping("/posts/{id}/comments")
    public Result<List<PostCommentVO>> comments(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        return Result.ok(postService.comments(id, userId));
    }

    @Operation(summary = "发表帖子评论/回复")
    @PostMapping("/posts/{id}/comments")
    public Result<PostCommentVO> addComment(@PathVariable Long id,
                                            @Valid @RequestBody PostCommentRequest request) {
        return Result.ok(postService.addComment(id, request, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "帖子评论点赞/取消")
    @PostMapping("/post-comments/{id}/like")
    public Result<Map<String, Object>> likeComment(@PathVariable Long id) {
        return Result.ok(postService.likeComment(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "删除帖子评论")
    @DeleteMapping("/post-comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        postService.deleteComment(id, SecurityUtils.requireUser());
        return Result.ok();
    }
}
