package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudread.common.BusinessException;
import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.post.PostCommentRequest;
import com.cloudread.dto.post.PostCommentVO;
import com.cloudread.dto.post.PostVO;
import com.cloudread.entity.Post;
import com.cloudread.entity.PostComment;
import com.cloudread.entity.PostCommentLike;
import com.cloudread.entity.PostLike;
import com.cloudread.entity.SysUser;
import com.cloudread.mapper.PostCommentLikeMapper;
import com.cloudread.mapper.PostCommentMapper;
import com.cloudread.mapper.PostLikeMapper;
import com.cloudread.mapper.PostMapper;
import com.cloudread.mapper.SysUserMapper;
import com.cloudread.security.JwtUser;
import com.cloudread.storage.StorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PostService {

    /** 发帖防刷屏：同一用户 5 秒内只能发一个帖子 */
    public static final long POST_RATE_LIMIT_MS = 5000;

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCommentMapper postCommentMapper;
    private final PostCommentLikeMapper postCommentLikeMapper;
    private final SysUserMapper userMapper;
    private final StorageService storageService;
    private final UserActionService userActionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<Long, Long> lastPostAt = new ConcurrentHashMap<>();

    public PostService(PostMapper postMapper,
                       PostLikeMapper postLikeMapper,
                       PostCommentMapper postCommentMapper,
                       PostCommentLikeMapper postCommentLikeMapper,
                       SysUserMapper userMapper,
                       StorageService storageService,
                       UserActionService userActionService) {
        this.postMapper = postMapper;
        this.postLikeMapper = postLikeMapper;
        this.postCommentMapper = postCommentMapper;
        this.postCommentLikeMapper = postCommentLikeMapper;
        this.userMapper = userMapper;
        this.storageService = storageService;
        this.userActionService = userActionService;
    }

    // ---------- 发帖 ----------

    @Transactional
    public PostVO create(Long userId, String title, String content, List<MultipartFile> images, String ip) {
        checkRateLimit(userId);
        if (title == null || title.isBlank()) {
            throw new BusinessException("帖子标题不能为空");
        }
        if (title.length() > 100) {
            throw new BusinessException("帖子标题长度不能超过100");
        }
        if (content != null && content.length() > 5000) {
            throw new BusinessException("帖子内容长度不能超过5000");
        }
        List<String> imageUrls = storageService.storePostImages(images);

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title.trim());
        post.setContent(content == null ? "" : content);
        post.setImages(toJson(imageUrls));
        post.setStatus(Post.STATUS_NORMAL);
        postMapper.insert(post);
        userActionService.record(userId, "POST", "发布帖子: " + post.getTitle(), ip);
        return detail(post.getId(), userId);
    }

    private void checkRateLimit(Long userId) {
        long now = System.currentTimeMillis();
        lastPostAt.compute(userId, (uid, last) -> {
            if (last != null && now - last < POST_RATE_LIMIT_MS) {
                long wait = (POST_RATE_LIMIT_MS - (now - last)) / 1000 + 1;
                throw new BusinessException(Result.CODE_BAD_REQUEST, "发帖过于频繁，请 " + wait + " 秒后再试");
            }
            return now;
        });
    }

    // ---------- 查询 ----------

    public PageResult<PostVO> list(long page, long size, String keyword, Long currentUserId) {
        Page<Post> p = new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, Post.STATUS_NORMAL);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Post::getTitle, kw).or().like(Post::getContent, kw));
        }
        wrapper.orderByDesc(Post::getId);
        Page<Post> result = postMapper.selectPage(p, wrapper);
        List<PostVO> vos = toVOList(result.getRecords(), currentUserId, false);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }

    public PostVO detail(Long id, Long currentUserId) {
        Post post = postMapper.selectById(id);
        if (post == null || post.getStatus() != Post.STATUS_NORMAL) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "帖子不存在");
        }
        return toVOList(List.of(post), currentUserId, true).get(0);
    }

    @Transactional
    public void delete(Long id, JwtUser user) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "帖子不存在");
        }
        if (!post.getUserId().equals(user.getId()) && !user.isAdmin()) {
            throw new BusinessException(Result.CODE_FORBIDDEN, "只能删除自己的帖子");
        }
        postMapper.deleteById(id);
        postLikeMapper.delete(new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, id));
        List<PostComment> comments = postCommentMapper.selectList(
                new LambdaQueryWrapper<PostComment>().eq(PostComment::getPostId, id));
        if (!comments.isEmpty()) {
            Set<Long> commentIds = comments.stream().map(PostComment::getId).collect(Collectors.toSet());
            postCommentLikeMapper.delete(new LambdaQueryWrapper<PostCommentLike>()
                    .in(PostCommentLike::getCommentId, commentIds));
        }
        postCommentMapper.delete(new LambdaQueryWrapper<PostComment>().eq(PostComment::getPostId, id));
        userActionService.record(user.getId(), "DELETE_POST", "删除帖子: " + post.getTitle());
    }

    // ---------- 点赞 ----------

    @Transactional
    public Map<String, Object> like(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != Post.STATUS_NORMAL) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "帖子不存在");
        }
        PostLike existing = postLikeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId));
        boolean liked;
        if (existing != null) {
            postLikeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            postLikeMapper.insert(like);
            liked = true;
        }
        Long count = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId));
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", count == null ? 0 : count);
        return result;
    }

    // ---------- 评论 ----------

    public List<PostCommentVO> comments(Long postId, Long currentUserId) {
        List<PostComment> all = postCommentMapper.selectList(new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getPostId, postId)
                .eq(PostComment::getStatus, 1)
                .orderByAsc(PostComment::getId));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, SysUser> users = loadUsers(all);
        Set<Long> likedIds = likedCommentIds(all, currentUserId);
        Map<Long, PostCommentVO> map = new LinkedHashMap<>();
        for (PostComment c : all) {
            PostCommentVO vo = toVO(c, users, likedIds);
            map.put(c.getId(), vo);
        }
        Map<Long, Long> replyRoot = new HashMap<>();
        for (PostComment c : all) {
            if (c.getParentId() == null) {
                continue;
            }
            PostComment parent = findById(all, c.getParentId());
            if (parent != null) {
                replyRoot.put(c.getId(), parent.getParentId() == null ? parent.getId() : parent.getParentId());
            }
        }
        List<PostCommentVO> roots = new ArrayList<>();
        for (PostComment c : all) {
            PostCommentVO vo = map.get(c.getId());
            Long rootId = replyRoot.get(c.getId());
            if (rootId == null) {
                roots.add(vo);
            } else {
                PostCommentVO root = map.get(rootId);
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
    public PostCommentVO addComment(Long postId, PostCommentRequest request, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != Post.STATUS_NORMAL) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "帖子不存在");
        }
        Long finalParentId = null;
        if (request.getParentId() != null) {
            PostComment parent = postCommentMapper.selectById(request.getParentId());
            if (parent == null || !parent.getPostId().equals(postId) || parent.getStatus() != 1) {
                throw new BusinessException("父评论不存在");
            }
            finalParentId = parent.getParentId() == null ? parent.getId() : parent.getParentId();
        }
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        comment.setParentId(finalParentId);
        comment.setLikeCount(0);
        comment.setStatus(1);
        postCommentMapper.insert(comment);
        userActionService.record(userId, "POST_COMMENT", "评论帖子: " + post.getTitle());
        return toVO(comment, loadUsers(List.of(comment)), Collections.emptySet());
    }

    @Transactional
    public Map<String, Object> likeComment(Long commentId, Long userId) {
        PostComment comment = postCommentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "评论不存在");
        }
        PostCommentLike existing = postCommentLikeMapper.selectOne(new LambdaQueryWrapper<PostCommentLike>()
                .eq(PostCommentLike::getCommentId, commentId).eq(PostCommentLike::getUserId, userId));
        boolean liked;
        if (existing != null) {
            postCommentLikeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            PostCommentLike like = new PostCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            postCommentLikeMapper.insert(like);
            liked = true;
        }
        Long count = postCommentLikeMapper.selectCount(new LambdaQueryWrapper<PostCommentLike>()
                .eq(PostCommentLike::getCommentId, commentId));
        comment.setLikeCount(count == null ? 0 : count.intValue());
        postCommentMapper.updateById(comment);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", comment.getLikeCount());
        return result;
    }

    @Transactional
    public void deleteComment(Long commentId, JwtUser user) {
        PostComment comment = postCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "评论不存在");
        }
        if (!comment.getUserId().equals(user.getId()) && !user.isAdmin()) {
            throw new BusinessException(Result.CODE_FORBIDDEN, "只能删除自己的评论");
        }
        comment.setStatus(0);
        postCommentMapper.updateById(comment);
    }

    // ---------- 组装 ----------

    public List<PostVO> toVOList(List<Post> posts, Long currentUserId) {
        return toVOList(posts, currentUserId, false);
    }

    public List<PostVO> toVOList(List<Post> posts, Long currentUserId, boolean fullContent) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> ids = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Map<Long, Long> likeCounts = postLikeMapper.selectList(
                        new LambdaQueryWrapper<PostLike>().in(PostLike::getPostId, ids))
                .stream().collect(Collectors.groupingBy(PostLike::getPostId, Collectors.counting()));
        Map<Long, Long> commentCounts = postCommentMapper.selectList(
                        new LambdaQueryWrapper<PostComment>().in(PostComment::getPostId, ids)
                                .eq(PostComment::getStatus, 1))
                .stream().collect(Collectors.groupingBy(PostComment::getPostId, Collectors.counting()));
        Set<Long> likedIds = Collections.emptySet();
        if (currentUserId != null) {
            likedIds = postLikeMapper.selectList(new LambdaQueryWrapper<PostLike>()
                            .eq(PostLike::getUserId, currentUserId).in(PostLike::getPostId, ids))
                    .stream().map(PostLike::getPostId).collect(Collectors.toSet());
        }
        Map<Long, SysUser> users = loadPostUsers(posts);
        List<PostVO> vos = new ArrayList<>(posts.size());
        for (Post post : posts) {
            PostVO vo = new PostVO();
            vo.setId(post.getId());
            vo.setUserId(post.getUserId());
            SysUser user = users.get(post.getUserId());
            vo.setUsername(user == null ? "已注销" : user.getUsername());
            vo.setNickname(user == null ? "已注销" : displayName(user));
            vo.setAvatar(user == null ? null : user.getAvatarUrl());
            vo.setTitle(post.getTitle());
            vo.setContent(fullContent ? post.getContent() : summary(post.getContent()));
            vo.setImages(parseImages(post.getImages()));
            vo.setLikeCount(likeCounts.getOrDefault(post.getId(), 0L));
            vo.setCommentCount(commentCounts.getOrDefault(post.getId(), 0L));
            vo.setLiked(likedIds.contains(post.getId()));
            vo.setCreatedAt(post.getCreatedAt());
            vos.add(vo);
        }
        return vos;
    }

    private PostCommentVO toVO(PostComment c, Map<Long, SysUser> users, Set<Long> likedIds) {
        PostCommentVO vo = new PostCommentVO();
        vo.setId(c.getId());
        vo.setPostId(c.getPostId());
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

    private Map<Long, SysUser> loadUsers(List<PostComment> comments) {
        Set<Long> ids = comments.stream().map(PostComment::getUserId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
    }

    private Map<Long, SysUser> loadPostUsers(List<Post> posts) {
        Set<Long> ids = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
    }

    private Set<Long> likedCommentIds(List<PostComment> comments, Long userId) {
        if (userId == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> ids = comments.stream().map(PostComment::getId).collect(Collectors.toSet());
        return postCommentLikeMapper.selectList(new LambdaQueryWrapper<PostCommentLike>()
                        .eq(PostCommentLike::getUserId, userId).in(PostCommentLike::getCommentId, ids))
                .stream().map(PostCommentLike::getCommentId).collect(Collectors.toSet());
    }

    private PostComment findById(List<PostComment> comments, Long id) {
        for (PostComment c : comments) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    private String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }

    private String summary(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String plain = content.replace('\n', ' ').trim();
        return plain.length() > 120 ? plain.substring(0, 120) + "..." : plain;
    }

    private String toJson(List<String> urls) {
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseImages(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
