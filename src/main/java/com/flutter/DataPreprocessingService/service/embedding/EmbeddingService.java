package com.flutter.DataPreprocessingService.service.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Upstage의 임베딩 API와 통신하는 서비스 클래스.
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${upstage.api.url}")
    private String upstageApiUrl;

    @Value("${upstage.api.key}")
    private String upstageApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 쿼리에 대한 임베딩을 생성합니다.
     *
     * @param query 임베딩할 쿼리
     * @return 쿼리 임베딩 벡터
     */
    public List<Double> getQueryEmbedding(String query) {
        return getEmbedding(query, "solar-embedding-1-large-query");
    }

    /**
     * 문서에 대한 임베딩을 생성합니다.
     *
     * @param passage 임베딩할 문서 내용
     * @return 문서 임베딩 벡터
     */
    public List<Double> getPassageEmbedding(String passage) {
        return getEmbedding(passage, "solar-embedding-1-large-passage");
    }

    /**
     * Upstage의 임베딩 API를 호출하여 임베딩을 생성합니다.
     *
     * @param input 임베딩할 입력 텍스트
     * @param model 사용하려는 임베딩 모델
     * @return 임베딩 벡터
     */
    private List<Double> getEmbedding(String input, String model) {
        String endpoint = upstageApiUrl + "/v1/solar/embeddings";

        try {
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + upstageApiKey);  // Authorization 헤더 추가
            headers.set("Content-Type", "application/json");

            // 요청 본문 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", input);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // API 호출
            ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);

            // 응답에서 임베딩 추출
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (!data.isEmpty() && data.get(0).containsKey("embedding")) {
                    // 올바른 타입으로 변환
                    return ((List<?>) data.get(0).get("embedding")).stream()
                            .map(value -> ((Number) value).doubleValue())  // Number로 변환 후 doubleValue 호출
                            .collect(Collectors.toList());
                }
            }
            throw new IllegalStateException("임베딩 생성 실패: 응답이 올바르지 않습니다.");
        } catch (Exception e) {
            logger.error("임베딩 생성 중 오류 발생: ", e);
            throw new IllegalStateException("임베딩 생성 중 오류 발생", e);
        }
    }
}
