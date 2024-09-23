package com.flutter.DataPreprocessingService.repository.search;

import java.util.List;
import java.util.Map;

/**
 * 문서 검색을 위한 레포지토리 인터페이스 정의.
 */
public interface DocumentSearchRepository {

    /**
     * 키워드를 기반으로 문서를 검색한다.
     *
     * @param query 검색할 키워드
     * @return 검색된 문서 목록
     */
    List<Map<String, Object>> searchDocumentsByKeyword(String query);


    /**
     * 주어진 키워드를 기반으로 Elasticsearch에서 문서를 검색한다.
     *
     * @param query 검색할 키워드
     * @param topK 상위 K개의 검색 결과
     * @return 검색된 문서 목록
     */
    List<Map<String, Object>> searchDocumentsTopKByKeyword(String query, int topK);


    List<Map<String, Object>> searchDocumentsByQueryAndProductName(String query, String productName, int topK);
}
