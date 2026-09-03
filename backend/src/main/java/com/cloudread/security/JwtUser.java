package com.cloudread.security;

public class JwtUser {

    private final Long id;
    private final String username;
    private final String role;
    private final Integer status;

    public JwtUser(Long id, String username, String role, Integer status) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public Integer getStatus() {
        return status;
    }

    public boolean isAdmin() {
        return SysUserRole.isAdmin(role);
    }

    private static class SysUserRole {
        static boolean isAdmin(String role) {
            return "ADMIN".equalsIgnoreCase(role);
        }
    }
}
