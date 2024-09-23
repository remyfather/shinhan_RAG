package com.flutter.DataPreprocessingService.repository.document_meta;

import com.flutter.DataPreprocessingService.entity.DocumentMetadata;
import com.flutter.DataPreprocessingService.entity.DocumentMetadata.ChunkingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {

    /**
     * 청킹 상태에 따른 문서 메타데이터 조회
     *
     * @param chunkingStatus 청킹 상태 (NOT_STARTED, IN_PROGRESS, COMPLETED)
     * @return 해당 청킹 상태의 문서 목록
     */
    List<DocumentMetadata> findByChunkingStatus(ChunkingStatus chunkingStatus);

    /**
     * 청킹 완료 상태가 아닌 문서 메타데이터 조회
     *
     * @return 청킹 완료되지 않은 문서 목록
     */
    List<DocumentMetadata> findByChunkingStatusNot(ChunkingStatus chunkingStatus);


    @Query("SELECT DISTINCT dm.productName FROM DocumentMetadata dm")
    List<String> findDistinctProductNames();
}
