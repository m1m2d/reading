package com.cloudread.controller;

import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.CategoryVO;
import com.cloudread.dto.admin.ConfigItem;
import com.cloudread.dto.admin.ReviewRequest;
import com.cloudread.dto.admin.UserStatusRequest;
import com.cloudread.dto.request.ProcessPasswordRequest;
import com.cloudread.dto.request.UserRequestVO;
import com.cloudread.dto.auth.UserVO;
import com.cloudread.dto.book.BookQuery;
import com.cloudread.dto.book.BookVO;
import com.cloudread.dto.comment.CommentVO;
import com.cloudread.entity.FrontendMonitor;
import com.cloudread.entity.SystemLog;
import com.cloudread.entity.UserActionLog;
import com.cloudread.security.JwtUser;
import com.cloudread.security.SecurityUtils;
import com.cloudread.service.AdminService;
import com.cloudread.service.BookService;
import com.cloudread.service.CategoryService;
import com.cloudread.service.CommentService;
import com.cloudread.service.MonitorService;
import com.cloudread.service.UserRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端", description = "内容审核、用户管理、分类、配置与可观测性")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final AdminService adminService;
    private final CommentService commentService;
    private final MonitorService monitorService;
    private final UserRequestService userRequestService;

    public AdminController(BookService bookService,
                           CategoryService categoryService,
                           AdminService adminService,
                           CommentService commentService,
                           MonitorService monitorService,
                           UserRequestService userRequestService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
        this.adminService = adminService;
        this.commentService = commentService;
        this.monitorService = monitorService;
        this.userRequestService = userRequestService;
    }

    // ---------- 书籍审核 ----------

    @Operation(summary = "全部书籍（含待审核）")
    @GetMapping("/books")
    public Result<PageResult<BookVO>> books(BookQuery query) {
        return Result.ok(bookService.adminList(query));
    }

    @Operation(summary = "审核书籍（通过/驳回）")
    @PatchMapping("/books/{id}/review")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        JwtUser admin = SecurityUtils.requireUser();
        bookService.review(id, request.getStatus(), request.getReason(), admin);
        return Result.ok();
    }

    @Operation(summary = "删除书籍")
    @DeleteMapping("/books/{id}")
    public Result<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id, SecurityUtils.requireUser());
        return Result.ok();
    }

    // ---------- 分类管理 ----------

    @Operation(summary = "分类树（管理端）")
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(categoryService.tree());
    }

    @Operation(summary = "新增分类")
    @PostMapping("/categories")
    public Result<Void> createCategory(@RequestBody Map<String, Object> body) {
        categoryService.create((String) body.get("name"),
                body.get("parentId") == null ? null : Long.valueOf(body.get("parentId").toString()),
                body.get("sort") == null ? 0 : Integer.valueOf(body.get("sort").toString()));
        return Result.ok();
    }

    @Operation(summary = "修改分类")
    @PutMapping("/categories/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        categoryService.update(id, (String) body.get("name"),
                body.get("parentId") == null ? null : Long.valueOf(body.get("parentId").toString()),
                body.get("sort") == null ? 0 : Integer.valueOf(body.get("sort").toString()));
        return Result.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }

    // ---------- 用户管理 ----------

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(adminService.users(keyword, page, size));
    }

    @Operation(summary = "封禁/解封用户")
    @PatchMapping("/users/{id}/status")
    public Result<Void> setUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        adminService.setUserStatus(id, request.getStatus(), SecurityUtils.requireUser());
        return Result.ok();
    }

    @Operation(summary = "用户行为日志")
    @GetMapping("/users/{id}/actions")
    public Result<PageResult<UserActionLog>> userActions(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size) {
        return Result.ok(adminService.userActions(id, page, size));
    }

    // ---------- 评论管理 ----------

    @Operation(summary = "评论列表")
    @GetMapping("/comments")
    public Result<PageResult<CommentVO>> comments(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(commentService.adminList(keyword, page, size));
    }

    @Operation(summary = "删除评论（软删除）")
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        JwtUser admin = SecurityUtils.requireUser();
        commentService.delete(id, admin.getId(), true);
        return Result.ok();
    }

    // ---------- 系统配置 ----------

    @Operation(summary = "获取系统配置")
    @GetMapping("/config")
    public Result<List<ConfigItem>> config() {
        return Result.ok(adminService.configList());
    }

    @Operation(summary = "更新系统配置")
    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody List<ConfigItem> items) {
        adminService.updateConfig(items);
        return Result.ok();
    }

    // ---------- 可观测性 ----------

    @Operation(summary = "后端运行指标（JVM/HTTP/DB）")
    @GetMapping("/monitor/backend")
    public Result<Map<String, Object>> backendMetrics() {
        return Result.ok(monitorService.backendMetrics());
    }

    @Operation(summary = "前端监控上报数据")
    @GetMapping("/monitor/frontend")
    public Result<PageResult<FrontendMonitor>> frontendMonitors(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "10") long size) {
        return Result.ok(monitorService.frontendList(page, size));
    }

    @Operation(summary = "系统日志查询")
    @GetMapping("/monitor/logs")
    public Result<PageResult<SystemLog>> systemLogs(@RequestParam(required = false) String level,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return Result.ok(monitorService.systemLogs(level, keyword, page, size));
    }

    // ---------- 用户请求（忘记密码） ----------

    @Operation(summary = "用户请求列表（待处理/全部）")
    @GetMapping("/user-requests")
    public Result<PageResult<UserRequestVO>> userRequests(@RequestParam(required = false) Integer status,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(userRequestService.list(status, keyword, page, size));
    }

    @Operation(summary = "重置该用户的密码")
    @PostMapping("/user-requests/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody ProcessPasswordRequest request) {
        userRequestService.resetPassword(id, request.getNewPassword(), SecurityUtils.requireUser());
        return Result.ok();
    }

    @Operation(summary = "归档用户请求（对号）")
    @PostMapping("/user-requests/{id}/archive")
    public Result<Void> archiveRequest(@PathVariable Long id) {
        userRequestService.archive(id, SecurityUtils.requireUser());
        return Result.ok();
    }
}
