package com.flutter.DataPreprocessingService.controller.search;

import com.flutter.DataPreprocessingService.service.search.ElasticsearchProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ElasticsearchProductSearchService searchService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> searchByProductName(
            @RequestParam("query") String query,
            @RequestParam("productName") String productName) {

        List<Map<String, Object>> results = searchService.searchDocumentsByProductName(query, productName);

        return ResponseEntity.ok(results);
    }
}
