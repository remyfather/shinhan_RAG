package com.flutter.DataPreprocessingService.controller.search;

import com.flutter.DataPreprocessingService.service.search.ElasticsearchDocumentSearchService;  // 수정된 서비스로 변경
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 문서 검색을 위한 REST 컨트롤러 클래스.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticsearchDocumentSearchService documentSearchService;  // 수정된 서비스 클래스 사용

    /**
     * 키워드를 기반으로 문서를 검색한다.
     *
     * @param query 검색할 키워드
     * @return 검색된 문서 목록
     */
    @GetMapping("single")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam("query") String query) {
        System.out.println("쿼리----------->" + query);
        List<Map<String, Object>> results = documentSearchService.searchDocumentsByKeyword(query);

        System.out.println("결과!!!" + results);
        return ResponseEntity.ok(results);
    }


    /**
     * 사용자가 제공한 쿼리를 바탕으로 Elasticsearch에서 문서를 검색하고 상위 K개의 결과를 반환합니다.
     *
     * @param query 검색할 키워드
     * @param topK 상위 K개의 결과 수
     * @return 검색된 문서 목록
     */
    @GetMapping("/top-k")
    public ResponseEntity<List<Map<String, Object>>> searchTopK(@RequestParam("query") String query, @RequestParam(value = "topK", defaultValue = "10") int topK) {
        List<Map<String, Object>> results = documentSearchService.searchDocumentsTopKByKeyword(query, topK);
        return ResponseEntity.ok(results);
    }
}
