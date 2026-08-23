package com.careerpilot.controller;

import com.careerpilot.dto.MatchAnalysisRequest;
import com.careerpilot.dto.MatchAnalysisResponse;
import com.careerpilot.service.MatchAnalysisService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/match-analyses")
public class MatchAnalysisController {

    private final MatchAnalysisService matchAnalysisService;

    public MatchAnalysisController(MatchAnalysisService matchAnalysisService) {
        this.matchAnalysisService = matchAnalysisService;
    }

    @PostMapping
    public ResponseEntity<MatchAnalysisResponse> createAnalysis(
            @Valid @RequestBody MatchAnalysisRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchAnalysisService.createAnalysis(request));
    }

    @GetMapping
    public List<MatchAnalysisResponse> getAnalyses() {
        return matchAnalysisService.getAnalyses();
    }

    @GetMapping("/{id}")
    public MatchAnalysisResponse getAnalysis(@PathVariable Long id) {
        return matchAnalysisService.getAnalysis(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Long id) {
        matchAnalysisService.deleteAnalysis(id);
        return ResponseEntity.noContent().build();
    }
}
