package com.cloudread.security;

import com.cloudread.common.BusinessException;
import com.cloudread.common.Result;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<JwtUser> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUser jwtUser) {
            return Optional.of(jwtUser);
        }
        return Optional.empty();
    }

    public static JwtUser requireUser() {
        return currentUser().orElseThrow(() -> new BusinessException(Result.CODE_UNAUTHORIZED, "请先登录"));
    }

    public static Long currentUserId() {
        return requireUser().getId();
    }

    public static boolean isAdmin() {
        return currentUser().map(JwtUser::isAdmin).orElse(false);
    }
}
