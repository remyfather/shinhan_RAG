package com.flutter.DataPreprocessingService.controller.indexing;

import com.flutter.DataPreprocessingService.service.indexing.ElasticsearchIndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Elasticsearch 인덱싱 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class IndexingController {

    private final ElasticsearchIndexingService elasticsearchIndexingService;

    /**
     * Elasticsearch에 청킹 및 인덱싱 작업을 수행한다.
     *
     * @return 인덱싱 결과 메시지
     */
    @PostMapping("/process")
    public ResponseEntity<String> processIndexingForDocuments() {
        elasticsearchIndexingService.processChunkingAndIndexing();
        return new ResponseEntity<>("청킹 및 인덱싱 작업이 완료되었습니다.", HttpStatus.OK);
    }
}
