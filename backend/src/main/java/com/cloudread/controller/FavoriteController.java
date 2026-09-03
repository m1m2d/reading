package com.cloudread.controller;

import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.book.BookVO;
import com.cloudread.dto.favorite.FavoriteRequest;
import com.cloudread.security.SecurityUtils;
import com.cloudread.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "收藏模块")
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/toggle")
    public Result<Map<String, Boolean>> toggle(@Valid @RequestBody FavoriteRequest request) {
        boolean favorited = favoriteService.toggle(request.getBookId(), SecurityUtils.currentUserId());
        return Result.ok(Map.of("favorited", favorited));
    }

    @Operation(summary = "我的收藏（按时间倒序）")
    @GetMapping
    public Result<PageResult<BookVO>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "12") long size) {
        return Result.ok(favoriteService.list(SecurityUtils.currentUserId(), page, size));
    }
}
