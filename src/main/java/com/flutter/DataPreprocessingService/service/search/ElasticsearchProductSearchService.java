package com.flutter.DataPreprocessingService.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElasticsearchProductSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchProductSearchService.class);

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.elasticsearch.index-name}")
    private String indexName;

    public List<Map<String, Object>> searchDocumentsByProductName(String query, String productName) {
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
                                    .must(productNameQuery)
                                    .should(chunkQuery)
                                    .should(fileNameQuery)
                                    .should(channelQuery)
                            )
                    ), ObjectNode.class
            );

            // 검색 결과 반환
            List<Map<String, Object>> searchResults = response.hits().hits().stream()
                    .map(hit -> objectMapper.convertValue(hit.source(), Map.class))  // ObjectNode를 Map<String, Object>으로 변환
                    .map(map -> (Map<String, Object>) map)  // 명시적으로 Map<String, Object> 타입으로 캐스팅
                    .collect(Collectors.toList());

            return searchResults;

        } catch (IOException e) {
            logger.error("Elasticsearch 검색 중 오류 발생: ", e);
            return List.of();
        }
    }
}
