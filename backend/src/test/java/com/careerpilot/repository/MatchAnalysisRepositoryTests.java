package com.careerpilot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerpilot.model.Job;
import com.careerpilot.model.MatchAnalysis;
import com.careerpilot.model.Resume;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class MatchAnalysisRepositoryTests {

    @Autowired
    private MatchAnalysisRepository matchAnalysisRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndFindsAnalysisWithJobAndResume() {
        Job job = jobRepository.save(new Job("OpenAI", "Software Engineer", "Job description", null));
        Resume resume = resumeRepository.save(new Resume("Backend Resume", "Resume content"));

        MatchAnalysis savedAnalysis = matchAnalysisRepository.saveAndFlush(analysis(job, resume, 84));
        Long analysisId = savedAnalysis.getId();
        entityManager.clear();

        MatchAnalysis foundAnalysis = matchAnalysisRepository.findById(analysisId).orElseThrow();

        assertThat(foundAnalysis.getId()).isEqualTo(analysisId);
        assertThat(foundAnalysis.getCreatedAt()).isNotNull();
        assertThat(foundAnalysis.getJob().getCompany()).isEqualTo("OpenAI");
        assertThat(foundAnalysis.getResume().getName()).isEqualTo("Backend Resume");
    }

    @Test
    void findsNewestAnalysesFirst() {
        Job job = jobRepository.save(new Job("OpenAI", "Software Engineer", "Job description", null));
        Resume resume = resumeRepository.save(new Resume("Backend Resume", "Resume content"));
        MatchAnalysis olderAnalysis = analysis(job, resume, 72);
        MatchAnalysis newerAnalysis = analysis(job, resume, 86);
        ReflectionTestUtils.setField(olderAnalysis, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(newerAnalysis, "createdAt", Instant.parse("2026-01-02T00:00:00Z"));
        matchAnalysisRepository.saveAllAndFlush(List.of(olderAnalysis, newerAnalysis));

        List<MatchAnalysis> analyses = matchAnalysisRepository.findAllByOrderByCreatedAtDesc();

        assertThat(analyses).extracting(MatchAnalysis::getMatchScore)
                .containsExactly(86, 72);
    }

    @Test
    void filtersHistoryByJobAndResume() {
        Job targetJob = jobRepository.save(
                new Job("OpenAI", "Software Engineer", "Job description", null)
        );
        Job otherJob = jobRepository.save(
                new Job("Example", "Platform Engineer", "Other description", null)
        );
        Resume resume = resumeRepository.save(new Resume("Backend Resume", "Resume content"));
        matchAnalysisRepository.saveAllAndFlush(List.of(
                analysis(targetJob, resume, 81),
                analysis(targetJob, resume, 88),
                analysis(otherJob, resume, 64)
        ));

        List<MatchAnalysis> analyses =
                matchAnalysisRepository.findAllByJob_IdAndResume_IdOrderByCreatedAtDesc(
                        targetJob.getId(),
                        resume.getId()
                );

        assertThat(analyses).hasSize(2);
        assertThat(analyses).allSatisfy(analysis -> {
            assertThat(analysis.getJob().getId()).isEqualTo(targetJob.getId());
            assertThat(analysis.getResume().getId()).isEqualTo(resume.getId());
        });
    }

    private static MatchAnalysis analysis(Job job, Resume resume, int score) {
        return new MatchAnalysis(
                job,
                resume,
                score,
                "Strong backend alignment.",
                "Java and PostgreSQL experience.",
                "Limited Kubernetes evidence.",
                "Add a production deployment project.",
                "test-model"
        );
    }
}
