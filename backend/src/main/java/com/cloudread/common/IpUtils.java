package com.cloudread.common;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtils {

    private IpUtils() {
    }

    public static String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        int comma = ip.indexOf(',');
        if (comma > 0) {
            ip = ip.substring(0, comma);
        }
        return mask(ip == null ? "" : ip.trim());
    }

    /**
     * IP 脱敏：192.168.1.45 -> 192.168.*.*，IPv6 保留前 4 段。
     */
    public static String mask(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    sb.append(':');
                }
                sb.append(i < 2 ? parts[i] : "*");
            }
            return sb.toString();
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        return ip;
    }
}
