package com.flutter.DataPreprocessingService.controller.ui;

import com.flutter.DataPreprocessingService.repository.document_meta.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductNameController {

    private final DocumentMetadataRepository documentMetadataRepository;

    @GetMapping("/product-names")
    public List<String> getDistinctProductNames() {
        return documentMetadataRepository.findDistinctProductNames();
    }
}
