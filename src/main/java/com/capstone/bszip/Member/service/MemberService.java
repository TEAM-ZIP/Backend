package com.capstone.bszip.Member.service;

import com.capstone.bszip.Member.domain.Member;
import com.capstone.bszip.Member.domain.MemberJoinType;
import com.capstone.bszip.Member.domain.TempMember;
import com.capstone.bszip.Member.repository.MemberRepository;
import com.capstone.bszip.Member.security.JwtUtil;
import com.capstone.bszip.Member.service.dto.LoginRequest;
import com.capstone.bszip.Member.service.dto.SignupRequest;
import com.capstone.bszip.Member.service.dto.SignupAddRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private TempMemberService tempMemberService;


    // 이메일 중복 체크 -> 회원 가입 시 이용
    public boolean checkEmailDuplication(String email) {
        return memberRepository.existsByEmail(email);
    }

    // 닉네임 중복 체크 -> 회원 가입 및 닉네임 재설정 시 이용
    public boolean checkNicknameDuplication(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    // 비밀번호 해쉬 처리
    public String makeHashedPassword(String password) {
        return passwordEncoder.encode(password);
    }

    // redis에 이메일, 해쉬된 비밀번호 저장
    public void registerEmailandPassword(@RequestBody SignupRequest signupRequest) {
        TempMember tempMember = new TempMember(signupRequest.getEmail(), makeHashedPassword(signupRequest.getPassword()));
        tempMemberService.saveTempMember(tempMember);
    }

    public boolean existFirstStep(HttpSession session) {
        String email = (String) session.getAttribute("signupEmail");
        return email != null;
    }

    public boolean existTempMember(HttpSession session) {
        TempMember tempMember = tempMemberService.getTempMember(session.getAttribute("signupEmail").toString());
        return tempMember != null;
    }

    public void registerMember(@RequestBody SignupAddRequest signupAddRequest, HttpSession session) {
        TempMember tempMember = tempMemberService.getTempMember(session.getAttribute("signupEmail").toString());
        Member member = new Member();
        member.setEmail(tempMember.getEmail());
        member.setPassword(tempMember.getPassword());
        member.setNickname(signupAddRequest.getNickname());
        member.setMemberJoinType(MemberJoinType.DEFAULT);
        memberRepository.save(member);

        tempMemberService.deleteTempMember(tempMember.getEmail());
        session.removeAttribute("signupEmail");
    }

    public void loginMember(@RequestBody LoginRequest loginRequest, HttpSession session) {
        // Session Fixation 방지 코드 추가 필요
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 이메일 혹은 비밀번호"));
        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 이메일 혹은 비밀번호");
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new User(member.getEmail(), "", Collections.emptyList()),
                null,
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

    }
}

