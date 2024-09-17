package com.flutter.DataPreprocessingService.service;

import com.flutter.DataPreprocessingService.dto.UploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);
    private final String UPLOAD_DIR = "/Users/yongho/DeepLearningProject/Flutter/DataPreProcessing/uploads/";



    public String storeFile(MultipartFile file) {
        // 업로드된 파일의 원래 이름을 가져옵니다.
        String originalFilename = file.getOriginalFilename();
        // 업로드된 파일의 확장자와 현재 시간을 사용하여 새로운 파일명을 생성합니다.
        String newFilename = System.currentTimeMillis() + "_" + originalFilename;
        String filePath = UPLOAD_DIR + newFilename;

        // 디렉토리 확인 및 생성
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // 디렉토리가 없으면 생성
            logger.info("업로드 디렉토리 생성: " + UPLOAD_DIR);
        }

        try {
            // 파일 경로 설정 및 저장
            Path path = Paths.get(filePath);
            Files.write(path, file.getBytes());

            // 파일 저장 로그 출력
            logger.info("파일 저장 성공: " + path.toAbsolutePath());
            return path.toAbsolutePath().toString();
        } catch (IOException e) {
            logger.error("파일 저장 실패: " + originalFilename, e);
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }
}
