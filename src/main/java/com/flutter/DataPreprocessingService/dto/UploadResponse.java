package com.flutter.DataPreprocessingService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok 어노테이션: 게터, 세터, toString, equals, hashCode 메서드 자동 생성
@AllArgsConstructor // Lombok 어노테이션: 모든 필드를 사용하는 생성자 생성
@NoArgsConstructor  // Lombok 어노테이션: 기본 생성자 생성
public class UploadResponse {
    private String message;
    private String filePath;
}
