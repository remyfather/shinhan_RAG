package com.flutter.DataPreprocessingService.service.similarity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 코사인 유사도를 기반으로 문서를 재정렬하는 서비스 클래스.
 */
@Service
public class SimilarityService {

    private static final Logger logger = LoggerFactory.getLogger(SimilarityService.class);

    /**
     * 문서와 쿼리의 임베딩을 사용해 코사인 유사도를 계산하고 문서를 정렬합니다.
     *
     * @param queryEmbedding 사용자의 쿼리 임베딩
     * @param documents 문서 목록
     * @return 정렬된 문서 목록
     */
    public List<Map<String, Object>> rankDocumentsBySimilarity(List<Double> queryEmbedding, List<Map<String, Object>> documents) {
        List<Map<String, Object>> rankedDocuments = new ArrayList<>();

        for (Map<String, Object> document : documents) {
            List<Double> documentEmbedding = (List<Double>) document.get("embedding");

            // 유사도 계산 로그
            logger.info("Calculating cosine similarity for document ID: {}", document.get("id"));
            double similarity = calculateCosineSimilarity(queryEmbedding, documentEmbedding);

            document.put("similarity", similarity);
            rankedDocuments.add(document);

            // 유사도 계산 결과 로그
            logger.info("Document ID: {} has a similarity score of: {}", document.get("id"), similarity);
        }

        // 유사도 순으로 정렬
        rankedDocuments.sort((doc1, doc2) -> Double.compare((Double) doc2.get("similarity"), (Double) doc1.get("similarity")));

        // 리랭킹된 결과 로그
        logger.info("Re-ranked documents based on cosine similarity:");
        for (Map<String, Object> rankedDocument : rankedDocuments) {
            logger.info("Document ID: {}, Similarity Score: {}", rankedDocument.get("id"), rankedDocument.get("similarity"));
        }

        return rankedDocuments;
    }

    /**
     * 코사인 유사도를 계산합니다.
     *
     * @param vectorA 벡터 A
     * @param vectorB 벡터 B
     * @return 코사인 유사도 값
     */
    private double calculateCosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("벡터의 크기가 일치하지 않습니다.");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
