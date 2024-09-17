package com.flutter.DataPreprocessingService.controller;

import com.flutter.DataPreprocessingService.dto.document_meta.DocumentMetadataDto;
import com.flutter.DataPreprocessingService.entity.DocumentMetadata;
import com.flutter.DataPreprocessingService.service.document_processing.DocumentMetadataService;
import com.flutter.DataPreprocessingService.service.document_processing.DocumentProcessingService;
import com.flutter.DataPreprocessingService.service.document_upload.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentMetadataService documentMetadataService;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentService documentService;

    /**
     * 문서 업로드 및 메타데이터 저장
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentMetadata> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("metadata") String metadataJson) {
        try {
            // JSON 문자열을 DocumentMetadataDto 객체로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            DocumentMetadataDto metadataDto = objectMapper.readValue(metadataJson, DocumentMetadataDto.class);

            // 파일 업로드 및 메타데이터 저장
            Map<String, Object> response = documentService.uploadPdfAndSaveMetadata(file, metadataDto);

            // 저장된 메타데이터를 응답으로 반환
            DocumentMetadata savedMetadata = (DocumentMetadata) response.get("metadata");
            return new ResponseEntity<>(savedMetadata, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 청킹이 완료되지 않은 문서 목록 조회
     */
    @GetMapping("/incomplete-chunking")
    public ResponseEntity<List<DocumentMetadata>> getDocumentsWithIncompleteChunking() {
        List<DocumentMetadata> documents = documentMetadataService.getDocumentsWithIncompleteChunking();
        return new ResponseEntity<>(documents, HttpStatus.OK);
    }

    /**
     * 문서 청킹 및 인덱싱
     */
    @PostMapping("/process-chunking")
    public ResponseEntity<String> processChunkingForDocuments() {
        documentProcessingService.processChunkingAndIndexing();
        return new ResponseEntity<>("청킹 및 인덱싱 작업이 완료되었습니다.", HttpStatus.OK);
    }
}
