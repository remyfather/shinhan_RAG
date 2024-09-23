package com.flutter.DataPreprocessingService.service.chunking;

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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnhancedChunkingService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedChunkingService.class);
    private final DocumentMetadataRepository documentMetadataRepository;
    private final PdfParsingService pdfParsingService;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.elasticsearch.index-name}")
    private String indexName;

    private static final int CHUNK_SIZE = 700;  // 청킹 사이즈
    private static final int OVERLAP_SIZE = 100; // 오버랩 사이즈

    public void processChunkingAndIndexing(DocumentMetadata documentMetadata) {
        logger.info("문서 청킹 시작: {}", documentMetadata.getFilePath());
        List<DocumentMetadata> pendingDocuments = documentMetadataRepository.findByChunkingStatusNot(DocumentMetadata.ChunkingStatus.COMPLETED);

        for (DocumentMetadata document : pendingDocuments) {
            try {
                logger.info("문서 청킹 시작: {}", document.getFilePath());
                List<File> chunkedFiles = pdfParsingService.chunkPdf(document.getFilePath(), 50);

                for (File chunk : chunkedFiles) {
                    Map<String, Object> parsedResult = pdfParsingService.analyzeDocumentWithUpstage(chunk.getAbsolutePath());

                    if (parsedResult != null && !parsedResult.isEmpty()) {
                        logger.info("문서 파싱 완료, Elasticsearch에 저장 시작");
                        List<Map<String, Object>> chunks = createChunksWithOverlap((List<Map<String, Object>>) parsedResult.get("elements"));
                        saveChunksToElasticsearch(chunks, document);

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

    public void processChunkingAndIndexing2(DocumentMetadata documentMetadata) {
        // 전달된 단일 문서에 대해 청킹 수행
        try {
            logger.info("문서 청킹 시작: {}", documentMetadata.getFilePath());
            List<File> chunkedFiles = pdfParsingService.chunkPdf(documentMetadata.getFilePath(), 15);

            for (File chunk : chunkedFiles) {
                Map<String, Object> parsedResult = pdfParsingService.analyzeDocumentWithUpstage(chunk.getAbsolutePath());

                if (parsedResult != null && !parsedResult.isEmpty()) {
                    logger.info("문서 파싱 완료, Elasticsearch에 저장 시작");
                    List<Map<String, Object>> chunks = createChunksWithOverlap((List<Map<String, Object>>) parsedResult.get("elements"));
                    saveChunksToElasticsearch(chunks, documentMetadata);

                    if (chunk.delete()) {
                        logger.info("청크 파일 삭제 성공: {}", chunk.getAbsolutePath());
                    } else {
                        logger.error("청크 파일 삭제 실패: {}", chunk.getAbsolutePath());
                    }
                } else {
                    logger.error("API 호출 실패로 인해 청크 파일을 삭제하지 않음: {}", chunk.getAbsolutePath());
                }
            }

            // 청킹 상태를 완료로 업데이트
            documentMetadata.setChunkingStatus(DocumentMetadata.ChunkingStatus.COMPLETED);
            documentMetadataRepository.save(documentMetadata);
            logger.info("문서 청킹 및 인덱싱 완료: {}", documentMetadata.getFilePath());

        } catch (IOException e) {
            logger.error("문서 청킹 및 인덱싱 실패: {}", documentMetadata.getFilePath(), e);
        }
    }



    public List<Map<String, Object>> createChunksWithOverlap(List<Map<String, Object>> elements) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        List<Map<String, Object>> currentChunkElements = new ArrayList<>();

        for (int i = 0; i < elements.size(); i++) {
            Map<String, Object> element = elements.get(i);
            String content = (String) ((Map<String, Object>) element.get("content")).get("html");

            if (currentChunk.length() + content.length() <= CHUNK_SIZE) {
                currentChunk.append(content);
                currentChunkElements.add(element);
            } else {
                // 현재 청크를 저장하고 새 청크로 전환
                chunks.add(Map.of(
                        "chunk", currentChunk.toString(),
                        "elements", new ArrayList<>(currentChunkElements)
                ));
                currentChunk.setLength(0);  // 청크 초기화
                currentChunkElements.clear();

                // 700이 넘는 element는 그대로 청크에 추가
                if (content.length() > CHUNK_SIZE) {
                    chunks.add(Map.of(
                            "chunk", content,
                            "elements", List.of(element)
                    ));
                } else {
                    currentChunk.append(content);
                    currentChunkElements.add(element);
                }
            }
        }

        // 마지막 청크 저장
        if (currentChunk.length() > 0) {
            chunks.add(Map.of(
                    "chunk", currentChunk.toString(),
                    "elements", new ArrayList<>(currentChunkElements)
            ));
        }

        // 청크 개수와 각 청크별 사이즈 출력
        logChunkInfo(chunks);

        // 오버랩 처리
        return addOverlapToChunks(chunks);
    }

    private void logChunkInfo(List<Map<String, Object>> chunks) {
        logger.info("총 청크 개수: {}", chunks.size());
        int k =0;
        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = (String) chunks.get(i).get("chunk");
            logger.info("청크 {}: 크기 = {}", i + 1, chunkContent.length());
            k=k+chunkContent.length();
        }
    }

    private List<Map<String, Object>> addOverlapToChunks(List<Map<String, Object>> chunks) {
        List<Map<String, Object>> overlappedChunks = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> chunk = chunks.get(i);
            overlappedChunks.add(chunk);

            if (i < chunks.size() - 1) {
                String nextChunk = (String) chunks.get(i + 1).get("chunk");
                String overlap = nextChunk.substring(0, Math.min(OVERLAP_SIZE, nextChunk.length()));
                overlappedChunks.add(Map.of("chunk", overlap));
            }
        }

        return overlappedChunks;
    }

    public void saveChunksToElasticsearch(List<Map<String, Object>> chunks, DocumentMetadata metadata) {
        try {
            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {  // 변경: i 대신 chunkIndex 사용
                Map<String, Object> chunk = chunks.get(chunkIndex);
                String elementId = UUID.randomUUID().toString();  // UUID를 사용하여 고유한 ID 생성
                long uploadDateEpoch = metadata.getUploadDate().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();  // LocalDate를 long 타입 에포크 시간으로 변환

                Map<String, Object> data = Map.of(
                        "chunk", chunk.get("chunk"),
                        "productName", metadata.getProductName() != null ? metadata.getProductName() : "unknown",
                        "saleStartDate", metadata.getSaleStartDate() != null ? metadata.getSaleStartDate().toString() : "unknown",
                        "saleEndDate", metadata.getSaleEndDate() != null ? metadata.getSaleEndDate().toString() : "unknown",
                        "channel", metadata.getChannel() != null ? metadata.getChannel() : "unknown",
                        "fileName", metadata.getFileName() != null ? metadata.getFileName() : "unknown",
                        "uploadDate", uploadDateEpoch  // uploadDate를 long으로 변환하여 저장
                );

                elasticsearchClient.index(indexRequest -> indexRequest  // 변경: i 대신 indexRequest 사용
                        .index(indexName)
                        .id(elementId)  // 고유한 elementId 사용
                        .document(data)
                );

                // 청크 데이터와 elementId 로그 출력
                logger.info("Elasticsearch에 저장된 청크: \nElement ID: {}", elementId);
            }
            logger.info("Elasticsearch에 모든 데이터 저장 성공");
        } catch (Exception e) {
            logger.error("Elasticsearch에 데이터 저장 실패", e);
        }
    }
}
