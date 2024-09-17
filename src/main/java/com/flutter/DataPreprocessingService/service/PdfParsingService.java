package com.flutter.DataPreprocessingService.service;

import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class PdfParsingService {

    @Value("${upstage.api.key}")
    private String upstageApiKey;

    private static final Logger logger = LoggerFactory.getLogger(PdfParsingService.class);

    public Map<String, Object> parsePdf(String filePath) {
        Map<String, Object> parsedData = new HashMap<>();

        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDDocumentInformation info = document.getDocumentInformation();
            String title = info.getTitle();
            String author = info.getAuthor();
            int numberOfPages = document.getNumberOfPages();

            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);

            parsedData.put("numberOfPages", numberOfPages);
            parsedData.put("text", text);

            logger.info("페이지 수={}", numberOfPages);

        } catch (IOException e) {
            logger.error("PDF 파일 파싱 오류: " + filePath, e);
        }

        return parsedData;
    }

    public List<File> chunkPdf(String filePath, int chunkSize) throws IOException {
        List<File> chunkedFiles = new ArrayList<>();
        File file = new File(filePath);

        try (PDDocument document = PDDocument.load(file)) {
            Splitter splitter = new Splitter();
            splitter.setSplitAtPage(chunkSize);

            List<PDDocument> pages = splitter.split(document);
            int chunkIndex = 0;

            for (PDDocument chunk : pages) {
                String chunkFileName = file.getParent() + File.separator + "chunk_" + chunkIndex + "_" + file.getName();
                File chunkFile = new File(chunkFileName);
                chunk.save(chunkFile);
                chunkedFiles.add(chunkFile);
                chunk.close();
                chunkIndex++;
            }
        }

        logger.info("PDF 파일을 {} 페이지씩 청킹 완료: {}개 파일 생성", chunkSize, chunkedFiles.size());
        return chunkedFiles;
    }

    public boolean analyzeDocumentWithUpstage(String filePath) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.upstage.ai/v1/document-ai/document-parse";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + upstageApiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("document", new FileSystemResource(new File(filePath)));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            logger.info("Upstage API 호출 성공: " + response.getBody());
            return true;
        } else {
            logger.error("Upstage API 호출 실패: " + response.getStatusCode());
            return false;
        }
    }
}
