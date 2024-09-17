package com.flutter.DataPreprocessingService.service.document_processing;

import com.flutter.DataPreprocessingService.entity.DocumentMetadata;
import com.flutter.DataPreprocessingService.repository.document_meta.DocumentMetadataRepository;
import com.flutter.DataPreprocessingService.service.PdfParsingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);
    private final DocumentMetadataRepository documentMetadataRepository;
    private final PdfParsingService pdfParsingService;

    public void processChunkingAndIndexing() {
        // 청킹이 완료되지 않은 문서 목록 조회
        List<DocumentMetadata> pendingDocuments = documentMetadataRepository.findByChunkingStatusNot(DocumentMetadata.ChunkingStatus.COMPLETED);

        for (DocumentMetadata document : pendingDocuments) {
            try {
                // 파일 경로에서 문서를 50페이지 단위로 분할
                List<File> chunkedFiles = pdfParsingService.chunkPdf(document.getFilePath(), 50);

                // 각 청크 파일에 대해 DocumentParse API 호출
                for (File chunk : chunkedFiles) {
                    boolean success = pdfParsingService.analyzeDocumentWithUpstage(chunk.getAbsolutePath());

                    // API 호출 성공 시 청크 파일 삭제
                    if (success) {
                        if (chunk.delete()) {
                            logger.info("청크 파일 삭제 성공: " + chunk.getAbsolutePath());
                        } else {
                            logger.error("청크 파일 삭제 실패: " + chunk.getAbsolutePath());
                        }
                    } else {
                        logger.error("API 호출 실패로 인해 청크 파일을 삭제하지 않음: " + chunk.getAbsolutePath());
                    }
                }

                // 청킹 상태 업데이트
                document.setChunkingStatus(DocumentMetadata.ChunkingStatus.COMPLETED);
                documentMetadataRepository.save(document);

            } catch (IOException e) {
                logger.error("문서 청킹 및 인덱싱 실패: " + document.getFilePath(), e);
            }
        }
    }
}
