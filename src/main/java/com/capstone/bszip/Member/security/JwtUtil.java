package com.capstone.bszip.Member.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
@Component
public class JwtUtil {
//    private static String SECRET_KEY;
//    private static Key key;
//
//    @Value("${jwt.secret}")
//    public void setSecretKey(String secret) {
//        SECRET_KEY = secret;
//        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
//        key = Keys.hmacShaKeyFor(keyBytes);
//    }
//
//    private static final long EXPIRATION_TIME = 1000 * 60 * 15; // 15분
//
//    private static Key getSigningKey() {
//        if(key==null){
//            throw new IllegalStateException("jwt key가 초기화되지 않음");
//        }
//        return key;
//    }
//
//    // 토큰 생성
//    public static String generateToken(String email) {
//        return Jwts.builder()
//                .setSubject(email)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
//                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    // 토큰에서 이메일 추출
//    public static String extractEmail(String token) {
//        try {
//            return Jwts.parser()
//                    .setSigningKey(SECRET_KEY) // secretKey가 JwtUtil에 정의되어 있어야 함
//                    .parseClaimsJws(token.replace("Bearer ", ""))
//                    .getBody()
//                    .getSubject();
//        } catch (Exception e) {
//            throw new JwtException("유효하지 않은 토큰입니다.");
//        }
//    }
//
//    // 토큰 유효성 검증
//    public static boolean validateToken(String token) {
//        try {
//            if (token.startsWith("Bearer ")) {
//                token = token.substring(7);
//            }
//            Jwts.parserBuilder()
//                    .setSigningKey(getSigningKey())
//                    .build()
//                    .parseClaimsJws(token);
//            return true;
//        } catch (JwtException | IllegalArgumentException e) {
//            return false;
//        }
//    }
}

