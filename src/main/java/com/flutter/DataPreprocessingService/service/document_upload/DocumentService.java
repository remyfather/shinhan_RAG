package com.flutter.DataPreprocessingService.service.document_upload;

import com.flutter.DataPreprocessingService.dto.document_meta.DocumentMetadataDto;
import com.flutter.DataPreprocessingService.entity.DocumentMetadata;
import com.flutter.DataPreprocessingService.repository.document_meta.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMetadataRepository documentMetadataRepository;

    private static final String UPLOAD_DIR = "/Users/yongho/DeepLearningProject/Flutter/DataPreProcessingService/uploads/";

    public Map<String, Object> uploadPdfAndSaveMetadata(MultipartFile file, DocumentMetadataDto metadataDto) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 새로운 파일명 생성
            String newFileName = generateNewFileName(file.getOriginalFilename(), metadataDto);
            String filePath = UPLOAD_DIR + newFileName;

            // 파일 저장
            File savedFile = new File(filePath);
            file.transferTo(savedFile);

            // 메타데이터 저장
            DocumentMetadata metadata = new DocumentMetadata();
            metadata.setChannel(metadataDto.getChannel());
            metadata.setProductType(metadataDto.getProductType());
            metadata.setSaleStartDate(metadataDto.getSaleStartDate());
            metadata.setSaleEndDate(metadataDto.getSaleEndDate());
            metadata.setProductName(metadataDto.getProductName());
            metadata.setFilePath(filePath); // 파일 경로 추가
            metadata.setFileName(newFileName); // 새로 생성한 파일명 추가
            metadata.setUploadDate(LocalDate.now()); // 현재 날짜를 업로드 날짜로 저장

            documentMetadataRepository.save(metadata); // JPA를 통해 메타데이터 저장

            response.put("message", "파일 업로드 및 메타데이터 저장 완료");
            response.put("metadata", metadata);

        } catch (IOException e) {
            response.put("error", "파일 업로드 중 오류 발생: " + e.getMessage());
        }

        return response;
    }

    private String generateNewFileName(String originalFileName, DocumentMetadataDto metadataDto) {
        // 현재 날짜를 "yyyyMMdd_HHmmss" 형식으로 추가
        String currentDate = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // 원본 파일명을 Unicode NFC 형식으로 정규화
        String normalizedFileName = Normalizer.normalize(originalFileName, Normalizer.Form.NFC);

        // 새로운 파일명을 메타데이터 정보를 기반으로 생성
        String newFileName = String.format("%s_%s_%s_%s_%s_%s_%s",
                metadataDto.getChannel(),
                metadataDto.getProductType(),
                metadataDto.getSaleStartDate(),
                metadataDto.getSaleEndDate(),
                metadataDto.getProductName(),
                currentDate,
                normalizedFileName);

        // 파일명에서 허용된 문자(한글, 영문, 숫자, '.', '_', '-') 외의 문자만 언더스코어(_)로 대체
        return newFileName.replaceAll("[^가-힣a-zA-Z0-9._-]", "_");
    }
}
