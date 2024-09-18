package com.flutter.DataPreprocessingService.controller.rerank;

import com.flutter.DataPreprocessingService.service.embedding.EmbeddingService;
import com.flutter.DataPreprocessingService.service.prompt.CreatePrompt;  // 새로 추가된 서비스
import com.flutter.DataPreprocessingService.service.prompt.StreamingPromptService;
import com.flutter.DataPreprocessingService.service.search.ElasticsearchDocumentSearchService;
import com.flutter.DataPreprocessingService.service.similarity.SimilarityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 리랭킹 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/rerank")
@RequiredArgsConstructor
public class ReRankingController {

    private static final Logger logger = LoggerFactory.getLogger(ReRankingController.class);

    private final ElasticsearchDocumentSearchService searchService;
    private final SimilarityService similarityService;
    private final EmbeddingService embeddingService;
    private final CreatePrompt createPrompt;  // 추가된 CreatePrompt 서비스
    private final StreamingPromptService streamingPromptService;  // 새로 추가된 서비스

    /**
     * 리랭킹된 문서 검색 결과를 반환하고 LLM API를 호출하여 응답을 받습니다.
     *
     * @param query 검색 쿼리
     * @return 리랭킹된 문서 목록과 LLM API 응답
     */
    @GetMapping("/top-k")
    public ResponseEntity<List<Map<String, Object>>> getReRankedResults(@RequestParam("query") String query) {
        // 입력 유효성 검사
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        try {
            // 1차 검색 수행
            List<Map<String, Object>> topKDocuments = searchService.searchDocumentsTopKByKeyword(query, 10);
            // 1차 검색 결과 원본을 로그로 출력
            logger.info("Initial Search Results: {}", topKDocuments);

            // 쿼리 임베딩 생성
            List<Double> queryEmbedding = embeddingService.getQueryEmbedding(query);

            // 문서 임베딩 생성 및 리랭킹 수행
            List<Map<String, Object>> documentsWithEmbeddings = topKDocuments.stream().map(doc -> {
                String content = (String) ((Map<String, Object>) doc.get("content")).get("html");
                if (content == null || content.trim().isEmpty()) {
                    throw new IllegalStateException("문서 내용이 비어 있습니다.");
                }

                List<Double> passageEmbedding = embeddingService.getPassageEmbedding(content);
                doc.put("embedding", passageEmbedding);
                return doc;
            }).collect(Collectors.toList());

            // 리랭킹 수행
            List<Map<String, Object>> reRankedDocuments = similarityService.rankDocumentsBySimilarity(queryEmbedding, documentsWithEmbeddings);

            // 리랭킹된 결과에서 상위 5개만 선택
            List<Map<String, Object>> top5Documents = reRankedDocuments.stream().limit(5).collect(Collectors.toList());

            // 원본 문서 정보 추출 (임베딩된 결과에서 디코딩)
            List<Map<String, Object>> decodedDocuments = decodeDocuments(top5Documents);

            // 디코딩된 결과를 로그로 출력
            logger.info("Decoded Documents for LLM Input: {}", decodedDocuments);

            // LLM API 호출 준비 및 호출
            String llmResponse = createPrompt.generateResponse(query, decodedDocuments);

            // LLM 응답 반환
            return ResponseEntity.ok(Collections.singletonList(Map.of("response", llmResponse)));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonList(Map.of("error", e.getMessage())));
        }
    }

    /**
     * 임베딩된 문서 결과에서 원본 정보를 디코딩하여 추출합니다.
     *
     * @param documents 리랭킹된 문서 결과 목록
     * @return 디코딩된 원본 문서 정보 목록
     */
