package com.flutter.DataPreprocessingService.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flutter.DataPreprocessingService.repository.search.DocumentSearchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch를 이용한 문서 검색 서비스 구현체.
 */
@Service
@RequiredArgsConstructor
public class ElasticsearchDocumentSearchService implements DocumentSearchRepository {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchDocumentSearchService.class);

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.elasticsearch.index-name}")
    private String indexName;

    @Override
    public List<Map<String, Object>> searchDocumentsByKeyword(String query) {
        try {
            // 각 필드에 대한 검색 조건을 설정합니다.
            Query chunkQuery = Query.of(m -> m.match(t -> t.field("chunk").query(query)));
            Query fileNameQuery = Query.of(m -> m.match(t -> t.field("fileName").query(query)));
            Query productNameQuery = Query.of(m -> m.match(t -> t.field("productName").query(query)));
            Query channelQuery = Query.of(m -> m.match(t -> t.field("channel").query(query)));

            // Elasticsearch 검색 쿼리 구성
            SearchResponse<ObjectNode> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(q -> q
                                    .bool(b -> b
                                            .should(List.of(chunkQuery, fileNameQuery, productNameQuery, channelQuery)) // 검색 대상 필드를 업데이트
                                    )
                            ),
                    ObjectNode.class  // ObjectNode 클래스로 반환 타입 지정
            );

            // 검색 결과 반환
            List<Map<String, Object>> searchResults = response.hits().hits().stream()
                    .map(hit -> objectMapper.convertValue(hit.source(), Map.class))  // ObjectNode를 Map<String, Object>으로 변환
                    .map(map -> (Map<String, Object>) map)  // 명시적으로 Map<String, Object> 타입으로 캐스팅
                    .collect(Collectors.toList());

            logger.info("Elasticsearch에서 {}개의 검색 결과를 찾았습니다.", searchResults.size());
            return searchResults;

        } catch (IOException e) {
            logger.error("Elasticsearch 검색 중 오류 발생: ", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> searchDocumentsTopKByKeyword(String query, int topK) {
        try {
            // 각 필드에 대한 검색 조건을 설정합니다.
            Query chunkQuery = Query.of(m -> m.match(t -> t.field("chunk").query(query)));
            Query fileNameQuery = Query.of(m -> m.match(t -> t.field("fileName").query(query)));
            Query productNameQuery = Query.of(m -> m.match(t -> t.field("productName").query(query)));
            Query channelQuery = Query.of(m -> m.match(t -> t.field("channel").query(query)));

            // Elasticsearch 검색 쿼리 구성
            SearchResponse<ObjectNode> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .size(topK)  // 상위 K개 문서만 검색
                            .query(q -> q
                                    .bool(b -> b
                                            .should(List.of(chunkQuery, fileNameQuery, productNameQuery, channelQuery)) // 검색 대상 필드를 업데이트
                                    )
                            ),
                    ObjectNode.class  // ObjectNode 클래스로 반환 타입 지정
            );

            // 검색 결과 반환
            List<Map<String, Object>> searchResults = response.hits().hits().stream()
                    .map(hit -> objectMapper.convertValue(hit.source(), Map.class))  // ObjectNode를 Map<String, Object>으로 변환
                    .map(map -> (Map<String, Object>) map)  // 명시적으로 Map<String, Object> 타입으로 캐스팅
                    .collect(Collectors.toList());

            logger.info("Elasticsearch에서 상위 {}개의 검색 결과를 찾았습니다.", searchResults.size());
            return searchResults;

        } catch (IOException e) {
            logger.error("Elasticsearch 검색 중 오류 발생: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * query와 productName을 기반으로 문서를 검색합니다.
     *
     * @param query       검색어
     * @param productName 상품명
     * @param topK        상위 K개의 문서
     * @return 검색된 문서 목록
     */
    @Override
    public List<Map<String, Object>> searchDocumentsByQueryAndProductName(String query, String productName, int topK) {
        try {
            // 검색 조건 설정
            Query chunkQuery = Query.of(m -> m.match(t -> t.field("chunk").query(query)));
            Query fileNameQuery = Query.of(m -> m.match(t -> t.field("fileName").query(query)));
            Query productNameQuery = Query.of(m -> m.match(t -> t.field("productName").query(productName))); // 상품명 필터 추가
            Query channelQuery = Query.of(m -> m.match(t -> t.field("channel").query(query)));

            // Elasticsearch 검색 쿼리 구성
            SearchResponse<ObjectNode> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(q -> q
                                    .bool(b -> b
                                            .must(productNameQuery)  // productName 필터 적용
                                            .should(List.of(chunkQuery, fileNameQuery, channelQuery)) // 다른 필드에 대한 검색
                                    )
                            )
                            .size(topK),  // 상위 K개의 결과 반환
                    ObjectNode.class  // ObjectNode 클래스로 반환 타입 지정
            );

            // 검색 결과 반환
            List<Map<String, Object>> searchResults = response.hits().hits().stream()
                    .map(hit -> objectMapper.convertValue(hit.source(), Map.class))  // ObjectNode를 Map<String, Object>으로 변환
                    .map(map -> (Map<String, Object>) map)  // 명시적으로 Map<String, Object> 타입으로 캐스팅
                    .collect(Collectors.toList());

            logger.info("Elasticsearch에서 {}개의 검색 결과를 찾았습니다.", searchResults.size());
            return searchResults;

        } catch (IOException e) {
            logger.error("Elasticsearch 검색 중 오류 발생: ", e);
            return List.of(); // 오류 발생 시 빈 리스트 반환
        }
    }
}
