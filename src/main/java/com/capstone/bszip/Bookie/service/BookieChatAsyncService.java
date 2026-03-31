package com.capstone.bszip.Bookie.service;

import com.capstone.bszip.Bookie.domain.BookieChat;
import com.capstone.bszip.Bookie.dto.request.ChatRequest;
import com.capstone.bszip.Bookie.repository.BookieChatRepository;
import com.capstone.bszip.Member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookieChatAsyncService {

    private final BookieChatRepository bookieChatRepository;

    @Async
    @Transactional
    public void saveChatToDB(String chatJson, Member member, ChatRequest chatRequest) {
        BookieChat bookieChat = BookieChat.builder()
                .question(chatRequest.getMessage())
                .answer(chatJson)
                .member(member)
                .build();
        log.info("{}에 대한 응답 저장", bookieChat.getQuestion());
        bookieChatRepository.save(bookieChat);
    }
}
