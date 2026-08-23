package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.careerpilot.ai.AiClientException;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MatchAnalysisServiceTests {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private MatchAnalysisRepository matchAnalysisRepository;

    @Mock
    private MatchAnalysisClient matchAnalysisClient;

    @InjectMocks
    private MatchAnalysisService matchAnalysisService;

    @Test
    void createsAnalysisFromJobResumeAndGeneratedResult() {
        Job job = persistedJob(1L, "OpenAI", "Software Engineer");
        Resume resume = persistedResume(2L, "Backend Resume");
        GeneratedMatchAnalysis generated = generatedAnalysis();
        Instant createdAt = Instant.parse("2026-08-22T12:00:00Z");
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(resumeRepository.findById(2L)).thenReturn(Optional.of(resume));
        when(matchAnalysisClient.analyze(any(MatchAnalysisInput.class))).thenReturn(generated);
        when(matchAnalysisRepository.save(any(MatchAnalysis.class))).thenAnswer(invocation -> {
            MatchAnalysis savedAnalysis = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedAnalysis, "id", 3L);
            ReflectionTestUtils.setField(savedAnalysis, "createdAt", createdAt);
            return savedAnalysis;
        });

        MatchAnalysisResponse response = matchAnalysisService.createAnalysis(new MatchAnalysisRequest(1L, 2L));

        ArgumentCaptor<MatchAnalysisInput> inputCaptor = ArgumentCaptor.forClass(MatchAnalysisInput.class);
        verify(matchAnalysisClient).analyze(inputCaptor.capture());
        assertThat(inputCaptor.getValue()).isEqualTo(new MatchAnalysisInput(
                "OpenAI",
                "Software Engineer",
                "Build reliable products.",
                "Backend Resume",
                "Java and Spring experience."
        ));

        ArgumentCaptor<MatchAnalysis> analysisCaptor = ArgumentCaptor.forClass(MatchAnalysis.class);
        verify(matchAnalysisRepository).save(analysisCaptor.capture());
        MatchAnalysis persisted = analysisCaptor.getValue();
        assertThat(persisted.getJob()).isSameAs(job);
        assertThat(persisted.getResume()).isSameAs(resume);
        assertThat(persisted.getMatchScore()).isEqualTo(84);
        assertThat(persisted.getModelName()).isEqualTo("gpt-5.6");

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.jobId()).isEqualTo(1L);
        assertThat(response.resumeId()).isEqualTo(2L);
        assertThat(response.matchScore()).isEqualTo(84);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void stopsWhenJobDoesNotExist() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchAnalysisService.createAnalysis(new MatchAnalysisRequest(99L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Job not found with id: 99");
        verifyNoInteractions(resumeRepository, matchAnalysisClient, matchAnalysisRepository);
    }

    @Test
    void stopsWhenResumeDoesNotExist() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(persistedJob(1L, "OpenAI", "Engineer")));
        when(resumeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchAnalysisService.createAnalysis(new MatchAnalysisRequest(1L, 99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resume not found with id: 99");
        verifyNoInteractions(matchAnalysisClient, matchAnalysisRepository);
    }

    @Test
    void doesNotPersistWhenAiClientFails() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(persistedJob(1L, "OpenAI", "Engineer")));
        when(resumeRepository.findById(2L)).thenReturn(Optional.of(persistedResume(2L, "Resume")));
        when(matchAnalysisClient.analyze(any(MatchAnalysisInput.class)))
                .thenThrow(new AiClientException("OpenAI request failed"));

        assertThatThrownBy(() -> matchAnalysisService.createAnalysis(new MatchAnalysisRequest(1L, 2L)))
                .isInstanceOf(AiClientException.class)
                .hasMessage("OpenAI request failed");
        verify(matchAnalysisRepository, never()).save(any(MatchAnalysis.class));
    }

    @Test
    void returnsAnalysesInRepositoryOrder() {
        MatchAnalysis newest = persistedAnalysis(2L, "2026-08-22T12:00:00Z");
        MatchAnalysis older = persistedAnalysis(1L, "2026-08-21T12:00:00Z");
        when(matchAnalysisRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(newest, older));

        List<MatchAnalysisResponse> responses = matchAnalysisService.getAnalyses();

        assertThat(responses).extracting(MatchAnalysisResponse::id).containsExactly(2L, 1L);
    }

    @Test
    void returnsAnalysesForJobAndResume() {
        MatchAnalysis analysis = persistedAnalysis(3L, "2026-08-22T12:00:00Z");
        when(matchAnalysisRepository.findAllByJob_IdAndResume_IdOrderByCreatedAtDesc(1L, 2L))
                .thenReturn(List.of(analysis));

        List<MatchAnalysisResponse> responses = matchAnalysisService.getAnalyses(1L, 2L);

        assertThat(responses).singleElement().extracting(MatchAnalysisResponse::id).isEqualTo(3L);
    }

    @Test
    void returnsAnalysisById() {
        when(matchAnalysisRepository.findById(3L))
                .thenReturn(Optional.of(persistedAnalysis(3L, "2026-08-22T12:00:00Z")));

        MatchAnalysisResponse response = matchAnalysisService.getAnalysis(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.company()).isEqualTo("OpenAI");
        assertThat(response.resumeName()).isEqualTo("Backend Resume");
    }

    @Test
    void throwsWhenAnalysisDoesNotExist() {
        when(matchAnalysisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchAnalysisService.getAnalysis(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Match analysis not found with id: 99");
    }

    @Test
    void deletesExistingAnalysis() {
        MatchAnalysis analysis = persistedAnalysis(3L, "2026-08-22T12:00:00Z");
        when(matchAnalysisRepository.findById(3L)).thenReturn(Optional.of(analysis));

        matchAnalysisService.deleteAnalysis(3L);

        verify(matchAnalysisRepository).delete(analysis);
    }

    @Test
    void throwsWhenDeletingMissingAnalysis() {
        when(matchAnalysisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchAnalysisService.deleteAnalysis(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Match analysis not found with id: 99");
    }

    private static Job persistedJob(Long id, String company, String title) {
        Job job = new Job(company, title, "Build reliable products.", null);
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    private static Resume persistedResume(Long id, String name) {
        Resume resume = new Resume(name, "Java and Spring experience.");
        ReflectionTestUtils.setField(resume, "id", id);
        return resume;
    }

    private static GeneratedMatchAnalysis generatedAnalysis() {
        return new GeneratedMatchAnalysis(
                84,
                "Strong overall match.",
                "Relevant backend experience.",
                "Limited cloud evidence.",
                "Add measurable cloud achievements.",
                "gpt-5.6"
        );
    }

    private static MatchAnalysis persistedAnalysis(Long id, String createdAt) {
        MatchAnalysis analysis = new MatchAnalysis(
                persistedJob(1L, "OpenAI", "Software Engineer"),
                persistedResume(2L, "Backend Resume"),
                84,
                "Strong overall match.",
                "Relevant backend experience.",
                "Limited cloud evidence.",
                "Add measurable cloud achievements.",
                "gpt-5.6"
        );
        ReflectionTestUtils.setField(analysis, "id", id);
        ReflectionTestUtils.setField(analysis, "createdAt", Instant.parse(createdAt));
        return analysis;
    }
}
