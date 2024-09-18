package com.flutter.DataPreprocessingService.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeyWordSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper = new ObjectMapper();  // ObjectMapper 추가

    @Value("${spring.elasticsearch.index-name}")
    private String indexName;

    public List<Map<String, Object>> searchDocuments(String query) {
        try {
            // Elasticsearch 검색 쿼리 구성
            SearchResponse<ObjectNode> response = elasticsearchClient.search(s -> s
                            .index(indexName)  // 검색할 인덱스 이름
                            .query(q -> q
                                    .bool(b -> b
                                            .should(List.of(
                                                    Query.of(m -> m.match(t -> t.field("content.html").query(query))),  // content.html 필드에서 검색
                                                    Query.of(m -> m.match(t -> t.field("productName").query(query))),  // productName 필드에서 검색
                                                    Query.of(m -> m.match(t -> t.field("category").query(query))),  // category 필드에서 검색
                                                    Query.of(m -> m.match(t -> t.field("fileName").query(query)))  // fileName 필드에서 검색
                                            ))
                                    )
                            ),
                    ObjectNode.class  // ObjectNode 클래스로 반환 타입 지정
            );

            // 검색 결과 반환
            return response.hits().hits().stream()
                    .map(hit -> objectMapper.convertValue(hit.source(), Map.class))  // ObjectNode를 Map<String, Object>으로 변환
                    .map(map -> (Map<String, Object>) map)  // 명시적으로 Map<String, Object> 타입으로 캐스팅
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
