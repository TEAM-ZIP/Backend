package com.capstone.bszip.auth.blackList;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class BlackListService {
    private final BlackListRepository blackListRepository;

    public void addToBlackList(String token, Date expirationDate){
        BlackList blackList = new BlackList(token, expirationDate);
        blackListRepository.save(blackList);
    }

    public boolean isTokenBlackList(String token){
        return blackListRepository.findById(token).isPresent();
    }
}
