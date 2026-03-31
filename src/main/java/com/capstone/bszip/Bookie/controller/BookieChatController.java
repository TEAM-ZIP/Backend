package com.capstone.bszip.Bookie.controller;

import com.capstone.bszip.Bookie.dto.request.ChatRequest;
import com.capstone.bszip.Bookie.dto.response.ChatResponse;
import com.capstone.bszip.Bookie.dto.response.MemberChatResponses;
import com.capstone.bszip.Bookie.service.BookieChatService;
import com.capstone.bszip.Member.domain.Member;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/bookie")
@RequiredArgsConstructor
@Tag(name = "Bookie", description = "chatbot api")
public class BookieChatController {

    private final BookieChatService bookieChatService;

    @GetMapping("/history")
    @Operation(summary = "이전 히스토리 조회", description = "[로그인 필수] 해당 회원의 이전 기록을 최신순으로 반환",
            responses = {
                    @ApiResponse(responseCode = "200", description = "AI 응답 반환 성공",
                            content = @Content(schema = @Schema(implementation = MemberChatResponses.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            }
    )
    public ResponseEntity<?> getChatHistory(@AuthenticationPrincipal Member member) {
        try {
            return ResponseEntity.ok(bookieChatService.getChatHistory(member));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "chat 반환", description = "[로그인 필수] ai 모델 응답을 한 번에 반환",
            responses = {
                    @ApiResponse(responseCode = "200", description = "AI 응답 반환 성공",
                            content = @Content(schema = @Schema(implementation = ChatResponse.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            }
    )
    public ResponseEntity<?> getChat(@AuthenticationPrincipal Member member, @RequestBody ChatRequest chatRequest)
            throws JsonProcessingException {
        try {
            return ResponseEntity.ok(bookieChatService.getChat(member, chatRequest));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    @Operation(summary = "chat stream 반환", description = "[로그인 필수] ai 모델 SSE 응답을 실시간으로 반환",
            responses = {
                    @ApiResponse(responseCode = "200", description = "AI SSE 응답 반환 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            }
    )
    public SseEmitter streamChat(@AuthenticationPrincipal Member member, @RequestBody ChatRequest chatRequest) {
        return bookieChatService.streamChat(member, chatRequest);
    }
}
