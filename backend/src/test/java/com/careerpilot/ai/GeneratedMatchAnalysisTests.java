package com.careerpilot.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GeneratedMatchAnalysisTests {

    @Test
    void rejectsScoreOutsideSupportedRange() {
        assertThatThrownBy(() -> analysis(101, "Summary"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Match score must be between 0 and 100");
    }

    @Test
    void rejectsBlankRequiredText() {
        assertThatThrownBy(() -> analysis(80, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Summary is required");
    }

    private static GeneratedMatchAnalysis analysis(int score, String summary) {
        return new GeneratedMatchAnalysis(
                score,
                summary,
                "Strong Java experience.",
                "Limited Kubernetes evidence.",
                "Add a deployment project.",
                "test-model"
        );
    }
}
