package com.capstone.bszip.dto;

import com.capstone.bszip.security.AuthTokens;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "로그인 dto")
public class LoginResponse {
    @Schema(description = "멤버 id", example = "1")
    private Long id;
    @Schema(description = "멤버 닉네임", example = "이구역독서짱")
    private String nickname;
    @Schema(description = "멤버 이메일", example = "bszip@example.com")
    private String email;
    @Schema(description = "JWT 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private AuthTokens token;
    @Schema(description = "에러 코드 (정상 요청 시 null)", example = "E01", nullable = true)
    private String errorCode;
    @Schema(description = "에러 메시지 (정상 요청 시 null)", example = "기존 회원가입 이력이 있는 회원입니다.", nullable = true)
    private String errorMessage;

    public LoginResponse(Long id, String nickname, String email, AuthTokens token){
        this.id = id;
        this.nickname = nickname;
        this.email = email;
        this.token = token;
    }

    public LoginResponse(String errorCode, String errorMessage){
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}