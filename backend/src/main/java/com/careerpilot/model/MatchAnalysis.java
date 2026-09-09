package com.careerpilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "match_score", nullable = false)
    private int matchScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String strengths;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String gaps;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MatchAnalysis(
            Job job,
            Resume resume,
            int matchScore,
            String summary,
            String strengths,
            String gaps,
            String recommendations,
            String modelName
    ) {
        validateScore(matchScore);
        this.job = job;
        this.resume = resume;
        this.matchScore = matchScore;
        this.summary = summary;
        this.strengths = strengths;
        this.gaps = gaps;
        this.recommendations = recommendations;
        this.modelName = modelName;
    }
    public static MatchAnalysis restore(
            Job job,
            Resume resume,
            int matchScore,
            String summary,
            String strengths,
            String gaps,
            String recommendations,
            String modelName,
            Instant createdAt
    ) {
        MatchAnalysis analysis = new MatchAnalysis(
                job,
                resume,
                matchScore,
                summary,
                strengths,
                gaps,
                recommendations,
                modelName
        );
        analysis.createdAt = java.util.Objects.requireNonNull(
                createdAt,
                "Creation time is required"
        );
        return analysis;
    }

    @PrePersist
    void setCreationTime() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private static void validateScore(int matchScore) {
        if (matchScore < 0 || matchScore > 100) {
            throw new IllegalArgumentException("Match score must be between 0 and 100");
        }
    }
}
