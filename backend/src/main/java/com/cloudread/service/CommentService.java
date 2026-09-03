package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudread.common.BusinessException;
import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.comment.CommentVO;
import com.cloudread.dto.comment.LikeResponse;
import com.cloudread.entity.Book;
import com.cloudread.entity.Comment;
import com.cloudread.entity.CommentLike;
import com.cloudread.entity.SysUser;
import com.cloudread.mapper.BookMapper;
import com.cloudread.mapper.CommentLikeMapper;
import com.cloudread.mapper.CommentMapper;
import com.cloudread.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper likeMapper;
    private final BookMapper bookMapper;
    private final SysUserMapper userMapper;
    private final UserActionService userActionService;

    public CommentService(CommentMapper commentMapper,
                          CommentLikeMapper likeMapper,
                          BookMapper bookMapper,
                          SysUserMapper userMapper,
                          UserActionService userActionService) {
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.bookMapper = bookMapper;
        this.userMapper = userMapper;
        this.userActionService = userActionService;
    }

    public List<CommentVO> listByBook(Long bookId, Long currentUserId) {
        List<Comment> all = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getBookId, bookId)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getId));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, SysUser> users = loadUsers(all);
        Set<Long> likedIds = likedIds(all, currentUserId);

        Map<Long, CommentVO> map = new LinkedHashMap<>();
        Map<Long, Long> replyRoot = new HashMap<>();
        for (Comment c : all) {
            CommentVO vo = toVO(c, users, likedIds);
            map.put(c.getId(), vo);
        }
        for (Comment c : all) {
            if (c.getParentId() == null) {
                continue;
            }
            Comment parent = commentById(all, c.getParentId());
            Long rootId = parent == null ? null : (parent.getParentId() == null ? parent.getId() : parent.getParentId());
            if (rootId != null) {
                replyRoot.put(c.getId(), rootId);
            }
        }
        List<CommentVO> roots = new ArrayList<>();
        for (Comment c : all) {
            CommentVO vo = map.get(c.getId());
            Long rootId = replyRoot.get(c.getId());
            if (rootId == null) {
                roots.add(vo);
            } else {
                CommentVO root = map.get(rootId);
                if (root != null) {
                    root.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    @Transactional
    public CommentVO add(Long bookId, String content, Long parentId, Long userId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() != Book.STATUS_APPROVED) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "书籍不存在或未通过审核");
        }
        Long finalParentId = null;
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getBookId().equals(bookId) || parent.getStatus() != 1) {
                throw new BusinessException("父评论不存在");
            }
            // 最多两层嵌套：回复“回复”时挂到根评论下
            finalParentId = parent.getParentId() == null ? parent.getId() : parent.getParentId();
        }
        Comment comment = new Comment();
        comment.setBookId(bookId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(finalParentId);
        comment.setLikeCount(0);
        comment.setStatus(1);
        commentMapper.insert(comment);
        userActionService.record(userId, "COMMENT", "评论《" + book.getTitle() + "》");
        return toVO(comment, loadUsers(List.of(comment)), java.util.Collections.emptySet());
    }

    @Transactional
    public LikeResponse like(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "评论不存在");
        }
        CommentLike existing = likeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId).eq(CommentLike::getUserId, userId));
        boolean liked;
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            likeMapper.insert(like);
            liked = true;
        }
        Long count = likeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId));
        comment.setLikeCount(count == null ? 0 : count.intValue());
        commentMapper.updateById(comment);
        return new LikeResponse(liked, comment.getLikeCount());
    }

    @Transactional
    public void delete(Long commentId, Long userId, boolean admin) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "评论不存在");
        }
        if (!admin && !comment.getUserId().equals(userId)) {
            throw new BusinessException(Result.CODE_FORBIDDEN, "只能删除自己的评论");
        }
        comment.setStatus(0);
        commentMapper.updateById(comment);
    }

    // ---------- 管理端 ----------

    public PageResult<CommentVO> adminList(String keyword, long page, long size) {
        Page<Comment> p = new Page<>(Math.max(1, page), Math.min(200, Math.max(1, size)));
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Comment::getContent, keyword.trim());
        }
        wrapper.orderByDesc(Comment::getId);
        Page<Comment> result = commentMapper.selectPage(p, wrapper);
        List<CommentVO> vos = result.getRecords().stream()
                .map(c -> toVO(c, loadUsers(result.getRecords()), java.util.Collections.emptySet()))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }

    // ---------- 辅助 ----------

    private CommentVO toVO(Comment c, Map<Long, SysUser> users, Set<Long> likedIds) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setBookId(c.getBookId());
        vo.setUserId(c.getUserId());
        SysUser user = users.get(c.getUserId());
        vo.setUsername(user == null ? "已注销" : user.getUsername());
        vo.setNickname(user == null ? "已注销" : displayName(user));
        vo.setAvatar(user == null ? null : user.getAvatarUrl());
        vo.setContent(c.getContent());
        vo.setParentId(c.getParentId());
        vo.setLikeCount(c.getLikeCount());
        vo.setLiked(likedIds.contains(c.getId()));
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }

    private Map<Long, SysUser> loadUsers(List<Comment> comments) {
        Set<Long> ids = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
    }

    private Set<Long> likedIds(List<Comment> comments, Long userId) {
        if (userId == null || comments.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        Set<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toSet());
        return likeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getUserId, userId).in(CommentLike::getCommentId, commentIds))
                .stream().map(CommentLike::getCommentId).collect(Collectors.toSet());
    }

    private Comment commentById(List<Comment> comments, Long id) {
        for (Comment c : comments) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    private String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }
}
