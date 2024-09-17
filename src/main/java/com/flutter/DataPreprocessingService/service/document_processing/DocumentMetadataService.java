package com.flutter.DataPreprocessingService.service.document_processing;

import com.flutter.DataPreprocessingService.entity.DocumentMetadata;
import com.flutter.DataPreprocessingService.repository.document_meta.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentMetadataService.class);
    private final DocumentMetadataRepository documentMetadataRepository;

    public List<DocumentMetadata> getDocumentsWithIncompleteChunking() {
        return documentMetadataRepository.findByChunkingStatusNot(DocumentMetadata.ChunkingStatus.COMPLETED);
    }

    public Optional<DocumentMetadata> getDocumentMetadataById(Long id) {
        return documentMetadataRepository.findById(id);
    }

    public DocumentMetadata saveOrUpdateMetadata(DocumentMetadata metadata) {
        return documentMetadataRepository.save(metadata);
    }

    public void deleteMetadata(Long id) {
        documentMetadataRepository.deleteById(id);
        logger.info("문서 메타데이터 삭제 완료: ID = " + id);
    }
}


