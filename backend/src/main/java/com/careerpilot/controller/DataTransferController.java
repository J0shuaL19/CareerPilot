package com.careerpilot.controller;

import com.careerpilot.dto.DataTransferCapabilitiesResponse;
import com.careerpilot.dto.DataTransferImportResponse;
import com.careerpilot.dto.DataTransferPreviewResponse;
import com.careerpilot.service.DataTransferFile;
import com.careerpilot.service.DataTransferService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/data-transfer")
public class DataTransferController {

    private final DataTransferService dataTransferService;

    public DataTransferController(DataTransferService dataTransferService) {
        this.dataTransferService = dataTransferService;
    }

    @GetMapping("/capabilities")
    public DataTransferCapabilitiesResponse capabilities() {
        return dataTransferService.capabilities();
    }

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportData() {
        DataTransferFile file = dataTransferService.exportData();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\""
                )
                .body(file.content());
    }

    @PostMapping(path = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataTransferPreviewResponse previewImport(@RequestPart("file") MultipartFile file) {
        return dataTransferService.previewImport(file);
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataTransferImportResponse importData(@RequestPart("file") MultipartFile file) {
        return dataTransferService.importData(file);
    }
}
