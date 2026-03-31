package com.capstone.bszip.Bookie.service;

import com.capstone.bszip.Bookie.domain.BookieChat;
import com.capstone.bszip.Bookie.dto.request.APIChatRequest;
import com.capstone.bszip.Bookie.dto.request.ChatRequest;
import com.capstone.bszip.Bookie.dto.response.ChatResponse;
import com.capstone.bszip.Bookie.dto.response.MemberChatHistoryResponse;
import com.capstone.bszip.Bookie.dto.response.MemberChatResponses;
import com.capstone.bszip.Bookie.dto.response.RecommendedBook;
import com.capstone.bszip.Bookie.dto.response.SpeakerType;
import com.capstone.bszip.Bookie.repository.BookieChatRepository;
import com.capstone.bszip.Member.domain.Member;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookieChatService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final BookieChatRepository bookieChatRepository;
    private final BookieChatAsyncService bookieChatAsyncService;
    private final ObjectMapper objectMapper;

    @Value("${ai.base-uri}")
    String embeddingURI;

    @Transactional(readOnly = true)
    public MemberChatResponses getChatHistory(Member member) throws JsonProcessingException {
        List<BookieChat> bookieChatList = bookieChatRepository.findByMemberAndCreatedDateAfterOrderByCreatedDate(
                member,
                LocalDateTime.now().minusDays(3)
        );
        List<MemberChatHistoryResponse> responses = new ArrayList<>();
        for (BookieChat bookieChat : bookieChatList) {
            responses.add(MemberChatHistoryResponse.fromUserMessage(bookieChat));
            JsonNode node = objectMapper.readTree(bookieChat.getAnswer());
            responses.add(MemberChatHistoryResponse.builder()
                    .text(node.path("message").asText())
                    .type(SpeakerType.system)
                    .books(extractBooks(node.path("books")))
                    .createdAt(bookieChat.getCreatedDate())
                    .build());
        }
        return MemberChatResponses.fromChat(responses);
    }

    @Transactional
    public String getChat(Member member, ChatRequest chatRequest) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<APIChatRequest> httpEntity = new HttpEntity<>(APIChatRequest.fromEntity(member, chatRequest), httpHeaders);
        RestTemplate restTemplate = new RestTemplate();
        String chatJson = restTemplate.postForEntity(embeddingURI + "/chat", httpEntity, String.class).getBody();
        log.info("AI 응답: {}", chatJson);
        bookieChatAsyncService.saveChatToDB(chatJson, member, chatRequest);
        return chatJson;
    }

    public SseEmitter streamChat(Member member, ChatRequest chatRequest) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> streamChatResponse(member, chatRequest, emitter));
        return emitter;
    }

    private void streamChatResponse(Member member, ChatRequest chatRequest, SseEmitter emitter) {
        try {
            String requestBody = objectMapper.writeValueAsString(APIChatRequest.fromEntity(member, chatRequest));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingURI + "/chat/stream"))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<Stream<String>> response = HTTP_CLIENT
                    .send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() >= 400) {
                String errorBody = readErrorBody(response.body());
                log.error("AI SSE 호출 실패. status={}, requestBody={}, responseBody={}",
                        response.statusCode(), requestBody, errorBody);
                emitError(emitter, "AI 서버 호출에 실패했습니다.");
                return;
            }

            relaySseStream(response.body(), member, chatRequest, emitter);
            emitter.complete();
        } catch (Exception e) {
            log.error("SSE 채팅 스트리밍 중 오류", e);
            try {
                emitError(emitter, "채팅 스트리밍 중 오류가 발생했습니다.");
            } catch (IOException ioException) {
                emitter.completeWithError(ioException);
            }
        }
    }

    private String readErrorBody(Stream<String> lines) {
        try (lines) {
            return lines.reduce((left, right) -> left + "\n" + right).orElse("");
        }
    }

    private void relaySseStream(Stream<String> lines, Member member, ChatRequest chatRequest, SseEmitter emitter)
            throws IOException {
        String currentEvent = null;
        StringBuilder dataBuffer = new StringBuilder();

        try (lines) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();
                    continue;
                }

                if (line.startsWith("data:")) {
                    if (dataBuffer.length() > 0) {
                        dataBuffer.append('\n');
                    }
                    dataBuffer.append(line.substring(5).trim());
                    continue;
                }

                if (line.isBlank()) {
                    forwardEvent(currentEvent, dataBuffer.toString(), member, chatRequest, emitter);
                    currentEvent = null;
                    dataBuffer.setLength(0);
                }
            }
        }

        if (dataBuffer.length() > 0) {
            forwardEvent(currentEvent, dataBuffer.toString(), member, chatRequest, emitter);
        }
    }

    private void forwardEvent(
            String eventName,
            String eventData,
            Member member,
            ChatRequest chatRequest,
            SseEmitter emitter
    ) throws IOException {
        if (eventData == null || eventData.isBlank()) {
            return;
        }

        String resolvedEvent = (eventName == null || eventName.isBlank()) ? "message" : eventName;
        emitter.send(SseEmitter.event()
                .name(resolvedEvent)
                .data(eventData, MediaType.APPLICATION_JSON));

        if ("done".equals(resolvedEvent)) {
            bookieChatAsyncService.saveChatToDB(eventData, member, chatRequest);
        }
    }

    private void emitError(SseEmitter emitter, String message) throws IOException {
        String payload = objectMapper.writeValueAsString(ChatResponse.fromAPIResponse(message, List.of()));
        emitter.send(SseEmitter.event()
                .name("error")
                .data(payload, MediaType.APPLICATION_JSON));
        emitter.complete();
    }

    private List<RecommendedBook> extractBooks(JsonNode booksNode) {
        List<RecommendedBook> books = new ArrayList<>();
        if (booksNode != null && booksNode.isArray()) {
            for (JsonNode book : booksNode) {
                books.add(RecommendedBook.fromJsonProperty(
                        book.path("title").asText(),
                        book.path("bookId").asText(),
                        book.path("bookImageUrl").asText()
                ));
            }
        }
        return books;
    }
}
