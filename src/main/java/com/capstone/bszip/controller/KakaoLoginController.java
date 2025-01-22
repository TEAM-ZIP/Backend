package com.capstone.bszip.controller;

import com.capstone.bszip.dto.LoginResponse;
import com.capstone.bszip.service.KakaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kakao")
@Tag(name="카카오 로그인", description="프론트에서 인가코드 받고 카카오 로그인 진행")
public class KakaoLoginController {
    private final KakaoService kakaoService;

    @ResponseBody
    @GetMapping("/oauth/login")
    @Operation(summary = "웹 카카오 로그인",
    responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = """
                    {
                      "uid": 12345,
                      "nickname": "홍길동",
                      "email": "user@example.com",
                      "token": {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                      },
                      "errorCode": null,
                      "errorMessage": null
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "기존 회원 이메일로 카카오 로그인 시도",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "이메일 중복 에러",
                                    value = """
                    {
                      "uid": null,
                      "nickname": null,
                      "email": null,
                      "token": null,
                      "errorCode": "E01",
                      "errorMessage": "기존 회원가입 이력이 있는 회원입니다."
                    }
                    """
                            )
                    )
            )
    })
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestParam String code){
        LoginResponse response = kakaoService.kakaoLogin(code);
        if(response.getErrorCode().equals("E01")){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }



}
