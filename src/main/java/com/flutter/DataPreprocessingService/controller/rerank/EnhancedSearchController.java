package com.flutter.DataPreprocessingService.controller.rerank;

import com.flutter.DataPreprocessingService.service.search.EnhancedSearchService;
import com.flutter.DataPreprocessingService.service.embedding.EmbeddingService;
import com.flutter.DataPreprocessingService.service.prompt.CreatePrompt;
import com.flutter.DataPreprocessingService.service.similarity.SimilarityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * query와 productName을 입력받아 두 가지 기능을 제공하는 컨트롤러:
 * 1. 1차 검색 후 LLM API 호출
 * 2. 1차 검색 후 리랭킹된 문서 목록과 LLM API 응답 반환
 */
@RestController
@RequestMapping("/api/enhanced-search")
@RequiredArgsConstructor
public class EnhancedSearchController {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedSearchController.class);

    private final EnhancedSearchService searchService;
    private final CreatePrompt createPrompt;
    private final SimilarityService similarityService;
    private final EmbeddingService embeddingService;

    /**
     * 기능 1: 1차 검색 수행 후 LLM API 호출하여 응답 반환
     *
     * @param query 검색어
     * @param productName 상품명
     * @return 검색된 문서 및 LLM API 응답
     */
    @GetMapping("/simple-search")
    public ResponseEntity<Map<String, Object>> simpleSearch(
            @RequestParam("query") String query,
            @RequestParam("productName") String productName) {

        try {
            // 1차 검색 수행
            List<Map<String, Object>> documents = searchService.searchDocumentsByQueryAndProductName(query, productName, 15);

            // LLM API 호출
            Map<String, Object> llmResponseMap = createPrompt.generateResponse(query, documents);
            String llmResponse = llmResponseMap.toString(); // LLM API 응답 변환

            // 응답 생성
            Map<String, Object> result = new HashMap<>();
            result.put("query", query);
            result.put("productName", productName);
            result.put("llmResponse", llmResponse);
            result.put("documents", documents);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("검색 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * 기능 2: 1차 검색 후 리랭킹된 문서 목록과 LLM API 응답 반환
     *
     * @param query 검색어
     * @param productName 상품명
     * @return 리랭킹된 문서 목록과 LLM API 응답
     */
    @GetMapping("/reranked-search")
    public ResponseEntity<Map<String, Object>> rerankedSearch(
            @RequestParam("query") String query,
            @RequestParam("productName") String productName) {

        try {
            // 1차 검색 수행
            List<Map<String, Object>> documents = searchService.searchDocumentsByQueryAndProductName(query, productName, 15);

            // 쿼리 임베딩 생성
            List<Double> queryEmbedding = embeddingService.getQueryEmbedding(query);

            // 문서 임베딩 생성 및 리랭킹 수행
            List<Map<String, Object>> documentsWithEmbeddings = documents.stream().map(doc -> {
                String content = (String) doc.get("chunk");
                List<Double> passageEmbedding = embeddingService.getPassageEmbedding(content);
                doc.put("embedding", passageEmbedding);
                return doc;
            }).toList();

            // 리랭킹 수행
            List<Map<String, Object>> reRankedDocuments = similarityService.rankDocumentsBySimilarity(queryEmbedding, documentsWithEmbeddings);

            // 리랭킹된 결과에서 상위 10개만 선택
            List<Map<String, Object>> top5Documents = reRankedDocuments.stream().limit(10).collect(Collectors.toList());


            // 원본 문서 정보 추출 (임베딩된 결과에서 디코딩)
            List<Map<String, Object>> decodedDocuments = decodeDocuments(top5Documents);

            // LLM API 호출
            Map<String, Object> llmResponseMap = createPrompt.generateResponse(query, decodedDocuments);
            String llmResponse = llmResponseMap.toString(); // LLM API 응답 변환

            // 응답 생성
            Map<String, Object> result = new HashMap<>();
            result.put("many", decodedDocuments.size());
            result.put("query", query);
            result.put("productName", productName);
            result.put("llmResponse", llmResponse);
            result.put("documents", decodedDocuments);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("검색 처리 중 오류 발생", e);
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
