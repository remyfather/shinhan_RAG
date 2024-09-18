package com.flutter.DataPreprocessingService.service.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM API와 통신하는 서비스 클래스.
 */
@Service
public class CreatePrompt {

    private static final Logger logger = LoggerFactory.getLogger(CreatePrompt.class);

    @Value("${upstage.api.url}")
    private String upstageApiUrl;

    @Value("${upstage.api.key}")
    private String upstageApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * LLM API를 호출하여 대화 응답을 생성합니다.
     *
     * @param query 사용자 쿼리
     * @param top5Documents 상위 5개의 리랭킹된 문서
     * @return LLM API 응답
     */
    public String generateResponse(String query, List<Map<String, Object>> top5Documents) {
        String endpoint = upstageApiUrl + "/v1/solar/chat/completions";
        try {
            // HTTP 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + upstageApiKey);
            headers.set("Content-Type", "application/json");

            // 문서 컨텍스트 생성
            StringBuilder contextBuilder = new StringBuilder();
            for (Map<String, Object> doc : top5Documents) {
                contextBuilder.append(doc.get("content")).append(" ");
            }
            String context = contextBuilder.toString().trim();

            // 요청 메시지 생성
            Map<String, Object> requestBody = Map.of(
                    "model", "solar-1-mini-chat",  // 모델 설정
                    "messages", List.of(
                            Map.of("role", "system", "content", "당신은 사용자의 질문에 대해 제공된 청크 데이터에 기반하여 답변하는 LLM입니다. 주어진 청크 데이터 외의 내용을 답변에 포함하지 마세요. 만약 질문에 대한 답변을 제공할 수 없다면, '제공된 데이터에서는 답변을 찾을 수 없습니다'라고 말하세요."),
                            Map.of("role", "user", "content", "Context: " + context + ". Question: " + query)
                    ),
                    "stream", false
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);

            // LLM 응답 반환
            if (response.getBody() != null) {
                return response.getBody().toString();
            } else {
                throw new IllegalStateException("LLM 응답이 비어 있습니다.");
            }

        } catch (Exception e) {
            logger.error("LLM 응답 생성 중 오류 발생: ", e);
            throw new IllegalStateException("LLM 응답 생성 중 오류 발생", e);
        }
    }
}
