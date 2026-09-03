package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudread.common.BusinessException;
import com.cloudread.common.Result;
import com.cloudread.dto.book.BookVO;
import com.cloudread.dto.post.ReceivedCommentVO;
import com.cloudread.dto.post.PostVO;
import com.cloudread.dto.user.UserContributionsVO;
import com.cloudread.dto.user.UserProfileVO;
import com.cloudread.entity.Book;
import com.cloudread.entity.Comment;
import com.cloudread.entity.Post;
import com.cloudread.entity.PostComment;
import com.cloudread.entity.SysUser;
import com.cloudread.mapper.BookMapper;
import com.cloudread.mapper.CommentMapper;
import com.cloudread.mapper.PostCommentMapper;
import com.cloudread.mapper.PostMapper;
import com.cloudread.mapper.SysUserMapper;
import com.cloudread.security.UserCache;
import com.cloudread.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个人中心：聚合“别人对我的内容（帖子/书籍）的评论”。
 */
@Service
public class UserProfileService {

    private static final int LIMIT = 100;

    private final PostMapper postMapper;
    private final PostCommentMapper postCommentMapper;
    private final BookMapper bookMapper;
    private final CommentMapper commentMapper;
    private final SysUserMapper userMapper;
    private final FollowService followService;
    private final BookService bookService;
    private final PostService postService;
    private final StorageService storageService;
    private final UserCache userCache;

    public UserProfileService(PostMapper postMapper,
                              PostCommentMapper postCommentMapper,
                              BookMapper bookMapper,
                              CommentMapper commentMapper,
                              SysUserMapper userMapper,
                              FollowService followService,
                              BookService bookService,
                              PostService postService,
                              StorageService storageService,
                              UserCache userCache) {
        this.postMapper = postMapper;
        this.postCommentMapper = postCommentMapper;
        this.bookMapper = bookMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.followService = followService;
        this.bookService = bookService;
        this.postService = postService;
        this.storageService = storageService;
        this.userCache = userCache;
    }

    public com.cloudread.dto.auth.UserVO updateAvatar(Long userId, MultipartFile avatar) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(Result.CODE_UNAUTHORIZED, "用户不存在");
        }
        String url = storageService.storeAvatar(avatar);
        user.setAvatarUrl(url);
        userMapper.updateById(user);
        userCache.evict(user.getUsername());
        return AuthService.toVO(user);
    }

    public UserProfileVO publicProfile(Long userId, Long currentUserId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != SysUser.STATUS_NORMAL) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "用户不存在");
        }
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setFollowCount(followService.followCount(userId));
        vo.setFollowerCount(followService.followerCount(userId));
        vo.setFollowedByMe(followService.followedByMe(userId, currentUserId));
        Long books = bookMapper.selectCount(new LambdaQueryWrapper<Book>()
                .eq(Book::getUploaderId, userId).eq(Book::getStatus, Book.STATUS_APPROVED));
        Long posts = postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId).eq(Post::getStatus, Post.STATUS_NORMAL));
        vo.setBookCount(books == null ? 0 : books);
        vo.setPostCount(posts == null ? 0 : posts);
        return vo;
    }

    public UserContributionsVO contributions(Long userId, Long currentUserId) {
        List<Book> books = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getUploaderId, userId)
                .eq(Book::getStatus, Book.STATUS_APPROVED)
                .orderByDesc(Book::getId)
                .last("LIMIT 100"));
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, Post.STATUS_NORMAL)
                .orderByDesc(Post::getId)
                .last("LIMIT 100"));
        List<BookVO> bookVOs = bookService.toVOList(books, currentUserId);
        List<PostVO> postVOs = postService.toVOList(posts, currentUserId);
        return new UserContributionsVO(bookVOs, postVOs);
    }

    public List<ReceivedCommentVO> receivedComments(Long userId) {
        List<ReceivedCommentVO> result = new ArrayList<>();

        // 别人评论我发布的帖子
        List<Post> myPosts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId).orderByDesc(Post::getId));
        if (!myPosts.isEmpty()) {
            Set<Long> postIds = myPosts.stream().map(Post::getId).collect(Collectors.toSet());
            Map<Long, String> titles = myPosts.stream()
                    .collect(Collectors.toMap(Post::getId, Post::getTitle));
            List<PostComment> comments = postCommentMapper.selectList(new LambdaQueryWrapper<PostComment>()
                    .in(PostComment::getPostId, postIds)
                    .eq(PostComment::getStatus, 1)
                    .ne(PostComment::getUserId, userId)
                    .orderByDesc(PostComment::getId)
                    .last("LIMIT " + LIMIT));
            for (PostComment c : comments) {
                ReceivedCommentVO vo = new ReceivedCommentVO();
                vo.setId(c.getId());
                vo.setSource("post");
                vo.setSourceId(c.getPostId());
                vo.setSourceTitle(titles.get(c.getPostId()));
                vo.setContent(c.getContent());
                vo.setCommenterId(c.getUserId());
                vo.setCreatedAt(c.getCreatedAt());
                result.add(vo);
            }
        }

        // 别人评论我上传的书籍
        List<Book> myBooks = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getUploaderId, userId).orderByDesc(Book::getId));
        if (!myBooks.isEmpty()) {
            Set<Long> bookIds = myBooks.stream().map(Book::getId).collect(Collectors.toSet());
            Map<Long, String> titles = myBooks.stream()
                    .collect(Collectors.toMap(Book::getId, Book::getTitle));
            List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                    .in(Comment::getBookId, bookIds)
                    .eq(Comment::getStatus, 1)
                    .ne(Comment::getUserId, userId)
                    .orderByDesc(Comment::getId)
                    .last("LIMIT " + LIMIT));
            for (Comment c : comments) {
                ReceivedCommentVO vo = new ReceivedCommentVO();
                vo.setId(c.getId());
                vo.setSource("book");
                vo.setSourceId(c.getBookId());
                vo.setSourceTitle(titles.get(c.getBookId()));
                vo.setContent(c.getContent());
                vo.setCommenterId(c.getUserId());
                vo.setCreatedAt(c.getCreatedAt());
                result.add(vo);
            }
        }

        // 评论人昵称
        Set<Long> commenterIds = result.stream().map(ReceivedCommentVO::getCommenterId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (!commenterIds.isEmpty()) {
            Map<Long, SysUser> users = userMapper.selectBatchIds(commenterIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u));
            for (ReceivedCommentVO vo : result) {
                SysUser user = users.get(vo.getCommenterId());
                vo.setCommenter(user == null ? "已注销"
                        : (user.getNickname() == null || user.getNickname().isBlank()
                        ? user.getUsername() : user.getNickname()));
                vo.setCommenterAvatar(user == null ? null : user.getAvatarUrl());
            }
        }

        result.sort(Comparator.comparing(ReceivedCommentVO::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result.size() > LIMIT ? result.subList(0, LIMIT) : result;
    }
}
