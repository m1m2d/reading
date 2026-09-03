package com.cloudread.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;
    private final long refreshMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-hours}") long expireHours,
                   @Value("${app.jwt.refresh-hours}") long refreshHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
        this.refreshMillis = refreshHours * 3600_000L;
    }

    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 无感续签：token 过期但仍在刷新窗口内时，允许换取新 token。
     */
    public boolean withinRefreshWindow(Claims claims) {
        Date exp = claims.getExpiration();
        if (exp == null) {
            return false;
        }
        return exp.getTime() > System.currentTimeMillis() - refreshMillis;
    }

    public long getExpireMillis() {
        return expireMillis;
    }
}
