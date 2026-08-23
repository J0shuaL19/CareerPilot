package com.careerpilot.service;

import com.careerpilot.ai.GeneratedMatchAnalysis;
import com.careerpilot.ai.MatchAnalysisClient;
import com.careerpilot.ai.MatchAnalysisInput;
import com.careerpilot.dto.MatchAnalysisRequest;
import com.careerpilot.dto.MatchAnalysisResponse;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Job;
import com.careerpilot.model.MatchAnalysis;
import com.careerpilot.model.Resume;
import com.careerpilot.repository.JobRepository;
import com.careerpilot.repository.MatchAnalysisRepository;
import com.careerpilot.repository.ResumeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchAnalysisService {

    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;
    private final MatchAnalysisClient matchAnalysisClient;

    public MatchAnalysisService(
            JobRepository jobRepository,
            ResumeRepository resumeRepository,
            MatchAnalysisRepository matchAnalysisRepository,
            MatchAnalysisClient matchAnalysisClient
    ) {
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.matchAnalysisRepository = matchAnalysisRepository;
        this.matchAnalysisClient = matchAnalysisClient;
    }

    public MatchAnalysisResponse createAnalysis(MatchAnalysisRequest request) {
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", request.jobId()));
        Resume resume = resumeRepository.findById(request.resumeId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume", request.resumeId()));

        GeneratedMatchAnalysis generated = matchAnalysisClient.analyze(new MatchAnalysisInput(
                job.getCompany(),
                job.getTitle(),
                job.getDescription(),
                resume.getName(),
                resume.getContent()
        ));

        MatchAnalysis analysis = new MatchAnalysis(
                job,
                resume,
                generated.matchScore(),
                generated.summary(),
                generated.strengths(),
                generated.gaps(),
                generated.recommendations(),
                generated.modelName()
        );

        return toResponse(matchAnalysisRepository.save(analysis));
    }

    @Transactional(readOnly = true)
    public List<MatchAnalysisResponse> getAnalyses() {
        return matchAnalysisRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(MatchAnalysisService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchAnalysisResponse> getAnalyses(Long jobId, Long resumeId) {
        return matchAnalysisRepository.findAllByJob_IdAndResume_IdOrderByCreatedAtDesc(jobId, resumeId).stream()
                .map(MatchAnalysisService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchAnalysisResponse getAnalysis(Long id) {
        return toResponse(findAnalysis(id));
    }

    @Transactional
    public void deleteAnalysis(Long id) {
        matchAnalysisRepository.delete(findAnalysis(id));
    }

    private MatchAnalysis findAnalysis(Long id) {
        return matchAnalysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match analysis", id));
    }

    private static MatchAnalysisResponse toResponse(MatchAnalysis analysis) {
        Job job = analysis.getJob();
        Resume resume = analysis.getResume();

        return new MatchAnalysisResponse(
                analysis.getId(),
                job.getId(),
                job.getCompany(),
                job.getTitle(),
                resume.getId(),
                resume.getName(),
                analysis.getMatchScore(),
                analysis.getSummary(),
                analysis.getStrengths(),
                analysis.getGaps(),
                analysis.getRecommendations(),
                analysis.getModelName(),
                analysis.getCreatedAt()
        );
    }
}
