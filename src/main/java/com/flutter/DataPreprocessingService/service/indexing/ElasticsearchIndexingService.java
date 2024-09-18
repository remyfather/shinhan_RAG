package com.flutter.DataPreprocessingService.service.indexing;

import com.flutter.DataPreprocessingService.entity.DocumentMetadata;
import com.flutter.DataPreprocessingService.repository.document_meta.DocumentMetadataRepository;
import com.flutter.DataPreprocessingService.service.pdf_parse.PdfParsingService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
public class ElasticsearchIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexingService.class);
    private final DocumentMetadataRepository documentMetadataRepository;
    private final PdfParsingService pdfParsingService;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.elasticsearch.index-name}")
    private String indexName;

    /**
     * 청킹이 완료되지 않은 문서들을 처리하여 Elasticsearch에 인덱싱한다.
     */
    public void processChunkingAndIndexing() {
        logger.info("청킹이 완료되지 않은 문서 목록 조회 시작");
        List<DocumentMetadata> pendingDocuments = documentMetadataRepository.findByChunkingStatusNot(DocumentMetadata.ChunkingStatus.COMPLETED);

        for (DocumentMetadata document : pendingDocuments) {
            try {
                logger.info("문서 청킹 시작: {}", document.getFilePath());
                List<File> chunkedFiles = pdfParsingService.chunkPdf(document.getFilePath(), 50);

                for (File chunk : chunkedFiles) {
                    Map<String, Object> parsedResult = pdfParsingService.analyzeDocumentWithUpstage(chunk.getAbsolutePath());

                    if (parsedResult != null && !parsedResult.isEmpty()) {
                        logger.info("문서 파싱 완료, Elasticsearch에 저장 시작");
                        saveToElasticsearch(parsedResult, document);

                        if (chunk.delete()) {
                            logger.info("청크 파일 삭제 성공: {}", chunk.getAbsolutePath());
                        } else {
                            logger.error("청크 파일 삭제 실패: {}", chunk.getAbsolutePath());
                        }
                    } else {
                        logger.error("API 호출 실패로 인해 청크 파일을 삭제하지 않음: {}", chunk.getAbsolutePath());
                    }
                }

                document.setChunkingStatus(DocumentMetadata.ChunkingStatus.COMPLETED);
                documentMetadataRepository.save(document);
                logger.info("문서 청킹 및 인덱싱 완료: {}", document.getFilePath());

            } catch (IOException e) {
                logger.error("문서 청킹 및 인덱싱 실패: {}", document.getFilePath(), e);
            }
        }
    }

    /**
     * Elasticsearch에 데이터를 저장한다.
     */
    public void saveToElasticsearch(Map<String, Object> parsedResult, DocumentMetadata metadata) {
        try {
            List<Map<String, Object>> elements = (List<Map<String, Object>>) parsedResult.get("elements");
            if (elements != null) {
                for (Map<String, Object> element : elements) {
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
                    elasticsearchClient.index(i -> i
                            .index(indexName)
                            .id(element.get("id").toString())
                            .document(data)
                    );

                    logger.info("Elasticsearch에 데이터 저장 성공. ID: {}", element.get("id"));
                }
                logger.info("Elasticsearch에 모든 데이터 저장 성공");
            }
        } catch (Exception e) {
            logger.error("Elasticsearch에 데이터 저장 실패", e);
        }
    }
}
