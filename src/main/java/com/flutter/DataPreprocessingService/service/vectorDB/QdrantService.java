package com.flutter.DataPreprocessingService.service.vectorDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qdrant 벡터 DB와의 통신을 위한 서비스 클래스.
 */
@Service
public class QdrantService {

    private static final Logger logger = LoggerFactory.getLogger(QdrantService.class);

    @Value("${qdrant.url}")
    private String qdrantUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Qdrant에 데이터를 저장합니다.
     *
     * @param data 저장할 데이터
     * @param ids 데이터에 대한 고유 ID 목록
     */
    public void storeData(List<Map<String, Object>> data, List<Integer> ids) {
        String endpoint = qdrantUrl + "/collections/your-collection-name/points";

        try {
            // JSON 본문 생성
            Map<String, Object> requestBody = new HashMap<>();

            // Qdrant가 기대하는 형식으로 데이터 구성
            requestBody.put("points", data.stream().map(point -> {
                Map<String, Object> pointData = new HashMap<>(point);
                pointData.put("id", point.get("id").toString()); // 각 point의 id를 추가하여 JSON 형식 맞추기
                return pointData;
            }).toList());

            // Qdrant에 데이터 저장 요청
            restTemplate.postForEntity(endpoint, requestBody, String.class);
            logger.info("Qdrant에 데이터 저장 요청 성공. IDs: {}", ids);
        } catch (Exception e) {
            logger.error("Qdrant에 데이터 저장 요청 실패", e);
        }
    }
}
