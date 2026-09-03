package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudread.common.BusinessException;
import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.book.BookVO;
import com.cloudread.entity.Book;
import com.cloudread.entity.Favorite;
import com.cloudread.mapper.BookMapper;
import com.cloudread.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final BookMapper bookMapper;
    private final BookService bookService;
    private final UserActionService userActionService;

    public FavoriteService(FavoriteMapper favoriteMapper,
                           BookMapper bookMapper,
                           BookService bookService,
                           UserActionService userActionService) {
        this.favoriteMapper = favoriteMapper;
        this.bookMapper = bookMapper;
        this.bookService = bookService;
        this.userActionService = userActionService;
    }

    @Transactional
    public boolean toggle(Long bookId, Long userId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() != Book.STATUS_APPROVED) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "书籍不存在或未通过审核");
        }
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getBookId, bookId).eq(Favorite::getUserId, userId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            userActionService.record(userId, "UNFAVORITE", "取消收藏: " + book.getTitle());
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setBookId(bookId);
        favorite.setUserId(userId);
        favoriteMapper.insert(favorite);
        userActionService.record(userId, "FAVORITE", "收藏书籍: " + book.getTitle());
        return true;
    }

    public PageResult<BookVO> list(Long userId, long page, long size) {
        Page<Favorite> p = new Page<>(Math.max(1, page), Math.min(200, Math.max(1, size)));
        Page<Favorite> result = favoriteMapper.selectPage(p, new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).orderByDesc(Favorite::getId));
        if (result.getRecords().isEmpty()) {
            return PageResult.of(0, result.getCurrent(), result.getSize(), Collections.emptyList());
        }
        List<Long> ids = result.getRecords().stream().map(Favorite::getBookId).toList();
        Map<Long, Book> books = bookMapper.selectBatchIds(ids).stream()
                .filter(b -> b.getStatus() == Book.STATUS_APPROVED)
                .collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
        // 保持收藏时间倒序
        List<Book> ordered = result.getRecords().stream()
                .map(f -> books.get(f.getBookId()))
                .filter(java.util.Objects::nonNull)
                .toList();
        List<BookVO> vos = bookService.toVOList(ordered, userId);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }
}
