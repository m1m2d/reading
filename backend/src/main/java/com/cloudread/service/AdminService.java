package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudread.common.BusinessException;
import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.admin.ConfigItem;
import com.cloudread.dto.auth.UserVO;
import com.cloudread.entity.SysUser;
import com.cloudread.entity.UserActionLog;
import com.cloudread.mapper.SysUserMapper;
import com.cloudread.mapper.UserActionLogMapper;
import com.cloudread.security.SecurityUtils;
import com.cloudread.security.JwtUser;
import com.cloudread.security.UserCache;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    private final SysUserMapper userMapper;
    private final UserActionLogMapper actionLogMapper;
    private final UserCache userCache;
    private final ConfigService configService;

    public AdminService(SysUserMapper userMapper,
                        UserActionLogMapper actionLogMapper,
                        UserCache userCache,
                        ConfigService configService) {
        this.userMapper = userMapper;
        this.actionLogMapper = actionLogMapper;
        this.userCache = userCache;
        this.configService = configService;
    }

    public PageResult<UserVO> users(String keyword, long page, long size) {
        Page<SysUser> p = new Page<>(Math.max(1, page), Math.min(200, Math.max(1, size)));
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getNickname, kw));
        }
        wrapper.orderByDesc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(p, wrapper);
        List<UserVO> vos = new ArrayList<>();
        for (SysUser user : result.getRecords()) {
            UserVO vo = AuthService.toVO(user);
            vos.add(vo);
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }

    public void setUserStatus(Long id, int status, JwtUser admin) {
        if (id.equals(admin.getId())) {
            throw new BusinessException("不能修改自己的账号状态");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "用户不存在");
        }
        if (SysUser.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException("不能封禁管理员账号");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        userCache.evict(user.getUsername());
    }

    public PageResult<UserActionLog> userActions(Long userId, long page, long size) {
        Page<UserActionLog> p = new Page<>(Math.max(1, page), Math.min(200, Math.max(1, size)));
        Page<UserActionLog> result = actionLogMapper.selectPage(p,
                new LambdaQueryWrapper<UserActionLog>().eq(UserActionLog::getUserId, userId)
                        .orderByDesc(UserActionLog::getId));
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public List<ConfigItem> configList() {
        List<ConfigItem> items = new ArrayList<>();
        configService.all().forEach((k, v) -> {
            ConfigItem item = new ConfigItem();
            item.setConfigKey(k);
            item.setConfigValue(v);
            items.add(item);
        });
        return items;
    }

    public void updateConfig(List<ConfigItem> items) {
        configService.update(items);
    }
}
