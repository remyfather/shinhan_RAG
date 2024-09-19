package com.flutter.DataPreprocessingService.controller.kakao;

import com.flutter.DataPreprocessingService.service.embedding.EmbeddingService;
import com.flutter.DataPreprocessingService.service.prompt.CreatePrompt;
import com.flutter.DataPreprocessingService.service.similarity.SimilarityService;
import com.flutter.DataPreprocessingService.service.search.ElasticsearchDocumentSearchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 리랭킹 API 컨트롤러 - 카카오톡 채널용 스킬 서버 연동.
 */
@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
public class KakaoReRankingController {

    private static final Logger logger = LoggerFactory.getLogger(KakaoReRankingController.class);

    private final ElasticsearchDocumentSearchService searchService;
    private final SimilarityService similarityService;
    private final EmbeddingService embeddingService;
    private final CreatePrompt createPrompt;

    /**
     * 카카오톡 채널 스킬 서버로부터 요청을 받고 처리합니다.
     * 사용자의 utterance에 대한 응답을 반환합니다.
     *
     * @param request 카카오톡 채널에서 들어온 요청
     * @return 카카오톡 응답 형식으로 리랭킹된 검색 결과를 반환
     */
    @PostMapping("/skill")
    public ResponseEntity<Map<String, Object>> handleRequest(@RequestBody Map<String, Object> request) {
        // 요청에서 utterance 추출
        String query = extractUtteranceFromRequest(request);

        if (query == null || query.trim().isEmpty()) {
            return buildKakaoResponse("검색어를 입력해주세요.");
        }

        try {
            // 1차 검색 수행
            List<Map<String, Object>> topKDocuments = searchService.searchDocumentsTopKByKeyword(query, 20);

            // 쿼리 임베딩 생성
            List<Double> queryEmbedding = embeddingService.getQueryEmbedding(query);

            // 문서 임베딩 생성 및 리랭킹 수행
            List<Map<String, Object>> documentsWithEmbeddings = topKDocuments.stream().map(doc -> {
                String content = (String) doc.get("chunk");
                if (content == null || content.trim().isEmpty()) {
                    throw new IllegalStateException("문서 내용이 비어 있습니다.");
                }

                List<Double> passageEmbedding = embeddingService.getPassageEmbedding(content);
                doc.put("embedding", passageEmbedding);
                return doc;
            }).collect(Collectors.toList());

            // 리랭킹 수행
            List<Map<String, Object>> reRankedDocuments = similarityService.rankDocumentsBySimilarity(queryEmbedding, documentsWithEmbeddings);

            // 리랭킹된 결과에서 상위 5개 선택
            List<Map<String, Object>> top5Documents = reRankedDocuments.stream().limit(5).collect(Collectors.toList());

            // 문서 내용 추출
            String responseText = top5Documents.stream()
                    .map(doc -> (String) doc.get("chunk"))
                    .findFirst()
                    .orElse("결과가 없습니다.");

            // LLM API 호출 및 응답 생성
            Map<String, Object> llmResponseMap = createPrompt.generateResponse(query, top5Documents);
            String llmResponse = llmResponseMap.getOrDefault("content", "응답 생성 실패").toString();

            // 카카오톡 응답 형식에 맞춰 결과 반환
            return buildKakaoResponse(llmResponse);

        } catch (Exception e) {
            logger.error("검색 처리 중 오류 발생", e);
            return buildKakaoResponse("오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    /**
     * 카카오톡 채널 요청에서 utterance를 추출하는 메서드
     *
     * @param request 사용자가 보낸 요청
     * @return 추출된 utterance
     */
    private String extractUtteranceFromRequest(Map<String, Object> request) {
        System.out.println("시발년아11111~~~~"+request.get("userRequest"));

        try {
            Map<String, Object> userRequest = (Map<String, Object>) request.get("userRequest");
            return (String) userRequest.get("utterance");
        } catch (Exception e) {
            logger.error("utterance 추출 실패", e);
            return null;
        }
    }

    /**
     * 카카오톡 채널 응답 형식에 맞춰 응답을 생성하는 메서드
     *
     * @param text 사용자가 볼 응답 텍스트
     * @return 규격화된 카카오톡 응답
     */
    private ResponseEntity<Map<String, Object>> buildKakaoResponse(String text) {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "2.0");

        Map<String, Object> template = new HashMap<>();
        template.put("outputs", Arrays.asList(Map.of(
                "simpleText", Map.of("text", text)
        )));

        response.put("template", template);
        return ResponseEntity.ok(response);
    }
}
