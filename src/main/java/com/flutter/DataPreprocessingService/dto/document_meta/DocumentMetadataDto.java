package com.flutter.DataPreprocessingService.dto.document_meta;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentMetadataDto {
    private String channel;
    private String productType;
    private String saleStartDate;
    private String saleEndDate;
    private String productName;
    private String fileName;     // 추가된 파일명 필드
    private LocalDate uploadDate; // 추가된 업로드 날짜 필드
}
