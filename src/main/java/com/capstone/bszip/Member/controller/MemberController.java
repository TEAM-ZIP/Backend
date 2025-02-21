package com.capstone.bszip.Member.controller;

import com.capstone.bszip.Member.service.dto.TokenResponse;
import com.capstone.bszip.auth.AuthService;
import com.capstone.bszip.auth.security.JwtUtil;
import com.capstone.bszip.Member.service.MemberService;
import com.capstone.bszip.Member.service.dto.LoginRequest;
import com.capstone.bszip.Member.service.dto.SignupAddRequest;
import com.capstone.bszip.Member.service.dto.SignupRequest;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 관리 API")
@RequestMapping("/auth")
public class MemberController {
    private final MemberService memberService;
    private final AuthService authService;

    //회원 가입
    @PostMapping("/signup")
    @Operation(summary = "회원 가입", description = "이메일과 비밀번호로 회원 가입을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "회원가입-이메일,비밀번호 성공",
            content = @Content(schema = @Schema(implementation = Map.class)))
    @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class)))
    public ResponseEntity<?> signup(
            @Valid @RequestBody SignupRequest signupRequest){
        try {
            // 이메일 및 비밀번호 저장
            memberService.registerMember(signupRequest);
            // 이메일을 포함한 임시 JWT 토큰 발급
            String token = JwtUtil.issueTempToken(signupRequest.getEmail());
            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(Map.of("message", "TEMP_TOKEN_ISSUED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    //회원 가입-닉네임
    @PostMapping("/signup/add")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "회원 가입 추가 정보", description = "닉네임을 추가하여 회원 가입을 완료합니다.")
    @ApiResponse(responseCode = "200", description = "회원가입 완료",
            content = @Content(schema = @Schema(implementation = Map.class)))
    @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰",
            content = @Content(schema = @Schema(implementation = Map.class)))
    @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = Map.class)))
    public ResponseEntity<?> add(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody SignupAddRequest signupAddRequest){
        System.out.println(token);
        try {
            // 토큰에서 이메일 추출
            String email = JwtUtil.extractEmail(token);

            // 닉네임과 이메일을 이용해 최종 회원가입
            memberService.registerMemberNickname(email, signupAddRequest.getNickname());

            return ResponseEntity.ok(Map.of("message", "회원가입 완료"));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "유효하지 않은 토큰입니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    //로그인
    @PostMapping("/signin")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "401", description = "인증 실패 - 잘못된 자격 증명",
            content = @Content(schema = @Schema(implementation = Map.class)))
    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = Map.class)))

    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response){
        try{
             TokenResponse tokens = memberService.loginUser(loginRequest);

            return ResponseEntity.ok(tokens);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "잘못된 접근"));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    //로그아웃
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "refresh token을 삭제하며 로그아웃합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = Map.class)))
    public ResponseEntity<?> logout(HttpServletRequest httpServletRequest){
        String authorizationHeader = httpServletRequest.getHeader("Authorization");

        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error","토큰을 찾을 수 없습니다"));
        }
        String accessToken = authorizationHeader.substring(7);
        try {
            String email = JwtUtil.extractEmail(accessToken);
            Date expirationDate = JwtUtil.getExpiration(accessToken);
            authService.logout(email, accessToken, expirationDate);

            return ResponseEntity.ok().body(Map.of("message", "로그아웃 성공"));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

