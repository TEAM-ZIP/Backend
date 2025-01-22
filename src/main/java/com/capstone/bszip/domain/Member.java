package com.capstone.bszip.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String nickname;
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberJoinType memberJoinType;

    @CreatedDate
    private Date createdAt;
    @LastModifiedDate
    private LocalDate updatedAt;

}