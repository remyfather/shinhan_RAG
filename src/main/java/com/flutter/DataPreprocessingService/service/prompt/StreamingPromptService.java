package com.flutter.DataPreprocessingService.service.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * WebClient를 사용하여 스트리밍 방식으로 LLM API와 통신하는 서비스 클래스.
 */
@Service
public class StreamingPromptService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingPromptService.class);

    @Value("${upstage.api.url}")
    private String upstageApiUrl;

    @Value("${upstage.api.key}")
    private String upstageApiKey;

    private WebClient webClient;

    @PostConstruct
    private void init() {
        this.webClient = WebClient.builder()
                .baseUrl(upstageApiUrl)
                .defaultHeader("Authorization", "Bearer " + upstageApiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * LLM API를 호출하여 스트리밍 방식으로 대화 응답을 생성합니다.
     *
     * @param query     사용자 쿼리
     * @param documents 상위 5개의 리랭킹된 문서
     * @return LLM API 응답 (Flux 스트림)
     */
    public Flux<String> streamResponse(String query, List<Map<String, String>> documents) {
        // 시스템 메시지 설정
        Map<String, String> systemMessage = Map.of(
                "role", "system",
                "content", "당신은 사용자의 질문에 대해 제공된 청크 데이터에 기반하여 답변하는 LLM입니다. 주어진 청크 데이터 외의 내용을 답변에 포함하지 마세요. 만약 질문에 대한 답변을 제공할 수 없다면, '제공된 데이터에서는 답변을 찾을 수 없습니다'라고 말하세요."
        );

        // 사용자 질문 메시지 설정
        Map<String, String> userMessage = Map.of(
                "role", "user",
                "content", query
        );

        // 청크 데이터를 assistant 메시지로 설정
        List<Map<String, String>> assistantMessages = documents.stream()
                .map(doc -> Map.of(
                        "role", "assistant",
                        "content", "청크: " + doc.get("content")
                ))
                .toList();

        // 전체 메시지 리스트 구성
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        messages.addAll(assistantMessages);

        // 요청 본문 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "solar-1-mini-chat");
        requestBody.put("messages", messages);
        requestBody.put("stream", true);

        // WebClient를 사용하여 스트리밍 방식으로 API 호출
        return webClient.post()
                .uri("/v1/solar/chat/completions")
                .body(BodyInserters.fromValue(requestBody))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(response -> logger.info("Streaming LLM Response: {}", response))
                .doOnError(error -> logger.error("스트리밍 중 오류 발생: ", error));
    }
}
