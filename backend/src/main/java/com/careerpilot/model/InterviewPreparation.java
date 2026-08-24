package com.careerpilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interview_preparations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewPreparation {

    public static final int TOTAL_SECTIONS = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false, unique = true)
    private JobActivity activity;

    @Column(name = "company_research", columnDefinition = "TEXT")
    private String companyResearch;

    @Column(name = "company_research_done", nullable = false)
    private boolean companyResearchDone;

    @Column(name = "role_priorities", columnDefinition = "TEXT")
    private String rolePriorities;

    @Column(name = "role_priorities_done", nullable = false)
    private boolean rolePrioritiesDone;

    @Column(name = "star_stories", columnDefinition = "TEXT")
    private String starStories;

    @Column(name = "star_stories_done", nullable = false)
    private boolean starStoriesDone;

    @Column(name = "questions_to_ask", columnDefinition = "TEXT")
    private String questionsToAsk;

    @Column(name = "questions_to_ask_done", nullable = false)
    private boolean questionsToAskDone;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InterviewPreparation(JobActivity activity, Instant updatedAt) {
        this.activity = Objects.requireNonNull(activity, "Interview activity is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update time is required");
    }

    public int completedSections() {
        int completedSections = 0;
        if (companyResearchDone) {
            completedSections++;
        }
        if (rolePrioritiesDone) {
            completedSections++;
        }
        if (starStoriesDone) {
            completedSections++;
        }
        if (questionsToAskDone) {
            completedSections++;
        }
        return completedSections;
    }

    public int progressPercent() {
        return completedSections() * 100 / TOTAL_SECTIONS;
    }

    public void update(
            String companyResearch,
            boolean companyResearchDone,
            String rolePriorities,
            boolean rolePrioritiesDone,
            String starStories,
            boolean starStoriesDone,
            String questionsToAsk,
            boolean questionsToAskDone,
            Instant updatedAt
    ) {
        this.companyResearch = companyResearch;
        this.companyResearchDone = companyResearchDone;
        this.rolePriorities = rolePriorities;
        this.rolePrioritiesDone = rolePrioritiesDone;
        this.starStories = starStories;
        this.starStoriesDone = starStoriesDone;
        this.questionsToAsk = questionsToAsk;
        this.questionsToAskDone = questionsToAskDone;
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update time is required");
    }
}
