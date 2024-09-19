package com.flutter.DataPreprocessingService.controller.rerank;

import com.flutter.DataPreprocessingService.service.embedding.EmbeddingService;
import com.flutter.DataPreprocessingService.service.prompt.CreatePrompt;
import com.flutter.DataPreprocessingService.service.prompt.StreamingPromptService;
import com.flutter.DataPreprocessingService.service.search.ElasticsearchDocumentSearchService;
import com.flutter.DataPreprocessingService.service.similarity.SimilarityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final CreatePrompt createPrompt;
    private final StreamingPromptService streamingPromptService;

    /**
     * GET 요청을 처리하여 리랭킹된 문서 검색 결과와 LLM API 응답을 반환합니다.
     *
     * @param query 검색 쿼리
     * @return 리랭킹된 문서 목록과 LLM API 응답
     */
    @GetMapping("/top-k")
    public ResponseEntity<Map<String, Object>> getReRankedResults(@RequestParam("query") String query) {
        return processReRanking(query);
    }


    /**
     * 리랭킹 로직을 수행하고 결과를 반환하는 메서드
     *
     * @param query 검색 쿼리
     * @return 리랭킹된 문서 목록과 LLM API 응답
     */
    private ResponseEntity<Map<String, Object>> processReRanking(String query) {
        try {
            // 1차 검색 수행
            List<Map<String, Object>> topKDocuments = searchService.searchDocumentsTopKByKeyword(query, 20);
            for (int i = 0; i < topKDocuments.size(); i++) {
                logger.info("{} 번째 결과: {}", i + 1, topKDocuments.get(i));
            }

            // 쿼리 임베딩 생성
            List<Double> queryEmbedding = embeddingService.getQueryEmbedding(query);

            // 문서 임베딩 생성 및 리랭킹 수행
            List<Map<String, Object>> documentsWithEmbeddings = topKDocuments.stream().map(doc -> {
                String content = (String) doc.get("chunk");  // chunk 필드에서 데이터 추출
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
            List<Map<String, Object>> top5Documents = reRankedDocuments.stream().limit(10).collect(Collectors.toList());

            // 원본 문서 정보 추출 (임베딩된 결과에서 디코딩)
            List<Map<String, Object>> decodedDocuments = decodeDocuments(top5Documents);

            // 디코딩된 결과를 로그로 출력
            logger.info("Decoded Documents for LLM Input: {}", decodedDocuments);

            // LLM API 호출 준비 및 호출
            Map<String, Object> llmResponseMap = createPrompt.generateResponse(query, decodedDocuments);
            String llmResponse = llmResponseMap.toString();  // Map을 String으로 변환

            // 응답 결과 생성
            Map<String, Object> result = new HashMap<>();
            result.put("query", query);
            result.put("llmResponse", llmResponse);
            result.put("contents", decodedDocuments);

            // 응답 반환
            return ResponseEntity.ok(result);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * 리랭킹된 문서에서 원본 데이터를 디코딩하여 추출합니다.
     *
     * @param documents 리랭킹된 문서 목록
     * @return 디코딩된 문서 정보 목록
     */
    private List<Map<String, Object>> decodeDocuments(List<Map<String, Object>> documents) {
        return documents.stream().map(doc -> {
            // 각 문서의 원본 필드들을 추출
            Map<String, Object> decodedDoc = new HashMap<>();
            decodedDoc.put("productName", doc.get("productName"));
            decodedDoc.put("saleStartDate", doc.get("saleStartDate"));
            decodedDoc.put("saleEndDate", doc.get("saleEndDate"));
            decodedDoc.put("uploadDate", doc.get("uploadDate"));
            decodedDoc.put("chunk", doc.get("chunk"));
            decodedDoc.put("channel", doc.get("channel"));
            decodedDoc.put("fileName", doc.get("fileName"));
            return decodedDoc;
        }).collect(Collectors.toList());
    }
}
