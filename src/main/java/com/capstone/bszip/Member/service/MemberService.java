package com.capstone.bszip.Member.service;

import com.capstone.bszip.Member.domain.Member;
import com.capstone.bszip.Member.domain.MemberJoinType;
import com.capstone.bszip.Member.repository.MemberRepository;
import com.capstone.bszip.Member.service.dto.TokenResponse;
import com.capstone.bszip.auth.refreshToken.RefreshToken;
import com.capstone.bszip.auth.refreshToken.RefreshTokenRepository;
import com.capstone.bszip.auth.security.JwtUtil;
import com.capstone.bszip.Member.service.dto.LoginRequest;
import com.capstone.bszip.Member.service.dto.SignupRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static com.capstone.bszip.Member.domain.MemberJoinType.DEFAULT;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final Map<String, String> temporaryStorage = new HashMap<>(); // 임시 데이터 저장소

    @Transactional
    public void registerMember(SignupRequest signupRequest){
        //이메일 중복 확인
        if (memberRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        if(signupRequest.getPassword().equals("kakao-password")){
            temporaryStorage.put(signupRequest.getEmail(), signupRequest.getPassword());
        }
        else{
            temporaryStorage.put(signupRequest.getEmail(), passwordEncoder.encode(signupRequest.getPassword()));
        }
         // 이메일-비밀번호 임시 저장
    }
    @Transactional
    public void registerMemberNickname(String email,String nickname){
        // 이메일이 임시 저장소에 없으면 에러 처리
        if (!temporaryStorage.containsKey(email)) {
            throw new IllegalArgumentException("임시 저장소에 해당 이메일 정보가 없습니다. 다시 진행해주세요.");
        }
        // 임시 저장소에서 비밀번호 가져오기
        String password = temporaryStorage.get(email);

        //닉네임 중복 확인
        if (memberRepository.existsByNickname(nickname)){
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }

        Member member = new Member();
        //사용자 정보 설정
        member.setEmail(email);
        member.setPassword(password);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        member.setNickname(nickname);
        member.setTempPassword(0);
        if(password.equals("kakao-password")){
            member.setMemberJoinType(MemberJoinType.KAKAO);
        } else {
            member.setMemberJoinType(DEFAULT);
        }
        //권한 추가
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        member.setRoles(roles);

        memberRepository.save(member);

        // 임시 저장소에서 데이터 삭제
        temporaryStorage.remove(email);
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
    @Transactional
    public TokenResponse loginUser(LoginRequest loginRequest){
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일입니다."));
        if(member.getMemberJoinType() != DEFAULT){
            throw new RuntimeException("카카오로 로그인해주세요");
        }
        if(passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            String email = member.getEmail();
            //토큰 생성
            String accessToken = JwtUtil.createAccessToken(email);
            String refreshToken = JwtUtil.createRefreshToken(email);
            //refresh 토큰 db 저장
            updateRefreshToken(email,refreshToken);
            return new TokenResponse(accessToken, refreshToken);
        }
        else {
            throw new RuntimeException("일치하지 않는 비밀번호입니다.");
        }
    }

    @Transactional
    public void setTempPassword(String email, String password){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(()-> new RuntimeException("등록된 이메일을 찾을 수 없습니다."));
        member.setPassword(passwordEncoder.encode(password));
        memberRepository.save(member);
    }


    public void showAllMembers(){
        memberRepository.findAll().forEach(System.out::println);
    }

    public void setTempPassword(String email){
        try{
            Member member = memberRepository.findByEmail(email).orElseThrow(()-> new EntityNotFoundException("Member Not Found"));
            member.setTempPassword(1);
            memberRepository.save(member);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}

