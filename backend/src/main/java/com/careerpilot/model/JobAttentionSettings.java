package com.careerpilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_attention_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobAttentionSettings {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;

    @Id
    private Long id;

    @Column(name = "applied_days", nullable = false)
    private int appliedDays;

    @Column(name = "online_assessment_days", nullable = false)
    private int onlineAssessmentDays;

    @Column(name = "interview_days", nullable = false)
    private int interviewDays;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public JobAttentionSettings(
            Long id,
            int appliedDays,
            int onlineAssessmentDays,
            int interviewDays,
            Instant updatedAt
    ) {
        this.id = id;
        update(appliedDays, onlineAssessmentDays, interviewDays, updatedAt);
    }

    public void update(
            int appliedDays,
            int onlineAssessmentDays,
            int interviewDays,
            Instant updatedAt
    ) {
        this.appliedDays = validateDays(appliedDays);
        this.onlineAssessmentDays = validateDays(onlineAssessmentDays);
        this.interviewDays = validateDays(interviewDays);
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time is required");
    }

    private static int validateDays(int days) {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new IllegalArgumentException("Reminder days must be between 1 and 90");
        }
        return days;
    }
}
