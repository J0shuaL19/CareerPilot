package com.careerpilot.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MatchAnalysisTests {

    @Test
    void createsAnalysisForJobAndResume() {
        Job job = new Job("OpenAI", "Software Engineer", "Job description", null);
        Resume resume = new Resume("Backend Resume", "Resume content");

        MatchAnalysis analysis = analysis(job, resume, 84);

        assertThat(analysis.getJob()).isSameAs(job);
        assertThat(analysis.getResume()).isSameAs(resume);
        assertThat(analysis.getMatchScore()).isEqualTo(84);
        assertThat(analysis.getSummary()).isEqualTo("Strong backend alignment.");
        assertThat(analysis.getStrengths()).contains("Java");
        assertThat(analysis.getGaps()).contains("Kubernetes");
        assertThat(analysis.getRecommendations()).contains("deployment project");
        assertThat(analysis.getModelName()).isEqualTo("test-model");
        assertThat(analysis.getId()).isNull();
        assertThat(analysis.getCreatedAt()).isNull();
    }

    @Test
    void rejectsScoreOutsideSupportedRange() {
        Job job = new Job("OpenAI", "Software Engineer", "Job description", null);
        Resume resume = new Resume("Backend Resume", "Resume content");

        assertThatThrownBy(() -> analysis(job, resume, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Match score must be between 0 and 100");
        assertThatThrownBy(() -> analysis(job, resume, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Match score must be between 0 and 100");
    }

    @Test
    void persistenceCallbackSetsCreationTime() {
        MatchAnalysis analysis = analysis(
                new Job("OpenAI", "Software Engineer", "Job description", null),
                new Resume("Backend Resume", "Resume content"),
                84
        );

        analysis.setCreationTime();

        assertThat(analysis.getCreatedAt()).isNotNull();
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
