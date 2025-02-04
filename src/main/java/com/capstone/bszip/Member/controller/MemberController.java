package com.capstone.bszip.Member.controller;

import com.capstone.bszip.Member.security.JwtUtil;
import com.capstone.bszip.Member.service.MemberService;
import com.capstone.bszip.Member.service.dto.JwtResponse;
import com.capstone.bszip.Member.service.dto.LoginRequest;
import com.capstone.bszip.Member.service.dto.SignupAddRequest;
import com.capstone.bszip.Member.service.dto.SignupRequest;
import com.capstone.bszip.dto.SuccessResponse;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class MemberController {

    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/kk")
    public ResponseEntity<?> index(){
        return ResponseEntity.ok("dd");
    }


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest, HttpSession session) {
        try{
            // 중복되는 이메일 입력 시
            if(memberService.checkEmailDuplication(signupRequest.getEmail())) {
                return ResponseEntity.ok(
                        SuccessResponse.builder()
                                .result(false)
                                .status(HttpServletResponse.SC_CONFLICT)
                                .data(null)
                                .message("이미 존재하는 이메일입니다.")
                                .build()
                );
            }

            memberService.registerEmailandPassword(signupRequest);
            // tempUser를 찾기 위해 session에 이메일 추가
            session.setAttribute("signupEmail", signupRequest.getEmail());
            return ResponseEntity.ok(
                    SuccessResponse.builder()
                            .result(true)
                            .status(HttpServletResponse.SC_OK)
                            .data(null)
                            .message("아이디와 비밀번호 입력 완료. 닉네임 입력 단계로 이동")
                            .build()
            );

        }catch (Exception e){
            return ResponseEntity.ok(
                    SuccessResponse.builder()
                            .result(false)
                            .status(HttpServletResponse.SC_BAD_REQUEST)
                            .data(null)
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/signup/nickname")
    public ResponseEntity<?> addNickname(@RequestBody SignupAddRequest signupAddRequest, HttpSession session) {
        try{
            if(!memberService.existFirstStep(session)){
                return ResponseEntity.ok(SuccessResponse.builder()
                        .result(false)
                        .status(HttpServletResponse.SC_NOT_FOUND)
                        .data(null)
                        .message("이메일 미입력! 회원가입 1단계를 거쳐야 합니다.")
                        .build()
                );
            }
            if(!memberService.existTempMember(session)){
                return ResponseEntity.ok(SuccessResponse.builder()
                        .result(false)
                        .status(HttpServletResponse.SC_NOT_FOUND)
                        .data(null)
                        .message("세션이 만료되었습니다.")
                        .build()
                );
            }

            memberService.registerMember(signupAddRequest, session);
            return ResponseEntity.ok(
                    SuccessResponse.builder()
                            .result(true)
                            .status(HttpServletResponse.SC_OK)
                            .data(null).message("회원가입 완료").build()
            );

        }catch (Exception e){
            return ResponseEntity.ok(
                    SuccessResponse.builder()
                            .result(false)
                            .status(HttpServletResponse.SC_BAD_REQUEST)
                            .data(null)
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        System.out.println(loginRequest.getEmail());
        memberService.loginMember(loginRequest, session);
        return ResponseEntity.ok(
                SuccessResponse.builder()
                        .result(true)
                        .status(HttpServletResponse.SC_OK)
                        .data(null).message("로그인 완료")
                        .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(
                SuccessResponse.builder()
                        .result(true)
                        .status(HttpServletResponse.SC_OK)
                        .data(null).message("로그아웃 완료")
                        .build());
    }
}

