package com.capstone.bszip.Member.service;

import com.capstone.bszip.Member.domain.Member;
import com.capstone.bszip.Member.domain.MemberJoinType;
import com.capstone.bszip.Member.repository.MemberRepository;
import com.capstone.bszip.Member.service.dto.TokenResponse;
import com.capstone.bszip.auth.refreshToken.RefreshToken;
import com.capstone.bszip.auth.refreshToken.RefreshTokenRepository;
import com.capstone.bszip.auth.security.JwtUtil;
import com.capstone.bszip.commonDto.SuccessResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class KakaoService {

    private final MemberRepository memberRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final Map<String, String> temporaryStorage = new HashMap<>(); // 임시 데이터 저장소

    @Value("${kakao.client.id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Transactional
    public String getkakaoEmail(String code){
        String redirectUri = this.redirectUri;
        // 프론트에서 준 인가 코드로 카카오의 액세스 토큰 요청
        String kakaoAccessToken = getKakaoAccessToken(code, redirectUri);

        // 가져온 토큰으로 카카오 api 호출
        HashMap<String, Object> memberInfo = getKakaoMemberInfo(kakaoAccessToken);
        return memberInfo.get("email").toString();
    }

    @Transactional
    public String getKakaoAccessToken(String code, String redirectUri){
        // http 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);


        // http로 요청하여 엑세스 토큰 받기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class);

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try{
            jsonNode = objectMapper.readTree(responseBody);
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonNode.get("access_token").asText(); // 토큰 전송
    }

    private HashMap<String, Object> getKakaoMemberInfo(String accessToken){
        HashMap<String, Object> memberInfo = new HashMap<>();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Accept", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        // responsebody에 있는 정보 꺼내기
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try{
            jsonNode = objectMapper.readTree(responseBody);
        } catch (Exception e){
            e.printStackTrace();
        }
        Long id = jsonNode.get("id").asLong();
        String email = jsonNode.get("kakao_account").get("email").asText();

        memberInfo.put("id", id);
        memberInfo.put("email", email);

        return memberInfo;
    }

    // 기본 로그인으로 회원가입된 상황
    public int whichLoginWay(String kakaoEmail){

        Member member = memberRepository.findByEmail(kakaoEmail).orElse(null);

        if(member == null){
            // 회원가입 시켜야 돼
            return 1;
        }else if(member.getMemberJoinType().equals(MemberJoinType.DEFAULT)){
            // 기본 로그인으로 로그인하라고 알려줘
            return 2;
        }

        // 카카오 로그인 시키면 돼..
        return 3;

    }

    public void updateRefreshToken(String email,String refreshToken){
        Optional<Member> memberOptional = memberRepository.findByEmail(email);
        if(memberOptional.isPresent()){
            Member member = memberOptional.get();
            RefreshToken token = new RefreshToken();
            token.setEmail(member.getEmail());
            token.setRefreshToken(refreshToken);
            token.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));

            refreshTokenRepository.save(token);
        }
    }

    public TokenResponse loginUser(String kakaoEmail){
        String accessToken = JwtUtil.createAccessToken(kakaoEmail);
        String refreshToken = JwtUtil.createRefreshToken(kakaoEmail);
        //refresh 토큰 db 저장
        updateRefreshToken(kakaoEmail,refreshToken);
        return new TokenResponse(accessToken, refreshToken);
    }
}
