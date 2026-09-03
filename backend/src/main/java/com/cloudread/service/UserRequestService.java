package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudread.common.BusinessException;
import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.request.PasswordResetRequest;
import com.cloudread.dto.request.UserRequestVO;
import com.cloudread.entity.SysUser;
import com.cloudread.entity.UserRequest;
import com.cloudread.mapper.SysUserMapper;
import com.cloudread.mapper.UserRequestMapper;
import com.cloudread.security.JwtUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserRequestService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRequestMapper requestMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserActionService userActionService;

    public UserRequestService(UserRequestMapper requestMapper,
                              SysUserMapper userMapper,
                              PasswordEncoder passwordEncoder,
                              UserActionService userActionService) {
        this.requestMapper = requestMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userActionService = userActionService;
    }

    /**
     * 登录页提交忘记密码请求（无需登录）。
     */
    public void createPasswordReset(PasswordResetRequest request, Long requestedBy) {
        String username = request.getUsername().trim();
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(Result.CODE_BAD_REQUEST, "用户名不存在，无法提交忘记密码请求");
        }
        Long pending = requestMapper.selectCount(new LambdaQueryWrapper<UserRequest>()
                .eq(UserRequest::getUsername, username)
                .eq(UserRequest::getStatus, UserRequest.STATUS_PENDING));
        if (pending != null && pending > 0) {
            throw new BusinessException("该用户名已有待处理请求，请耐心等待管理员处理");
        }
        UserRequest entity = new UserRequest();
        entity.setType(UserRequest.TYPE_PASSWORD_RESET);
        entity.setUsername(username);
        entity.setEmail(request.getEmail().trim());
        entity.setDetail(request.getDetail() == null ? "" : request.getDetail().trim());
        entity.setStatus(UserRequest.STATUS_PENDING);
        entity.setRequestedBy(requestedBy);
        requestMapper.insert(entity);
    }

    public PageResult<UserRequestVO> list(Integer status, String keyword, long page, long size) {
        Page<UserRequest> p = new Page<>(Math.max(1, page), Math.min(200, Math.max(1, size)));
        LambdaQueryWrapper<UserRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(UserRequest::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(UserRequest::getUsername, kw)
                    .or().like(UserRequest::getEmail, kw)
                    .or().like(UserRequest::getDetail, kw));
        }
        wrapper.orderByDesc(UserRequest::getId);
        Page<UserRequest> result = requestMapper.selectPage(p, wrapper);
        List<UserRequestVO> vos = toVOList(result.getRecords());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }

    /**
     * 管理员重置该用户的密码。
     */
    @Transactional
    public void resetPassword(Long id, String newPassword, JwtUser admin) {
        UserRequest request = requireRequest(id);
        if (request.getStatus() == UserRequest.STATUS_ARCHIVED) {
            throw new BusinessException("该请求已归档，无法再修改密码");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "用户名「" + request.getUsername() + "」对应的用户不存在，无法重置密码");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        request.setProcessedBy(admin.getId());
        request.setProcessedAt(now());
        requestMapper.updateById(request);
        userActionService.record(admin.getId(), "RESET_PASSWORD", "重置用户密码: " + request.getUsername());
    }

    /**
     * 管理员点击“对号”归档请求。
     */
    @Transactional
    public void archive(Long id, JwtUser admin) {
        UserRequest request = requireRequest(id);
        if (request.getStatus() == UserRequest.STATUS_ARCHIVED) {
            throw new BusinessException("该请求已归档");
        }
        request.setStatus(UserRequest.STATUS_ARCHIVED);
        if (request.getProcessedBy() == null) {
            request.setProcessedBy(admin.getId());
        }
        request.setProcessedAt(now());
        requestMapper.updateById(request);
        userActionService.record(admin.getId(), "ARCHIVE_REQUEST", "归档用户请求: " + request.getUsername());
    }

    private UserRequest requireRequest(Long id) {
        UserRequest request = requestMapper.selectById(id);
        if (request == null) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "请求不存在");
        }
        return request;
    }

    private List<UserRequestVO> toVOList(List<UserRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> adminIds = requests.stream().map(UserRequest::getProcessedBy)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> admins = adminIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(adminIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        List<UserRequestVO> vos = new ArrayList<>(requests.size());
        for (UserRequest r : requests) {
            UserRequestVO vo = new UserRequestVO();
            vo.setId(r.getId());
            vo.setType(r.getType());
            vo.setUsername(r.getUsername());
            vo.setEmail(r.getEmail());
            vo.setDetail(r.getDetail());
            vo.setStatus(r.getStatus());
            vo.setProcessedBy(r.getProcessedBy());
            SysUser admin = r.getProcessedBy() == null ? null : admins.get(r.getProcessedBy());
            vo.setProcessedByName(admin == null ? null
                    : (admin.getNickname() == null || admin.getNickname().isBlank()
                    ? admin.getUsername() : admin.getNickname()));
            vo.setProcessedAt(r.getProcessedAt());
            vo.setCreatedAt(r.getCreatedAt());
            vos.add(vo);
        }
        return vos;
    }

    private String now() {
        return LocalDateTime.now().format(TIME);
    }
}