// 디코딩된 문서를 반환하는 메서드
    private List<Map<String, Object>> decodeDocuments(List<Map<String, Object>> documents) {
        return documents.stream().map(doc -> {
            // content 내의 html만 추출하여 문자열로 변환
            String contentHtml = (String) ((Map<String, Object>) doc.get("content")).get("html");

            // 다른 필드를 문자열로 변환
            String uploadDate = ((List<Integer>) doc.get("uploadDate")).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("-"));  // "2024-9-18" 형식으로 변환

            Map<String, Object> decodedDoc = new HashMap<>();
            decodedDoc.put("content", contentHtml != null ? contentHtml : "");  // null 확인 후 처리
            decodedDoc.put("uploadDate", uploadDate);
            decodedDoc.put("productName", String.valueOf(doc.get("productName")));  // 다른 필드들도 String으로 변환
            decodedDoc.put("channel", String.valueOf(doc.get("channel")));
            decodedDoc.put("saleStartDate", String.valueOf(doc.get("saleStartDate")));
            decodedDoc.put("saleEndDate", String.valueOf(doc.get("saleEndDate")));
            decodedDoc.put("fileName", String.valueOf(doc.get("fileName")));

            return decodedDoc;
        }).collect(Collectors.toList());
    }


    /**
     * WebClient를 사용하여 스트리밍 방식으로 LLM API 호출 결과를 반환합니다.
     *
     * @param query 검색 쿼리
     * @return Flux 스트림으로 LLM API 응답
     */
    @GetMapping(value = "/stream-top-k", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getStreamedReRankedResults(@RequestParam("query") String query) {
        // 입력 유효성 검사
        if (query == null || query.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("Invalid query provided"));
        }

        try {
            // 1차 검색 수행
            List<Map<String, Object>> topKDocuments = searchService.searchDocumentsTopKByKeyword(query, 10);
            logger.info("Initial Search Results for Streaming: {}", topKDocuments);

            // 쿼리 임베딩 생성
            List<Double> queryEmbedding = embeddingService.getQueryEmbedding(query);

            // 문서 임베딩 생성 및 리랭킹 수행
            List<Map<String, Object>> documentsWithEmbeddings = topKDocuments.stream().map(doc -> {
                String content = (String) ((Map<String, Object>) doc.get("content")).get("html");
                if (content == null || content.trim().isEmpty()) {
                    throw new IllegalStateException("문서 내용이 비어 있습니다.");
                }

                List<Double> passageEmbedding = embeddingService.getPassageEmbedding(content);
                doc.put("embedding", passageEmbedding);
                return doc;
            }).collect(Collectors.toList());

            // 리랭킹 수행
            List<Map<String, Object>> reRankedDocuments = similarityService.rankDocumentsBySimilarity(queryEmbedding, documentsWithEmbeddings);

            // 리랭킹된 결과에서 상위 5개만 선택
            List<Map<String, Object>> top5Documents = reRankedDocuments.stream().limit(5).collect(Collectors.toList());

            // 원본 문서 정보 추출 (임베딩된 결과에서 디코딩)
            List<Map<String, String>> decodedDocuments = decodeDocumentsForStreaming(top5Documents);

            // 디코딩된 결과를 로그로 출력
            logger.info("Decoded Documents for Streaming Input: {}", decodedDocuments);

            // WebClient를 사용한 스트리밍 API 호출
            return streamingPromptService.streamResponse(query, decodedDocuments);

        } catch (Exception e) {
            logger.error("Error processing streamed response: ", e);
            return Flux.error(new IllegalStateException("Error processing streamed response", e));
        }
    }

    // 스트리밍에 사용할 decodeDocumentsForStreaming 메서드
    private List<Map<String, String>> decodeDocumentsForStreaming(List<Map<String, Object>> documents) {
        return documents.stream().map(doc -> {
            String contentHtml = (String) ((Map<String, Object>) doc.get("content")).get("html");
            String uploadDate = ((List<Integer>) doc.get("uploadDate")).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("-"));
            return Map.of(
                    "content", contentHtml != null ? contentHtml : "",
                    "uploadDate", uploadDate,
                    "productName", String.valueOf(doc.get("productName")),
                    "channel", String.valueOf(doc.get("channel")),
                    "saleStartDate", String.valueOf(doc.get("saleStartDate")),
                    "saleEndDate", String.valueOf(doc.get("saleEndDate")),
                    "fileName", String.valueOf(doc.get("fileName"))
            );
        }).toList();
    }

}
