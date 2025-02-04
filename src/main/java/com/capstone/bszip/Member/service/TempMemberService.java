package com.capstone.bszip.Member.service;

import com.capstone.bszip.Member.domain.TempMember;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TempMemberService {
    @Autowired
    private RedisTemplate<String, TempMember> redisTemplate;
    private static final long EXPIRATION_TIME = 600;

    public void saveTempMember(TempMember tempMember) {
        redisTemplate.opsForValue().set(tempMember.getEmail(), tempMember, EXPIRATION_TIME, TimeUnit.SECONDS);

    }

    public TempMember getTempMember(String email) {
        return redisTemplate.opsForValue().get(email);
    }

    public void deleteTempMember(String email) {
        redisTemplate.delete(email);
    }
}
