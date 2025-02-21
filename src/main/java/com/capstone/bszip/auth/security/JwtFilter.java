package com.capstone.bszip.auth.security;

import com.capstone.bszip.auth.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain
    ) throws ServletException, IOException {
        //헤더에서 jwt 추출
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); //token 추출
        } else { // 토큰 없는 경우 다음 필터
            filterChain.doFilter(request, response);
            return;
        }

        //유효성 검사
        Claims claims;
        try {
            claims = jwtUtil.extractToken(token);
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); //401
            response.getWriter().write("토큰이 만료되었습니다.");
            return;
        } catch (JwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); //401
            response.getWriter().write("유효하지 않은 토큰입니다.");
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); //500
            response.getWriter().write("서버 오류가 발생했습니다.");
            return;
        }

        //로그아웃 검사
        if(authService.isTokenBlackList(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); //401
            response.getWriter().write("로그아웃된 사용자입니다.");
            return;
        }

        String email = claims.getSubject();
        List <GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        System.out.println("Claims: " + claims);

        filterChain.doFilter(request, response);
    }
}
