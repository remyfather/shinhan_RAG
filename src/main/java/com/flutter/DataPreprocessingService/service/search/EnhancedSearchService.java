package com.flutter.DataPreprocessingService.service.search;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnhancedSearchService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedSearchService.class);
    private final ElasticsearchDocumentSearchService elasticsearchService;

    public List<Map<String, Object>> searchDocumentsByQueryAndProductName(String query, String productName, int topK) {
        logger.info("Query: {}, ProductName: {}", query, productName);

        // Elasticsearch에서 query와 productName을 기반으로 검색 수행
        return elasticsearchService.searchDocumentsByQueryAndProductName(query, productName, topK);
    }
}
