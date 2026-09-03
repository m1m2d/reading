package com.cloudread.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudread.entity.SysUser;
import com.cloudread.mapper.SysUserMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class UserCache {

    private final SysUserMapper userMapper;
    private final Cache<String, SysUser> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();

    public UserCache(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public SysUser getByUsername(String username) {
        return cache.get(username, name -> userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, name)));
    }

    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    public void evict(String username) {
        cache.invalidate(username);
    }
}
