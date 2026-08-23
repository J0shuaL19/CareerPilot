package com.careerpilot.controller;

import com.careerpilot.dto.ResumeExtractionResponse;
import com.careerpilot.dto.ResumeRequest;
import com.careerpilot.dto.ResumeResponse;
import com.careerpilot.service.ResumeFileExtractionService;
import com.careerpilot.service.ResumeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeFileExtractionService resumeFileExtractionService;

    public ResumeController(
            ResumeService resumeService,
            ResumeFileExtractionService resumeFileExtractionService
    ) {
        this.resumeService = resumeService;
        this.resumeFileExtractionService = resumeFileExtractionService;
    }

    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(@Valid @RequestBody ResumeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.createResume(request));
    }

    @PostMapping(path = "/extract", consumes = "multipart/form-data")
    public ResumeExtractionResponse extractResume(@RequestPart("file") MultipartFile file) {
        return resumeFileExtractionService.extract(file);
    }

    @GetMapping
    public List<ResumeResponse> getResumes() {
        return resumeService.getResumes();
    }

    @GetMapping("/{id}")
    public ResumeResponse getResume(@PathVariable Long id) {
        return resumeService.getResume(id);
    }

    @PutMapping("/{id}")
    public ResumeResponse updateResume(
            @PathVariable Long id,
            @Valid @RequestBody ResumeRequest request
    ) {
        return resumeService.updateResume(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id) {
        resumeService.deleteResume(id);
        return ResponseEntity.noContent().build();
    }
}
