package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudread.common.BusinessException;
import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.auth.UserVO;
import com.cloudread.entity.Follow;
import com.cloudread.entity.SysUser;
import com.cloudread.mapper.FollowMapper;
import com.cloudread.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private final FollowMapper followMapper;
    private final SysUserMapper userMapper;
    private final UserActionService userActionService;

    public FollowService(FollowMapper followMapper, SysUserMapper userMapper, UserActionService userActionService) {
        this.followMapper = followMapper;
        this.userMapper = userMapper;
        this.userActionService = userActionService;
    }

    @Transactional
    public Map<String, Object> toggle(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BusinessException("不能关注自己");
        }
        SysUser followee = userMapper.selectById(followeeId);
        if (followee == null || followee.getStatus() != SysUser.STATUS_NORMAL) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "用户不存在");
        }
        Follow existing = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId).eq(Follow::getFolloweeId, followeeId));
        boolean followed;
        if (existing != null) {
            followMapper.deleteById(existing.getId());
            followed = false;
            userActionService.record(followerId, "UNFOLLOW", "取消关注: " + displayName(followee));
        } else {
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFolloweeId(followeeId);
            followMapper.insert(follow);
            followed = true;
            userActionService.record(followerId, "FOLLOW", "关注用户: " + displayName(followee));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("followed", followed);
        result.put("followerCount", followerCount(followeeId));
        return result;
    }

    public PageResult<UserVO> following(Long userId, long page, long size) {
        Page<Follow> p = new Page<>(Math.max(1, page), Math.min(200, Math.max(1, size)));
        Page<Follow> result = followMapper.selectPage(p, new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId).orderByDesc(Follow::getId));
        if (result.getRecords().isEmpty()) {
            return PageResult.of(0, result.getCurrent(), result.getSize(), new ArrayList<>());
        }
        List<Long> ids = result.getRecords().stream().map(Follow::getFolloweeId).toList();
        Map<Long, SysUser> users = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        List<UserVO> vos = new ArrayList<>();
        for (Long id : ids) {
            SysUser user = users.get(id);
            if (user != null) {
                vos.add(AuthService.toVO(user));
            }
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }

    public long followCount(Long userId) {
        Long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId));
        return count == null ? 0 : count;
    }

    public long followerCount(Long userId) {
        Long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, userId));
        return count == null ? 0 : count;
    }

    public boolean followedByMe(Long followeeId, Long me) {
        if (me == null) {
            return false;
        }
        Long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, me).eq(Follow::getFolloweeId, followeeId));
        return count != null && count > 0;
    }

    private String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }
}
