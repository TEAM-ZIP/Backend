package com.capstone.bszip.auth;

import com.capstone.bszip.auth.blackList.BlackList;
import com.capstone.bszip.auth.blackList.BlackListRepository;
import com.capstone.bszip.auth.refreshToken.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final BlackListRepository blackListRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void logout (String email,String token, Date expirationDate){
        refreshTokenRepository.deleteByEmail(email);

        BlackList blackList = new BlackList(token, expirationDate);
        blackListRepository.save(blackList);
    }

    public boolean isTokenBlackList(String token){
        return blackListRepository.findById(token).isPresent();
    }
}
