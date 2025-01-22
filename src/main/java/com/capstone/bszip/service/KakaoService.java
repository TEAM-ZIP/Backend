package com.capstone.bszip.service;


import com.capstone.bszip.domain.MemberJoinType;
import com.capstone.bszip.domain.Member;
import com.capstone.bszip.dto.LoginResponse;
import com.capstone.bszip.repository.MemberRepository;
import com.capstone.bszip.security.AuthTokens;
import com.capstone.bszip.security.AuthTokensGenerator;
import com.capstone.bszip.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class KakaoService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MemberRepository memberRepository;
    private final AuthTokensGenerator authTokensGenerator;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    public LoginResponse kakaoLogin(String code) {
        String redirectUri = this.redirectUri;
        // 프론트에서 준 인가 코드로 엑세스 토큰 요청
        String accessToken = getAccessToken(code, redirectUri);

        // 가져온 토큰으로 카카오 api 호출
        HashMap<String, Object> memberInfo = getKakaoMemberInfo(accessToken);

        // 카카오에서 가져온 정보로 회원가입 및 로그인 처리
        LoginResponse kakaoMemberResponse = kakaoUserLogin(memberInfo);

        return kakaoMemberResponse;
    }

    public String getAccessToken(String code, String redirectUri) {

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

    // 토큰으로 사용자의 정보 얻기
    private HashMap<String, Object> getKakaoMemberInfo(String accessToken) {
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
        String nickname = jsonNode.get("properties").get("nickname").asText();

        memberInfo.put("id", id);
        memberInfo.put("email", email);
        memberInfo.put("nickname", nickname);

        return memberInfo;

    }

    private LoginResponse kakaoUserLogin(HashMap<String, Object> memberInfo) {
        Long uid = Long.valueOf(memberInfo.get("id").toString());
        String kakaoEmail = memberInfo.get("email").toString();
        String nickname = memberInfo.get("nickname").toString();

        Member kakaoMember = memberRepository.findByEmail(kakaoEmail).orElse(null);

        // 회원가입
        if(kakaoMember == null) {
            kakaoMember = new Member();
            kakaoMember.setEmail(kakaoEmail);
            kakaoMember.setNickname(nickname);
            kakaoMember.setMemberJoinType(MemberJoinType.KAKAO);
            memberRepository.save(kakaoMember);
        }
        AuthTokens token = authTokensGenerator.generate(kakaoMember.getId());
        return new LoginResponse(uid, nickname, kakaoEmail, token);
    }
}
