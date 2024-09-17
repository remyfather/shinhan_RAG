package com.flutter.DataPreprocessingService.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "document_metadata")
@Data
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String channel;           // 판매 채널
    private String productType;       // 보험 상품 종류
    private String saleStartDate;     // 판매 시작 일자
    private String saleEndDate;       // 판매 종료 일자
    private String productName;       // 상품명
    private String filePath;          // 파일 경로
    private String fileName;          // 파일명

    @Enumerated(EnumType.STRING)
    private ChunkingStatus chunkingStatus = ChunkingStatus.PENDING; // 청킹 상태

    private LocalDate uploadDate;     // 파일 업로드 날짜

    public enum ChunkingStatus {
        PENDING,        // 청킹 대기 중
        IN_PROGRESS,    // 청킹 진행 중
        COMPLETED       // 청킹 완료
    }
}
