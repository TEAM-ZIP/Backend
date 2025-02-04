package com.capstone.bszip.Member.domain;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "tempUser", timeToLive = 600) // 10분 자동 삭제
public class TempMember implements Serializable {
    private static final long serialVersionUID = 1L; // 버전 관리

    private String email;
    private String password; // 해싱된 비밀번호를 저장
}
