package com.careerpilot.service;

import com.careerpilot.dto.ResumeRequest;
import com.careerpilot.dto.ResumeResponse;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Resume;
import com.careerpilot.repository.ResumeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;

    public ResumeService(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    @Transactional
    public ResumeResponse createResume(ResumeRequest request) {
        Resume resume = new Resume(
                request.name().trim(),
                request.content().trim()
        );

        return toResponse(resumeRepository.save(resume));
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumes() {
        return resumeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ResumeService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponse getResume(Long id) {
        return resumeRepository.findById(id)
                .map(ResumeService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", id));
    }

    private static ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getName(),
                resume.getContent(),
                resume.getCreatedAt()
        );
    }
}
