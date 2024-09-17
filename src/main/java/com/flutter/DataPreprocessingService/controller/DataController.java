package com.flutter.DataPreprocessingService.controller;


import com.flutter.DataPreprocessingService.dto.UploadResponse;
import com.flutter.DataPreprocessingService.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/data")
public class DataController {

    private final DataService dataService;

    /**
     * 문서를 업로드하는 API를 구현해야 합니다. 사용자 인터페이스 또는 API를 통해 문서를 업로드하고 서버에 저장하는 기능을 제공하겠습니다.
     * 문서를 업로드하고 서버에 저장하는 기능 구현
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        // 파일을 서버에 저장하고 저장된 파일 경로를 반환합니다.
        String filePath = dataService.storeFile(file);
        return ResponseEntity.ok(new UploadResponse("File uploaded successfully", filePath));
    }
}

