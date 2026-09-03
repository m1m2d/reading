package com.cloudread.controller;

import com.cloudread.common.Result;
import com.cloudread.dto.comment.CommentRequest;
import com.cloudread.dto.comment.CommentVO;
import com.cloudread.dto.comment.LikeResponse;
import com.cloudread.security.JwtUser;
import com.cloudread.security.SecurityUtils;
import com.cloudread.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "互动模块", description = "评论、回复、点赞")
@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "获取书籍评论区（两级嵌套）")
    @GetMapping("/books/{bookId}/comments")
    public Result<List<CommentVO>> list(@PathVariable Long bookId) {
        Long userId = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        return Result.ok(commentService.listByBook(bookId, userId));
    }

    @Operation(summary = "发表评论/回复")
    @PostMapping("/comments")
    public Result<CommentVO> add(@Valid @RequestBody CommentRequest request) {
        return Result.ok(commentService.add(request.getBookId(), request.getContent(),
                request.getParentId(), SecurityUtils.currentUserId()));
    }

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/comments/{id}/like")
    public Result<LikeResponse> like(@PathVariable Long id) {
        return Result.ok(commentService.like(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "删除自己的评论")
    @DeleteMapping("/comments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        JwtUser user = SecurityUtils.requireUser();
        commentService.delete(id, user.getId(), user.isAdmin());
        return Result.ok();
    }
}
