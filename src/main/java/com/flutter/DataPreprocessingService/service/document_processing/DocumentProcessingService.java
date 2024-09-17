package com.flutter.DataPreprocessingService.service.document_processing;

import com.flutter.DataPreprocessingService.entity.DocumentMetadata;
import com.flutter.DataPreprocessingService.repository.document_meta.DocumentMetadataRepository;
import com.flutter.DataPreprocessingService.service.pdf_parse.PdfParsingService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);
    private final DocumentMetadataRepository documentMetadataRepository;
    private final PdfParsingService pdfParsingService;
    private final ElasticsearchClient elasticsearchClient; // ElasticsearchClient 사용

    @Value("${spring.elasticsearch.index-name}")
    private String indexName;

    public void processChunkingAndIndexing() {
        // 청킹이 완료되지 않은 문서 목록 조회
        List<DocumentMetadata> pendingDocuments = documentMetadataRepository.findByChunkingStatusNot(DocumentMetadata.ChunkingStatus.COMPLETED);

        for (DocumentMetadata document : pendingDocuments) {
            try {
                // 파일 경로에서 문서를 50페이지 단위로 분할
                List<File> chunkedFiles = pdfParsingService.chunkPdf(document.getFilePath(), 50);

                // 각 청크 파일에 대해 DocumentParse API 호출
                for (File chunk : chunkedFiles) {
                    // Upstage API 호출 및 결과 파싱
                    Map<String, Object> parsedResult = pdfParsingService.analyzeDocumentWithUpstage(chunk.getAbsolutePath());

                    if (parsedResult != null && !parsedResult.isEmpty()) {
                        // 성공적으로 분석된 경우, API 응답을 받아서 Elasticsearch에 저장
                        saveToElasticsearch(parsedResult, document);

                        // 청크 파일 삭제
                        if (chunk.delete()) {
                            logger.info("청크 파일 삭제 성공: {}", chunk.getAbsolutePath());
                        } else {
                            logger.error("청크 파일 삭제 실패: {}", chunk.getAbsolutePath());
                        }
                    } else {
                        logger.error("API 호출 실패로 인해 청크 파일을 삭제하지 않음: {}", chunk.getAbsolutePath());
                    }
                }

                // 청킹 상태 업데이트
                document.setChunkingStatus(DocumentMetadata.ChunkingStatus.COMPLETED);
                documentMetadataRepository.save(document);

            } catch (IOException e) {
                logger.error("문서 청킹 및 인덱싱 실패: {}", document.getFilePath(), e);
            }
        }
    }

    // Elasticsearch에 데이터 저장
    private void saveToElasticsearch(Map<String, Object> parsedResult, DocumentMetadata metadata) {
        try {
            // parsedResult의 elements를 저장
            List<Map<String, Object>> elements = (List<Map<String, Object>>) parsedResult.get("elements");
            if (elements != null) {
                for (Map<String, Object> element : elements) {
                    // Elasticsearch에 저장할 데이터를 구성
                    Map<String, Object> data = Map.of(
                            "id", element.get("id"),
                            "category", element.get("category"),
                            "content", element.get("content"),
                            "productName", metadata.getProductName(),
                            "saleStartDate", metadata.getSaleStartDate(),
                            "saleEndDate", metadata.getSaleEndDate(),
                            "channel", metadata.getChannel(),
                            "fileName", metadata.getFileName(),
                            "uploadDate", metadata.getUploadDate()
                    );

                    // Elasticsearch 인덱스 요청
                    IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>()
                            .index(indexName)
                            .id(element.get("id").toString()) // 고유한 ID 설정
                            .document(data)
                            .build();

                    IndexResponse response = elasticsearchClient.index(request);
                    logger.info("Elasticsearch에 데이터 저장 성공. ID: {}", response.id());
                }
                logger.info("Elasticsearch에 모든 데이터 저장 성공");
            }
        } catch (Exception e) {
            logger.error("Elasticsearch에 데이터 저장 실패", e);
        }
    }
}
