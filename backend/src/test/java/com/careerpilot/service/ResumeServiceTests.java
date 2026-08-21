package com.careerpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerpilot.dto.ResumeRequest;
import com.careerpilot.dto.ResumeResponse;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.model.Resume;
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
class ResumeServiceTests {

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private ResumeService resumeService;

    @Test
    void createsResumeFromNormalizedRequest() {
        ResumeRequest request = new ResumeRequest(
                "  Backend Engineer Resume  ",
                "  Experienced Java engineer.  "
        );
        Instant createdAt = Instant.parse("2026-08-21T12:00:00Z");
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume savedResume = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedResume, "id", 1L);
            ReflectionTestUtils.setField(savedResume, "createdAt", createdAt);
            return savedResume;
        });

        ResumeResponse response = resumeService.createResume(request);

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume persistedResume = resumeCaptor.getValue();
        assertThat(persistedResume.getName()).isEqualTo("Backend Engineer Resume");
        assertThat(persistedResume.getContent()).isEqualTo("Experienced Java engineer.");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Backend Engineer Resume");
        assertThat(response.content()).isEqualTo("Experienced Java engineer.");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void returnsResumesInRepositoryOrder() {
        Resume newestResume = persistedResume(2L, "Resume v2", "2026-08-21T12:00:00Z");
        Resume olderResume = persistedResume(1L, "Resume v1", "2026-08-20T12:00:00Z");
        when(resumeRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(newestResume, olderResume));

        List<ResumeResponse> responses = resumeService.getResumes();

        assertThat(responses).extracting(ResumeResponse::id).containsExactly(2L, 1L);
        assertThat(responses).extracting(ResumeResponse::name)
                .containsExactly("Resume v2", "Resume v1");
    }

    @Test
    void returnsResumeById() {
        Resume resume = persistedResume(1L, "Backend Resume", "2026-08-21T12:00:00Z");
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        ResumeResponse response = resumeService.getResume(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Backend Resume");
        assertThat(response.content()).isEqualTo("Resume content");
    }

    @Test
    void throwsWhenResumeDoesNotExist() {
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.getResume(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resume not found with id: 999");
    }

    private static Resume persistedResume(Long id, String name, String createdAt) {
        Resume resume = new Resume(name, "Resume content");
        ReflectionTestUtils.setField(resume, "id", id);
        ReflectionTestUtils.setField(resume, "createdAt", Instant.parse(createdAt));
        return resume;
    }
}
