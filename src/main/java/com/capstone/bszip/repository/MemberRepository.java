package com.capstone.bszip.repository;

import com.capstone.bszip.domain.Member;

import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findByEmail(String email);
}
