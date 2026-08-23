package com.careerpilot.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerpilot.model.Resume;
import com.careerpilot.repository.ResumeRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResumeRepository resumeRepository;

    @BeforeEach
    void clearResumes() {
        resumeRepository.deleteAll();
    }

    @Test
    void createsPersistsAndReadsResume() throws Exception {
        mockMvc.perform(post("/api/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Backend Resume",
                                  "content": "Experienced Java engineer."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Backend Resume"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        List<Resume> persistedResumes = resumeRepository.findAll();
        assertThat(persistedResumes).hasSize(1);
        Resume persistedResume = persistedResumes.getFirst();
        assertThat(persistedResume.getContent()).isEqualTo("Experienced Java engineer.");

        mockMvc.perform(get("/api/resumes/{id}", persistedResume.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(persistedResume.getId()))
                .andExpect(jsonPath("$.name").value("Backend Resume"))
                .andExpect(jsonPath("$.content").value("Experienced Java engineer."));
    }

    @Test
    void listsResumesNewestFirst() throws Exception {
        Resume olderResume = resume("Resume v1", "2026-08-20T12:00:00Z");
        Resume newestResume = resume("Resume v2", "2026-08-21T12:00:00Z");
        resumeRepository.saveAllAndFlush(List.of(olderResume, newestResume));

        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Resume v2"))
                .andExpect(jsonPath("$[1].name").value("Resume v1"));
    }

    @Test
    void rejectsInvalidRequestWithoutPersistingResume() throws Exception {
        mockMvc.perform(post("/api/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.content").exists());

        assertThat(resumeRepository.count()).isZero();
    }

    @Test
    void updatesAndDeletesResume() throws Exception {
        Resume resume = resumeRepository.saveAndFlush(new Resume("Master Resume", "Original content"));

        mockMvc.perform(put("/api/resumes/{id}", resume.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Backend Resume  ",
                                  "content": "  Updated experience.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Backend Resume"))
                .andExpect(jsonPath("$.content").value("Updated experience."));

        Resume updatedResume = resumeRepository.findById(resume.getId()).orElseThrow();
        assertThat(updatedResume.getName()).isEqualTo("Backend Resume");
        assertThat(updatedResume.getContent()).isEqualTo("Updated experience.");

        mockMvc.perform(delete("/api/resumes/{id}", resume.getId()))
                .andExpect(status().isNoContent());

        assertThat(resumeRepository.existsById(resume.getId())).isFalse();
    }

    private static Resume resume(String name, String createdAt) {
        Resume resume = new Resume(name, "Resume content");
        ReflectionTestUtils.setField(resume, "createdAt", Instant.parse(createdAt));
        return resume;
    }
}
