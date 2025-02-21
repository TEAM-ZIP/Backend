package com.capstone.bszip.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
@Component
public class JwtUtil {
    private static String SECRET_KEY;
    private static Key key;

    @Value("${jwt.secret}")
    public void setSecretKey(String secret) {
        SECRET_KEY = secret;
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    private static final long TEMP_TOKEN_EXPIRATION = 1000 * 60 * 30; // 30분
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60; // 1시간
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7; // 7일

    private static Key getSigningKey() {
        if (key == null) {
            throw new IllegalStateException("jwt key가 초기화되지 않음");
        }
        return key;
    }

    // 이메일 임시 토큰 생성
    public static String issueTempToken(String email) {
        Date now = new Date(System.currentTimeMillis());
        Date tempTokenExpiredAt = new Date((now).getTime() + TEMP_TOKEN_EXPIRATION);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(tempTokenExpiredAt)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 이메일 추출
    public static String extractEmail(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token.replace("Bearer ", ""))
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            throw new JwtException("유효하지 않은 토큰입니다.");
        }
    }

    // JWT access token 생성
    public static String createAccessToken(String email) {
        Date now = new Date(System.currentTimeMillis());
        Date accessTokenExpiredAt = new Date((now).getTime() + ACCESS_TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(accessTokenExpiredAt)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT refresh token 생성
    public static String createRefreshToken(String email) {
        Date now = new Date(System.currentTimeMillis());
        Date refreshTokenExpiredAt = new Date((now).getTime()+ REFRESH_TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(refreshTokenExpiredAt)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT 토큰에서 클레임 추출하는 함수
    public static Claims extractToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // JWT 토큰에서 만료 시간 추출
    public static Date getExpiration(String token){
        Claims claim = extractToken(token);
        return claim.getExpiration();
    }


}

