package com.capstone.bszip.auth.blackList;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class BlackList {
    @Id
    private String token; //로그아웃한 사용자의 access token

    private Date expirationDate; // token 만료 시간

}
