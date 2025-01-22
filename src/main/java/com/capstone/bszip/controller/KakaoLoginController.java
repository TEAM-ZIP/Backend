package com.capstone.bszip.controller;

import com.capstone.bszip.dto.LoginResponse;
import com.capstone.bszip.service.KakaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    @Operation(summary = "웹 카카오 로그인")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestParam String code){
        return ResponseEntity.ok(kakaoService.kakaoLogin(code));
    }



}
