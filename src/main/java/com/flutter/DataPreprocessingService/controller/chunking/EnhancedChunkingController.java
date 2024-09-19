package com.flutter.DataPreprocessingService.controller.chunking;

import com.flutter.DataPreprocessingService.entity.DocumentMetadata;
import com.flutter.DataPreprocessingService.repository.document_meta.DocumentMetadataRepository;
import com.flutter.DataPreprocessingService.service.chunking.EnhancedChunkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enhanced-chunk")
@RequiredArgsConstructor
public class EnhancedChunkingController {

    private final EnhancedChunkingService chunkingService;
    private final DocumentMetadataRepository documentMetadataRepository;

    @GetMapping("/process")
    public ResponseEntity<String> processChunking(@RequestParam("documentId") Long documentId) {
        // documentId를 사용해 DocumentMetadata 조회
        DocumentMetadata documentMetadata = documentMetadataRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid document ID: " + documentId));

        // 해당 문서에 대해 청킹 및 인덱싱 처리
        chunkingService.processChunkingAndIndexing2(documentMetadata);

        return ResponseEntity.ok("문서 청킹 및 인덱싱이 완료되었습니다.");
    }
}
